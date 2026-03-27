package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.BugLabel;
import com.pbm5.bugtracker.exception.LabelNotFoundException;
import com.pbm5.bugtracker.repository.BugLabelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class BugLabelService {

    private static final Logger logger = LoggerFactory.getLogger(BugLabelService.class);

    @Autowired
    private BugLabelRepository bugLabelRepository;

    // CRUD Operations

    /**
     * Create a new custom label
     */
    public BugLabel createLabel(String name, String color, String description) {
        // Validate name uniqueness
        if (bugLabelRepository.existsByName(name)) {
            throw new IllegalArgumentException("Label with name '" + name + "' already exists");
        }

        BugLabel label = new BugLabel(name, color, description, false);
        return bugLabelRepository.save(label);
    }

    /**
     * Get label by ID
     */
    public BugLabel getLabelById(Long id) {
        return bugLabelRepository.findById(id)
                .orElseThrow(() -> new LabelNotFoundException("Label not found with id: " + id));
    }

    /**
     * Get label by name
     */
    public Optional<BugLabel> getLabelByName(String name) {
        return bugLabelRepository.findByName(name);
    }

    /**
     * Update label
     */
    public BugLabel updateLabel(Long id, String name, String color, String description) {
        BugLabel label = getLabelById(id);

        // Check if label can be modified
        if (label.isSystemLabel()) {
            throw new IllegalArgumentException("System labels cannot be modified");
        }

        // Validate name uniqueness if changed
        if (!name.equals(label.getName()) && bugLabelRepository.existsByName(name)) {
            throw new IllegalArgumentException("Label with name '" + name + "' already exists");
        }

        label.setName(name);
        label.setColor(color);
        label.setDescription(description);

        return bugLabelRepository.save(label);
    }

    /**
     * Delete label
     */
    public void deleteLabel(Long id) {
        BugLabel label = getLabelById(id);

        // Check if label can be deleted
        if (label.isSystemLabel()) {
            throw new IllegalArgumentException("System labels cannot be deleted");
        }

        // Check if label is in use
        if (label.getBugCount() > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete label that is in use by " + label.getBugCount() + " bugs");
        }

        bugLabelRepository.delete(label);
    }

    // Query Operations

    /**
     * Get all labels
     */
    public List<BugLabel> getAllLabels() {
        return bugLabelRepository.findAllByOrderByName();
    }

    /**
     * Get all labels for a specific project (system + custom)
     */
    public List<BugLabel> getLabelsForProject(UUID projectId) {
        List<BugLabel> systemLabels = getSystemLabels();
        List<BugLabel> customLabels = getCustomLabelsForProject(projectId);

        // Combine system and custom labels
        List<BugLabel> allLabels = new ArrayList<>(systemLabels);
        allLabels.addAll(customLabels);

        // Sort by name
        allLabels.sort(Comparator.comparing(BugLabel::getName));

        return allLabels;
    }

    /**
     * Get custom labels for a specific project
     */
    public List<BugLabel> getCustomLabelsForProject(UUID projectId) {
        // For now, return all custom labels since labels are global
        // In the future, this could be enhanced to support project-specific labels
        return getCustomLabels();
    }

    /**
     * Get system labels
     */
    public List<BugLabel> getSystemLabels() {
        return bugLabelRepository.findByIsSystemOrderByName(true);
    }

    /**
     * Get custom labels
     */
    public List<BugLabel> getCustomLabels() {
        return bugLabelRepository.findByIsSystemOrderByName(false);
    }

    /**
     * Search labels by name
     */
    public List<BugLabel> searchLabelsByName(String name) {
        return bugLabelRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Get labels used in project
     */
    public List<BugLabel> getLabelsUsedInProject(Long projectId) {
        return bugLabelRepository.findLabelsUsedInProject(projectId);
    }

    /**
     * Get labels used in project ordered by usage
     */
    public List<BugLabel> getLabelsUsedInProjectOrderByUsage(Long projectId) {
        return bugLabelRepository.findLabelsUsedInProjectOrderByUsage(projectId);
    }

    /**
     * Get custom labels used in project
     */
    public List<BugLabel> getCustomLabelsUsedInProject(Long projectId) {
        return bugLabelRepository.findCustomLabelsUsedInProject(projectId);
    }

    /**
     * Get unused custom labels
     */
    public List<BugLabel> getUnusedCustomLabels() {
        return bugLabelRepository.findUnusedCustomLabels();
    }

    // Utility Methods

    /**
     * Find labels by IDs
     */
    public Set<BugLabel> findByIds(Set<Long> labelIds) {
        logger.info("BugLabelService -> findByIds -> Starting label lookup for {} label IDs",
                labelIds != null ? labelIds.size() : 0);

        if (labelIds == null || labelIds.isEmpty()) {
            logger.info("BugLabelService -> findByIds -> No label IDs provided, returning empty set");
            return new HashSet<>();
        }

        Set<BugLabel> labels = new HashSet<>();
        for (Long id : labelIds) {
            try {
                logger.info("BugLabelService -> findByIds -> Looking up label with ID: {}", id);
                BugLabel label = getLabelById(id);
                labels.add(label);
                logger.info("BugLabelService -> findByIds -> Label found and added - ID: {}, Name: {}, Color: {}",
                        id, label.getName(), label.getColor());
            } catch (LabelNotFoundException e) {
                logger.error("BugLabelService -> findByIds -> Label not found with ID: {}, Error: {}", id,
                        e.getMessage());
                throw new IllegalArgumentException("Label not found with id: " + id);
            }
        }

        logger.info("BugLabelService -> findByIds -> Labels lookup completed. Requested: {}, Found: {}",
                labelIds.size(), labels.size());

        if (!labels.isEmpty()) {
            String labelDetails = labels.stream()
                    .map(label -> String.format("ID:%d,Name:%s", label.getId(), label.getName()))
                    .collect(Collectors.joining(", "));
            logger.info("BugLabelService -> findByIds -> Found labels: {}", labelDetails);
        }

        return labels;
    }

    /**
     * Validate label IDs exist
     */
    public boolean validateLabelIds(Set<Long> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return true;
        }

        for (Long id : labelIds) {
            if (!bugLabelRepository.existsById(id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get label usage statistics
     */
    public List<Object[]> getLabelUsageStats(UUID projectId) {
        return bugLabelRepository.findLabelUsageStats(projectId);
    }

    /**
     * Get labels with minimum bug count
     */
    public List<BugLabel> getLabelsWithMinimumBugCount(long minBugCount) {
        return bugLabelRepository.findLabelsWithMinimumBugCount(minBugCount);
    }

    // Analytics

    /**
     * Get label count by type
     */
    public long getSystemLabelCount() {
        return bugLabelRepository.countByIsSystem(true);
    }

    public long getCustomLabelCount() {
        return bugLabelRepository.countByIsSystem(false);
    }

    /**
     * Check if label name exists
     */
    public boolean labelNameExists(String name) {
        return bugLabelRepository.existsByName(name);
    }

    /**
     * Check if label name exists (excluding current label)
     */
    public boolean labelNameExistsExcluding(String name, Long excludeId) {
        return bugLabelRepository.existsByNameAndIdNot(name, excludeId);
    }
}