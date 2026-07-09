package com.sribalajiads.media_app.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "media")
public class Media {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "belongs_to", nullable = false)
    private Company belongsTo;

    @Column(name = "media_code", nullable = false, unique = true)
    private String mediaCode;

    @Column(nullable = false)
    private String location;

    @Column(name = "traffic_view")
    private String trafficView;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String specifications;

    @Column
    private String illumination;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "image_path")
    private String imagePath;
    // ADD these three
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "public_id")
    private String publicId;

    @Column(name = "storage_provider")
    private String storageProvider;
    @Column(name = "location_url")
    private String locationUrl;

    @Column(name = "coordinates")
    private String coordinates;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
