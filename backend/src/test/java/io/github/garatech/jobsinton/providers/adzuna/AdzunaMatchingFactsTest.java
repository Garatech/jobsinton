package io.github.garatech.jobsinton.providers.adzuna;

import io.github.garatech.jobsinton.matching.MatchingInput.Opportunity;
import io.github.garatech.jobsinton.matching.MatchingInput.Role;
import io.github.garatech.jobsinton.matching.MatchingInput.WorkMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AdzunaMatchingFactsTest {
    private static final Opportunity UNKNOWN = new Opportunity(null, null, null, null, null, null, null);

    @Test
    void readsTheSuppliedExcerptPastTheCompanyIntroduction() {
        String intro = "Somos una empresa de software. ".repeat(30);
        var facts = AdzunaMatchingFacts.from("  Desarrolladora backend ", intro
                + "Modalidad: híbrida. Se requieren dos días presenciales por semana.");
        assertThat(facts).isEqualTo(new Opportunity(Role.BACKEND, null, null, WorkMode.HYBRID, 2, null, null));
    }

    @ParameterizedTest
    @CsvSource({
        "Frontend developer, FRONTEND", "Full-stack developer, FULL_STACK", "Desarrolladora SAP, SAP",
        "Ingeniera de datos, DATA", "Platform engineer, PLATFORM"
    })
    void recognisesAnExplicitRoleWithoutInferringSkills(String title, Role role) {
        assertThat(AdzunaMatchingFacts.from(title, null))
                .isEqualTo(new Opportunity(role, null, null, null, null, null, null));
    }

    @ParameterizedTest
    @CsvSource({
        "Two days in the office per week required., 2", "CINCO DÍAS EN LA OFICINA A LA SEMANA., 5",
        "Un día presencial por semana., 1", "Zero office days per week., 0",
        "Dos días presenciales por semana. Dos días presenciales por semana., 2"
    })
    void aWeeklyCountDoesNotImplyAModeOrAnOffice(String excerpt, int days) {
        assertThat(AdzunaMatchingFacts.from("Java developer", excerpt))
                .isEqualTo(new Opportunity(null, null, null, null, days, null, null));
    }

    @ParameterizedTest
    @CsvSource({"Trabajo 100% remoto., REMOTE", "Work mode: on-site., ONSITE", "Work mode: hybrid., HYBRID"})
    void anExplicitModeDoesNotSupplyANumericScheduleOrResidencePolicy(String excerpt, WorkMode mode) {
        assertThat(AdzunaMatchingFacts.from("Java developer", excerpt))
                .isEqualTo(new Opportunity(null, null, null, mode, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Somos una empresa que apuesta por el desarrollo…", "Modelo flexible.",
        "No se requieren dos días presenciales por semana.", "Hasta dos días presenciales por semana.",
        "Dos o tres días presenciales por semana.", "Dos días presenciales por mes.",
        "Dos días presenciales por semana solo durante la formación.", "Two office days per week if desired.",
        "Se requieren dos días presenciales por…", "5 días de teletrabajo por semana.",
        "Two office days per week…", "Six office days per week.", "Java, Madrid. Buscamos talento."})
    void vagueQualifiedOrIncompleteTextIsNotEvidence(String excerpt) {
        assertThat(AdzunaMatchingFacts.from("Java developer", excerpt)).isEqualTo(UNKNOWN);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Ubicación: Madrid. Presencial. Horario: lunes a viernes, de 09:00 a 18:00.",
        "Nuestro equipo busca refuerzos en Madrid (Paseo del Ejemplo) Presencial Horario: Lunes a Viernes, 09:00 18…"
    })
    void readsTheOnsiteFieldWithoutTurningWorkingHoursIntoAttendance(String excerpt) {
        assertThat(AdzunaMatchingFacts.from("Java developer", excerpt))
                .isEqualTo(new Opportunity(null, null, null, WorkMode.ONSITE, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Ubicación: Madrid. Entrevista presencial. Horario: lunes a viernes.",
        "Ubicación: Madrid. Formación presencial inicial. Horario: lunes a viernes.",
        "Ubicación: Madrid. Presencial opcional. Horario: lunes a viernes.",
        "Ubicación: Madrid. Presencial. Horario: lunes a viernes. Modalidad por confirmar.",
        "Trabajo 100% remoto. Madrid (Calle Ejemplo) Presencial Horario: lunes a viernes.",
        "Ubicación: Madrid. Presencial… Horario: lunes a viernes.",
        "Presencial. Horario: lunes a viernes.", "Madrid (Calle Ejemplo) Presencial"
    })
    void incompleteQualifiedOrConflictingOnsiteFieldsStayUnknown(String excerpt) {
        assertThat(AdzunaMatchingFacts.from("Java developer", excerpt)).isEqualTo(UNKNOWN);
    }

    @Test
    void aScheduleQualifierInTheTitleCannotBeOverriddenByTheExcerpt() {
        assertThat(AdzunaMatchingFacts.from("Backend developer (fully remote)", "Two office days per week."))
                .isEqualTo(UNKNOWN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Trabajo 100% remoto. Dos días presenciales por semana.",
        "Modalidad: híbrida. Modalidad: presencial.", "Two office days per week. Three office days per week.",
        "Work mode: on-site. Zero office days per week.", "Dos días presenciales por semana. Asistencia opcional.",
        "Modalidad: híbrida. Hasta dos días presenciales por semana."})
    void aDisputedScheduleRetainsOnlyIndependentRoleEvidence(String excerpt) {
        assertThat(AdzunaMatchingFacts.from("Backend developer", excerpt))
                .isEqualTo(new Opportunity(Role.BACKEND, null, null, null, null, null, null));
    }
}
