package com.sribalajiads.media_app.storage;

import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Service
public class CloudinaryStorageService implements ImageStorageService {

    private static final String FOLDER = "media_app";

    private final StorageProviderManager providerManager;

    public CloudinaryStorageService(StorageProviderManager providerManager) {
        this.providerManager = providerManager;
    }

    @Override
    public UploadResult upload(MultipartFile file, String mediaCode) {
        try {
            return upload(file.getBytes(), mediaCode, extractExtension(file.getOriginalFilename()));
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file for " + mediaCode, e);
        }
    }

    @Override
    public UploadResult upload(byte[] data, String mediaCode, String extension) {
        if (data == null || data.length == 0) {
            throw new StorageException("Cannot upload empty file for " + mediaCode);
        }

        return providerManager.executeWithFailover((cloudinary, providerName) -> {
            Map<String, Object> options = ObjectUtils.asMap(
                    "public_id", mediaCode,
                    "folder", FOLDER,
                    "overwrite", true,
                    "invalidate", true,
                    "resource_type", "image"
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = cloudinary.uploader().upload(data, options);

            return new UploadResult(
                    (String) response.get("public_id"),
                    (String) response.get("secure_url"),
                    providerName
            );
        });
    }

    @Override
    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        providerManager.executeWithFailover((cloudinary, providerName) ->
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image")));
    }

    @Override
    public UploadResult rename(String oldPublicId, String newPublicId) {
        return providerManager.executeWithFailover((cloudinary, providerName) -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = cloudinary.uploader().rename(
                    oldPublicId, newPublicId, ObjectUtils.asMap("overwrite", true));

            return new UploadResult(
                    (String) response.get("public_id"),
                    (String) response.get("secure_url"),
                    providerName
            );
        });
    }

    @Override
    public byte[] download(String publicId) {
        return providerManager.downloadWithFailover(publicId);
    }

    @Override
    public byte[] downloadFromUrl(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new StorageException("Failed to download from URL: " + imageUrl, e);
        }
    }

    @Override
    public boolean exists(String publicId) {
        try {
            return providerManager.executeWithFailover((cloudinary, providerName) ->
                    cloudinary.api().resource(publicId, ObjectUtils.emptyMap()) != null);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return "jpg";
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? "jpg" : filename.substring(idx + 1);
    }
}