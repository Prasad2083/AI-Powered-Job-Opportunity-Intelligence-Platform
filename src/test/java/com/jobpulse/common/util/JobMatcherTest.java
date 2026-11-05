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
    @Test void experienceRangesUseTheirMinimumNotTheirUpperBound() {
        for(String range: new String[]{"3-5 years of professional experience", "3–5 years experience", "3—5 yrs relevant experience"}) {
            var result=JobMatcher.evaluate("3 years of professional experience", "Engineer", range, "Toronto", "Engineer", "Toronto");
            assertEquals(3.0,result.get("requiredYearsMentioned"),range);
            assertEquals(false,result.get("experienceReviewRequired"),range);
        }
    }
    @Test void companyAgeAndEducationDurationAreNotExperienceEvidence() {
        var result=JobMatcher.evaluate("4 years at university. Java engineer.", "Engineer",
            "Serving customers for 25 years. Requires 5+ years of experience.", null, null, null);
        assertEquals(5.0,result.get("requiredYearsMentioned"));
        assertNull(result.get("resumeYearsMentioned"));
        assertEquals(true,result.get("experienceReviewRequired"));
    }
    @Test void unrelatedYearMentionsDoNotCreateARequirement() {
        var result=JobMatcher.evaluate("Java", "Engineer", "For 25 years we have built software. Four-year degree.", null, null, null);
        assertNull(result.get("requiredYearsMentioned"));
        assertEquals(false,result.get("experienceReviewRequired"));
    }
    @Test void fractionalExperienceAndMultipleRequirementsRemainExplicit() {
        var result=JobMatcher.evaluate("2.5 years hands-on experience", "Engineer",
            "2 years relevant experience; 3+ years of professional experience preferred", null, null, null);
        assertEquals(3.0,result.get("requiredYearsMentioned"));
        assertEquals(2.5,result.get("resumeYearsMentioned"));
        assertEquals(true,result.get("experienceReviewRequired"));
    }
    @Test void locationMatchingUsesWordBoundariesAndNormalizedPunctuation() {
        assertEquals(false,JobMatcher.evaluate("", "", "", "Yorkshire", "", "York").get("locationPreferenceMatched"));
        assertEquals(true,JobMatcher.evaluate("", "", "", "Greater-Toronto Area, Canada", "", "Greater Toronto Area").get("locationPreferenceMatched"));
    }
    @Test void remoteCanadaSupportsProviderLabelOrderWithoutTreatingAllRemoteJobsAsCanadian() {
        assertEquals(true,JobMatcher.evaluate("", "", "", "Canada (Remote)", "", "Remote Canada").get("locationPreferenceMatched"));
        assertEquals(false,JobMatcher.evaluate("", "", "", "Remote - US", "", "Remote Canada").get("locationPreferenceMatched"));
    }
    @Test void missingInputsNeverBecomeLiteralNullEvidence() {
        var result=JobMatcher.evaluate(null,null,null,null,null,null);
        assertEquals(Set.of(),result.get("matchedSkills"));
        assertEquals(false,result.get("locationPreferenceMatched"));
        assertNull(result.get("requiredYearsMentioned"));
        assertTrue(Double.isFinite((double)result.get("score")));
    }
    @Test void zeroYearsDoesNotRequireExperienceOrProduceInvalidScores() {
        for(String resume: new String[]{"", "0 years experience"}) {
            var result=JobMatcher.evaluate(resume,"Engineer","0 years experience required",null,null,null);
            assertEquals(false,result.get("experienceReviewRequired"));
            assertTrue(Double.isFinite((double)result.get("score")));
        }
    }
}
