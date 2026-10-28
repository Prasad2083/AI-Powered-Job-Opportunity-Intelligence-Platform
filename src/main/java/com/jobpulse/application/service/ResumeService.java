package com.jobpulse.application.service;

import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ResumeRepository;
// Avoid direct PDFBox APIs to keep compatibility; use a lightweight PDF text heuristic fallback.
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class ResumeService {

    private final Logger log = LoggerFactory.getLogger(ResumeService.class);
    private final ResumeRepository resumeRepository;

    public ResumeService(ResumeRepository resumeRepository) {
        this.resumeRepository = resumeRepository;
    }

    public Resume storeResume(MultipartFile file, Long userId) throws Exception {
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || file.getSize() > 10_000_000) throw new IllegalArgumentException("Invalid resume size");
        if (filename == null || !(filename.toLowerCase().endsWith(".pdf") || filename.toLowerCase().endsWith(".docx") || filename.toLowerCase().endsWith(".txt"))) throw new IllegalArgumentException("Only PDF, DOCX, and TXT are supported");
        String text = extractText(file.getInputStream(), filename);
        if (text.isBlank()) throw new IllegalArgumentException("No readable text in resume");
        Resume r = new Resume();
        r.setUserId(userId == null ? 1L : userId);
        r.setFileName(filename == null ? "upload" : filename);
        r.setContentText(text);
        r.setIsPrimary(true);
        Resume saved = resumeRepository.save(r);
        log.info("Saved resume id={} filename={}", saved.getId(), saved.getFileName());
        return saved;
    }

    private String extractText(InputStream in, String filename) throws Exception {
        if (filename == null) filename = "file";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            try (var document = Loader.loadPDF(in.readAllBytes())) {
                return new PDFTextStripper().getText(document);
            }
        } else if (lower.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(in); XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                return ex.getText();
            }
        } else { // fallback to text
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return r.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}




