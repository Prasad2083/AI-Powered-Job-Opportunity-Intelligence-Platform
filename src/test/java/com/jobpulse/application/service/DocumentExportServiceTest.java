package com.jobpulse.application.service;

import org.junit.jupiter.api.Test;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;

class DocumentExportServiceTest {
    @Test void wordRoundTripPreservesUnicodeAndOriginalProfessionalFacts() throws Exception {
        String text="Zoë García — Toronto\nJava & Selenium • API testing\nCOMPLETE ORIGINAL RESUME\nExample employer, 2020–2025\nBuilt automated tests.";
        byte[] result=new DocumentExportService().docx(text);
        try(var document=new XWPFDocument(new ByteArrayInputStream(result)); var extractor=new XWPFWordExtractor(document)) {
            assertEquals(text,extractor.getText().trim());
        }
    }
    @Test void rejectsEmptyDocuments() {
        assertThrows(IllegalArgumentException.class,()->new DocumentExportService().docx("  "));
    }
}
