package com.sribalajiads.media_app.service;

import com.sribalajiads.media_app.model.Media;
import com.sribalajiads.media_app.storage.ImageStorageService;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.VerticalAlignment;
import org.apache.poi.xslf.usermodel.*;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PptGenerationService {

    private static final String NO_LOGO_OPTION = "NO_LOGO";
    private static final String FOLDER = "media_app";

    private static final int SLIDE_WIDTH = 960;
    private static final int SLIDE_HEIGHT = 540;

    private static final double LOGO_X = 20;
    private static final double LOGO_Y = 10;
    private static final double LOGO_MAX_W = 180;
    private static final double LOGO_MAX_H = 60;

    private static final double LOC_X = 220;
    private static final double LOC_Y = 20;
    private static final double LOC_W = 720;
    private static final double LOC_H = 30;

    private static final double MAIN_IMG_X = 20;
    private static final double MAIN_IMG_Y = 80;
    private static final double MAIN_IMG_W = 920;
    private static final double MAIN_IMG_H = 400;

    private static final double FOOTER_Y = 490;
    private static final double FOOTER_H = 40;

    private final ImageStorageService imageStorageService;

    public PptGenerationService(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    public ByteArrayInputStream generatePpt(List<Media> mediaList, String companyName) throws IOException {

        try (XMLSlideShow ppt = new XMLSlideShow()) {
            ppt.setPageSize(new Dimension(SLIDE_WIDTH, SLIDE_HEIGHT));

            boolean isNoLogo = NO_LOGO_OPTION.equalsIgnoreCase(companyName);

            if (companyName != null && !companyName.isBlank() && !isNoLogo) {
                addFullScreenSlide(ppt, companyName + "_WELCOME");
            }

            for (Media media : mediaList) {
                XSLFSlide slide = ppt.createSlide();

                if (!isNoLogo) {
                    String mediaOwner = media.getBelongsTo().name();
                    byte[] logoBytes = safeDownload(FOLDER + "/" + mediaOwner + "_LOGO");
                    addImageToSlide(ppt, slide, logoBytes, LOGO_X, LOGO_Y, LOGO_MAX_W, LOGO_MAX_H, "Logo Not Available");
                }

                XSLFTextBox locBox = slide.createTextBox();
                locBox.setAnchor(new Rectangle2D.Double(LOC_X, LOC_Y, LOC_W, LOC_H));
                XSLFTextParagraph locPara = locBox.addNewTextParagraph();
                locPara.setTextAlign(TextParagraph.TextAlign.RIGHT);
                XSLFTextRun locRun = locPara.addNewTextRun();
                locRun.setText(media.getLocation().toUpperCase());
                locRun.setFontColor(new Color(237, 28, 36));
                locRun.setFontSize(14.0);
                locRun.setBold(true);

                byte[] mainImgBytes = safeDownloadFromUrl(media.getImageUrl());
                addImageToSlide(ppt, slide, mainImgBytes, MAIN_IMG_X, MAIN_IMG_Y, MAIN_IMG_W, MAIN_IMG_H, "Image Not Found");

                XSLFTextBox detailsBox = slide.createTextBox();
                detailsBox.setAnchor(new Rectangle2D.Double(20, FOOTER_Y, SLIDE_WIDTH * 0.75, FOOTER_H));
                XSLFTextParagraph detailsPara = detailsBox.addNewTextParagraph();
                detailsPara.setTextAlign(TextParagraph.TextAlign.LEFT);

                java.util.StringJoiner joiner = new java.util.StringJoiner(" | ");
                if (media.getMediaCode() != null) joiner.add(media.getMediaCode());
                if (media.getMediaType() != null) joiner.add(media.getMediaType());
                if (media.getSpecifications() != null) joiner.add(media.getSpecifications());
                if (media.getIllumination() != null && !media.getIllumination().isBlank()) joiner.add(media.getIllumination());
                if (media.getTrafficView() != null && !media.getTrafficView().isBlank()) joiner.add(media.getTrafficView());
                if (media.getCity() != null) joiner.add(media.getCity());
                if (media.getCoordinates() != null && !media.getCoordinates().isBlank()) joiner.add(media.getCoordinates());

                XSLFTextRun detailsRun = detailsPara.addNewTextRun();
                detailsRun.setText(joiner.toString().toUpperCase());
                detailsRun.setFontColor(new Color(192, 0, 0));
                detailsRun.setFontSize(12.0);
                detailsRun.setBold(true);

                String locationUrl = media.getLocationUrl();
                if (locationUrl != null && !locationUrl.isBlank()) {
                    XSLFTextBox linkBox = slide.createTextBox();
                    linkBox.setAnchor(new Rectangle2D.Double(SLIDE_WIDTH - 200, FOOTER_Y, 180, FOOTER_H));
                    XSLFTextParagraph linkPara = linkBox.addNewTextParagraph();
                    linkPara.setTextAlign(TextParagraph.TextAlign.RIGHT);
                    XSLFTextRun linkRun = linkPara.addNewTextRun();
                    linkRun.setText("\uD83D\uDDFA Street View");
                    linkRun.setFontColor(Color.BLUE);
                    linkRun.setUnderlined(true);
                    linkRun.setFontSize(12.0);
                    XSLFHyperlink link = linkRun.createHyperlink();
                    link.setAddress(locationUrl);
                }
            }

            if (companyName != null && !companyName.isBlank() && !isNoLogo) {
                addFullScreenSlide(ppt, companyName + "_THANKYOU");
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ppt.write(out);
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
    // Slide drawing
    // ============================================================

    private void addImageToSlide(XMLSlideShow ppt, XSLFSlide slide, byte[] imgBytes, double x, double y, double w, double h, String errorMsg) {
        if (imgBytes != null && imgBytes.length > 0) {
            try {
                byte[] normalized = normalizeToPngOrJpeg(imgBytes);
                PictureData.PictureType type = detectPictureType(normalized);
                PictureData pd = ppt.addPicture(normalized, type);
                XSLFPictureShape pic = slide.createPicture(pd);
                resizePictureToFit(pic, w, h, x, y);
            } catch (Exception e) {
                e.printStackTrace();
                drawErrorPlaceholder(slide, errorMsg + " (Error)", x, y, w, h);
            }
        } else {
            drawErrorPlaceholder(slide, errorMsg, x, y, w, h);
        }
    }

    private void addFullScreenSlide(XMLSlideShow ppt, String baseFileName) {
        byte[] imgBytes = safeDownload(FOLDER + "/" + baseFileName);
        if (imgBytes == null || imgBytes.length == 0) return;

        try {
            XSLFSlide slide = ppt.createSlide();
            byte[] normalized = normalizeToPngOrJpeg(imgBytes);
            PictureData.PictureType type = detectPictureType(normalized);
            PictureData pd = ppt.addPicture(normalized, type);
            XSLFPictureShape pic = slide.createPicture(pd);
            pic.setAnchor(new Rectangle2D.Double(0, 0, SLIDE_WIDTH, SLIDE_HEIGHT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // Image byte helpers
    // ============================================================

    /**
     * POI's XSLFPictureShape only reliably supports PNG/JPEG. If Cloudinary
     * ever hands back webp/other bytes, decode + re-encode as PNG.
     */
    private byte[] normalizeToPngOrJpeg(byte[] bytes) throws IOException {
        if (isPng(bytes) || isJpeg(bytes)) {
            return bytes;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null) {
                throw new IOException("Unsupported image format for slide picture");
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                return baos.toByteArray();
            }
        }
    }

    private PictureData.PictureType detectPictureType(byte[] bytes) {
        return isPng(bytes) ? PictureData.PictureType.PNG : PictureData.PictureType.JPEG;
    }

    private boolean isPng(byte[] b) {
        return b != null && b.length > 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    private boolean isJpeg(byte[] b) {
        return b != null && b.length > 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8;
    }

    // ============================================================
    // Layout helpers
    // ============================================================

    private void resizePictureToFit(XSLFPictureShape shape, double boxW, double boxH, double x, double y) {
        Dimension dim = shape.getPictureData().getImageDimension();
        double origW = dim.getWidth();
        double origH = dim.getHeight();
        double ratio = origW / origH;
        double newW = boxW;
        double newH = newW / ratio;
        if (newH > boxH) {
            newH = boxH;
            newW = newH * ratio;
        }
        double offsetX = x + (boxW - newW) / 2;
        double offsetY = y + (boxH - newH) / 2;
        shape.setAnchor(new Rectangle2D.Double(offsetX, offsetY, newW, newH));
    }

    private void drawErrorPlaceholder(XSLFSlide slide, String errorMessage, double x, double y, double w, double h) {
        XSLFTextBox errorBox = slide.createTextBox();
        errorBox.setAnchor(new Rectangle2D.Double(x, y, w, h));
        errorBox.setFillColor(Color.LIGHT_GRAY);
        errorBox.setVerticalAlignment(VerticalAlignment.MIDDLE);
        XSLFTextParagraph p = errorBox.addNewTextParagraph();
        p.setTextAlign(TextParagraph.TextAlign.CENTER);
        XSLFTextRun r = p.addNewTextRun();
        r.setText(errorMessage);
        r.setFontColor(Color.RED);
        r.setFontSize(14.0);
        r.setBold(true);
    }
}