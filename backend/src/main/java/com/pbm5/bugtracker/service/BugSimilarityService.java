package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.dto.BugSimilarityResult;
import com.pbm5.bugtracker.dto.BugSummaryResponse;
import com.pbm5.bugtracker.dto.BugDuplicateSummaryResponse;
import com.pbm5.bugtracker.dto.DuplicateInfoResponse;
import com.pbm5.bugtracker.dto.DuplicateRelationshipInfo;
import com.pbm5.bugtracker.dto.DuplicateAnalyticsResponse;
import com.pbm5.bugtracker.entity.*;
import com.pbm5.bugtracker.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import com.pbm5.bugtracker.dto.BugSimilarityRelationship;

/**
 * Service for detecting duplicate bugs using text similarity algorithms.
 * 
 * This service implements intelligent duplicate detection using multiple
 * similarity algorithms from Apache Commons Text library:
 * - CosineSimilarity for vector-based text comparison
 * - JaccardSimilarity for set-based overlap analysis
 * - LevenshteinDistance for character-level differences
 * 
 * The service includes caching, performance optimization, and configurable
 * algorithm weights per project.
 * 
 * Citations:
 * - Apache Software Foundation. (2024). Apache Commons Text.
 * Version 1.12.0. https://commons.apache.org/proper/commons-text/
 * - Salton, G., & McGill, M. J. (1983). Introduction to Modern Information
 * Retrieval.
 * - Manning, C. D., Raghavan, P., & Schütze, H. (2008). Introduction to
 * Information Retrieval.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BugSimilarityService {

    private final BugRepository bugRepository;
    private final BugDuplicateRepository bugDuplicateRepository;
    private final SimilarityConfigRepository similarityConfigRepository;
    private final UserRepository userRepository;

    // Default similarity configurations
    private static final Map<String, SimilarityConfig> DEFAULT_SIMILARITY_CONFIGS = Map.of(
            "COSINE", new SimilarityConfig(null, SimilarityAlgorithm.COSINE,
                    BigDecimal.valueOf(0.6), BigDecimal.valueOf(0.4), true),
            "JACCARD", new SimilarityConfig(null, SimilarityAlgorithm.JACCARD,
                    BigDecimal.valueOf(0.3), BigDecimal.valueOf(0.4), true),
            "LEVENSHTEIN", new SimilarityConfig(null, SimilarityAlgorithm.LEVENSHTEIN,
                    BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.4), true));

    /**
     * Initialize default similarity configurations for a project.
     * This method is called automatically when a new project is created.
     * 
     * @param projectId Project UUID
     */
    public void initializeDefaultSimilarityConfigurations(UUID projectId) {
        log.info("Initializing default similarity configurations for project: {}", projectId);

        try {
            List<SimilarityConfig> configs = new ArrayList<>();

            for (SimilarityConfig defaultConfig : DEFAULT_SIMILARITY_CONFIGS.values()) {
                SimilarityConfig projectConfig = new SimilarityConfig(
                        null, // Project will be set below
                        defaultConfig.getAlgorithmName(),
                        defaultConfig.getWeight(),
                        defaultConfig.getThreshold(),
                        defaultConfig.isEnabled());

                // Set the project ID using reflection or create a minimal project entity
                Project project = new Project();
                project.setId(projectId);
                projectConfig.setProject(project);

                configs.add(projectConfig);
            }

            similarityConfigRepository.saveAll(configs);

            log.info("Successfully initialized {} similarity configurations for project: {}",
                    configs.size(), projectId);

        } catch (Exception e) {
            log.error("Failed to initialize similarity configurations for project: {}", projectId, e);
            throw new RuntimeException("Failed to initialize similarity configurations", e);
        }
    }

    /**
     * Manually initialize similarity configurations for an existing project.
     * This is useful for projects created before automatic initialization was
     * implemented.
     * 
     * @param projectId Project UUID
     * @return true if initialization was successful, false if configs already exist
     */
    public boolean initializeSimilarityConfigurationsIfMissing(UUID projectId) {
        // Check if configs already exist
        List<SimilarityConfig> existingConfigs = similarityConfigRepository.findByProjectId(projectId);

        if (!existingConfigs.isEmpty()) {
            log.info("Similarity configurations already exist for project: {}, skipping initialization", projectId);
            return false;
        }

        // Initialize default configs
        initializeDefaultSimilarityConfigurations(projectId);
        return true;
    }

    private final SimilarityCalculator similarityCalculator;
    private final TextPreprocessor textPreprocessor;
    private final BugSimilarityCacheRepository similarityCacheRepository;

    // Default configuration values
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.75;
    private static final double QUICK_CHECK_THRESHOLD = 0.2;
    private static final int DEFAULT_MAX_RESULTS = 10;

    /**
     * Find similar bugs for a new bug before creation.
     * Uses caching and performance optimization for fast response.
     * 
     * @param title       Bug title
     * @param description Bug description
     * @param projectId   Project UUID
     * @return List of similar bugs with similarity scores
     */
    @org.springframework.cache.annotation.Cacheable(value = "similarityCache", key = "#projectId + '_' + T(java.util.Objects).hash(#title + #description)")
    public List<BugSimilarityResult> findSimilarBugs(String title, String description, UUID projectId) {
        return findSimilarBugs(title, description, projectId, DEFAULT_SIMILARITY_THRESHOLD, DEFAULT_MAX_RESULTS, false);
    }

    /**
     * Find similar bugs with custom parameters.
     * 
     * @param title             Bug title
     * @param description       Bug description
     * @param projectId         Project UUID
     * @param threshold         Similarity threshold
     * @param maxResults        Maximum number of results
     * @param includeClosedBugs Whether to include closed bugs
     * @return List of similar bugs with similarity scores
     */
    public List<BugSimilarityResult> findSimilarBugs(String title, String description, UUID projectId,
            double threshold, int maxResults, boolean includeClosedBugs) {
        log.debug("Finding similar bugs for project {} with threshold {}", projectId, threshold);
        log.debug("Input - Title: '{}', Description length: {}", title,
                description != null ? description.length() : 0);

        try {
            // Validate input
            if (title == null || title.trim().isEmpty() ||
                    description == null || description.trim().isEmpty()) {
                log.warn("Empty title or description provided for similarity search");
                return Collections.emptyList();
            }

            // Get project-specific algorithm configurations
            List<SimilarityConfig> configs;
            try {
                configs = similarityConfigRepository.findEnabledByProjectId(projectId);
                if (configs.isEmpty()) {
                    log.warn("No enabled similarity algorithms found for project {}", projectId);
                    return Collections.emptyList();
                }
                log.debug("Found {} enabled similarity algorithms for project {}", configs.size(), projectId);
            } catch (Exception e) {
                log.error("Failed to retrieve similarity configurations for project {}: {}", projectId,
                        e.getMessage());
                return Collections.emptyList();
            }

            // Generate text fingerprint for caching
            String textFingerprint;
            try {
                textFingerprint = textPreprocessor.generateTextFingerprint(title, description);
                log.debug("Generated text fingerprint: {}", textFingerprint);
            } catch (Exception e) {
                log.warn("Failed to generate text fingerprint: {}", e.getMessage());
                // Use a fallback fingerprint
                textFingerprint = "fallback_" + System.currentTimeMillis();
                log.debug("Using fallback text fingerprint: {}", textFingerprint);
            }

            // Check cache first
            List<BugSimilarityResult> cachedResults;
            try {
                cachedResults = getCachedSimilarities(textFingerprint, threshold);
                if (!cachedResults.isEmpty()) {
                    log.debug("Found {} cached results for fingerprint {}", cachedResults.size(), textFingerprint);
                    return cachedResults.stream().limit(maxResults).collect(Collectors.toList());
                }
                log.debug("No cached results found, proceeding with similarity calculation");
            } catch (Exception e) {
                log.warn("Cache check failed, proceeding with similarity calculation: {}", e.getMessage());
                // Continue without cache
            }

            // Get candidate bugs from the project
            List<Bug> candidateBugs;
            try {
                candidateBugs = getCandidateBugs(projectId, includeClosedBugs);
                if (candidateBugs.isEmpty()) {
                    log.debug("No candidate bugs found in project {}", projectId);
                    return Collections.emptyList();
                }
                log.debug("Found {} candidate bugs for similarity comparison", candidateBugs.size());
            } catch (Exception e) {
                log.error("Failed to retrieve candidate bugs for project {}: {}", projectId, e.getMessage());
                return Collections.emptyList();
            }

            // Create corpus for TF-IDF calculation
            List<String> corpus;
            try {
                corpus = createCorpus(candidateBugs, title, description);
                if (corpus.isEmpty()) {
                    log.warn("Failed to create corpus, returning empty results");
                    return Collections.emptyList();
                }
                log.debug("Created corpus with {} documents for TF-IDF calculation", corpus.size());
            } catch (Exception e) {
                log.error("Failed to create corpus: {}", e.getMessage());
                return Collections.emptyList();
            }

            // Calculate similarities
            log.debug("Starting similarity calculation with {} algorithms", configs.size());
            List<BugSimilarityResult> results;
            try {
                results = calculateSimilarities(
                        title, description, candidateBugs, configs, corpus, threshold, textFingerprint);
                log.debug("Similarity calculation completed, found {} results above threshold {}", results.size(),
                        threshold);
            } catch (Exception e) {
                log.error("Failed to calculate similarities: {}", e.getMessage());
                return Collections.emptyList();
            }

            // Cache results for future queries (using text fingerprint as cache key)
            try {
                cacheSimilarityResults(textFingerprint, results, threshold);
            } catch (Exception cacheError) {
                log.warn("Caching failed but continuing with results: {}", cacheError.getMessage());
                // Don't let caching failure break the similarity search
            }

            // Sort by similarity score (highest first) and limit results
            List<BugSimilarityResult> finalResults;
            try {
                finalResults = results.stream()
                        .sorted((a, b) -> Double.compare(b.getSimilarityScore(), a.getSimilarityScore()))
                        .limit(maxResults)
                        .collect(Collectors.toList());

                log.debug("Returning {} final results (limited from {} total results)", finalResults.size(),
                        results.size());
                return finalResults;
            } catch (Exception e) {
                log.error("Failed to process final results: {}", e.getMessage());
                // Return the original results if sorting/limiting fails
                return results;
            }

        } catch (Exception e) {
            log.error("Error finding similar bugs for project {}: {}", projectId, e.getMessage(), e);
            // Log the full stack trace for debugging
            log.error("Full stack trace:", e);
            return Collections.emptyList();
        }
    }

    /**
     * Mark a bug as duplicate of another bug.
     * 
     * @param projectId         Project UUID
     * @param duplicateBugId    Bug to mark as duplicate
     * @param originalBugId     Original bug ID
     * @param markedByUserId    User marking the duplicate
     * @param confidenceScore   Confidence score (0.0 to 1.0)
     * @param detectionMethod   Detection method (manual, automatic, hybrid)
     * @param additionalContext Optional additional context
     */
    public void markAsDuplicate(UUID projectId, Long duplicateBugId, Long originalBugId,
            UUID markedByUserId, double confidenceScore,
            DuplicateDetectionMethod detectionMethod, String additionalContext) {
        log.info("Marking bug {} as duplicate of {} by user {}", duplicateBugId, originalBugId, markedByUserId);

        try {
            // Validate bugs exist and belong to the project
            Bug duplicateBug;
            Bug originalBug;
            try {
                duplicateBug = bugRepository.findByProjectIdAndId(projectId, duplicateBugId)
                        .orElseThrow(() -> new IllegalArgumentException("Duplicate bug not found"));
                originalBug = bugRepository.findByProjectIdAndId(projectId, originalBugId)
                        .orElseThrow(() -> new IllegalArgumentException("Original bug not found"));
            } catch (Exception e) {
                log.error("Failed to retrieve bugs for duplicate marking: {}", e.getMessage());
                throw e;
            }

            // Check if relationship already exists
            try {
                if (bugDuplicateRepository.findDuplicateRelationship(originalBugId, duplicateBugId).isPresent()) {
                    log.warn("Duplicate relationship already exists between {} and {}", originalBugId,
                            duplicateBugId);
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to check existing duplicate relationship: {}", e.getMessage());
                throw e;
            }

            // Check for circular relationships
            try {
                if (bugDuplicateRepository.hasCircularDuplicateRelationship(originalBugId, duplicateBugId)) {
                    throw new IllegalArgumentException("Cannot create circular duplicate relationship");
                }
            } catch (Exception e) {
                log.error("Failed to check circular duplicate relationship: {}", e.getMessage());
                throw e;
            }

            // Create duplicate relationship
            BugDuplicate duplicate = new BugDuplicate();
            duplicate.setOriginalBug(originalBug);
            duplicate.setDuplicateBug(duplicateBug);
            duplicate.setMarkedByUser(getUserById(markedByUserId));
            duplicate.setConfidenceScore(BigDecimal.valueOf(confidenceScore));
            duplicate.setDetectionMethod(detectionMethod);
            duplicate.setAdditionalContext(additionalContext);

            try {
                bugDuplicateRepository.save(duplicate);
            } catch (Exception e) {
                log.error("Failed to save duplicate relationship: {}", e.getMessage());
                throw e;
            }

            // Optional: Update duplicate bug status
            if (duplicateBug.getStatus() != BugStatus.CLOSED) {
                duplicateBug.setStatus(BugStatus.CLOSED);
                try {
                    bugRepository.save(duplicateBug);
                } catch (Exception e) {
                    log.warn("Failed to update duplicate bug status: {}", e.getMessage());
                    // Don't fail the entire operation if status update fails
                }
            }

            log.info("Successfully marked bug {} as duplicate of {}", duplicateBugId, originalBugId);

        } catch (Exception e) {
            log.error("Error marking bug {} as duplicate: {}", duplicateBugId, e.getMessage(), e);
            throw new RuntimeException("Failed to mark bug as duplicate", e);
        }
    }

    /**
     * Calculate weighted similarity score using multiple algorithms.
     * 
     * @param text1   First text to compare
     * @param text2   Second text to compare
     * @param configs Algorithm configurations with weights
     * @param corpus  Corpus for optimization
     * @return Weighted similarity score (0.0 to 1.0)
     */
    public double calculateWeightedSimilarity(String text1, String text2,
            List<SimilarityConfig> configs, List<String> corpus) {
        try {
            if (configs == null || configs.isEmpty()) {
                // Fallback to cosine similarity with default weight
                return similarityCalculator.calculateCosineSimilarity(text1, text2);
            }

            // Quick pre-filter using keyword overlap
            if (!similarityCalculator.passesQuickSimilarityCheck(text1, text2, corpus, QUICK_CHECK_THRESHOLD)) {
                return 0.0;
            }

            Map<SimilarityAlgorithm, Double> weights = configs.stream()
                    .collect(Collectors.toMap(
                            SimilarityConfig::getAlgorithmName,
                            SimilarityConfig::getWeightAsDouble));

            return similarityCalculator.calculateWeightedSimilarity(text1, text2, weights);
        } catch (Exception e) {
            log.error("Error calculating weighted similarity: {}", e.getMessage(), e);
            // Fallback to cosine similarity
            try {
                return similarityCalculator.calculateCosineSimilarity(text1, text2);
            } catch (Exception fallbackError) {
                log.error("Fallback similarity calculation also failed: {}", fallbackError.getMessage());
                return 0.0;
            }
        }
    }

    /**
     * Get algorithm-specific similarity scores for debugging/display.
     * 
     * @param text1 First text
     * @param text2 Second text
     * @return Map of algorithm-specific scores
     */
    public Map<SimilarityAlgorithm, Double> getDetailedSimilarityScores(String text1, String text2) {
        try {
            return similarityCalculator.calculateAllSimilarities(text1, text2);
        } catch (Exception e) {
            log.error("Error getting detailed similarity scores: {}", e.getMessage(), e);
            // Return empty map if calculation fails
            return new HashMap<>();
        }
    }

    // === Private Helper Methods ===

    private List<BugSimilarityResult> getCachedSimilarities(String textFingerprint, double threshold) {
        try {
            log.debug("Checking cache for text fingerprint: {}", textFingerprint);
            List<BugSimilarityCache> cachedEntries;
            try {
                cachedEntries = similarityCacheRepository.findByTextFingerprint(textFingerprint);
            } catch (Exception e) {
                log.warn("Failed to retrieve cached entries: {}", e.getMessage());
                return Collections.emptyList();
            }
            log.debug("Found {} cached entries for fingerprint: {}", cachedEntries.size(), textFingerprint);

            List<BugSimilarityResult> results = cachedEntries.stream()
                    .filter(cache -> {
                        try {
                            double score = cache.getSimilarityScoreAsDouble();
                            boolean aboveThreshold = score >= threshold;
                            log.debug("Cache entry for bug {}: score={}, above threshold={}? {}",
                                    cache.getSimilarBug() != null ? cache.getSimilarBug().getId() : "null",
                                    score, threshold, aboveThreshold);
                            return aboveThreshold;
                        } catch (Exception e) {
                            log.warn("Error processing cache entry: {}", e.getMessage());
                            return false;
                        }
                    })
                    .map(this::convertCacheToResult)
                    .filter(result -> result != null) // Filter out null results
                    .collect(Collectors.toList());

            log.debug("Returning {} cached results above threshold {}", results.size(), threshold);
            return results;
        } catch (Exception e) {
            log.warn("Error retrieving cached similarities: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Bug> getCandidateBugs(UUID projectId, boolean includeClosedBugs) {
        try {
            List<Bug> bugs;
            if (includeClosedBugs) {
                try {
                    bugs = bugRepository.findByProjectId(projectId);
                    log.debug("Retrieved {} bugs from project {} (including closed)", bugs.size(), projectId);
                } catch (Exception e) {
                    log.error("Failed to retrieve bugs from project {}: {}", projectId, e.getMessage());
                    return Collections.emptyList();
                }
            } else {
                // Exclude closed bugs
                List<BugStatus> activeStatuses = Arrays.asList(
                        BugStatus.OPEN, BugStatus.REOPENED);
                try {
                    bugs = bugRepository.findByProjectIdAndStatusIn(projectId, activeStatuses);
                    log.debug("Retrieved {} active bugs from project {} (excluding closed)", bugs.size(), projectId);
                } catch (Exception e) {
                    log.error("Failed to retrieve active bugs from project {}: {}", projectId, e.getMessage());
                    return Collections.emptyList();
                }
            }

            // Log some basic info about the bugs for debugging
            if (!bugs.isEmpty()) {
                log.debug("Sample bug IDs: {}", bugs.stream().limit(3).map(Bug::getId).collect(Collectors.toList()));
            }

            return bugs;
        } catch (Exception e) {
            log.error("Error retrieving candidate bugs for project {}: {}", projectId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<String> createCorpus(List<Bug> bugs, String queryTitle, String queryDescription) {
        try {
            List<String> corpus = bugs.stream()
                    .map(bug -> {
                        try {
                            String title = bug.getTitle() != null ? bug.getTitle() : "";
                            String description = bug.getDescription() != null ? bug.getDescription() : "";
                            return title + " " + description;
                        } catch (Exception e) {
                            log.warn("Error processing bug ID {} for corpus: {}", bug.getId(), e.getMessage());
                            return "";
                        }
                    })
                    .filter(text -> !text.trim().isEmpty())
                    .collect(Collectors.toList());

            // Add query text to corpus for IDF calculation
            String queryText = (queryTitle != null ? queryTitle : "") + " "
                    + (queryDescription != null ? queryDescription : "");
            if (!queryText.trim().isEmpty()) {
                corpus.add(queryText);
            }

            log.debug("Created corpus with {} documents ({} bugs + 1 query)", corpus.size(), bugs.size());

            return corpus;
        } catch (Exception e) {
            log.error("Error creating corpus: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private List<BugSimilarityResult> calculateSimilarities(String title, String description,
            List<Bug> candidateBugs,
            List<SimilarityConfig> configs,
            List<String> corpus, double threshold,
            String textFingerprint) {
        String queryText = title + " " + description;
        List<BugSimilarityResult> results = new ArrayList<>();

        log.debug("Calculating similarities for {} candidate bugs with threshold {}", candidateBugs.size(),
                threshold);

        for (Bug candidate : candidateBugs) {
            try {
                log.debug("Processing candidate bug ID: {}", candidate.getId());

                // Validate candidate bug data
                if (candidate.getTitle() == null || candidate.getDescription() == null) {
                    log.warn("Bug ID {} has null title or description, skipping", candidate.getId());
                    continue;
                }

                String candidateText = candidate.getTitle() + " " + candidate.getDescription();

                // Calculate weighted similarity
                double similarity;
                try {
                    similarity = calculateWeightedSimilarity(queryText, candidateText, configs, corpus);
                    log.debug("Bug ID {} similarity score: {}", candidate.getId(), similarity);
                } catch (Exception e) {
                    log.warn("Failed to calculate similarity for bug ID {}: {}", candidate.getId(), e.getMessage());
                    continue; // Skip this bug if similarity calculation fails
                }

                if (similarity >= threshold) {
                    log.debug("Bug ID {} meets threshold, creating result", candidate.getId());
                    BugSimilarityResult result;
                    try {
                        result = convertBugToResult(candidate, similarity);
                    } catch (Exception e) {
                        log.warn("Failed to convert bug ID {} to result: {}", candidate.getId(), e.getMessage());
                        continue; // Skip this bug if conversion fails
                    }

                    // Add algorithm-specific scores
                    Map<SimilarityAlgorithm, Double> algorithmScores;
                    try {
                        algorithmScores = getDetailedSimilarityScores(queryText, candidateText);
                        result.setAlgorithmScores(algorithmScores);
                    } catch (Exception e) {
                        log.warn("Failed to get detailed similarity scores for bug ID {}: {}", candidate.getId(),
                                e.getMessage());
                        // Continue without algorithm scores
                    }

                    try {
                        result.setTextFingerprint(
                                textPreprocessor.generateTextFingerprint(candidate.getTitle(),
                                        candidate.getDescription()));
                    } catch (Exception e) {
                        log.warn("Failed to generate text fingerprint for bug ID {}: {}", candidate.getId(),
                                e.getMessage());
                        // Continue without text fingerprint
                    }

                    // Check if already marked as duplicate
                    try {
                        Optional<BugDuplicate> duplicateInfo = bugDuplicateRepository
                                .findOriginalOfDuplicate(candidate.getId());
                        if (duplicateInfo.isPresent()) {
                            result.setAlreadyMarkedDuplicate(true);
                            result.setOriginalBugId(duplicateInfo.get().getOriginalBug().getId());
                            log.debug("Bug ID {} is already marked as duplicate of {}", candidate.getId(),
                                    result.getOriginalBugId());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to check duplicate status for bug ID {}: {}", candidate.getId(),
                                e.getMessage());
                        // Continue without duplicate information
                    }

                    results.add(result);
                    log.debug("Successfully added result for bug ID {}", candidate.getId());
                } else {
                    log.debug("Bug ID {} below threshold {}, skipping", candidate.getId(), threshold);
                }
            } catch (Exception e) {
                log.warn("Error processing candidate bug ID {}: {}", candidate.getId(), e.getMessage());
                // Continue with other bugs
            }
        }

        log.debug("Similarity calculation completed, found {} results above threshold", results.size());
        return results;
    }

    private void cacheSimilarityResults(String textFingerprint, List<BugSimilarityResult> results, double threshold) {
        try {
            log.debug("Attempting to cache {} similarity results for fingerprint: {}", results.size(),
                    textFingerprint);

            // NOTE: Caching is currently disabled for text-based similarity searches
            // because the BugSimilarityCache entity design expects both bug_id and
            // similar_bug_id
            // to be different bugs, but text-based search doesn't have a specific "current
            // bug"
            // to compare against.
            //
            // Option 1: Create a separate text_similarity_cache table
            // Option 2: Modify BugSimilarityCache to handle text-based searches
            // Option 3: Use a different caching strategy (e.g., Redis with text
            // fingerprints)

            log.debug("Caching disabled for text-based similarity search. Results will not be cached.");

            // For now, we'll skip caching to avoid the constraint violation
            // This ensures the similarity search continues to work without errors

        } catch (Exception e) {
            log.warn("Failed to cache similarity results: {}", e.getMessage());
            // Don't throw exception - caching failure shouldn't break similarity search
        }
    }

    private BugSimilarityResult convertBugToResult(Bug bug, double similarity) {
        try {
            log.debug("Converting bug ID {} to similarity result", bug.getId());

            BugSimilarityResult result = new BugSimilarityResult();
            result.setBugId(bug.getId());
            result.setProjectTicketNumber(bug.getProjectTicketNumber());
            result.setTitle(bug.getTitle());
            result.setDescription(bug.getDescription());
            result.setSimilarityScore(similarity);
            result.setStatus(bug.getStatus());
            result.setPriority(bug.getPriority());
            result.setCreatedAt(bug.getCreatedAt());
            result.setUpdatedAt(bug.getUpdatedAt());

            // Set assignee and reporter names
            if (bug.getAssignee() != null) {
                result.setAssigneeName(bug.getAssignee().getFirstName() + " " + bug.getAssignee().getLastName());
            }
            if (bug.getReporter() != null) {
                result.setReporterName(bug.getReporter().getFirstName() + " " + bug.getReporter().getLastName());
            }

            log.debug("Successfully converted bug ID {} to similarity result", bug.getId());
            return result;
        } catch (Exception e) {
            log.error("Error converting bug ID {} to similarity result: {}", bug.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to convert bug to similarity result", e);
        }
    }

    private BugSimilarityResult convertCacheToResult(BugSimilarityCache cache) {
        try {
            Bug bug = cache.getSimilarBug();
            if (bug == null) {
                log.warn("Cache entry has null similarBug, skipping conversion");
                return null;
            }

            log.debug("Converting cache entry for bug ID: {}", bug.getId());
            BugSimilarityResult result = convertBugToResult(bug, cache.getSimilarityScoreAsDouble());
            log.debug("Successfully converted cache entry to result for bug ID: {}", bug.getId());
            return result;
        } catch (Exception e) {
            log.warn("Error converting cache entry to result: {}", e.getMessage());
            return null;
        }
    }

    private User getUserById(UUID userId) {
        try {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        } catch (Exception e) {
            log.error("Failed to retrieve user {}: {}", userId, e.getMessage());
            throw e;
        }
    }

    /**
     * Clean up expired cache entries.
     * Should be called periodically by a scheduled task.
     */
    @org.springframework.transaction.annotation.Transactional
    public int cleanupExpiredCache() {
        log.info("Cleaning up expired similarity cache entries");
        try {
            int deletedCount = similarityCacheRepository.deleteExpiredEntries();
            log.info("Deleted {} expired cache entries", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to cleanup expired cache: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Compare two specific bugs and get detailed similarity scores.
     * 
     * @param projectId Project UUID
     * @param bugId1    First bug ID
     * @param bugId2    Second bug ID
     * @return Map of algorithm-specific similarity scores
     */
    public Map<SimilarityAlgorithm, Double> compareSpecificBugs(UUID projectId, Long bugId1, Long bugId2) {
        log.debug("Comparing bugs {} and {} in project {}", bugId1, bugId2, projectId);

        try {
            // Get both bugs and validate they belong to the project
            Bug bug1;
            Bug bug2;
            try {
                bug1 = bugRepository.findByProjectIdAndId(projectId, bugId1)
                        .orElseThrow(() -> new IllegalArgumentException("Bug " + bugId1 + " not found in project"));
                bug2 = bugRepository.findByProjectIdAndId(projectId, bugId2)
                        .orElseThrow(() -> new IllegalArgumentException("Bug " + bugId2 + " not found in project"));
            } catch (Exception e) {
                log.error("Failed to retrieve bugs for comparison: {}", e.getMessage());
                throw e;
            }

            // Prepare texts for comparison
            String text1 = bug1.getTitle() + " " + bug1.getDescription();
            String text2 = bug2.getTitle() + " " + bug2.getDescription();

            // Calculate individual algorithm scores
            Map<SimilarityAlgorithm, Double> scores = new HashMap<>();
            try {
                scores.put(SimilarityAlgorithm.COSINE,
                        similarityCalculator.calculateCosineSimilarity(text1, text2));
                scores.put(SimilarityAlgorithm.JACCARD,
                        similarityCalculator.calculateJaccardSimilarity(text1, text2));
                scores.put(SimilarityAlgorithm.LEVENSHTEIN,
                        similarityCalculator.calculateLevenshteinSimilarity(text1, text2));
            } catch (Exception e) {
                log.error("Failed to calculate similarity scores: {}", e.getMessage());
                throw e;
            }

            log.debug("Comparison results for bugs {} and {}: {}", bugId1, bugId2, scores);
            return scores;

        } catch (Exception e) {
            log.error("Error comparing bugs {} and {}: {}", bugId1, bugId2, e.getMessage(), e);
            throw new RuntimeException("Failed to compare bugs", e);
        }
    }

    /**
     * Get similarity statistics for a project.
     * 
     * @param projectId Project UUID
     * @return Map of similarity statistics
     */
    public Map<String, Object> getSimilarityStatistics(UUID projectId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Cache statistics
            Object[] cacheStats = similarityCacheRepository.getCacheStatistics(projectId);
            stats.put("validCacheEntries", cacheStats[0]);
            stats.put("expiredCacheEntries", cacheStats[1]);
            stats.put("totalCacheEntries", cacheStats[2]);

            // Duplicate statistics
            long duplicateCount = bugDuplicateRepository.countDuplicatesInProject(projectId);
            stats.put("totalDuplicates", duplicateCount);

            // Algorithm usage statistics
            List<Object[]> algorithmStats = similarityCacheRepository.countCacheEntriesByAlgorithm(projectId);
            Map<String, Object> algorithmCounts = new HashMap<>();
            for (Object[] stat : algorithmStats) {
                algorithmCounts.put((String) stat[0], stat[1]);
            }
            stats.put("algorithmUsage", algorithmCounts);

            // Configuration status
            boolean hasConfigs = similarityConfigRepository.hasEnabledAlgorithms(projectId);
            stats.put("hasEnabledAlgorithms", hasConfigs);

            return stats;

        } catch (Exception e) {
            log.error("Error getting similarity statistics for project {}: {}", projectId, e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to retrieve statistics");
        }
    }

    /**
     * Get similarity system health status for a project.
     * 
     * @param projectId Project UUID
     * @return Map containing health status and recommendations
     */
    public Map<String, Object> getSimilarityHealth(UUID projectId) {
        Map<String, Object> health = new HashMap<>();
        try {
            boolean hasEnabledAlgorithms;
            try {
                hasEnabledAlgorithms = similarityConfigRepository.hasEnabledAlgorithms(projectId);
            } catch (Exception e) {
                log.error("Failed to check enabled algorithms: {}", e.getMessage());
                hasEnabledAlgorithms = false;
            }
            health.put("hasEnabledAlgorithms", hasEnabledAlgorithms);

            Object[] cacheStats;
            try {
                cacheStats = similarityCacheRepository.getCacheStatistics(projectId);
            } catch (Exception e) {
                log.error("Failed to get cache statistics: {}", e.getMessage());
                cacheStats = new Object[] { 0L, 0L, 0L };
            }

            long totalCacheEntries = (Long) cacheStats[2];
            String status = "HEALTHY";
            List<String> recommendations = new ArrayList<>();

            if (!hasEnabledAlgorithms) {
                status = "NEEDS_CONFIGURATION";
                recommendations.add("Enable at least one similarity algorithm");
            }
            if (totalCacheEntries > 1000) {
                status = "NEEDS_MAINTENANCE";
                recommendations.add("Consider cleaning up expired cache entries");
            }

            health.put("status", status);
            health.put("totalCacheEntries", totalCacheEntries);
            health.put("recommendations", recommendations);
            return health;
        } catch (Exception e) {
            log.error("Error getting similarity health for project {}: {}", projectId, e.getMessage(), e);
            health.put("status", "ERROR");
            health.put("error", e.getMessage());
            return health;
        }
    }

    public List<SimilarityConfig> getSimilarityConfigurations(UUID projectId) {
        try {
            return similarityConfigRepository.findByProjectId(projectId);
        } catch (Exception e) {
            log.error("Error retrieving similarity configurations for project {}: {}", projectId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public void updateSimilarityConfiguration(UUID projectId, String algorithmName, double weight, double threshold,
            boolean isEnabled) {
        try {
            SimilarityAlgorithm algorithm = SimilarityAlgorithm.valueOf(algorithmName);
            SimilarityConfig config = similarityConfigRepository.findByProjectIdAndAlgorithm(projectId, algorithm)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Configuration not found for algorithm: " + algorithmName));

            config.setWeight(BigDecimal.valueOf(weight));
            config.setThreshold(BigDecimal.valueOf(threshold));
            config.setEnabled(isEnabled);
            config.setUpdatedAt(LocalDateTime.now());

            similarityConfigRepository.save(config);
            log.info(
                    "Updated similarity configuration for project {} algorithm {}: weight={}, threshold={}, enabled={}",
                    projectId, algorithmName, weight, threshold, isEnabled);
        } catch (Exception e) {
            log.error("Error updating similarity configuration for project {} algorithm {}: {}", projectId,
                    algorithmName, e.getMessage(), e);
            throw new RuntimeException("Failed to update similarity configuration", e);
        }
    }

    /**
     * Get duplicate information for a specific bug.
     * 
     * @param projectId Project UUID
     * @param bugId     Bug ID
     * @return DuplicateInfoResponse containing duplicate status and relationship
     *         information
     */
    public DuplicateInfoResponse getDuplicateInfo(UUID projectId, Long bugId) {
        try {
            // Check if the bug exists and belongs to the project
            Bug bug = bugRepository.findById(bugId)
                    .orElseThrow(() -> new IllegalArgumentException("Bug not found: " + bugId));

            if (!bug.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Bug does not belong to the specified project");
            }

            // Check if this bug is marked as a duplicate
            Optional<BugDuplicate> duplicateRelationship = bugDuplicateRepository
                    .findByDuplicateBugId(bugId);

            if (duplicateRelationship.isPresent()) {
                BugDuplicate relationship = duplicateRelationship.get();
                Bug originalBug = relationship.getOriginalBug();

                // Get other duplicates of the same original bug
                List<BugDuplicate> otherDuplicates = bugDuplicateRepository
                        .findByOriginalBugId(originalBug.getId());

                // Convert to DTOs
                BugSummaryResponse originalBugSummary = convertToBugSummary(originalBug);
                DuplicateRelationshipInfo relationshipInfo = new DuplicateRelationshipInfo(
                        getUserDisplayName(relationship.getMarkedByUser()),
                        relationship.getMarkedAt());

                List<BugSummaryResponse> otherDuplicatesList = otherDuplicates.stream()
                        .filter(d -> !d.getDuplicateBug().getId().equals(bugId)) // Exclude current bug
                        .map(d -> convertToBugSummary(d.getDuplicateBug()))
                        .collect(Collectors.toList());

                return new DuplicateInfoResponse(true, originalBugSummary, relationshipInfo, otherDuplicatesList);
            } else {
                // Check if this bug has duplicates
                List<BugDuplicate> duplicates = bugDuplicateRepository.findByOriginalBugId(bugId);

                if (!duplicates.isEmpty()) {
                    List<BugSummaryResponse> duplicatesList = duplicates.stream()
                            .map(d -> convertToBugSummary(d.getDuplicateBug()))
                            .collect(Collectors.toList());

                    return new DuplicateInfoResponse(false, null, null, duplicatesList);
                } else {
                    return new DuplicateInfoResponse(false);
                }
            }
        } catch (Exception e) {
            log.error("Error getting duplicate info for bug {} in project {}: {}",
                    bugId, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve duplicate information", e);
        }
    }

    /**
     * Get duplicate information for a bug by project ticket number.
     * This method handles the mapping between project ticket numbers and actual bug
     * IDs.
     * 
     * @param projectId           Project UUID
     * @param projectTicketNumber Project ticket number
     * @return DuplicateInfoResponse containing duplicate status and relationship
     *         information
     */
    public DuplicateInfoResponse getDuplicateInfoByTicketNumber(UUID projectId, Integer projectTicketNumber) {
        try {
            // Find bug by project ticket number
            Bug bug = bugRepository.findByProjectIdAndProjectTicketNumber(projectId, projectTicketNumber)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Bug not found with ticket number: " + projectTicketNumber));

            // Use the existing method with the actual bug ID
            return getDuplicateInfo(projectId, bug.getId());
        } catch (Exception e) {
            log.error("Error getting duplicate info for bug with ticket number {} in project {}: {}",
                    projectTicketNumber, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve duplicate information by ticket number", e);
        }
    }

    /**
     * Get all duplicates of a specific bug (for original bug view).
     * 
     * @param projectId Project UUID
     * @param bugId     Original bug ID
     * @return List of BugDuplicateSummaryResponse containing duplicate information
     */
    public List<BugDuplicateSummaryResponse> getDuplicatesOfBug(UUID projectId, Long bugId) {
        try {
            // Check if the bug exists and belongs to the project
            Bug bug = bugRepository.findById(bugId)
                    .orElseThrow(() -> new IllegalArgumentException("Bug not found: " + bugId));

            if (!bug.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Bug does not belong to the specified project");
            }

            List<BugDuplicate> duplicates = bugDuplicateRepository.findByOriginalBugId(bugId);

            return duplicates.stream()
                    .map(this::convertToBugDuplicateSummary)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting duplicates for bug {} in project {}: {}",
                    bugId, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve duplicates", e);
        }
    }

    /**
     * Get the original bug for a duplicate bug.
     * 
     * @param projectId Project UUID
     * @param bugId     Duplicate bug ID
     * @return BugSummaryResponse containing original bug information
     */
    public BugSummaryResponse getOriginalBug(UUID projectId, Long bugId) {
        try {
            // Check if the bug exists and belongs to the project
            Bug bug = bugRepository.findById(bugId)
                    .orElseThrow(() -> new IllegalArgumentException("Bug not found: " + bugId));

            if (!bug.getProject().getId().equals(projectId)) {
                throw new IllegalArgumentException("Bug does not belong to the specified project");
            }

            Optional<BugDuplicate> duplicateRelationship = bugDuplicateRepository
                    .findByDuplicateBugId(bugId);

            if (duplicateRelationship.isPresent()) {
                Bug originalBug = duplicateRelationship.get().getOriginalBug();
                return convertToBugSummary(originalBug);
            } else {
                throw new IllegalArgumentException("Bug is not marked as duplicate");
            }
        } catch (Exception e) {
            log.error("Error getting original bug for duplicate {} in project {}: {}",
                    bugId, projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve original bug", e);
        }
    }

    /**
     * Get duplicate analytics for a project.
     * 
     * @param projectId Project UUID
     * @return DuplicateAnalyticsResponse containing analytics information
     */
    public DuplicateAnalyticsResponse getDuplicateAnalytics(UUID projectId) {
        try {
            // Get total duplicate count
            long totalDuplicates = bugDuplicateRepository.countByProjectId(projectId);

            // Get duplicates by detection method
            Map<DuplicateDetectionMethod, Long> duplicatesByMethod = bugDuplicateRepository
                    .findByProjectId(projectId)
                    .stream()
                    .collect(Collectors.groupingBy(
                            BugDuplicate::getDetectionMethod,
                            Collectors.counting()));

            // Get duplicates by user who marked them
            Map<String, Long> duplicatesByUser = bugDuplicateRepository
                    .findByProjectId(projectId)
                    .stream()
                    .collect(Collectors.groupingBy(
                            d -> getUserDisplayName(d.getMarkedByUser()),
                            Collectors.counting()));

            return new DuplicateAnalyticsResponse(totalDuplicates, duplicatesByMethod, duplicatesByUser);
        } catch (Exception e) {
            log.error("Error getting duplicate analytics for project {}: {}",
                    projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve duplicate analytics", e);
        }
    }

    /**
     * Get diagnostic information for debugging duplicate detection issues.
     * 
     * @param projectId Project UUID
     * @return Map containing diagnostic information
     */
    public Map<String, Object> getDiagnosticInfo(UUID projectId) {
        try {
            Map<String, Object> diagnostic = new HashMap<>();

            // Get all bugs in the project
            List<Bug> projectBugs = bugRepository.findByProjectId(projectId);
            diagnostic.put("totalBugs", projectBugs.size());

            // Get bug IDs and ticket numbers
            List<Map<String, Object>> bugInfo = projectBugs.stream()
                    .map(bug -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", bug.getId());
                        info.put("projectTicketNumber", bug.getProjectTicketNumber());
                        info.put("title", bug.getTitle());
                        info.put("status", bug.getStatus());
                        info.put("createdAt", bug.getCreatedAt());
                        return info;
                    })
                    .collect(Collectors.toList());
            diagnostic.put("bugs", bugInfo);

            // Get all duplicate relationships
            List<BugDuplicate> duplicates = bugDuplicateRepository.findByProjectId(projectId);
            diagnostic.put("totalDuplicates", duplicates.size());

            // Get duplicate relationship details
            List<Map<String, Object>> duplicateInfo = duplicates.stream()
                    .map(dup -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("id", dup.getId());
                        info.put("originalBugId", dup.getOriginalBug().getId());
                        info.put("duplicateBugId", dup.getDuplicateBug().getId());
                        info.put("detectionMethod", dup.getDetectionMethod());
                        info.put("confidenceScore", dup.getConfidenceScore());
                        info.put("markedBy", getUserDisplayName(dup.getMarkedByUser()));
                        info.put("markedAt", dup.getMarkedAt());
                        return info;
                    })
                    .collect(Collectors.toList());
            diagnostic.put("duplicates", duplicateInfo);

            // Get similarity configurations
            List<SimilarityConfig> configs = similarityConfigRepository.findByProjectId(projectId);
            diagnostic.put("similarityConfigs", configs.size());
            diagnostic.put("enabledAlgorithms", configs.stream()
                    .filter(SimilarityConfig::isEnabled)
                    .map(SimilarityConfig::getAlgorithmName)
                    .collect(Collectors.toList()));

            return diagnostic;

        } catch (Exception e) {
            log.error("Error getting diagnostic info for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve diagnostic information", e);
        }
    }

    /**
     * Convert Bug entity to BugSummaryResponse DTO.
     * 
     * @param bug Bug entity
     * @return BugSummaryResponse DTO
     */
    private BugSummaryResponse convertToBugSummary(Bug bug) {
        BugSummaryResponse summary = new BugSummaryResponse();
        summary.setId(bug.getId());
        summary.setProjectTicketNumber(bug.getProjectTicketNumber());
        summary.setTitle(bug.getTitle());
        summary.setStatus(bug.getStatus());
        summary.setPriority(bug.getPriority());
        summary.setCreatedAt(bug.getCreatedAt());
        summary.setUpdatedAt(bug.getUpdatedAt());

        if (bug.getAssignee() != null) {
            summary.setAssigneeName(getUserDisplayName(bug.getAssignee()));
        }
        if (bug.getReporter() != null) {
            summary.setReporterName(getUserDisplayName(bug.getReporter()));
        }

        return summary;
    }

    /**
     * Convert BugDuplicate entity to BugDuplicateSummaryResponse DTO.
     * 
     * @param duplicate BugDuplicate entity
     * @return BugDuplicateSummaryResponse DTO
     */
    private BugDuplicateSummaryResponse convertToBugDuplicateSummary(BugDuplicate duplicate) {
        Bug bug = duplicate.getDuplicateBug();
        BugDuplicateSummaryResponse summary = new BugDuplicateSummaryResponse();
        summary.setId(bug.getId());
        summary.setProjectTicketNumber(bug.getProjectTicketNumber());
        summary.setTitle(bug.getTitle());
        summary.setStatus(bug.getStatus().toString());
        summary.setPriority(bug.getPriority().toString());
        summary.setCreatedAt(bug.getCreatedAt());
        summary.setMarkedAsDuplicateAt(duplicate.getMarkedAt());
        summary.setMarkedByUserName(getUserDisplayName(duplicate.getMarkedByUser()));

        if (bug.getAssignee() != null) {
            summary.setAssigneeName(getUserDisplayName(bug.getAssignee()));
        }
        if (bug.getReporter() != null) {
            summary.setReporterName(getUserDisplayName(bug.getReporter()));
        }

        return summary;
    }

    /**
     * Get user display name from User entity.
     * 
     * @param user User entity
     * @return Display name (firstName + lastName)
     */
    private String getUserDisplayName(User user) {
        if (user == null) {
            return "Unknown User";
        }

        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";

        if (firstName.isEmpty() && lastName.isEmpty()) {
            return "Unknown User";
        } else if (firstName.isEmpty()) {
            return lastName;
        } else if (lastName.isEmpty()) {
            return firstName;
        } else {
            return firstName + " " + lastName;
        }
    }

    /**
     * Get similarity analysis for all bugs in a project.
     * This method calculates similarity scores between all bugs in the project
     * and returns them sorted by similarity score.
     * 
     * @param projectId     Project UUID
     * @param threshold     Minimum similarity threshold (0.0 to 1.0)
     * @param searchTerm    Optional search term to filter bugs
     * @param sortBy        Sort field (similarityScore, title, createdAt, etc.)
     * @param sortDirection Sort direction (asc or desc)
     * @param page          Page number for pagination
     * @param size          Page size for pagination
     * @return List of BugSimilarityResult with similarity scores
     */
    public Page<BugSimilarityRelationship> getProjectSimilarityAnalysis(
            UUID projectId,
            double threshold,
            String searchTerm,
            String sortBy,
            String sortDirection,
            int page,
            int size,
            String startDate,
            String endDate) {

        log.debug("Getting project similarity analysis for project {} with threshold {}", projectId, threshold);

        try {
            // Get all bugs in the project
            List<Bug> projectBugs = bugRepository.findByProjectId(projectId);
            if (projectBugs.isEmpty()) {
                log.debug("No bugs found in project {}", projectId);
                Pageable pageable = PageRequest.of(page, size);
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            log.debug("Found {} bugs in project {} for similarity analysis", projectBugs.size(), projectId);

            // Apply search filter if provided
            List<Bug> filteredBugs = projectBugs;
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                String lowerSearchTerm = searchTerm.toLowerCase();
                filteredBugs = projectBugs.stream()
                        .filter(bug -> bug.getTitle().toLowerCase().contains(lowerSearchTerm) ||
                                (bug.getDescription() != null
                                        && bug.getDescription().toLowerCase().contains(lowerSearchTerm)))
                        .collect(Collectors.toList());
                log.debug("Search filter applied, {} bugs match search term '{}'", filteredBugs.size(), searchTerm);
            }

            // Apply date range filter if provided
            if (startDate != null && endDate != null && !startDate.trim().isEmpty() && !endDate.trim().isEmpty()) {
                log.debug("Date filtering requested: startDate={}, endDate={}", startDate, endDate);
                try {
                    // Parse ISO 8601 format with timezone (e.g., "2025-08-25T00:00:00.000Z")
                    // Convert to LocalDateTime for comparison with bug creation dates
                    LocalDateTime startDateTime = OffsetDateTime.parse(startDate).toLocalDateTime();
                    LocalDateTime endDateTime = OffsetDateTime.parse(endDate).toLocalDateTime();

                    log.debug("Successfully parsed date range: startDateTime={}, endDateTime={}", startDateTime,
                            endDateTime);
                    log.debug("Original bugs count before date filtering: {}", filteredBugs.size());

                    filteredBugs = filteredBugs.stream()
                            .filter(bug -> {
                                LocalDateTime bugCreatedAt = bug.getCreatedAt();
                                boolean matches = bugCreatedAt != null &&
                                        !bugCreatedAt.isBefore(startDateTime) &&
                                        !bugCreatedAt.isAfter(endDateTime);

                                if (log.isDebugEnabled()) {
                                    log.debug("Bug {} (created: {}) matches date range: {}",
                                            bug.getId(), bugCreatedAt, matches);
                                }

                                return matches;
                            })
                            .collect(Collectors.toList());

                    log.debug("Date filter applied ({} to {}), {} bugs match date range",
                            startDate, endDate, filteredBugs.size());
                } catch (Exception e) {
                    log.warn(
                            "Invalid date format provided: startDate={}, endDate={}. Skipping date filtering. Error: {}",
                            startDate, endDate, e.getMessage());
                    log.debug("Stack trace:", e);
                }
            } else {
                log.debug("No date filtering applied - startDate={}, endDate={}", startDate, endDate);
            }

            // Get project similarity configurations
            List<SimilarityConfig> configs = similarityConfigRepository.findEnabledByProjectId(projectId);
            if (configs.isEmpty()) {
                log.warn("No enabled similarity algorithms found for project {}", projectId);
                Pageable pageable = PageRequest.of(page, size);
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }

            // Calculate similarities between all bugs
            List<BugSimilarityRelationship> results = new ArrayList<>();

            log.debug("Starting similarity calculation for {} bugs in project {}", filteredBugs.size(), projectId);

            for (int i = 0; i < filteredBugs.size(); i++) {
                Bug bug1 = filteredBugs.get(i);

                for (int j = i + 1; j < filteredBugs.size(); j++) {
                    Bug bug2 = filteredBugs.get(j);

                    // Calculate similarity between these two bugs
                    double similarityScore = calculateBugSimilarity(bug1, bug2, configs);

                    // Only include if above threshold
                    if (similarityScore >= threshold) {
                        log.debug("Found similarity {} between bug {} (ID: {}) and bug {} (ID: {})",
                                similarityScore, bug1.getTitle(), bug1.getId(), bug2.getTitle(), bug2.getId());

                        BugSimilarityRelationship relationship = createSimilarityRelationship(bug1, bug2,
                                similarityScore, configs);
                        results.add(relationship);
                        log.debug("Added similarity relationship: Bug A (ID: {}), Bug B (ID: {}), Similarity: {}",
                                relationship.getBugAId(), relationship.getBugBId(), relationship.getSimilarityScore());
                    }
                }
            }

            log.debug("Total similarity results before sorting: {}", results.size());
            // Commented out verbose logging for now
            // log.debug("Results breakdown by Bug A ID: {}",
            // results.stream()
            // .collect(Collectors.groupingBy(BugSimilarityRelationship::getBugAId,
            // Collectors.counting())));

            // Sort results
            Comparator<BugSimilarityRelationship> comparator = getSimilarityComparator(sortBy, sortDirection);
            results.sort(comparator);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, results.size());

            if (startIndex >= results.size()) {
                Pageable pageable = PageRequest.of(page, size);
                return new PageImpl<>(Collections.emptyList(), pageable, results.size());
            }

            List<BugSimilarityRelationship> paginatedResults = results.subList(startIndex, endIndex);
            log.debug("Returning {} similarity results for project {} (page {}, size {})",
                    paginatedResults.size(), projectId, page, size);

            // Final debug logging for paginated results - commented out for now
            // log.debug("Final paginated results breakdown by Bug A ID: {}",
            // paginatedResults.stream()
            // .collect(Collectors.groupingBy(BugSimilarityRelationship::getBugAId,
            // Collectors.counting())));

            // log.debug("Final results details: {}",
            // paginatedResults.stream()
            // .map(result -> String.format("Bug A (ID: %d, Title: '%s') vs Bug B (ID: %d,
            // Title: '%s'), Similarity: %.3f",
            // result.getBugAId(), result.getBugATitle(), result.getBugBId(),
            // result.getBugBTitle(), result.getSimilarityScore()))
            // .collect(Collectors.joining(", ")));

            // Create Page object with pagination metadata
            Pageable pageable = PageRequest.of(page, size);
            Page<BugSimilarityRelationship> finalPage = new PageImpl<>(paginatedResults, pageable, results.size());

            // Strategic logging for pagination debugging
            log.debug("=== PAGINATION DEBUG INFO ===");
            log.debug("Requested page: {}, size: {}", page, size);
            log.debug("Total results available: {}", results.size());
            log.debug("Paginated results count: {}", paginatedResults.size());
            log.debug("PageImpl created with:");
            log.debug("  - content size: {}", finalPage.getContent().size());
            log.debug("  - total elements: {}", finalPage.getTotalElements());
            log.debug("  - total pages: {}", finalPage.getTotalPages());
            log.debug("  - current page number: {}", finalPage.getNumber());
            log.debug("  - page size: {}", finalPage.getSize());
            log.debug("  - has next: {}", finalPage.hasNext());
            log.debug("  - has previous: {}", finalPage.hasPrevious());
            log.debug("=== END PAGINATION DEBUG ===");

            return finalPage;

        } catch (Exception e) {
            log.error("Error getting project similarity analysis for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve project similarity analysis", e);
        }
    }

    /**
     * Create a similarity relationship between two bugs.
     * 
     * @param bug1            First bug (source)
     * @param bug2            Second bug (similar to first)
     * @param similarityScore Similarity score between the bugs
     * @param configs         Similarity algorithm configurations
     * @return BugSimilarityRelationship object
     */
    private BugSimilarityRelationship createSimilarityRelationship(Bug bug1, Bug bug2, double similarityScore,
            List<SimilarityConfig> configs) {
        log.debug(
                "Creating similarity relationship: bug1 (ID: {}, Title: '{}') vs bug2 (ID: {}, Title: '{}') with score: {}",
                bug1.getId(), bug1.getTitle(), bug2.getId(), bug2.getTitle(), similarityScore);

        BugSimilarityRelationship relationship = new BugSimilarityRelationship();

        // Set Bug A information (source bug)
        relationship.setBugAId(bug1.getId());
        relationship.setBugAProjectTicketNumber(bug1.getProjectTicketNumber());
        relationship.setBugATitle(bug1.getTitle());
        relationship.setBugADescription(bug1.getDescription());
        relationship.setBugAStatus(bug1.getStatus());
        relationship.setBugAPriority(bug1.getPriority());
        relationship.setBugACreatedAt(bug1.getCreatedAt());
        relationship.setBugAUpdatedAt(bug1.getUpdatedAt());

        // Set Bug B information (similar bug)
        relationship.setBugBId(bug2.getId());
        relationship.setBugBProjectTicketNumber(bug2.getProjectTicketNumber());
        relationship.setBugBTitle(bug2.getTitle());
        relationship.setBugBDescription(bug2.getDescription());
        relationship.setBugBStatus(bug2.getStatus());
        relationship.setBugBPriority(bug2.getPriority());
        relationship.setBugBCreatedAt(bug2.getCreatedAt());
        relationship.setBugBUpdatedAt(bug2.getUpdatedAt());

        // Set similarity information
        relationship.setSimilarityScore(similarityScore);

        // Set assignee and reporter names for both bugs
        if (bug1.getAssignee() != null) {
            relationship.setBugAAssigneeName(getUserDisplayName(bug1.getAssignee()));
        } else {
            relationship.setBugAAssigneeName("Unassigned");
        }

        if (bug1.getReporter() != null) {
            relationship.setBugAReporterName(getUserDisplayName(bug1.getReporter()));
        } else {
            relationship.setBugAReporterName("Unknown");
        }

        if (bug2.getAssignee() != null) {
            relationship.setBugBAssigneeName(getUserDisplayName(bug2.getAssignee()));
        } else {
            relationship.setBugBAssigneeName("Unassigned");
        }

        if (bug2.getReporter() != null) {
            relationship.setBugBReporterName(getUserDisplayName(bug2.getReporter()));
        } else {
            relationship.setBugBReporterName("Unknown");
        }

        // Calculate algorithm-specific scores
        Map<SimilarityAlgorithm, Double> algorithmScores = new HashMap<>();
        try {
            String text1 = (bug1.getTitle() + " " + (bug1.getDescription() != null ? bug1.getDescription() : ""))
                    .trim();
            String text2 = (bug2.getTitle() + " " + (bug2.getDescription() != null ? bug2.getDescription() : ""))
                    .trim();

            for (SimilarityConfig config : configs) {
                if (config.isEnabled()) {
                    double score = similarityCalculator.calculateSimilarity(
                            text1, text2, config.getAlgorithmName());
                    algorithmScores.put(config.getAlgorithmName(), score);
                }
            }
        } catch (Exception e) {
            log.warn("Error calculating algorithm scores: {}", e.getMessage());
        }
        relationship.setAlgorithmScores(algorithmScores);

        // Generate text fingerprint
        try {
            String text = (bug2.getTitle() + " " + (bug2.getDescription() != null ? bug2.getDescription() : "")).trim();
            relationship.setTextFingerprint(textPreprocessor.generateTextFingerprint(text, ""));
        } catch (Exception e) {
            log.warn("Error generating text fingerprint: {}", e.getMessage());
            relationship.setTextFingerprint("fingerprint_error");
        }

        // Check if this relationship is already marked as duplicate
        try {
            Optional<BugDuplicate> duplicate = bugDuplicateRepository.findDuplicateRelationship(
                    bug1.getId(), bug2.getId());
            relationship.setAlreadyMarkedDuplicate(duplicate.isPresent());
            if (duplicate.isPresent()) {
                relationship.setOriginalBugId(duplicate.get().getOriginalBug().getId());
            }
        } catch (Exception e) {
            log.warn("Error checking duplicate status: {}", e.getMessage());
            relationship.setAlreadyMarkedDuplicate(false);
        }

        log.debug(
                "Created BugSimilarityRelationship: Bug A (ID: {}, Title: '{}') vs Bug B (ID: {}, Title: '{}') with Score: {}",
                relationship.getBugAId(), relationship.getBugATitle(), relationship.getBugBId(),
                relationship.getBugBTitle(), relationship.getSimilarityScore());

        return relationship;
    }

    /**
     * Calculate similarity between two bugs using configured algorithms.
     * 
     * @param bug1    First bug
     * @param bug2    Second bug
     * @param configs Similarity algorithm configurations
     * @return Combined similarity score (0.0 to 1.0)
     */
    private double calculateBugSimilarity(Bug bug1, Bug bug2, List<SimilarityConfig> configs) {
        try {
            String text1 = (bug1.getTitle() + " " + (bug1.getDescription() != null ? bug1.getDescription() : ""))
                    .trim();
            String text2 = (bug2.getTitle() + " " + (bug2.getDescription() != null ? bug2.getDescription() : ""))
                    .trim();

            if (text1.isEmpty() || text2.isEmpty()) {
                return 0.0;
            }

            double totalScore = 0.0;
            double totalWeight = 0.0;

            for (SimilarityConfig config : configs) {
                if (config.isEnabled()) {
                    double score = similarityCalculator.calculateSimilarity(
                            text1, text2, config.getAlgorithmName());
                    totalScore += score * config.getWeightAsDouble();
                    totalWeight += config.getWeightAsDouble();
                }
            }

            return totalWeight > 0 ? totalScore / totalWeight : 0.0;

        } catch (Exception e) {
            log.warn("Error calculating similarity between bugs {} and {}: {}",
                    bug1.getId(), bug2.getId(), e.getMessage());
            return 0.0;
        }
    }

    /**
     * Create BugSimilarityResult from two bugs and their similarity score.
     * 
     * @param bug1            First bug (the "source" bug)
     * @param bug2            Second bug (the "similar" bug)
     * @param similarityScore Similarity score between the bugs
     * @param configs         Similarity algorithm configurations
     * @return BugSimilarityResult DTO
     */
    // private BugSimilarityResult createSimilarityResult(Bug bug1, Bug bug2, double
    // similarityScore,
    // List<SimilarityConfig> configs) {
    // log.debug("Creating similarity result: bug1 (ID: {}, Title: '{}') vs bug2
    // (ID: {}, Title: '{}') with score: {}",
    // bug1.getId(), bug1.getTitle(), bug2.getId(), bug2.getTitle(),
    // similarityScore);

    // BugSimilarityResult result = new BugSimilarityResult();
    // result.setBugId(bug2.getId());
    // result.setProjectTicketNumber(bug2.getProjectTicketNumber());
    // result.setTitle(bug2.getTitle());
    // result.setDescription(bug2.getDescription());
    // result.setSimilarityScore(similarityScore);
    // result.setStatus(bug2.getStatus());
    // result.setPriority(bug2.getPriority());
    // result.setCreatedAt(bug2.getCreatedAt());
    // result.setUpdatedAt(bug2.getUpdatedAt());

    // log.debug("Created BugSimilarityResult with BugID: {}, ProjectTicket: {},
    // Title: '{}', Score: {}",
    // result.getBugId(), result.getProjectTicketNumber(), result.getTitle(),
    // result.getSimilarityScore());

    // // Set assignee and reporter names
    // if (bug2.getAssignee() != null) {
    // result.setAssigneeName(getUserDisplayName(bug2.getAssignee()));
    // } else {
    // result.setAssigneeName("Unassigned");
    // }

    // if (bug2.getReporter() != null) {
    // result.setReporterName(getUserDisplayName(bug2.getReporter()));
    // } else {
    // result.setReporterName("Unknown");
    // }

    // // Calculate algorithm-specific scores
    // Map<SimilarityAlgorithm, Double> algorithmScores = new HashMap<>();
    // try {
    // String text1 = (bug1.getTitle() + " " + (bug1.getDescription() != null ?
    // bug1.getDescription() : ""))
    // .trim();
    // String text2 = (bug2.getTitle() + " " + (bug2.getDescription() != null ?
    // bug2.getDescription() : ""))
    // .trim();

    // for (SimilarityConfig config : configs) {
    // if (config.isEnabled()) {
    // double score = similarityCalculator.calculateSimilarity(
    // text1, text2, config.getAlgorithmName());
    // algorithmScores.put(config.getAlgorithmName(), score);
    // }
    // }
    // } catch (Exception e) {
    // log.warn("Error calculating algorithm scores: {}", e.getMessage());
    // }
    // result.setAlgorithmScores(algorithmScores);

    // // Generate text fingerprint
    // try {
    // String text = (bug2.getTitle() + " " + (bug2.getDescription() != null ?
    // bug2.getDescription() : "")).trim();
    // result.setTextFingerprint(textPreprocessor.generateTextFingerprint(text,
    // ""));
    // } catch (Exception e) {
    // log.warn("Error generating text fingerprint: {}", e.getMessage());
    // result.setTextFingerprint("fingerprint_error");
    // }

    // // Check if this bug is already marked as duplicate
    // try {
    // Optional<BugDuplicate> duplicate =
    // bugDuplicateRepository.findDuplicateRelationship(
    // bug1.getId(), bug2.getId());
    // result.setAlreadyMarkedDuplicate(duplicate.isPresent());
    // if (duplicate.isPresent()) {
    // result.setOriginalBugId(duplicate.get().getOriginalBug().getId());
    // }
    // } catch (Exception e) {
    // log.warn("Error checking duplicate status: {}", e.getMessage());
    // result.setAlreadyMarkedDuplicate(false);
    // }

    // return result;
    // }

    /**
     * Get comparator for sorting similarity results.
     * 
     * @param sortBy        Sort field
     * @param sortDirection Sort direction
     * @return Comparator for BugSimilarityResult
     */
    private Comparator<BugSimilarityRelationship> getSimilarityComparator(String sortBy, String sortDirection) {
        Comparator<BugSimilarityRelationship> comparator;

        switch (sortBy.toLowerCase()) {
            case "title":
                comparator = Comparator.comparing(BugSimilarityRelationship::getBugATitle);
                break;
            case "createdat":
                comparator = Comparator.comparing(BugSimilarityRelationship::getBugACreatedAt);
                break;
            case "updatedat":
                comparator = Comparator.comparing(BugSimilarityRelationship::getBugAUpdatedAt);
                break;
            case "status":
                comparator = Comparator.comparing(result -> result.getBugAStatus().toString());
                break;
            case "priority":
                comparator = Comparator.comparing(result -> result.getBugAPriority().toString());
                break;
            case "similarityscore":
            default:
                comparator = Comparator.comparing(BugSimilarityRelationship::getSimilarityScore);
                break;
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return comparator;
    }
}