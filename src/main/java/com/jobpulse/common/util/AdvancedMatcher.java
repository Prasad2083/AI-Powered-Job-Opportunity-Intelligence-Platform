package com.jobpulse.common.util;

import java.util.*;

/**
 * Phase 6: Advanced Matcher - Weighted scoring for job-resume matching
 */
public final class AdvancedMatcher {

    private AdvancedMatcher() {}

    /**
     * Weighted job-resume matching
     * Considers: title match, skills match, description match
     */
    public static double scoreJobResume(String jobTitle, String jobCompany, String jobDescription, 
                                        String resumeTitle, String resumeSkills, String resumeText) {
        double titleScore = scoreTitle(jobTitle, resumeTitle) * 0.4;     // 40% weight
        double skillsScore = scoreSkills(jobDescription, resumeSkills) * 0.35; // 35% weight
        double descScore = TextMatcher.score(jobDescription, resumeText) * 0.25; // 25% weight

        return titleScore + skillsScore + descScore;
    }

    /**
     * Score job title vs resume title (higher weight for exact/similar match)
     */
    private static double scoreTitle(String jobTitle, String resumeTitle) {
        if (jobTitle == null || resumeTitle == null) return 0.0;

        String jobLower = jobTitle.toLowerCase();
        String resumeLower = resumeTitle.toLowerCase();

        // Exact match
        if (jobLower.equals(resumeLower)) return 1.0;

        // Partial match (contains)
        if (jobLower.contains(resumeLower) || resumeLower.contains(jobLower)) {
            return 0.8;
        }

        // Similar keywords
        return TextMatcher.score(jobLower, resumeLower);
    }

    /**
     * Score job description skills vs resume skills
     */
    private static double scoreSkills(String jobDescription, String resumeSkills) {
        if (jobDescription == null || resumeSkills == null) return 0.0;

        Map<String, String> jobSkills = ResumeParser.extractSkills(jobDescription);
        Map<String, String> resumeSkillsMap = ResumeParser.extractSkills(resumeSkills);

        if (jobSkills.isEmpty() || resumeSkillsMap.isEmpty()) {
            return 0.0;
        }

        // Count matching skills
        int matches = 0;
        for (String skill : jobSkills.keySet()) {
            if (resumeSkillsMap.containsKey(skill)) {
                matches++;
            }
        }

        // Calculate ratio
        double ratio = (double) matches / Math.max(jobSkills.size(), resumeSkillsMap.size());
        return Math.min(ratio, 1.0);
    }

    /**
     * Batch score multiple jobs against resume
     */
    public static Map<String, Double> scoreMultipleJobs(List<JobCandidate> jobs, String resumeText) {
        Map<String, Double> scores = new HashMap<>();

        for (JobCandidate job : jobs) {
            double score = scoreJobResume(
                job.getTitle(),
                job.getCompany(),
                job.getDescription(),
                "", // Resume title if available
                resumeText, // Skills extracted from resume
                resumeText
            );
            scores.put(job.getId(), score);
        }

        return scores;
    }

    /**
     * Helper class for job candidates
     */
    public static class JobCandidate {
        private String id;
        private String title;
        private String company;
        private String description;

        public JobCandidate(String id, String title, String company, String description) {
            this.id = id;
            this.title = title;
            this.company = company;
            this.description = description;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getCompany() { return company; }
        public String getDescription() { return description; }
    }
}

