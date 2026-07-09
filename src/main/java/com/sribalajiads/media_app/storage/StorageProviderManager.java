package com.sribalajiads.media_app.storage;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

@Component
public class StorageProviderManager {

    private final Cloudinary account1;
    private final Cloudinary account2;

    public StorageProviderManager(
            @Qualifier("cloudinaryAccount1") Cloudinary account1,
            @Qualifier("cloudinaryAccount2") Cloudinary account2) {
        this.account1 = account1;
        this.account2 = account2;
    }

    public <T> T executeWithFailover(CloudinaryOperation<T> operation) {
        try {
            return operation.execute(account1, "cloudinary1");
        } catch (Exception primaryEx) {
            System.err.println(">>> cloudinary1 failed [" + primaryEx.getMessage() + "], failing over to cloudinary2");
            try {
                return operation.execute(account2, "cloudinary2");
            } catch (Exception secondaryEx) {
                throw new StorageException("Both Cloudinary accounts failed", secondaryEx);
            }
        }
    }

    public byte[] downloadWithFailover(String publicId) {
        try {
            return fetch(account1, publicId);
        } catch (Exception e1) {
            try {
                return fetch(account2, publicId);
            } catch (Exception e2) {
                throw new StorageException("Could not download " + publicId + " from either account", e2);
            }
        }
    }

    private byte[] fetch(Cloudinary cloudinary, String publicId) throws IOException {
        String url = cloudinary.url().secure(true).generate(publicId);
        try (InputStream in = new URL(url).openStream()) {
            return in.readAllBytes();
        }
    }

    @FunctionalInterface
    public interface CloudinaryOperation<T> {
        T execute(Cloudinary cloudinary, String providerName) throws Exception;
    }
}