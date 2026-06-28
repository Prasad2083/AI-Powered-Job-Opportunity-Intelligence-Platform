package com.jobpulse.standalone;

import com.jobpulse.infrastructure.client.MockJobBoardClient;
import com.jobpulse.infrastructure.client.MockJobBoardClient.ExternalJob;
import com.jobpulse.domain.entity.Job;
import com.jobpulse.domain.entity.Resume;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Lightweight standalone runner that performs ingestion -> match -> apply
 * without Spring. Produces a simple results file for inspection.
 *
 * Usage:
 * java -jar job-pulse-auto-apply-local.jar [path/to/resume.(txt|docx|pdf)] [matchThreshold]
 */
public class LocalRunner {

    public static void main(String[] args) throws Exception {
        String resumePath = args.length > 0 ? args[0] : null;
        double threshold = 0.05;
        if (args.length > 1) {
            try { threshold = Double.parseDouble(args[1]); } catch (Exception ignored) {}
        }

        System.out.println("Starting standalone JobPulse local runner");
        MockJobBoardClient client = new MockJobBoardClient();
        List<ExternalJob> externalJobs = client.fetchJobs();

        String resumeText;
        Resume resumeEntity = new Resume();
        if (resumePath != null) {
            File f = new File(resumePath);
            if (!f.exists()) {
                System.err.println("Resume file not found: " + resumePath);
                return;
            }
            resumeText = extractTextFromFile(f);
            resumeEntity.setFileName(f.getName());
        } else {
            resumeText = "Experienced software engineer with Java and Spring Boot";
            resumeEntity.setFileName("default_resume.txt");
        }
        resumeEntity.setUserId(1L);
        resumeEntity.setContentText(resumeText);
        resumeEntity.setIsPrimary(true);

        List<Map<String,Object>> results = new ArrayList<>();

        for (ExternalJob ej : externalJobs) {
            Job job = Job.builder()
                    .sourceJobId(ej.externalId)
                    .title(ej.title)
                    .companyName(ej.company)
                    .location(ej.location)
                    .jobDescription(ej.description)
                    .applyUrl(ej.url)
                    .postedDate(LocalDateTime.now())
                    .applied(false)
                    .isArchived(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            String jobText = (job.getTitle() == null ? "" : job.getTitle() + " ")
                    + (job.getCompanyName() == null ? "" : job.getCompanyName() + " ")
                    + (job.getJobDescription() == null ? "" : job.getJobDescription());
            double score = com.jobpulse.common.util.TextMatcher.score(jobText, resumeText);

            boolean applied = false;
            String status = "SKIPPED";
            if (score >= threshold) {
                applied = client.apply(job, resumeEntity);
                status = applied ? "APPLIED" : "FAILED";
            }

            Map<String,Object> r = new LinkedHashMap<>();
            r.put("sourceJobId", job.getSourceJobId());
            r.put("title", job.getTitle());
            r.put("company", job.getCompanyName());
            r.put("score", score);
            r.put("status", status);
            results.add(r);
            System.out.println(String.format("Job %s - %s (score=%.4f) => %s", job.getSourceJobId(), job.getTitle(), score, status));
        }

        // write results
        File out = new File("autoapply-results.json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write(toJson(results));
        }
        System.out.println("Results written to " + out.getAbsolutePath());
    }

    private static String toJson(List<Map<String,Object>> results) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        for (Map<String,Object> m : results) {
            if (!first) sb.append(',');
            first = false;
            sb.append('{');
            boolean f2 = true;
            for (Map.Entry<String,Object> e : m.entrySet()) {
                if (!f2) sb.append(','); f2 = false;
                sb.append('"').append(escape(e.getKey())).append('"').append(':');
                Object v = e.getValue();
                if (v instanceof Number) sb.append(v.toString());
                else sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractTextFromFile(File f) throws Exception {
        String name = f.getName().toLowerCase();
        if (name.endsWith(".docx")) {
            try (InputStream in = Files.newInputStream(f.toPath()); org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(in); org.apache.poi.xwpf.extractor.XWPFWordExtractor ex = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
                return ex.getText();
            }
        } else if (name.endsWith(".txt") || name.endsWith(".text")) {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        } else if (name.endsWith(".pdf")) {
            // heuristic fallback for PDF: extract readable runs
            byte[] bytes = Files.readAllBytes(f.toPath());
            String raw = new String(bytes, StandardCharsets.ISO_8859_1);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\p{L}\\p{N}]{3,}").matcher(raw);
            StringBuilder sb = new StringBuilder();
            while (m.find()) { sb.append(m.group()).append(' '); }
            return sb.toString();
        } else {
            // fallback: read as text
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        }
    }
}