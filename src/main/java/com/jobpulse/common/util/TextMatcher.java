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
        if (a == null || b == null) return 0.0;
        Map<String, Double> fa = termFrequencies(a);
        Map<String, Double> fb = termFrequencies(b);

        // dot product
        double dot = 0.0;
        for (Map.Entry<String, Double> e : fa.entrySet()) {
            Double v = fb.get(e.getKey());
            if (v != null) dot += e.getValue() * v;
        }

        double na = norm(fa);
        double nb = norm(fb);
        if (na == 0 || nb == 0) return 0.0;
        return dot / (na * nb);
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
        return text.replaceAll("[^\\p{L}\\p{N}]+", " ").trim().split("\\\\s+");
    }
}


