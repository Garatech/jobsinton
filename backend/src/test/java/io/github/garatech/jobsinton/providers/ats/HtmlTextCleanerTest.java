package io.github.garatech.jobsinton.providers.ats;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlTextCleanerTest {

    @Test
    void decodesDoubleEncodedGreenhouseHtmlAndPreservesTechTokens() {
        String raw = "&lt;div&gt;&lt;p&gt;Build APIs with Java, Spring Boot &amp;amp; S/4HANA (years &lt; 3 welcome).&lt;/p&gt;"
                + "&lt;ul&gt;&lt;li&gt;C++ / Node.js / OData v4&lt;/li&gt;&lt;li&gt;Espa&amp;ntilde;a &amp;euro;45k&lt;/li&gt;&lt;/ul&gt;&lt;/div&gt;";

        String cleaned = HtmlTextCleaner.toPlainText(raw);

        assertThat(cleaned)
                .contains("Build APIs with Java, Spring Boot & S/4HANA (years < 3 welcome).")
                .contains("• C++ / Node.js / OData v4")
                .contains("• España €45k")
                .doesNotContain("<p>", "<div>", "&lt;", "&amp;");
    }

    @Test
    void stripsScriptsAndStylesWhileKeepingReadableParagraphs() {
        String raw = "<style>.a{color:red}</style><script>alert('x')</script>"
                + "<h2>Requirements</h2><p>Experience with ABAP OO &amp; RAP.</p><br/><p>Remote in Madrid.</p>";

        String cleaned = HtmlTextCleaner.toPlainText(raw);

        assertThat(cleaned)
                .doesNotContain("alert", "color:red", "<h2>")
                .contains("Requirements")
                .contains("Experience with ABAP OO & RAP.")
                .contains("Remote in Madrid.");
    }

    @Test
    void buildsWordBoundarySnippets() {
        String text = "First line\n\nSecond line with Spring Boot, Angular, PostgreSQL and Docker in Madrid.";
        assertThat(HtmlTextCleaner.snippet(text, 40))
                .endsWith("…")
                .hasSizeLessThanOrEqualTo(41)
                .startsWith("First line Second line");
        assertThat(HtmlTextCleaner.snippet("Short text", 40)).isEqualTo("Short text");
        assertThat(HtmlTextCleaner.snippet("   ", 40)).isEmpty();
    }
}
