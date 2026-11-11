package com.jobpulse.common.util;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Phase 4: Resume Parser - Extracts skills from resume text
 */
public final class ResumeParser {

    private static final Map<String, String> SKILL_CATEGORIES = new HashMap<>();
    private static final Map<String, Pattern> SKILL_PATTERNS = new HashMap<>();
    private static final Map<String, List<String>> ALIASES = Map.of(
        "csharp", List.of("c#", "c sharp", "c-sharp"),
        "rest assured", List.of("restassured", "rest-assured"),
        "postgresql", List.of("postgres"),
        "kubernetes", List.of("k8s")
    );

    static {
        // Languages
        SKILL_CATEGORIES.put("java", "Languages");
        SKILL_CATEGORIES.put("python", "Languages");
        SKILL_CATEGORIES.put("javascript", "Languages");
        SKILL_CATEGORIES.put("typescript", "Languages");
        SKILL_CATEGORIES.put("golang", "Languages");
        SKILL_CATEGORIES.put("csharp", "Languages");
        SKILL_CATEGORIES.put("kotlin", "Languages");
        SKILL_CATEGORIES.put("sql", "Languages");

        // Frameworks
        SKILL_CATEGORIES.put("spring", "Frameworks");
        SKILL_CATEGORIES.put("spring boot", "Frameworks");
        SKILL_CATEGORIES.put("react", "Frameworks");
        SKILL_CATEGORIES.put("angular", "Frameworks");
        SKILL_CATEGORIES.put("vue", "Frameworks");
        SKILL_CATEGORIES.put("django", "Frameworks");
        SKILL_CATEGORIES.put("flask", "Frameworks");
        SKILL_CATEGORIES.put("fastapi", "Frameworks");

        // Databases
        SKILL_CATEGORIES.put("postgresql", "Databases");
        SKILL_CATEGORIES.put("mysql", "Databases");
        SKILL_CATEGORIES.put("mongodb", "Databases");
        SKILL_CATEGORIES.put("redis", "Databases");
        SKILL_CATEGORIES.put("elasticsearch", "Databases");
        SKILL_CATEGORIES.put("cassandra", "Databases");

        for (String skill : List.of("selenium", "playwright", "cypress", "junit", "testng", "rest assured", "appium", "cucumber", "postman", "jmeter", "k6", "pytest", "ollama", "pytorch"))
            SKILL_CATEGORIES.put(skill,"Testing and AI");
        // Tools & Platforms
        SKILL_CATEGORIES.put("docker", "Tools");
        SKILL_CATEGORIES.put("kubernetes", "Tools");
        SKILL_CATEGORIES.put("git", "Tools");
        SKILL_CATEGORIES.put("jenkins", "Tools");
        SKILL_CATEGORIES.put("aws", "Tools");
        SKILL_CATEGORIES.put("azure", "Tools");
        SKILL_CATEGORIES.put("gcp", "Tools");
        SKILL_CATEGORIES.put("terraform", "Tools");

        for (String skill : SKILL_CATEGORIES.keySet()) {
            var names = new ArrayList<String>();
            names.add(skill);
            names.addAll(ALIASES.getOrDefault(skill, List.of()));
            String alternatives = names.stream().map(ResumeParser::phrasePattern)
                .collect(java.util.stream.Collectors.joining("|"));
            // Word boundaries alone fail for punctuation-ended names such as C#.
            SKILL_PATTERNS.put(skill, Pattern.compile("(?<![\\p{L}\\p{N}_])(?:" + alternatives + ")(?![\\p{L}\\p{N}_])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS));
        }
    }

    private ResumeParser() {}

    /**
     * Extract skills from resume text
     * Returns Map<skillName, category>
     */
    public static Map<String, String> extractSkills(String resumeText) {
        Map<String, String> foundSkills = new HashMap<>();

        if (resumeText == null || resumeText.isEmpty()) {
            return foundSkills;
        }

        // Mentions only: context, proficiency and negation still require review.
        for (Map.Entry<String, String> skill : SKILL_CATEGORIES.entrySet()) {
            if (SKILL_PATTERNS.get(skill.getKey()).matcher(resumeText).find()) {
                foundSkills.put(skill.getKey(), skill.getValue());
            }
        }

        return foundSkills;
    }

    private static String phrasePattern(String name) {
        return Arrays.stream(name.split(" ")).map(Pattern::quote)
            .collect(java.util.stream.Collectors.joining("\\s+"));
    }

    /**
     * Extract job titles from resume (basic pattern)
     */
    public static List<String> extractTitles(String resumeText) {
        List<String> titles = new ArrayList<>();
        if (resumeText == null) return titles;

        String[] commonTitles = {
            "Senior Developer", "Developer", "Engineer", "Architect",
            "Lead", "Manager", "Analyst", "DevOps", "QA",
            "Full Stack", "Backend", "Frontend", "Mobile"
        };

        String lowerText = resumeText.toLowerCase(Locale.ROOT);
        for (String title : commonTitles) {
            if (lowerText.contains(title.toLowerCase(Locale.ROOT))) {
                titles.add(title);
            }
        }
        return titles;
    }

    /**
     * Calculate a resume "strength" score based on skills
     */
    public static double calculateResumeStrength(Map<String, String> skills) {
        int count = skills.size();
        // Rough heuristic: more skills = stronger resume
        // Normalize to 0.0-1.0
        return Math.min((double) count / 20.0, 1.0);
    }
}

