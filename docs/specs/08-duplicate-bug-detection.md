# Duplicate Bug Detection System Specification

## Overview

The Duplicate Bug Detection System is an advanced feature that automatically identifies and manages duplicate bug reports using machine learning-based text similarity algorithms. The system employs multiple similarity algorithms from Apache Commons Text library, including Cosine Similarity, Jaccard Similarity, and Levenshtein Distance, to provide comprehensive duplicate detection capabilities with configurable thresholds and project-specific optimization.

## Business Requirements

### Core Concept

- **Duplicate Detection** is the process of identifying bug reports that describe the same issue using text similarity analysis
- The system uses multiple similarity algorithms to calculate similarity scores between bug reports
- Similarity scores range from 0.0 (completely different) to 1.0 (identical)
- The system supports both automatic detection and manual marking of duplicates
- Project-specific configuration allows customization of algorithm weights and thresholds

### User Stories

#### As a Bug Reporter

- I can see warnings when creating bugs that are similar to existing bugs
- I can view detailed similarity analysis showing why bugs are considered similar
- I can choose to proceed with bug creation even if similar bugs are found
- I can mark my bug as a duplicate of an existing bug if I discover it later
- I can provide additional context when marking bugs as duplicates

#### As a Bug Assignee

- I can receive notifications when bugs are marked as duplicates of bugs I'm working on
- I can view all bugs that are duplicates of bugs assigned to me
- I can access similarity analysis to understand the relationship between bugs
- I can contribute to the duplicate detection system by confirming or rejecting suggestions

#### As a Project Manager

- I can configure similarity detection settings for my project
- I can view analytics on duplicate detection effectiveness
- I can manage duplicate relationships and resolve conflicts
- I can export duplicate detection reports for analysis

#### As a System Administrator

- I can monitor the performance of similarity algorithms
- I can adjust system-wide similarity thresholds and configurations
- I can view duplicate detection analytics across all projects
- I can manage the similarity cache and optimize system performance

## Data Model

### Core Entities

#### BugDuplicate Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/BugDuplicate.java`](backend/src/main/java/com/pbm5/bugtracker/entity/BugDuplicate.java)

The BugDuplicate entity represents duplicate relationships between bugs:

- **UUID primary key** for efficient querying and relationship management
- **Original bug reference** with mandatory original_bug_id foreign key
- **Duplicate bug reference** with mandatory duplicate_bug_id foreign key
- **User tracking** with marked_by_user_id for audit trail
- **Confidence score** with precision 5,4 for accurate similarity representation
- **Detection method** enum (MANUAL, AUTOMATIC, HYBRID) for tracking how duplicates were identified
- **Additional context** for user-provided explanations
- **Timestamp tracking** with marked_at for audit purposes

#### DuplicateDetectionMethod Enum

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/DuplicateDetectionMethod.java`](backend/src/main/java/com/pbm5/bugtracker/entity/DuplicateDetectionMethod.java)

The DuplicateDetectionMethod enum defines how duplicates are identified:

- **MANUAL**: User explicitly marked bugs as duplicates
- **AUTOMATIC**: System automatically detected duplicates using similarity algorithms
- **HYBRID**: System suggested duplicates, confirmed by user

#### SimilarityAlgorithm Enum

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/SimilarityAlgorithm.java`](backend/src/main/java/com/pbm5/bugtracker/entity/SimilarityAlgorithm.java)

The SimilarityAlgorithm enum defines available similarity algorithms:

- **COSINE**: Vector-based similarity using term frequencies
- **JACCARD**: Set-based overlap similarity
- **LEVENSHTEIN**: Character-level edit distance similarity

### Supporting Entities

#### SimilarityConfig Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/SimilarityConfig.java`](backend/src/main/java/com/pbm5/bugtracker/entity/SimilarityConfig.java)

The SimilarityConfig entity manages project-specific algorithm configurations:

- **Project association** with project_id foreign key
- **Algorithm configuration** with algorithm_name, weight, and threshold
- **Enable/disable control** with is_enabled boolean flag
- **Audit tracking** with created_at and updated_at timestamps

