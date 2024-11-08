package com.epubconverter;

import nl.siegmann.epublib.domain.*;
import nl.siegmann.epublib.epub.EpubReader;
import org.jsoup.Jsoup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EpubToTextConverter {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java EpubToTextConverter <input.epub> <output.txt>");
            System.exit(1);
        }

        try {
            convertEpubToText(args[0], args[1]);
            System.out.println("Conversion completed successfully!");
        } catch (Exception e) {
            System.err.println("Error converting file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void convertEpubToText(String epubPath, String textPath) throws IOException {
        // Create EPUB reader
        EpubReader epubReader = new EpubReader();
        Book book = epubReader.readEpub(new FileInputStream(epubPath));

        // Create output file with UTF-8 encoding
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(textPath), StandardCharsets.UTF_8))) {

            // Write book title
            String title = book.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                writer.write(title);
                writer.newLine();
                writer.write("=".repeat(title.length()));
                writer.newLine();
                writer.newLine();
            }

            // Write author information
            List<Author> authors = book.getMetadata().getAuthors();
            if (!authors.isEmpty()) {
                writer.write("Author(s): ");
                writer.write(authors.stream()
                        .map(author -> author.getFirstname() + " " + author.getLastname())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Unknown"));
                writer.newLine();
                writer.newLine();
            }

            // Process table of contents and content
            List<SpineReference> spineReferences = book.getSpine().getSpineReferences();
            TableOfContents toc = book.getTableOfContents();

            // Write table of contents if available
            if (!toc.getTocReferences().isEmpty()) {
                writer.write("Table of Contents");
                writer.newLine();
                writer.write("=================");
                writer.newLine();
                writeTocEntry(toc.getTocReferences(), writer, 0);
                writer.newLine();
                writer.newLine();
            }

            // Process each chapter
            for (SpineReference spineRef : spineReferences) {
                Resource resource = spineRef.getResource();

                // Skip non-text resources
                if (!resource.getMediaType().toString().contains("html")) {
                    continue;
                }

                String html = new String(resource.getData(), StandardCharsets.UTF_8);
                String text = Jsoup.parse(html).text();

                if (!text.trim().isEmpty()) {
                    writer.write(text.trim());
                    writer.newLine();
                    writer.newLine();
                    // Add separator between chapters
                    writer.write("-".repeat(80));
                    writer.newLine();
                    writer.newLine();
                }
            }
        }
    }

    private static void writeTocEntry(List<TOCReference> tocReferences, BufferedWriter writer, int depth)
            throws IOException {
        String indent = "  ".repeat(depth);

        for (TOCReference tocReference : tocReferences) {
            writer.write(indent + tocReference.getTitle());
            writer.newLine();

            // Recursively process nested entries
            if (!tocReference.getChildren().isEmpty()) {
                writeTocEntry(tocReference.getChildren(), writer, depth + 1);
            }
        }
    }
}