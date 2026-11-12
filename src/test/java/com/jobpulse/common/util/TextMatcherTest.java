package com.jobpulse.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextMatcherTest {
    @Test void identicalResumeAndJobScoresOne() {
        assertEquals(1.0,TextMatcher.score("Java Selenium API","Java Selenium API"),0.00001);
    }
    @Test void unrelatedJobsScoreZero() {
        assertEquals(0.0,TextMatcher.score("Java Selenium","Bananas Oranges"),0.00001);
    }
    @Test void missingResumeScoresZero() {
        assertEquals(0.0,TextMatcher.score(null,"Java Selenium"));
    }
    @Test void preparedVectorsPreserveKnownCosineAcrossRepeatedComparisons() {
        var resume=TextMatcher.prepare("Java Java Selenium");
        assertEquals(2/Math.sqrt(10),resume.similarityTo(TextMatcher.prepare("Java Spring")),0.0000001);
        assertEquals(0,resume.similarityTo(TextMatcher.prepare("Python Docker")));
        assertEquals(1,resume.similarityTo(TextMatcher.prepare("Java Java Selenium")),0.0000001);
    }
    @Test void emptyPreparedInputsDoNotProduceInvalidScores() {
        assertEquals(0,TextMatcher.prepare(null).similarityTo(TextMatcher.prepare("Java")));
        assertEquals(0,TextMatcher.prepare("  ").similarityTo(TextMatcher.prepare("Java")));
        assertEquals(0,TextMatcher.prepare("Java").similarityTo(null));
    }
}
