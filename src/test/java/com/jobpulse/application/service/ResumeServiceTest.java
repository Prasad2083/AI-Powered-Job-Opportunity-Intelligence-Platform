package com.jobpulse.application.service;

import com.jobpulse.domain.entity.Resume;
import com.jobpulse.domain.repository.ResumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumeServiceTest {
    private Resume parse(String filename, byte[] data) throws Exception {
        var repository=mock(ResumeRepository.class);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
        return new ResumeService(repository).storeResume(new MockMultipartFile("file",filename,"application/octet-stream",data),42L);
    }
    @Test void extractsPdfAndPreservesOriginalBytes() throws Exception {
        try (var document=new org.apache.pdfbox.pdmodel.PDDocument(); var bytes=new ByteArrayOutputStream()) {
            var page=new org.apache.pdfbox.pdmodel.PDPage(); document.addPage(page);
            try(var stream=new org.apache.pdfbox.pdmodel.PDPageContentStream(document,page)) {
                stream.beginText(); stream.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),12);
                stream.newLineAtOffset(50,700); stream.showText("Java Quality Engineer"); stream.endText();
            }
            document.save(bytes);
            var resume=parse("cv.pdf",bytes.toByteArray());
            assertTrue(resume.getContentText().contains("Java Quality Engineer"));
            assertArrayEquals(bytes.toByteArray(),resume.getOriginalFile());
        }
    }
    @Test void extractsDocx() throws Exception {
        try(var document=new org.apache.poi.xwpf.usermodel.XWPFDocument();var bytes=new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("SDET Java Selenium");document.write(bytes);
            assertTrue(parse("cv.docx",bytes.toByteArray()).getContentText().contains("SDET Java Selenium"));
        }
    }
    @Test void rejectsInvalidFiles() {
        assertThrows(IllegalArgumentException.class,()->parse("cv.pdf","not a PDF".getBytes()));
        assertThrows(IllegalArgumentException.class,()->parse("cv.exe","binary".getBytes()));
        assertThrows(IllegalArgumentException.class,()->parse("cv.txt",new byte[0]));
    }
}
