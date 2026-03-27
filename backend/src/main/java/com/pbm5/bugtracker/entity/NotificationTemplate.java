package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing notification templates for different event types.
 * 
 * This entity stores templates for:
 * - Email notifications (subject, HTML, text)
 * - In-app notifications (short format)
 * - Toast notifications (brief format)
 * 
 * Templates support variable substitution and versioning.
 */
@Entity
@Table(name = "notification_templates")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "template_key", nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Template content for different channels
    @Column(name = "subject_template", columnDefinition = "TEXT")
    private String subjectTemplate;

    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate;

    @Column(name = "text_template", columnDefinition = "TEXT")
    private String textTemplate;

    @Column(name = "in_app_template", columnDefinition = "TEXT")
    private String inAppTemplate;

    @Column(name = "toast_template", columnDefinition = "TEXT")
    private String toastTemplate;

    // Template variables (JSON schema)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String variables;

    // Status and versioning
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "NotificationTemplate{" +
                "templateId=" + templateId +
                ", templateKey='" + templateKey + '\'' +
                ", name='" + name + '\'' +
                ", version=" + version +
                ", isActive=" + isActive +
                '}';
    }
}
