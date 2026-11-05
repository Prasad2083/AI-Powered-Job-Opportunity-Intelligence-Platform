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
}
