package com.blog.service;

import com.blog.exception.ValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfExtractorService {

    /**
     * Extracts plain text from a PDF multipart file.
     *
     * @param file the uploaded PDF file
     * @return the stripped text, or {@code ""} if the document is blank
     * @throws ValidationException if the file's content type is not {@code application/pdf} or if PDF parsing fails
     */
    public String extract(MultipartFile file) {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new ValidationException("Only PDF files are accepted. Received: " + file.getContentType());
        }

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");
            stripper.setParagraphStart("\n");

            String text = stripper.getText(document);
            String stripped = text.strip();
            return stripped.isBlank() ? "" : stripped;
        } catch (IOException ex) {
            throw new ValidationException("Failed to extract text from PDF: " + ex.getMessage());
        }
    }
}