#### BugSimilarityCache Entity

**File**: [`backend/src/main/java/com/pbm5/bugtracker/entity/BugSimilarityCache.java`](backend/src/main/java/com/pbm5/bugtracker/entity/BugSimilarityCache.java)

The BugSimilarityCache entity optimizes performance through caching:

- **Cache key management** with text_fingerprint (SHA-256 hash)
- **Similarity storage** with bug_id, similar_bug_id, and similarity_score
- **Algorithm tracking** with algorithm_used for cache invalidation
- **Expiration management** with expires_at for automatic cleanup

## Database Schema

### Core Tables

#### Bug Duplicates Table

**Migration**: [`V19__Create_bug_similarity_tables.sql`](backend/src/main/resources/db/migration/V19__Create_bug_similarity_tables.sql)

The bug_duplicates table stores duplicate relationships:

- **UUID primary key** for efficient relationship management
- **Bug references** with original_bug_id and duplicate_bug_id foreign keys
- **User tracking** with marked_by_user_id for audit trail
- **Confidence scoring** with NUMERIC(5,4) precision for accurate similarity representation
- **Detection method** with VARCHAR(50) and CHECK constraint for valid methods
- **Context storage** with TEXT field for additional user explanations
- **Unique constraints** preventing duplicate relationships and circular references

#### Similarity Cache Table

**Migration**: [`V19__Create_bug_similarity_tables.sql`](backend/src/main/resources/db/migration/V19__Create_bug_similarity_tables.sql)

The bug_similarity_cache table optimizes performance:

- **Cache management** with text_fingerprint (SHA-256 hash) for content-based caching
- **Similarity storage** with bug_id, similar_bug_id, and similarity_score
- **Algorithm tracking** with algorithm_used for cache invalidation strategies
- **Expiration control** with expires_at timestamp for automatic cleanup
- **Performance indexes** for efficient cache lookups and similarity queries

#### Similarity Configuration Table

**Migration**: [`V19__Create_bug_similarity_tables.sql`](backend/src/main/resources/db/migration/V19__Create_bug_similarity_tables.sql)

The similarity_config table manages project-specific settings:

- **Project association** with project_id foreign key
- **Algorithm configuration** with algorithm_name, weight, and threshold
- **Enable/disable control** with is_enabled boolean flag
- **Unique constraints** ensuring one configuration per project per algorithm
- **Audit tracking** with created_at and updated_at timestamps

### Performance Optimizations

**Migration**: [`V19__Create_bug_similarity_tables.sql`](backend/src/main/resources/db/migration/V19__Create_bug_similarity_tables.sql)

Strategic indexes optimize similarity queries:

- **Cache indexes**: bug_id, similarity_score, expires_at, text_fingerprint
- **Duplicate indexes**: original_bug_id, duplicate_bug_id, marked_by_user_id
- **Configuration indexes**: project_id, is_enabled for efficient algorithm lookup
- **Composite indexes**: Combined columns for complex similarity queries

## Similarity Algorithms

