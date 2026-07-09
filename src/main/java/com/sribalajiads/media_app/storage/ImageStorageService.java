package com.sribalajiads.media_app.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    UploadResult upload(MultipartFile file, String mediaCode);

    UploadResult upload(byte[] data, String mediaCode, String extension);

    void delete(String publicId);

    UploadResult rename(String oldPublicId, String newPublicId);

    byte[] download(String publicId);

    byte[] downloadFromUrl(String imageUrl);

    boolean exists(String publicId);
}