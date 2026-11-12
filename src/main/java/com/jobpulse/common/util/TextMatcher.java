package com.jobpulse.common.util;

import java.text.BreakIterator;
import java.util.*;

/**
 * Simple text matcher that computes cosine similarity between two texts
 * using term-frequency vectors. This is intentionally lightweight and
 * avoids heavy ML dependencies. Good for initial ranking.
 */
public final class TextMatcher {

    private TextMatcher() {}

    public static double score(String a, String b) {
        return prepare(a).similarityTo(prepare(b));
    }

    public static PreparedText prepare(String text) {
        return new PreparedText(text == null ? Map.of() : termFrequencies(text));
    }

    /** Immutable per-request text representation; never held in a shared user cache. */
    public static final class PreparedText {
        private final Map<String,Double> terms;
        private final double magnitude;
        private PreparedText(Map<String,Double> terms) {
            this.terms=Map.copyOf(terms);
            this.magnitude=norm(terms);
        }

        public double similarityTo(PreparedText other) {
            if(other==null || magnitude==0 || other.magnitude==0) return 0;
            double dot=0;
            for(var entry:terms.entrySet()) {
                Double value=other.terms.get(entry.getKey());
                if(value!=null) dot+=entry.getValue()*value;
            }
            return dot/(magnitude*other.magnitude);
        }
    }

    private static Map<String, Double> termFrequencies(String text) {
        Map<String, Double> freq = new HashMap<>();
        // simple tokenization
        String[] tokens = tokenize(text);
        for (String t : tokens) {
            if (t.length() < 2) continue;
            t = t.toLowerCase(Locale.ROOT);
            freq.put(t, freq.getOrDefault(t, 0.0) + 1.0);
        }
        // normalize to TF (could use log-scale)
        return freq;
    }

    private static double norm(Map<String, Double> m) {
        double s = 0.0;
        for (double v : m.values()) s += v * v;
        return Math.sqrt(s);
    }

    private static String[] tokenize(String text) {
        // Split on non-letter/digit characters and keep words
        return text.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\s+");
    }
}


