package com.jobpulse.common.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
class JobMatcherTest {
    @Test void explicitYearsAreNotInferredFromDates() {
        var result=JobMatcher.evaluate("Java engineer, 2020-2025","Engineer","Requires 5+ years experience","Toronto","Engineer","Toronto");
        assertEquals(5.0,result.get("requiredYearsMentioned"));assertNull(result.get("resumeYearsMentioned"));assertEquals(true,result.get("experienceReviewRequired"));
    }
    @Test void explainsEvidenceWithoutClaimingAbsentSkills() {
        var result=JobMatcher.evaluate("Java Selenium engineer","Senior SDET","Java Selenium Playwright","Toronto","SDET","Toronto");
        assertEquals(Set.of("java","selenium"),result.get("matchedSkills"));
        assertEquals(Set.of("playwright"),result.get("skillsNotEvidenced"));
        assertEquals(true,result.get("seniorityReviewRequired"));
        assertEquals(true,result.get("locationPreferenceMatched"));
        assertTrue((double)result.get("score")>=0 && (double)result.get("score")<=100);
    }
}
