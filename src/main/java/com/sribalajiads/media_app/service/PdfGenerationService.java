package com.sribalajiads.media_app.service;

import com.sribalajiads.media_app.model.Media;
import com.sribalajiads.media_app.storage.ImageStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfGenerationService {

    private static final String NO_LOGO_OPTION = "NO_LOGO";
    private static final String FOLDER = "media_app";

    // --- LAYOUT CONSTANTS (Widescreen 16:9 Page: 792 x 540 points) ---
    private static final float PAGE_WIDTH = 792;
    private static final float PAGE_HEIGHT = 540;

    private static final float LOGO_X = 20;
    private static final float LOGO_Y = PAGE_HEIGHT - 70;
    private static final float LOGO_MAX_W = 180;
    private static final float LOGO_MAX_H = 60;

    private static final float LOC_X = 220;
    private static final float LOC_Y = PAGE_HEIGHT - 50;
    private static final float LOC_W = PAGE_WIDTH - 240;
    private static final float LOC_H = 30;

    private static final float MAIN_IMG_X = 20;
    private static final float MAIN_IMG_Y = PAGE_HEIGHT - 480;
    private static final float MAIN_IMG_W = PAGE_WIDTH - 40;
    private static final float MAIN_IMG_H = 400;

    private static final float FOOTER_Y = 20;
    private static final float FOOTER_H = 40;

    private final ImageStorageService imageStorageService;

    public PdfGenerationService(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    public ByteArrayInputStream generatePdf(List<Media> mediaList, String companyName) throws IOException {

        try (PDDocument document = new PDDocument()) {
            boolean isNoLogo = NO_LOGO_OPTION.equalsIgnoreCase(companyName);

            // ============================================================
            // 1. WELCOME PAGE
            // ============================================================
            if (companyName != null && !companyName.isBlank() && !isNoLogo) {
                addFullScreenPage(document, companyName + "_WELCOME");
            }

            // ============================================================
            // 2. MEDIA PAGES
            // ============================================================
            for (Media media : mediaList) {
                PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
                document.addPage(page);

                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                    // --- A. COMPANY LOGO ---
                    if (!isNoLogo) {
                        String mediaOwner = media.getBelongsTo().name();
                        byte[] logoBytes = safeDownload(FOLDER + "/" + mediaOwner + "_LOGO");
                        drawImage(document, cs, logoBytes, LOGO_X, LOGO_Y, LOGO_MAX_W, LOGO_MAX_H);
                    }

                    // --- B. LOCATION TEXT ---
                    String locationText = media.getLocation() != null ? media.getLocation().toUpperCase() : "";
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                    cs.setNonStrokingColor(new Color(237, 28, 36));
                    float textWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(locationText) / 1000 * 14;
                    cs.newLineAtOffset(PAGE_WIDTH - 20 - textWidth, LOC_Y);
                    cs.showText(locationText);
                    cs.endText();

                    // --- C. MAIN MEDIA IMAGE ---
                    byte[] mainImgBytes = safeDownloadFromUrl(media.getImageUrl());
                    drawImage(document, cs, mainImgBytes, MAIN_IMG_X, MAIN_IMG_Y, MAIN_IMG_W, MAIN_IMG_H);

                    // --- D. DETAILS ---
                    StringBuilder details = new StringBuilder();
                    if (media.getMediaCode() != null) details.append(media.getMediaCode());
                    if (media.getMediaType() != null) details.append(" | ").append(media.getMediaType());
                    if (media.getSpecifications() != null) details.append(" | ").append(media.getSpecifications());
                    if (media.getIllumination() != null && !media.getIllumination().isBlank()) details.append(" | ").append(media.getIllumination());
                    if (media.getTrafficView() != null && !media.getTrafficView().isBlank()) details.append(" | ").append(media.getTrafficView());
                    if (media.getCity() != null) details.append(" | ").append(media.getCity());
                    if (media.getCoordinates() != null && !media.getCoordinates().isBlank()) details.append(" | ").append(media.getCoordinates());

                    String detailsText = details.toString().toUpperCase();

                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                    cs.setNonStrokingColor(new Color(192, 0, 0));
                    cs.newLineAtOffset(20, FOOTER_Y + 10);
                    cs.showText(detailsText);
                    cs.endText();

                    // --- E. MAP LINK ---
                    String locationUrl = media.getLocationUrl();
                    if (locationUrl != null && !locationUrl.isBlank()) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                        cs.setNonStrokingColor(Color.BLUE);

                        String linkText = "Street View";
                        float linkWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(linkText) / 1000 * 12;

                        cs.newLineAtOffset(PAGE_WIDTH - 20 - linkWidth, FOOTER_Y + 10);
                        cs.showText(linkText);
                        cs.endText();
                    }

                } catch (Exception e) {
                    throw new IOException("Failed to generate PDF", e);
                }
            }

            // ============================================================
            // 3. THANK YOU PAGE
            // ============================================================
            if (companyName != null && !companyName.isBlank() && !isNoLogo) {
                addFullScreenPage(document, companyName + "_THANKYOU");
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return new ByteArrayInputStream(out.toByteArray());
            }
        }
    }

    // ============================================================
    // Cloud download helpers
    // ============================================================

    private byte[] safeDownload(String publicId) {
        try {
            return imageStorageService.download(publicId);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] safeDownloadFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            return imageStorageService.downloadFromUrl(url);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // Page drawing
    // ============================================================

    private void addFullScreenPage(PDDocument document, String baseFileName) {
        byte[] imgBytes = safeDownload(FOLDER + "/" + baseFileName);
        if (imgBytes == null || imgBytes.length == 0) return;

        try {
            PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            document.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawImage(document, cs, imgBytes, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawImage(PDDocument document, PDPageContentStream cs, byte[] imgBytes,
                           float x, float y, float boxW, float boxH) {
        if (imgBytes == null || imgBytes.length == 0) return;

        File tempFile = null;
        try {
            byte[] normalized = normalizeToPng(imgBytes);

            tempFile = File.createTempFile("pdfbox_", ".png");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(normalized);
            }

            PDImageXObject pdImage = PDImageXObject.createFromFile(tempFile.getAbsolutePath(), document);

            // Calculate aspect-ratio-preserving dimensions
            float imgW = pdImage.getWidth();
            float imgH = pdImage.getHeight();
            float ratio = imgW / imgH;
            float newW = boxW;
            float newH = newW / ratio;
            if (newH > boxH) {
                newH = boxH;
                newW = newH * ratio;
            }
            // Center in the box
            float offsetX = x + (boxW - newW) / 2;
            float offsetY = y + (boxH - newH) / 2;

            cs.drawImage(pdImage, offsetX, offsetY, newW, newH);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    // ============================================================
    // Image byte helpers
    // ============================================================

    /**
     * PDImageXObject.createFromFile() needs PDFBox's JPEG/PNG/TIFF-capable
     * readers to recognize the format. Re-encode anything else (e.g. webp)
     * to PNG so it always loads cleanly.
     */
    private byte[] normalizeToPng(byte[] bytes) throws IOException {
        if (isPng(bytes) || isJpeg(bytes)) {
            return bytes;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new IOException("Unsupported image format for PDF embedding");
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            }
        }
    }

    private boolean isPng(byte[] b) {
        return b != null && b.length > 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private boolean isJpeg(byte[] b) {
        return b != null && b.length > 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
    }
}