package io.github.garatech.jobsinton.providers.adzuna;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.garatech.jobsinton.providers.adzuna.AdzunaSearch.*;
import static org.assertj.core.api.Assertions.*;

class AdzunaSearchTest {
    static final String ROW = """
            {"id":"one","title":"Java developer","redirect_url":"https://adzuna.es/details/1","location":{"display_name":"Madrid"}}
            """.strip();
    static AdzunaTransport.Reply reply(int status, String body) {
        return new AdzunaTransport.Reply(status, body.getBytes(StandardCharsets.UTF_8));
    }

    @Test void encodesTermsFetchesOnlyTheRequestedPageAndPreservesFirstUsableOccurrence() {
        var calls = new AtomicInteger();
        var search = new AdzunaSearch("dummy id", "fake&key", uri -> {
            calls.incrementAndGet();
            assertThat(uri.getScheme()).isEqualTo("https");
            assertThat(uri.getHost()).isEqualTo("api.adzuna.com");
            assertThat(uri.getPath()).isEqualTo("/v1/api/jobs/es/search/2");
            assertThat(uri.getRawQuery()).isEqualTo("what=C%2B%2B+%26+Java&where=A+Coru%C3%B1a&results_per_page=10&app_id=dummy+id&app_key=fake%26key");
            return reply(200, "{\"count\":99,\"results\":[{}," + ROW + "," + ROW.replace("developer","duplicate") + "," + ROW.replace("one","two") + "]}");
        });
        var page = search.search("C++ & Java", "A Coruña", 2);
        assertThat(calls.get()).isOne();
        assertThat(page.received()).isEqualTo(4);
        assertThat(page.rejected()).isOne();
        assertThat(page.duplicates()).isOne();
        assertThat(page.listings()).extracting("id").containsExactly("adzuna:one", "adzuna:two");
        assertThat(page.listings().getFirst().title()).isEqualTo("Java developer");
    }

    @Test void invalidInputsAndMissingCredentialsNeverUseTransport() {
        AdzunaTransport never = uri -> { throw new AssertionError("unexpected outbound request"); };
        var s = new AdzunaSearch("id", "key", never);
        for (int page : new int[]{0, LAST_PAGE + 1}) failure(() -> s.search("Java", "Madrid", page), Problem.INVALID_SEARCH);
        for (String what : new String[]{null, " ", "a".repeat(101), "a\nb"}) failure(() -> s.search(what, "Madrid", 1), Problem.INVALID_SEARCH);
        failure(() -> s.search("Java", " ", 1), Problem.INVALID_SEARCH);
        failure(() -> new AdzunaSearch("id", " ", never).search("Java","Madrid",1), Problem.NOT_CONFIGURED);
    }

    @ParameterizedTest @ValueSource(strings = {"", "null", "{}", "{\"results\":{}}", "{\"results\":[]} {}", "{invalid"})
    void invalidPageIsNotAnEmptySuccess(String body) {
        failure(() -> new AdzunaSearch("id","key", u -> reply(200, body)).search("Java","Madrid",1), Problem.INVALID_PROVIDER_RESPONSE);
    }

    @Test void emptyPageIsValidButOversizedPageIsNot() {
        var empty = new AdzunaSearch("id","key",u -> reply(200,"{\"results\":[]}")).search("Java","Madrid",1);
        assertThat(empty.received()).isZero();
        assertThat(empty.listings()).isEmpty();
        String oversized = "{\"results\":[" + "{},".repeat(10) + "{}]}";
        failure(() -> new AdzunaSearch("id","key",u -> reply(200,oversized)).search("Java","Madrid",1), Problem.INVALID_PROVIDER_RESPONSE);
    }

    static void failure(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, Problem problem) {
        assertThatThrownBy(action).isInstanceOf(Failure.class).hasMessage(problem.name()).hasNoCause();
    }
}
