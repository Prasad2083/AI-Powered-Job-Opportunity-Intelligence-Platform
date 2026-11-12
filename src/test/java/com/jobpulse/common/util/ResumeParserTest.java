package com.jobpulse.common.util;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ResumeParserTest {
    @Test void explicitDenialsRecognizeKnownListsAndAliases() {
        assertEquals(Set.of("java","selenium","postgresql"),ResumeParser.explicitlyNegatedSkills("No experience with Java, Selenium or Postgres."));
        assertEquals(Set.of("csharp","rest assured"),ResumeParser.explicitlyNegatedSkills("I don’t have experience in C# and REST-Assured."));
        assertEquals(Set.of("spring","spring boot","kubernetes"),ResumeParser.explicitlyNegatedSkills("Never used Spring Boot / K8s."));
        for(String prefix:new String[]{"not experienced in ","not proficient with ","unfamiliar with ","never worked with ","do not have experience with "})
            assertEquals(Set.of("java"),ResumeParser.explicitlyNegatedSkills(prefix+"Java"),prefix);
    }
    @Test void denialScopeDoesNotCrossSentencesOrUnrelatedExperience() {
        assertEquals(Set.of("java"),ResumeParser.explicitlyNegatedSkills("No experience with Java. Built Selenium tests."));
        assertEquals(Set.of("java"),ResumeParser.explicitlyNegatedSkills("No experience with Java, but built Selenium tests."));
        assertEquals(Set.of("java"),ResumeParser.explicitlyNegatedSkills("No experience with Java\nSelenium"));
        assertTrue(ResumeParser.explicitlyNegatedSkills("Not only Java but also Selenium. No production incidents with Java. JavaScript and NoSQL.").isEmpty());
        assertTrue(ResumeParser.explicitlyNegatedSkills(null).isEmpty());
    }

    @Test void recognizesPunctuationEndedCSharpWithoutMatchingEmbeddedWords() {
        for(String spelling:new String[]{"C#", "C Sharp", "c-sharp", "CSharp", "C#/.NET"})
            assertTrue(ResumeParser.extractSkills(spelling).containsKey("csharp"),spelling);
        assertTrue(ResumeParser.extractSkills("abcsharp csharpish C#ish").isEmpty());
    }

    @Test void aliasesUseCanonicalNamesWithoutDuplicateSkills() {
        var found=ResumeParser.extractSkills("REST-Assured RESTAssured REST Assured, Postgres PostgreSQL, K8s Kubernetes");
        assertEquals(Set.of("rest assured","postgresql","kubernetes"),found.keySet());
    }

    @Test void handlesWrappedAndNonBreakingWhitespaceFromResumeExtraction() {
        var found=ResumeParser.extractSkills("REST\nAssured and Spring\u00a0Boot");
        assertEquals(Set.of("rest assured","spring","spring boot"),found.keySet());
    }

    @Test void doesNotExtractSkillsFromSubstringsOrAmbiguousOrdinaryWords() {
        var found=ResumeParser.extractSkills("JavaScript reactive NoSQL csharpish postgresish k8sish REST-Assuredness. We go live today.");
        assertEquals(Set.of("javascript"),found.keySet());
    }

    @Test void extractionDoesNotDependOnTheMacOrServerDefaultLocale() {
        Locale previous=Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(Set.of("java","junit","git"),ResumeParser.extractSkills("JAVA JUNIT GIT").keySet());
            assertTrue(ResumeParser.extractTitles("ENGINEER").contains("Engineer"));
        } finally { Locale.setDefault(previous); }
    }
}
