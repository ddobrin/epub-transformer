package com.epubconverter;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class EpubToPdfConverter {
    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 12;
    private static final float LEADING = 1.5f * FONT_SIZE;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java EpubToPdfConverter <input.epub> <output.pdf>");
            System.exit(1);
        }

        try {
            convertEpubToPdf(args[0], args[1]);
            System.out.println("Conversion completed successfully!");
        } catch (Exception e) {
            System.err.println("Error converting file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void convertEpubToPdf(String epubPath, String pdfPath) throws IOException {
        // Create EPUB reader
        EpubReader epubReader = new EpubReader();
        Book book = epubReader.readEpub(new FileInputStream(epubPath));

        // Create PDF document
        PDDocument document = new PDDocument();

        try {
            // Add title page
            addTitlePage(document, book.getTitle());

            // Process each chapter/spine item
            List<SpineReference> spineReferences = book.getSpine().getSpineReferences();
            AtomicInteger pageNumber = new AtomicInteger(1);

            for (SpineReference spineRef : spineReferences) {
                try {
                    Resource resource = spineRef.getResource();
                    String html = new String(resource.getData(), StandardCharsets.UTF_8);
                    String text = Jsoup.parse(html).text();
                    addTextContent(document, text, pageNumber);
                } catch (IOException e) {
                    throw new RuntimeException("Error processing chapter", e);
                }
            }

            // Save the PDF
            document.save(pdfPath);
        } finally {
            document.close();
        }
    }

    private static void addTitlePage(PDDocument document, String title) throws IOException {
        PDPage titlePage = new PDPage();
        document.addPage(titlePage);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, titlePage)) {
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
            contentStream.newLineAtOffset(MARGIN, titlePage.getMediaBox().getHeight() - MARGIN);
            contentStream.showText(title != null ? title : "Untitled");
            contentStream.endText();
        }
    }

    private static void addTextContent(PDDocument document, String text, AtomicInteger pageNumber) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        PDPage currentPage = new PDPage();
        document.addPage(currentPage);
        PDPageContentStream contentStream = null;
        float yPosition = currentPage.getMediaBox().getHeight() - MARGIN;

        try {
            contentStream = new PDPageContentStream(document, currentPage);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
            contentStream.newLineAtOffset(MARGIN, yPosition);

            String[] words = text.split("\\s+");
            StringBuilder currentLine = new StringBuilder();
            float width = currentPage.getMediaBox().getWidth() - 2 * MARGIN;

            for (String word : words) {
                if (currentLine.length() + word.length() + 1 <= getLineCapacity(width)) {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                } else {
                    // Write current line
                    contentStream.showText(currentLine.toString());
                    yPosition -= LEADING;

                    // Check if we need a new page
                    if (yPosition < MARGIN) {
                        // End current page
                        contentStream.endText();
                        contentStream.close();

                        // Create new page
                        currentPage = new PDPage();
                        document.addPage(currentPage);
                        yPosition = currentPage.getMediaBox().getHeight() - MARGIN;

                        // Start new content stream
                        contentStream = new PDPageContentStream(document, currentPage);
                        contentStream.beginText();
                        contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                        contentStream.newLineAtOffset(MARGIN, yPosition);
                    } else {
                        contentStream.newLineAtOffset(0, -LEADING);
                    }

                    // Start new line with current word
                    currentLine = new StringBuilder(word);
                }
            }

            // Write any remaining text
            if (currentLine.length() > 0) {
                contentStream.showText(currentLine.toString());
            }

            contentStream.endText();
        } finally {
            if (contentStream != null) {
                contentStream.close();
            }
            pageNumber.incrementAndGet();
        }
    }

    private static int getLineCapacity(float width) {
        // Approximate characters that can fit in one line
        return (int) (width / (FONT_SIZE * 0.5));
    }
}