package com.jobpulse.common.util;

import org.junit.jupiter.api.Test;
import java.util.Locale;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ResumeParserTest {
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
