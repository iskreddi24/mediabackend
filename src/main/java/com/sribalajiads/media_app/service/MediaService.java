package com.sribalajiads.media_app.service;

import com.sribalajiads.media_app.dto.BulkImageUploadResult;
import com.sribalajiads.media_app.exception.ResourceNotFoundException;
import com.sribalajiads.media_app.model.Company;
import com.sribalajiads.media_app.model.Media;
import com.sribalajiads.media_app.repository.MediaRepository;
import com.sribalajiads.media_app.storage.ImageStorageService;
import com.sribalajiads.media_app.storage.UploadResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final ImageStorageService imageStorageService; // replaces FileStorageService
    private final PptGenerationService pptGenerationService;
    private final PdfGenerationService pdfGenerationService;

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");


    @Autowired
    public MediaService(MediaRepository mediaRepository, ImageStorageService imageStorageService,
                        PptGenerationService pptGenerationService, PdfGenerationService pdfGenerationService) {
        this.mediaRepository = mediaRepository;
        this.imageStorageService = imageStorageService;
        this.pptGenerationService = pptGenerationService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @Transactional
    public Media createMedia(String belongsTo, String mediaCode, String location, String city, String specifications, String illumination, String mediaType, MultipartFile imageFile, String trafficView, String locationUrl, String coordinates) {

        UploadResult uploadResult = imageStorageService.upload(imageFile, mediaCode);

        Media newMedia = new Media();
        newMedia.setBelongsTo(Company.valueOf(belongsTo.toUpperCase()));
        newMedia.setMediaCode(mediaCode);
        newMedia.setLocation(location);
        newMedia.setTrafficView(trafficView);
        newMedia.setCity(city);
        newMedia.setSpecifications(specifications);
        newMedia.setIllumination(illumination);
        newMedia.setMediaType(mediaType);
        newMedia.setImageUrl(uploadResult.getImageUrl());
        newMedia.setPublicId(uploadResult.getPublicId());
        newMedia.setStorageProvider(uploadResult.getProvider());
        newMedia.setImagePath(uploadResult.getPublicId()); // backward compat
        newMedia.setLocationUrl(locationUrl);
        newMedia.setCoordinates(coordinates);

        return mediaRepository.save(newMedia);
    }

    public Page<Media> getMedia(String company, String mediaType, String query, Pageable pageable) {
        final String searchQuery = (query == null) ? "" : query;
        boolean hasCompany = company != null && !company.isBlank();
        boolean hasMediaType = mediaType != null && !mediaType.isBlank() && !mediaType.equalsIgnoreCase("All");

        if (hasCompany) {
            Company companyEnum = Company.valueOf(company.toUpperCase());
            if (hasMediaType) {
                return mediaRepository.findByBelongsToAndMediaTypeIgnoreCaseAndMediaCodeContainingIgnoreCase(companyEnum, mediaType, searchQuery, pageable);
            } else {
                return mediaRepository.findByBelongsToAndMediaCodeContainingIgnoreCase(companyEnum, searchQuery, pageable);
            }
        } else {
            if (hasMediaType) {
                return mediaRepository.findByMediaTypeIgnoreCaseAndMediaCodeContainingIgnoreCase(mediaType, searchQuery, pageable);
            } else {
                return mediaRepository.findByMediaCodeContainingIgnoreCase(searchQuery, pageable);
            }
        }
    }

    public ByteArrayInputStream generatePresentationForMedia(List<Long> mediaIds, String companyName) throws IOException {
        List<Media> fetchedMedia = mediaRepository.findAllById(mediaIds);
        Map<Long, Media> mediaMap = fetchedMedia.stream()
                .collect(Collectors.toMap(Media::getId, Function.identity()));

        List<Media> orderedMedia = new ArrayList<>();
        for (Long id : mediaIds) {
            if (mediaMap.containsKey(id)) {
                orderedMedia.add(mediaMap.get(id));
            }
        }

        return pptGenerationService.generatePpt(orderedMedia, companyName);
    }

    public ByteArrayInputStream generatePdfForMedia(List<Long> mediaIds, String companyName) throws IOException {
        List<Media> fetchedMedia = mediaRepository.findAllById(mediaIds);
        Map<Long, Media> mediaMap = fetchedMedia.stream()
                .collect(Collectors.toMap(Media::getId, Function.identity()));

        List<Media> orderedMedia = new ArrayList<>();
        for (Long id : mediaIds) {
            if (mediaMap.containsKey(id)) {
                orderedMedia.add(mediaMap.get(id));
            }
        }

        return pdfGenerationService.generatePdf(orderedMedia, companyName);
    }

    public Media getMediaById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + id));
    }

    @Transactional
    public Media updateMedia(
            Long id,
            String belongsTo,
            String mediaCode,
            String location,
            String city,
            String specifications,
            String illumination,
            String mediaType,
            String trafficView,
            MultipartFile imageFile,
            String locationUrl,
            String coordinates
    ) {
        Media mediaToUpdate = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + id));

        String oldPublicId = mediaToUpdate.getPublicId();
        boolean mediaCodeChanged = !mediaCode.equals(mediaToUpdate.getMediaCode());

        if (imageFile != null && !imageFile.isEmpty()) {
            UploadResult result = imageStorageService.upload(imageFile, mediaCode);
            if (mediaCodeChanged && oldPublicId != null && !oldPublicId.equals(result.getPublicId())) {
                imageStorageService.delete(oldPublicId);
            }
            mediaToUpdate.setImageUrl(result.getImageUrl());
            mediaToUpdate.setPublicId(result.getPublicId());
            mediaToUpdate.setStorageProvider(result.getProvider());
            mediaToUpdate.setImagePath(result.getPublicId());
        } else if (mediaCodeChanged && oldPublicId != null && !oldPublicId.isBlank()) {
            UploadResult result = imageStorageService.rename(oldPublicId, mediaCode);
            mediaToUpdate.setImageUrl(result.getImageUrl());
            mediaToUpdate.setPublicId(result.getPublicId());
            mediaToUpdate.setStorageProvider(result.getProvider());
            mediaToUpdate.setImagePath(result.getPublicId());
        }

        mediaToUpdate.setBelongsTo(Company.valueOf(belongsTo.toUpperCase()));
        mediaToUpdate.setMediaCode(mediaCode);
        mediaToUpdate.setLocation(location);
        mediaToUpdate.setCity(city);
        mediaToUpdate.setSpecifications(specifications);
        mediaToUpdate.setIllumination(illumination);
        mediaToUpdate.setMediaType(mediaType);
        mediaToUpdate.setTrafficView(trafficView);
        mediaToUpdate.setLocationUrl(locationUrl);
        mediaToUpdate.setCoordinates(coordinates);

        return mediaRepository.save(mediaToUpdate);
    }

    public ByteArrayInputStream generateBulkPresentation(List<String> codes, String companyName) throws IOException {
        List<Media> fetched = mediaRepository.findByMediaCodeIn(codes);
        Map<String, Media> map = fetched.stream()
                .collect(Collectors.toMap(Media::getMediaCode, m -> m));

        List<Media> ordered = new ArrayList<>();
        for (String code : codes) {
            if (map.containsKey(code)) {
                ordered.add(map.get(code));
            }
        }

        return pptGenerationService.generatePpt(ordered, companyName);
    }

    public ByteArrayInputStream generateBulkPdf(List<String> codes, String companyName) throws IOException {
        List<Media> fetched = mediaRepository.findByMediaCodeIn(codes);
        Map<String, Media> map = fetched.stream()
                .collect(Collectors.toMap(Media::getMediaCode, m -> m));

        List<Media> ordered = new ArrayList<>();
        for (String code : codes) {
            if (map.containsKey(code)) {
                ordered.add(map.get(code));
            }
        }

        return pdfGenerationService.generatePdf(ordered, companyName);
    }

    @Transactional
    public void deleteMedia(Long id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + id));
        if (media.getPublicId() != null && !media.getPublicId().isBlank()) {
            imageStorageService.delete(media.getPublicId());
        }
        mediaRepository.deleteById(id);
    }

    @Transactional
    public BulkImageUploadResult processBulkImageZip(MultipartFile zipFile) throws IOException {
        BulkImageUploadResult result = new BulkImageUploadResult();

        // Pre-load all media into a map for case-insensitive lookup
        java.util.Map<String, Media> mediaByCodeUpper = new java.util.HashMap<>();
        for (Media m : mediaRepository.findAll()) {
            if (m.getMediaCode() != null) {
                mediaByCodeUpper.put(m.getMediaCode().trim().toUpperCase(), m);
            }
        }

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                // Get filename without folder prefix
                String fullName = entry.getName();
                String fileName = fullName.contains("/") ? fullName.substring(fullName.lastIndexOf('/') + 1) : fullName;

                // Get file extension
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    extension = fileName.substring(dotIndex + 1).toLowerCase();
                }

                // Skip non-image files
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                    result.getSkipped().add(fileName);
                    zis.closeEntry();
                    continue;
                }

                // Extract media code (filename without extension)
                String candidateCode = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;

                // Look up in DB (case-insensitive comparison via uppercasing)
                String normalizedCode = candidateCode.trim().toUpperCase();

                // Match case-insensitively using pre-loaded map
                Media matchedMedia = mediaByCodeUpper.get(normalizedCode);

                if (matchedMedia != null) {
                    try {
                        // Read the entry bytes
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            baos.write(buffer, 0, len);
                        }
                        byte[] imageBytes = baos.toByteArray();

                        String oldPublicId = matchedMedia.getPublicId();
                        UploadResult uploadResult = imageStorageService.upload(imageBytes, matchedMedia.getMediaCode(), extension);

                        if (oldPublicId != null && !oldPublicId.equals(uploadResult.getPublicId())) {
                            imageStorageService.delete(oldPublicId);
                        }

                        matchedMedia.setImageUrl(uploadResult.getImageUrl());
                        matchedMedia.setPublicId(uploadResult.getPublicId());
                        matchedMedia.setStorageProvider(uploadResult.getProvider());
                        matchedMedia.setImagePath(uploadResult.getPublicId());
                        mediaRepository.save(matchedMedia);

                        result.getMatched().add(matchedMedia.getMediaCode());
                    } catch (Exception e) {
                        result.getSkipped().add(fileName + " (error: " + e.getMessage() + ")");
                    }
                } else {
                    result.getUnmatched().add(fileName);
                }

                zis.closeEntry();
            }
        }

        return result;
    }
}
