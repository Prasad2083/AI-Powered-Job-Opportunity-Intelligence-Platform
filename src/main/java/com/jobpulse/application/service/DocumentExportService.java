package com.jobpulse.application.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

/** Format saved text without adding, inferring or rewriting professional facts. */
@Service
public class DocumentExportService {
    public byte[] docx(String content) throws IOException {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("Document content is required");
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            document.getProperties().getCoreProperties().setTitle("JobPulse resume draft");
            var section = document.getDocument().getBody().addNewSectPr();
            var size = section.addNewPgSz();
            size.setW(BigInteger.valueOf(12240)); size.setH(BigInteger.valueOf(15840));
            var margins = section.addNewPgMar();
            margins.setTop(BigInteger.valueOf(1000)); margins.setBottom(BigInteger.valueOf(1000));
            margins.setLeft(BigInteger.valueOf(1100)); margins.setRight(BigInteger.valueOf(1100));
            for (String line : content.split("\\R", -1)) {
                var paragraph = document.createParagraph();
                paragraph.setSpacingAfter(90); paragraph.setSpacingBetween(1.15);
                var run = paragraph.createRun();
                run.setFontFamily("Calibri"); run.setFontSize(11);
                if (line.equals("RELEVANT EXCERPTS FROM ORIGINAL RESUME") || line.equals("COMPLETE ORIGINAL RESUME")) {
                    run.setBold(true); run.setFontSize(12); paragraph.setSpacingBefore(180);
                }
                // XML-invalid control characters cannot be represented in a DOCX.
                run.setText(line.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", ""));
            }
            document.write(output);
            return output.toByteArray();
        }
    }
}
