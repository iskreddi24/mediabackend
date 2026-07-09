package com.sribalajiads.media_app.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResult {
    private String publicId;
    private String imageUrl;
    private String provider;
}