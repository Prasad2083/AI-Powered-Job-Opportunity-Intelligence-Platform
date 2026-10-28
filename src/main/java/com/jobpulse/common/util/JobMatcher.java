package com.jobpulse.common.util;

import java.util.*;

/** Evidence-based ranking aid, not a hiring probability or proof of eligibility. */
public final class JobMatcher {
    private JobMatcher() {}
    public static Map<String,Object> evaluate(String resume,String title,String description,String location,String roles,String locations) {
        String cv=resume==null?"":resume;
        String requirements=(title+" "+description).replaceAll("<[^>]*>"," ");
        var required=new TreeSet<>(ResumeParser.extractSkills(requirements).keySet());
        var evidenced=new TreeSet<>(ResumeParser.extractSkills(cv).keySet());
        var matched=new TreeSet<>(required);matched.retainAll(evidenced);
        var missing=new TreeSet<>(required);missing.removeAll(evidenced);
        double skills=required.isEmpty()?0:(double)matched.size()/required.size();
        double text=TextMatcher.score(requirements,cv);
        double titleScore=TextMatcher.score(title,cv);
        if(roles!=null) for(String role:roles.split(",")) titleScore=Math.max(titleScore,TextMatcher.score(title,role));
        String level=title==null?"":title.toLowerCase(Locale.ROOT);
        boolean senior=level.matches(".*\\b(senior|lead|principal|staff)\\b.*");
        boolean seniorEvidence=cv.toLowerCase(Locale.ROOT).matches("(?s).*\\b(senior|lead|principal|staff)\\b.*");
        double seniority=senior?(seniorEvidence?1:0):1;
        boolean locationMatch=false;
        String place=location==null?"":location.toLowerCase(Locale.ROOT);
        if(locations!=null) for(String preference:locations.split(","))
            if(!preference.isBlank() && place.contains(preference.trim().toLowerCase(Locale.ROOT))) locationMatch=true;
        Double requiredYears=explicitYears(requirements), evidencedYears=explicitYears(cv);
        double experience=requiredYears==null?1:evidencedYears==null?0:Math.min(1,evidencedYears/requiredYears);
        double score=100*(.45*skills+.15*text+.15*titleScore+.1*seniority+.1*experience+.05*(locationMatch?1:0));
        Map<String,Object> result=new LinkedHashMap<>(Map.of("score",Math.round(score*100.0)/100.0,"matchedSkills",matched,"skillsNotEvidenced",missing,
            "explanation","Ranking weights: skills 45%, resume text 15%, title/preferences 15%, seniority keywords 10%, explicit years 10%, location keywords 5%. Skills not evidenced are review prompts, not proven gaps. Years are explicit text mentions, not verified totals; mandatory requirements and work authorization require review.",
            "seniorityReviewRequired",senior&&!seniorEvidence,"locationPreferenceMatched",locationMatch));
        result.put("requiredYearsMentioned",requiredYears);result.put("resumeYearsMentioned",evidencedYears);
        result.put("experienceReviewRequired",requiredYears!=null && (evidencedYears==null || evidencedYears<requiredYears));
        return result;
    }

    private static Double explicitYears(String text) {
        var matcher=java.util.regex.Pattern.compile("(?i)\\b(\\d{1,2}(?:\\.\\d)?)\\+?\\s*(?:years?|yrs?)\\b").matcher(text);
        Double maximum=null;
        while(matcher.find()) {double years=Double.parseDouble(matcher.group(1));maximum=maximum==null?years:Math.max(maximum,years);}
        return maximum;
    }
}
