package com.jobpulse.common.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
class JobMatcherTest {
    @Test void explicitResumeDenialsNeverEarnSkillCreditEvenWithContradictoryMentions() {
        var context=JobMatcher.prepare("Java engineer. No experience with Java and Postgres. Built Selenium tests.",null,null);
        var result=context.evaluate("Engineer","Java, PostgreSQL and Selenium",null);
        assertEquals(Set.of("selenium"),result.get("matchedSkills"));
        assertEquals(Set.of("java","postgresql"),result.get("skillsRequiringContextReview"));
        assertEquals(Set.of("java","postgresql"),result.get("skillsNotEvidenced"));
        ((Set<?>)result.get("skillsRequiringContextReview")).clear();
        assertEquals(Set.of("java"),context.evaluate("Engineer","Java",null).get("skillsRequiringContextReview"));
        assertEquals(Set.of(),context.evaluate("Engineer","Python",null).get("skillsRequiringContextReview"));
    }
    @Test void denialChangesOnlySkillEvidenceAndNeverClaimsVerifiedEligibility() {
        var positive=JobMatcher.evaluate("Java", "Engineer","Java",null,null,null);
        var negative=JobMatcher.evaluate("No experience with Java", "Engineer","Java",null,null,null);
        assertTrue((double)negative.get("score") < (double)positive.get("score"));
        assertTrue(negative.get("explanation").toString().contains("not proof of proficiency"));
        assertEquals(Set.of("java"),positive.get("matchedSkills"));
        assertEquals(Set.of(),positive.get("skillsRequiringContextReview"));
    }

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
    @Test void commonSpellingsMatchCanonicalJobSkillsWithoutDuplicateCredit() {
        var result=JobMatcher.evaluate("C#, REST-Assured, Postgres and K8s", "Engineer",
            "CSharp, RESTAssured, PostgreSQL and Kubernetes", null, null, null);
        assertEquals(Set.of("csharp","rest assured","postgresql","kubernetes"),result.get("matchedSkills"));
        assertEquals(Set.of(),result.get("skillsNotEvidenced"));
        assertTrue(result.get("explanation").toString().contains("not proof of proficiency"));
    }
    @Test void preparedResumeHandlesDifferentJobsWithoutMutatingItsEvidence() {
        var context=JobMatcher.prepare("Java Selenium engineer, 5 years experience","Engineer","Toronto");
        var first=context.evaluate("Engineer","Java Selenium, 3 years experience","Toronto");
        ((Set<?>)first.get("matchedSkills")).clear();
        var next=context.evaluate("Engineer","Java PostgreSQL, 7 years experience","Toronto");
        assertEquals(Set.of("java"),next.get("matchedSkills"));
        assertEquals(Set.of("postgresql"),next.get("skillsNotEvidenced"));
        assertEquals(5.0,next.get("resumeYearsMentioned"));
        assertEquals(true,next.get("experienceReviewRequired"));
    }
    @Test void separatePreparedResumesKeepTheirPreferencesAndMentionsIsolated() {
        var alice=JobMatcher.prepare("Java engineer","Engineer","Toronto");
        var bob=JobMatcher.prepare("C# engineer","Engineer","Remote Canada");
        var a=alice.evaluate("Engineer","Java","Toronto");
        var b=bob.evaluate("Engineer","Java","Toronto");
        assertEquals(Set.of("java"),a.get("matchedSkills"));assertEquals(Set.of(),b.get("matchedSkills"));
        assertEquals(true,a.get("locationPreferenceMatched"));assertEquals(false,b.get("locationPreferenceMatched"));
        assertEquals(a.get("score"),alice.evaluate("Engineer","Java","Toronto").get("score"));
    }
    @Test void preparedContextCanBeReadConcurrentlyWithoutCrossJobResults() throws Exception {
        var context=JobMatcher.prepare("Java engineer","Engineer","Toronto");
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(4)) {
            var calls=new java.util.ArrayList<java.util.concurrent.Future<java.util.Map<String,Object>>>();
            for(int i=0;i<24;i++) {
                String skill=i%2==0?"Java":"Python";
                calls.add(executor.submit(()->context.evaluate("Engineer",skill,"Toronto")));
            }
            for(int i=0;i<calls.size();i++) assertEquals(i%2==0?Set.of("java"):Set.of(),
                calls.get(i).get(5,java.util.concurrent.TimeUnit.SECONDS).get("matchedSkills"));
        }
    }
    @Test void multilineSeniorTitlesStillRequireSeniorityReview() {
        var result=JobMatcher.prepare("Java engineer",null,null).evaluate("Senior\nQuality Engineer","Java",null);
        assertEquals(true,result.get("seniorityReviewRequired"));
    }
}
