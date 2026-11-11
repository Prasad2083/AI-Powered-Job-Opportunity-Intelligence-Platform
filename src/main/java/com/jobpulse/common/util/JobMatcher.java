package com.jobpulse.common.util;

import java.util.*;

/** Evidence-based ranking aid, not a hiring probability or proof of eligibility. */
public final class JobMatcher {
    private JobMatcher() {}
    private static final java.util.regex.Pattern SENIORITY=java.util.regex.Pattern.compile("\\b(senior|lead|principal|staff)\\b",java.util.regex.Pattern.CASE_INSENSITIVE);

    public static Map<String,Object> evaluate(String resume,String title,String description,String location,String roles,String locations) {
        return prepare(resume,roles,locations).evaluate(title,description,location);
    }

    public static RankingContext prepare(String resume,String roles,String locations) {
        return new RankingContext(resume,roles,locations);
    }

    /** Build once for an authenticated request; all fields are immutable and user-specific. */
    public static final class RankingContext {
        private final Set<String> evidenced;
        private final TextMatcher.PreparedText resumeText;
        private final List<TextMatcher.PreparedText> roleTexts;
        private final List<String> locationPreferences;
        private final boolean seniorEvidence;
        private final Double evidencedYears;

        private RankingContext(String resume,String roles,String locations) {
            String cv=resume==null?"":resume;
            evidenced=Set.copyOf(ResumeParser.extractSkills(cv).keySet());
            resumeText=TextMatcher.prepare(cv);
            roleTexts=roles==null?List.of():Arrays.stream(roles.split(",")).filter(role->!role.isBlank()).map(TextMatcher::prepare).toList();
            locationPreferences=locations==null?List.of():Arrays.stream(locations.split(",")).map(JobMatcher::normalizedLocation).filter(place->!place.isEmpty()).toList();
            seniorEvidence=SENIORITY.matcher(cv).find();
            evidencedYears=explicitYears(cv);
        }

        public Map<String,Object> evaluate(String title,String description,String location) {
            String requirements=((title==null?"":title)+" "+(description==null?"":description)).replaceAll("<[^>]*>"," ");
            var required=new TreeSet<>(ResumeParser.extractSkills(requirements).keySet());
            var matched=new TreeSet<>(required);matched.retainAll(evidenced);
            var missing=new TreeSet<>(required);missing.removeAll(evidenced);
            double skills=required.isEmpty()?0:(double)matched.size()/required.size();
            double text=resumeText.similarityTo(TextMatcher.prepare(requirements));
            var titleText=TextMatcher.prepare(title);
            double titleScore=resumeText.similarityTo(titleText);
            for(var role:roleTexts) titleScore=Math.max(titleScore,titleText.similarityTo(role));
            boolean senior=title!=null && SENIORITY.matcher(title).find();
            double seniority=senior?(seniorEvidence?1:0):1;
            boolean locationMatch=false;
            String place=normalizedLocation(location);
            for(String preference:locationPreferences)
                if(locationMatches(place,preference)) locationMatch=true;
            Double requiredYears=explicitYears(requirements);
            double experience=requiredYears==null || requiredYears==0?1:evidencedYears==null?0:Math.min(1,evidencedYears/requiredYears);
            double score=100*(.45*skills+.15*text+.15*titleScore+.1*seniority+.1*experience+.05*(locationMatch?1:0));
            Map<String,Object> result=new LinkedHashMap<>(Map.of("score",Math.round(score*100.0)/100.0,"matchedSkills",matched,"skillsNotEvidenced",missing,
                "explanation","Ranking weights: skills 45%, resume text 15%, title/preferences 15%, seniority keywords 10%, explicit years 10%, location keywords 5%. Skills are dictionary-based text mentions, including known aliases, not proof of proficiency. Negation and qualification context need review. Skills not detected are review prompts, not proven gaps. Years use explicit experience statements and the lower bound of ranges, not verified totals; mandatory requirements and work authorization require review.",
                "seniorityReviewRequired",senior&&!seniorEvidence,"locationPreferenceMatched",locationMatch));
            result.put("requiredYearsMentioned",requiredYears);result.put("resumeYearsMentioned",evidencedYears);
            result.put("experienceReviewRequired",requiredYears!=null && requiredYears>0 && (evidencedYears==null || evidencedYears<requiredYears));
            return result;
        }
    }

    // Restrict evidence to experience statements: company age, education duration and
    // employment dates are not interchangeable with a candidate's professional experience.
    private static final java.util.regex.Pattern EXPERIENCE = java.util.regex.Pattern.compile(
        "(?i)\\b(\\d{1,2}(?:\\.\\d+)?)\\s*(?:[-–—]\\s*\\d{1,2}(?:\\.\\d+)?\\s*)?\\+?\\s*(?:years?|yrs?)\\s*(?:of\\s+)?"
        + "(?:(?:relevant|professional|commercial|hands[- ]on|industry|work|related|overall|total|progressive)\\s+){0,3}experience\\b");

    private static Double explicitYears(String text) {
        var matcher=EXPERIENCE.matcher(text);
        Double maximum=null;
        while(matcher.find()) {
            double years=Double.parseDouble(matcher.group(1));
            maximum=maximum==null?years:Math.max(maximum,years);
        }
        return maximum;
    }

    private static String normalizedLocation(String value) {
        return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
    }

    private static boolean locationMatches(String place,String wanted) {
        if(wanted.isEmpty()) return false;
        String bounded=" "+place+" ";
        if(bounded.contains(" "+wanted+" ")) return true;
        // Providers often reverse country and remote labels: "Canada (Remote)".
        // Require both words; "Remote US" must not match "Remote Canada".
        return Set.of("remote canada","canada remote").contains(wanted)
            && bounded.contains(" remote ") && bounded.contains(" canada ");
    }
}