### Text Preprocessing

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/TextPreprocessor.java`](backend/src/main/java/com/pbm5/bugtracker/service/TextPreprocessor.java)

The TextPreprocessor service prepares text for similarity analysis:

#### Text Cleaning Process

1. **Case normalization**: Convert to lowercase for consistent comparison
2. **Code snippet removal**: Remove code blocks (`code`) that may contain non-meaningful content
3. **URL and email removal**: Remove URLs and email addresses that don't contribute to similarity
4. **Special character removal**: Remove punctuation while preserving spaces
5. **Whitespace normalization**: Replace multiple spaces with single spaces

#### Tokenization and Filtering

1. **Word tokenization**: Split text into individual words
2. **Length filtering**: Remove tokens shorter than 3 characters
3. **Number filtering**: Remove pure numeric tokens
4. **Stop word removal**: Remove common English words and programming terms
5. **Case normalization**: Convert all tokens to lowercase

#### Stop Words List

The system removes common English stop words and programming terms:

- **Basic stop words**: the, a, an, and, or, but, in, on, at, to, for, of, with, by
- **Pronouns**: i, me, my, myself, we, our, ours, ourselves, you, your, yours
- **Verbs**: am, is, are, was, were, be, been, being, have, has, had, having
- **Programming terms**: null, true, false, undefined, void, var, let, const
- **Bug report terms**: issue, problem, error, bug, fix, please, thanks, help

#### TF-IDF Vector Generation

1. **Term frequency calculation**: TF = (count of term) / (total number of terms)
2. **Inverse document frequency**: IDF = log(total_documents / documents_containing_term)
3. **TF-IDF combination**: TF-IDF = TF × IDF
4. **Vector normalization**: Prepare vectors for similarity calculations

#### Text Fingerprinting

1. **Content combination**: Combine preprocessed title and description
2. **SHA-256 hashing**: Generate unique fingerprint for caching
3. **Cache key generation**: Use fingerprint for efficient cache lookups

### Similarity Calculator

**File**: [`backend/src/main/java/com/pbm5/bugtracker/service/SimilarityCalculator.java`](backend/src/main/java/com/pbm5/bugtracker/service/SimilarityCalculator.java)

The SimilarityCalculator service implements multiple similarity algorithms using Apache Commons Text:

#### Cosine Similarity

**Algorithm**: Vector-based similarity using term frequencies
**Implementation**: Apache Commons Text CosineSimilarity
**Formula**: cosine_similarity = (A · B) / (||A|| × ||B||)
**Use case**: Effective for comparing documents of different lengths
**Advantages**: Normalizes for document length, good for semantic similarity

**Calculation Process**:

1. Create character frequency maps from preprocessed text
2. Use Apache Commons Text CosineSimilarity.cosineSimilarity()
3. Return similarity score between 0.0 and 1.0

#### Jaccard Similarity

**Algorithm**: Set-based overlap similarity
**Implementation**: Apache Commons Text JaccardSimilarity
**Formula**: jaccard_similarity = |A ∩ B| / |A ∪ B|
**Use case**: Effective for keyword-based comparison and shared terminology
**Advantages**: Simple to understand, good for exact term matching

**Calculation Process**:

1. Preprocess texts to remove noise and normalize
2. Use Apache Commons Text JaccardSimilarity.apply()
3. Return similarity score between 0.0 and 1.0

#### Levenshtein Similarity

**Algorithm**: Character-level edit distance similarity
**Implementation**: Apache Commons Text LevenshteinDistance
**Formula**: levenshtein_similarity = 1 - (distance / max_length)
**Use case**: Effective for detecting typos and minor variations
**Advantages**: Good for detecting near-duplicates with small differences

**Calculation Process**:

1. Preprocess texts for consistent comparison
2. Calculate Levenshtein distance using Apache Commons Text
3. Convert distance to similarity: similarity = 1 - (distance / max_length)
4. Return similarity score between 0.0 and 1.0

#### Weighted Similarity Combination

**Algorithm**: Weighted combination of multiple similarity measures
**Formula**: weighted_similarity = Σ(score_i × weight_i) / Σ(weight_i)
**Use case**: Leverages strengths of different algorithms
**Advantages**: More robust than single algorithm approach

**Calculation Process**:

1. Calculate similarity using each enabled algorithm
2. Apply algorithm-specific weights from configuration
3. Combine scores using weighted average
4. Normalize by total weight for consistent scoring

### Performance Optimizations

#### Quick Pre-filtering

**Purpose**: Avoid expensive calculations for obviously dissimilar texts
**Method**: Keyword overlap analysis using TF-IDF
**Threshold**: Configurable minimum overlap ratio (default: 0.2)
**Process**:

1. Extract top keywords from both texts using TF-IDF
2. Calculate keyword overlap ratio
3. Skip full similarity calculation if overlap is below threshold

#### Caching Strategy

**Purpose**: Avoid recalculating similarity for identical content
**Method**: SHA-256 text fingerprinting
**Cache duration**: 7 days with automatic expiration
**Cache key**: Project ID + text fingerprint
**Invalidation**: Automatic expiration and manual cache clearing

## API Endpoints

### Similarity Analysis

**Base URL**: `/api/bugtracker/v1/projects/{projectSlug}/similarity`

#### POST /api/bugtracker/v1/projects/{projectSlug}/similarity/check

- **Purpose**: Check for similar bugs before creating a new bug
- **Auth**: Required (Project members only)
- **Body**: `{ title, description, threshold?, maxResults?, includeClosedBugs? }`
- **Response**: List of similar bugs with similarity scores and algorithm details
- **Business Logic**:
  - Validate project access and user permissions
  - Preprocess input text using TextPreprocessor
  - Check cache for existing similarity results
  - Calculate similarity using configured algorithms
  - Return results sorted by similarity score

#### GET /api/bugtracker/v1/projects/{projectSlug}/similarity/{bugId}/similar

- **Purpose**: Find bugs similar to an existing bug
- **Auth**: Required (Project members only)
- **Query Parameters**:
  - `threshold` (optional): Similarity threshold (default: 0.75)
  - `maxResults` (optional): Maximum results (default: 10)
  - `includeClosedBugs` (optional): Include closed bugs (default: false)
- **Response**: List of similar bugs with detailed similarity analysis
- **Business Logic**:
  - Validate bug access and project permissions
  - Retrieve bug content (title and description)
  - Calculate similarity against all other bugs in project
  - Apply filters and return ranked results

### Duplicate Management

#### POST /api/bugtracker/v1/projects/{projectSlug}/similarity/{bugId}/mark-duplicate

- **Purpose**: Mark a bug as duplicate of another bug
- **Auth**: Required (Project members only)
- **Body**: `{ originalBugId, confidenceScore?, isAutomaticDetection?, additionalContext? }`
- **Response**: Success confirmation with duplicate relationship details
- **Business Logic**:
  - Validate both bugs exist and belong to project
  - Check for existing duplicate relationships
  - Prevent circular duplicate relationships
  - Create BugDuplicate entity with confidence score
  - Update duplicate bug status to CLOSED
  - Trigger notifications for relevant users

#### GET /api/bugtracker/v1/projects/{projectSlug}/similarity/{bugId}/duplicates

- **Purpose**: Get all duplicates of a specific bug
- **Auth**: Required (Project members only)
- **Response**: List of duplicate bugs with relationship details
- **Business Logic**:
  - Find all bugs marked as duplicates of the specified bug
  - Include confidence scores and detection methods
  - Return detailed relationship information

#### DELETE /api/bugtracker/v1/projects/{projectSlug}/similarity/duplicates/{duplicateId}

- **Purpose**: Remove a duplicate relationship
- **Auth**: Required (Project admin or duplicate marker only)
- **Response**: Success confirmation
- **Business Logic**:
  - Validate user permissions for duplicate removal
  - Remove BugDuplicate relationship
  - Update duplicate bug status if needed
  - Log removal for audit purposes

### Configuration Management

#### GET /api/bugtracker/v1/projects/{projectSlug}/similarity/config

- **Purpose**: Get similarity configuration for a project
- **Auth**: Required (Project members only)
- **Response**: Project similarity configuration with algorithm settings
- **Business Logic**:
  - Return enabled algorithms with weights and thresholds
  - Include default configurations if none exist
  - Provide algorithm descriptions and use cases

#### PUT /api/bugtracker/v1/projects/{projectSlug}/similarity/config

- **Purpose**: Update similarity configuration for a project
- **Auth**: Required (Project admin only)
- **Body**: Array of algorithm configurations with weights and thresholds
- **Response**: Updated configuration
- **Business Logic**:
  - Validate configuration parameters
  - Update or create SimilarityConfig entities
  - Clear similarity cache to apply new settings
  - Log configuration changes for audit

### Analytics and Reporting

#### GET /api/bugtracker/v1/projects/{projectSlug}/similarity/analytics

- **Purpose**: Get duplicate detection analytics for a project
- **Auth**: Required (Project members only)
- **Response**: Duplicate analytics with statistics and trends
- **Business Logic**:
  - Calculate total duplicate count and detection method distribution
  - Analyze user contributions to duplicate identification
  - Generate confidence score statistics
  - Provide system performance metrics

## Integration with Other Modules

### Bugs Module Integration

The duplicate detection system integrates with the bugs module:

- **Bug creation**: Automatic similarity checking during bug creation
- **Bug updates**: Re-check similarity when bug content is modified
- **Bug lifecycle**: Update duplicate bug status when marked as duplicate
- **Bug relationships**: Maintain duplicate relationships in bug data model

### Projects Module Integration

The system integrates with projects for configuration and access control:

- **Project-scoped configuration**: Each project has independent similarity settings
- **Access control**: Project membership required for similarity analysis
- **Project initialization**: Automatic configuration setup for new projects
- **Project analytics**: Project-specific duplicate detection metrics

### Notifications Module Integration

The system integrates with notifications for user communication:

- **Duplicate notifications**: Notify users when bugs are marked as duplicates
- **Similarity alerts**: Alert users about potential duplicates during bug creation
- **System notifications**: Notify administrators about duplicate detection events
- **User preferences**: Respect user notification preferences for duplicate events

### Analytics Module Integration

The system provides data for analytics and reporting:

- **Duplicate statistics**: Duplicate count and detection method analysis
- **User contribution tracking**: Who identified duplicates and their effectiveness
- **System performance**: Similarity algorithm performance and accuracy metrics
- **Trend analysis**: Duplicate detection trends over time

## Frontend Implementation

### Pages/Routes

**Location**: `frontend/src/pages/`

- `/projects/{projectSlug}/similarity` - Similarity analysis and duplicate management
- `/projects/{projectSlug}/bugs/create` - Bug creation with similarity checking
- `/projects/{projectSlug}/bugs/{bugId}` - Bug details with duplicate information

### Key Components

**Location**: `frontend/src/components/bugs/`

- `DuplicateDetectionWarning` - Warning component for similar bugs during creation
- `DuplicateManagementPanel` - Interface for managing duplicate relationships
- `DuplicateManagementInterface` - Comprehensive duplicate management interface
- `SimilarityConfigPanel` - Configuration interface for similarity settings
- `DuplicateRelationshipDetails` - Display duplicate relationship information

### Custom Hooks

**Location**: `frontend/src/hooks/`

- `useDuplicateDetection` - Main hook for similarity checking with caching
- `useDebouncedSimilarityCheck` - Debounced similarity checking for real-time analysis
- `useDuplicateInfo` - Hook for loading duplicate information for specific bugs
- `useDuplicateAnalytics` - Hook for duplicate analytics and statistics

### Service Integration

**File**: [`frontend/src/services/bugService.ts`](frontend/src/services/bugService.ts)

The BugService provides comprehensive duplicate detection API integration:

- **Similarity checking**: Check for similar bugs before creation
- **Duplicate management**: Mark and manage duplicate relationships
- **Configuration management**: Update similarity settings
- **Analytics integration**: Retrieve duplicate detection statistics

### Type Definitions

**File**: [`frontend/src/types/similarity.ts`](frontend/src/types/similarity.ts)

The similarity types define data structures for:

- **BugSimilarityResult**: Similarity analysis results with scores and details
- **SimilarityCheckRequest**: Request parameters for similarity checking
- **DuplicateInfoResponse**: Duplicate relationship information
- **SimilarityConfig**: Algorithm configuration and settings
- **DuplicateAnalyticsResponse**: Analytics and statistics data

### State Management

The frontend uses React hooks for duplicate detection state management:

- **Similarity results**: Cached similarity analysis results
- **Loading states**: UI feedback for async similarity operations
- **Error handling**: Centralized error state management
- **Cache management**: Client-side caching for performance optimization
- **Real-time updates**: Live similarity checking during bug creation

## Security & Authorization

### Access Control Matrix

| Action                         | Anonymous | User | Project Member | Project Admin |
| ------------------------------ | --------- | ---- | -------------- | ------------- |
| Check similarity               | No        | No   | Yes            | Yes           |
| Mark duplicates                | No        | No   | Yes            | Yes           |
| Remove duplicate relationships | No        | No   | No             | Yes           |
| Configure similarity           | No        | No   | No             | Yes           |
| View duplicate analytics       | No        | No   | Yes            | Yes           |

### Business Rules

1. **Project Association**: Similarity analysis is always project-scoped
2. **Project Membership**: Users must be project members to access similarity features
3. **Duplicate Marking**: Any project member can mark bugs as duplicates
4. **Configuration Access**: Only project admins can modify similarity settings
5. **Data Privacy**: Similarity analysis respects project confidentiality
6. **Audit Trail**: All duplicate marking actions are logged with user information

## Technical Considerations

### Performance Optimizations

- **Caching**: Redis caching for similarity results with 7-day expiration
- **Pre-filtering**: Quick keyword overlap check before expensive calculations
- **Database indexes**: Strategic indexes for efficient similarity queries
- **Batch processing**: Efficient similarity calculation for multiple bugs
- **Memory management**: Optimized text processing and vector calculations

### Algorithm Configuration

- **Project-specific settings**: Each project can configure algorithm weights and thresholds
- **Default configurations**: Automatic setup of sensible defaults for new projects
- **Algorithm selection**: Enable/disable specific algorithms per project
- **Threshold tuning**: Adjustable similarity thresholds for different use cases
- **Weight optimization**: Configurable algorithm weights for optimal performance

### Error Handling

- **Graceful degradation**: System continues to work with partial algorithm failures
- **Input validation**: Comprehensive validation of similarity check parameters
- **Cache fallback**: Fallback to direct calculation when cache is unavailable
- **User feedback**: Clear error messages and loading states
- **Logging**: Comprehensive error logging for debugging and monitoring

### Scalability Considerations

- **Horizontal scaling**: Stateless design supports multiple server instances
- **Database optimization**: Efficient queries and indexes for large datasets
- **Cache distribution**: Redis clustering for high availability
- **Load balancing**: Even distribution of similarity calculation workload
- **Resource management**: Memory and CPU optimization for large-scale operations

## Implementation Status

### Completed Features

- **Multi-algorithm similarity**: Cosine, Jaccard, and Levenshtein similarity algorithms
- **Text preprocessing**: Comprehensive text cleaning and normalization
- **Caching system**: Redis-based caching with automatic expiration
- **Project configuration**: Per-project algorithm weights and thresholds
- **Duplicate management**: Mark, view, and remove duplicate relationships
- **Frontend integration**: Complete UI for similarity checking and duplicate management
- **Analytics integration**: Duplicate detection statistics and reporting
- **Performance optimization**: Pre-filtering and efficient similarity calculations

### Current Capabilities

- Automatic similarity checking during bug creation
- Manual duplicate marking with confidence scores
- Project-specific similarity configuration
- Comprehensive duplicate analytics and reporting
- Real-time similarity checking with debouncing
- Client-side caching for performance optimization
- Multi-channel notification integration
- Complete audit trail and user tracking

## Future Enhancements

### Advanced Algorithms

- Machine learning-based similarity detection
- Semantic similarity using word embeddings
- Context-aware similarity analysis
- Multi-language similarity support

### Enhanced User Experience

- Visual similarity highlighting
- Interactive similarity exploration
- Advanced filtering and search
- Bulk duplicate management

### System Improvements

- Real-time similarity updates
- Advanced caching strategies
- Performance monitoring and optimization
- Integration with external similarity services

### Analytics Enhancements

- Predictive duplicate detection
- User behavior analysis
- System effectiveness metrics
- Advanced reporting and visualization
