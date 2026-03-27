package com.pbm5.bugtracker.repository;

import com.pbm5.bugtracker.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for NotificationTemplate entity operations.
 * 
 * Provides data access methods for managing notification templates including:
 * - Finding templates by key and status
 * - Template versioning support
 * - Active template queries
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    // Basic queries
    Optional<NotificationTemplate> findByTemplateKey(String templateKey);

    Optional<NotificationTemplate> findByTemplateKeyAndIsActiveTrue(String templateKey);

    boolean existsByTemplateKey(String templateKey);

    // Active template queries
    List<NotificationTemplate> findByIsActiveTrueOrderByName();

    List<NotificationTemplate> findByIsActiveTrue();

    // Version queries
    @Query("SELECT t FROM NotificationTemplate t WHERE t.templateKey = :templateKey ORDER BY t.version DESC")
    List<NotificationTemplate> findByTemplateKeyOrderByVersionDesc(@Param("templateKey") String templateKey);

    @Query("SELECT MAX(t.version) FROM NotificationTemplate t WHERE t.templateKey = :templateKey")
    Integer findMaxVersionByTemplateKey(@Param("templateKey") String templateKey);

    // Template content validation
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true AND t.htmlTemplate IS NOT NULL AND t.subjectTemplate IS NOT NULL")
    List<NotificationTemplate> findActiveEmailTemplates();

    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true AND t.inAppTemplate IS NOT NULL")
    List<NotificationTemplate> findActiveInAppTemplates();

    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true AND t.toastTemplate IS NOT NULL")
    List<NotificationTemplate> findActiveToastTemplates();

    // Bulk operations
    @Modifying
    @Query("UPDATE NotificationTemplate t SET t.isActive = false WHERE t.templateKey = :templateKey AND t.version < :currentVersion")
    int deactivateOlderVersions(@Param("templateKey") String templateKey,
            @Param("currentVersion") Integer currentVersion);
}
