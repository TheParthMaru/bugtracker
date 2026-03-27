package com.pbm5.bugtracker.service;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Text preprocessing utility for bug similarity analysis.
 * 
 * This service provides text cleaning, tokenization, and preprocessing
 * capabilities required for similarity calculations using Apache Commons Text.
 * 
 * Features:
 * - Text normalization and cleaning
 * - Stop word removal
 * - Tokenization
 * - TF-IDF vector generation
 * - Text fingerprinting for caching
 * 
 * Citation: Text preprocessing techniques based on standard NLP practices
 * and optimized for bug report similarity analysis.
 * 
 * @author Advanced Features Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class TextPreprocessor {

    // Common English stop words for bug reports
    private static final Set<String> STOP_WORDS = Set.of(
            // Basic stop words
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "up", "about", "into", "through", "during",
            "before", "after", "above", "below", "between", "among", "this", "that",
            "these", "those", "i", "me", "my", "myself", "we", "our", "ours", "ourselves",
            "you", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself",
            "she", "her", "hers", "herself", "it", "its", "itself", "they", "them", "their",
            "theirs", "themselves", "what", "which", "who", "whom", "whose", "am", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "having", "do", "does", "did", "doing", "will", "would",
            "could", "should", "may", "might", "must", "can", "shall",

            // Common programming terms (often not meaningful for similarity)
            "null", "true", "false", "undefined", "void", "var", "let", "const",

            // Common bug report words (too generic to be meaningful)
            "issue", "problem", "error", "bug", "fix", "please", "thanks", "help");

    // Patterns for text cleaning
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    private static final Pattern NUMBERS_ONLY_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern CODE_SNIPPET_PATTERN = Pattern.compile("```[\\s\\S]*?```|`[^`]*`");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+|www\\.\\S+");
    private static final Pattern EMAIL_PATTERN = Pattern
            .compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

    /**
     * Preprocess text for similarity analysis by cleaning and normalizing
     * 
     * @param text Raw text input
     * @return Preprocessed text ready for similarity calculation
     */
    public String preprocessText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String processed = text.toLowerCase().trim();

        // Remove code snippets (they often contain non-meaningful content for
        // similarity)
        processed = CODE_SNIPPET_PATTERN.matcher(processed).replaceAll(" ");

        // Remove URLs and email addresses
        processed = URL_PATTERN.matcher(processed).replaceAll(" ");
        processed = EMAIL_PATTERN.matcher(processed).replaceAll(" ");

        // Remove special characters but preserve spaces
        processed = SPECIAL_CHARS_PATTERN.matcher(processed).replaceAll(" ");

        // Normalize whitespace
        processed = MULTIPLE_SPACES_PATTERN.matcher(processed).replaceAll(" ");

        return processed.trim();
    }

    /**
     * Tokenize text and remove stop words
     * 
     * @param text Preprocessed text
     * @return List of meaningful tokens
     */
    public List<String> tokenizeAndFilter(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(text.split("\\s+"))
                .filter(token -> !token.isEmpty())
                .filter(token -> token.length() > 2) // Remove very short tokens
                .filter(token -> !NUMBERS_ONLY_PATTERN.matcher(token).matches()) // Remove pure numbers
                .filter(token -> !STOP_WORDS.contains(token.toLowerCase()))
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    /**
     * Generate term frequency map for a text
     * 
     * @param tokens List of tokens from text
     * @return Map of term frequencies
     */
    public Map<String, Double> calculateTermFrequency(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> termCounts = new HashMap<>();
        for (String token : tokens) {
            termCounts.merge(token, 1, Integer::sum);
        }

        // Calculate term frequency (TF) = (count of term) / (total number of terms)
        int totalTerms = tokens.size();
        Map<String, Double> termFrequency = new HashMap<>();

        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            double tf = (double) entry.getValue() / totalTerms;
            termFrequency.put(entry.getKey(), tf);
        }

        return termFrequency;
    }

    /**
     * Calculate inverse document frequency for terms across a corpus
     * 
     * @param corpus Collection of all documents (bug texts)
     * @return Map of IDF values for terms
     */
    public Map<String, Double> calculateInverseDocumentFrequency(List<String> corpus) {
        if (corpus == null || corpus.isEmpty()) {
            return Collections.emptyMap();
        }

        // Get all unique terms across all documents
        Set<String> allTerms = new HashSet<>();
        List<Set<String>> documentTerms = new ArrayList<>();

        for (String document : corpus) {
            String processed = preprocessText(document);
            List<String> tokens = tokenizeAndFilter(processed);
            Set<String> docTerms = new HashSet<>(tokens);
            documentTerms.add(docTerms);
            allTerms.addAll(docTerms);
        }

        int totalDocuments = corpus.size();
        Map<String, Double> idfMap = new HashMap<>();

        // Calculate IDF for each term: log(total_documents / documents_containing_term)
        for (String term : allTerms) {
            long documentsWithTerm = documentTerms.stream()
                    .mapToLong(docTerms -> docTerms.contains(term) ? 1 : 0)
                    .sum();

            if (documentsWithTerm > 0) {
                double idf = Math.log((double) totalDocuments / documentsWithTerm);
                idfMap.put(term, idf);
            }
        }

        return idfMap;
    }

    /**
     * Generate TF-IDF vector representation for a text
     * 
     * @param text   Input text
     * @param corpus Collection of all texts for IDF calculation
     * @return TF-IDF vector as Map<String, Double>
     */
    public Map<String, Double> generateTfIdfVector(String text, List<String> corpus) {
        String processed = preprocessText(text);
        List<String> tokens = tokenizeAndFilter(processed);

        if (tokens.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Double> tfVector = calculateTermFrequency(tokens);
        Map<String, Double> idfMap = calculateInverseDocumentFrequency(corpus);

        // Combine TF and IDF: TF-IDF = TF * IDF
        Map<String, Double> tfidfVector = new HashMap<>();
        for (Map.Entry<String, Double> entry : tfVector.entrySet()) {
            String term = entry.getKey();
            double tf = entry.getValue();
            double idf = idfMap.getOrDefault(term, 0.0);
            tfidfVector.put(term, tf * idf);
        }

        return tfidfVector;
    }

    /**
     * Create character frequency map for Apache Commons Text CosineSimilarity
     * This method prepares text for use with Apache Commons Text algorithms
     * 
     * @param text Preprocessed text
     * @return Character frequency map suitable for CosineSimilarity
     */
    public Map<CharSequence, Integer> createCharacterFrequencyMap(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyMap();
        }

        // For better similarity detection, we'll use word-based frequencies
        // rather than character frequencies
        List<String> tokens = tokenizeAndFilter(text);
        Map<CharSequence, Integer> frequencyMap = new HashMap<>();

        for (String token : tokens) {
            frequencyMap.merge(token, 1, Integer::sum);
        }

        return frequencyMap;
    }

    /**
     * Generate a SHA-256 fingerprint of combined title and description
     * Used for caching and quick comparison
     * 
     * @param title       Bug title
     * @param description Bug description
     * @return SHA-256 hash as hex string
     */
    public String generateTextFingerprint(String title, String description) {
        try {
            String combined = preprocessText(title) + " " + preprocessText(description);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash if SHA-256 is not available
            return String.valueOf((title + description).hashCode());
        }
    }

    /**
     * Extract keywords from text (high TF-IDF terms)
     * 
     * @param text        Input text
     * @param corpus      Corpus for IDF calculation
     * @param maxKeywords Maximum number of keywords to return
     * @return List of top keywords ordered by TF-IDF score
     */
    public List<String> extractKeywords(String text, List<String> corpus, int maxKeywords) {
        Map<String, Double> tfidfVector = generateTfIdfVector(text, corpus);

        return tfidfVector.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(maxKeywords)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Check if two texts are likely to be similar based on keyword overlap
     * Quick pre-filter before expensive similarity calculations
     * 
     * @param text1           First text
     * @param text2           Second text
     * @param corpus          Corpus for keyword extraction
     * @param minOverlapRatio Minimum ratio of overlapping keywords
     * @return true if texts have sufficient keyword overlap
     */
    public boolean hasSignificantKeywordOverlap(String text1, String text2, List<String> corpus,
            double minOverlapRatio) {
        List<String> keywords1 = extractKeywords(text1, corpus, 20);
        List<String> keywords2 = extractKeywords(text2, corpus, 20);

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return false;
        }

        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        int maxKeywords = Math.max(keywords1.size(), keywords2.size());
        double overlapRatio = (double) intersection.size() / maxKeywords;

        return overlapRatio >= minOverlapRatio;
    }

    /**
     * Get preprocessing statistics for debugging and monitoring
     * 
     * @param originalText Original text before preprocessing
     * @return Map of preprocessing statistics
     */
    public Map<String, Object> getPreprocessingStats(String originalText) {
        String processed = preprocessText(originalText);
        List<String> tokens = tokenizeAndFilter(processed);

        Map<String, Object> stats = new HashMap<>();
        stats.put("originalLength", originalText != null ? originalText.length() : 0);
        stats.put("processedLength", processed.length());
        stats.put("originalWordCount", originalText != null ? originalText.split("\\s+").length : 0);
        stats.put("tokenCount", tokens.size());
        stats.put("uniqueTokenCount", new HashSet<>(tokens).size());
        stats.put("stopWordsRemoved", originalText != null ? originalText.split("\\s+").length - tokens.size() : 0);

        return stats;
    }
}