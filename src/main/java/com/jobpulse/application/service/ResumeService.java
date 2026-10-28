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
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
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

    @Transactional
    public Resume storeResume(MultipartFile file, Long userId) throws Exception {
        if (userId == null) throw new IllegalArgumentException("Authenticated user required");
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || file.getSize() > 10_000_000) throw new IllegalArgumentException("Invalid resume size");
        if (filename != null && filename.length()>255) throw new IllegalArgumentException("Resume filename is too long");
        if (filename == null || !(filename.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf") || filename.toLowerCase(java.util.Locale.ROOT).endsWith(".docx") || filename.toLowerCase(java.util.Locale.ROOT).endsWith(".txt"))) throw new IllegalArgumentException("Only PDF, DOCX, and TXT are supported");
        byte[] original = file.getBytes();
        String text;
        try (InputStream input = new java.io.ByteArrayInputStream(original)) {
            text = extractText(input, filename);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read resume. Upload a valid, unencrypted PDF, DOCX, or UTF-8 TXT file.");
        }
        if (text.length()>100000 || text.indexOf(0)>=0) throw new IllegalArgumentException("Resume text must be at most 100000 characters and contain no binary data");
        if (text.isBlank()) throw new IllegalArgumentException("No readable text in resume");
        Resume r = new Resume();
        for (Resume existing : resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (Boolean.TRUE.equals(existing.getIsPrimary())) {
                existing.setIsPrimary(false);
                resumeRepository.save(existing);
            }
        }
        r.setUserId(userId);
        r.setOriginalFile(original);
        r.setFilePath("database://parsed-resumes/" + UUID.randomUUID());
        r.setFileSize(file.getSize());
        r.setFileName(filename == null ? "upload" : filename);
        r.setContentText(text);
        r.setIsPrimary(true);
        Resume saved = resumeRepository.save(r);
        log.info("Saved resume id={}", saved.getId());
        return saved;
    }

    private String extractText(InputStream in, String filename) throws Exception {
        if (filename == null) filename = "file";
        String lower = filename.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            try (var document = Loader.loadPDF(in.readAllBytes())) {
                return new PDFTextStripper().getText(document);
            }
        } else if (lower.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(in); XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                return ex.getText();
            }
        } else { // fallback to text
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)))) {
                return r.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}




