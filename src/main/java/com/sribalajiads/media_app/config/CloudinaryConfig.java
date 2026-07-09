package com.sribalajiads.media_app.config;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary1.cloud-name}")
    private String cloud1Name;
    @Value("${cloudinary1.api-key}")
    private String cloud1Key;
    @Value("${cloudinary1.api-secret}")
    private String cloud1Secret;

    @Value("${cloudinary2.cloud-name}")
    private String cloud2Name;
    @Value("${cloudinary2.api-key}")
    private String cloud2Key;
    @Value("${cloudinary2.api-secret}")
    private String cloud2Secret;

    @Bean(name = "cloudinaryAccount1")
    public Cloudinary cloudinaryAccount1() {
        return buildClient(cloud1Name, cloud1Key, cloud1Secret);
    }

    @Bean(name = "cloudinaryAccount2")
    public Cloudinary cloudinaryAccount2() {
        return buildClient(cloud2Name, cloud2Key, cloud2Secret);
    }

    private Cloudinary buildClient(String cloudName, String apiKey, String apiSecret) {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", "true");
        return new Cloudinary(config);
    }
}