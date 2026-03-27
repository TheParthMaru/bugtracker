# 🚨 **SIMILARITY ANALYSIS IMPLEMENTATION TODO**

## **Critical Backend & Frontend Integration Required**

---

## 📋 **CURRENT STATUS**

- **Frontend**: SimilarityAnalysisPage exists but uses hardcoded mock data
- **Backend**: No similarity analysis endpoints implemented
- **Functionality**: **NON-FUNCTIONAL** - Core duplicate detection not working
- **Priority**: **HIGH** - Affects core bug tracking functionality

---

## 🎯 **REQUIRED BACKEND IMPLEMENTATION**

### **1. BugSimilarityService**

**File**: `backend/src/main/java/com/pbm5/bugtracker/service/BugSimilarityService.java`

#### **Required Methods**:

```java
public interface BugSimilarityService {
    // Get all bugs in a project with similarity scores
    List<BugSimilarityResult> getProjectSimilarityAnalysis(
        String projectSlug,
        double similarityThreshold,
        String searchTerm,
        String sortBy,
        String sortDirection
    );

    // Mark a bug as duplicate of another bug
    void markBugAsDuplicate(
        String projectSlug,
        Long bugId,
        Long originalBugId,
        String reason
    );

    // Calculate similarity scores between bugs
    Map<Long, Double> calculateSimilarityScores(
        String projectSlug,
        Long targetBugId
    );
}
```

#### **Implementation Requirements**:

- **Similarity Algorithms**: Implement COSINE, JACCARD, LEVENSHTEIN
- **Performance**: Handle large numbers of bugs efficiently
- **Caching**: Cache similarity results to avoid recalculation
- **Batch Processing**: Process similarity analysis in batches

---

### **2. BugSimilarityController**

**File**: `backend/src/main/java/com/pbm5/bugtracker/controller/BugSimilarityController.java`

#### **Required Endpoints**:

```java
@RestController
@RequestMapping("/api/bugtracker/v1/projects/{projectSlug}/bugs/similarity")
public class BugSimilarityController {

    // Get similarity analysis for all bugs in project
    @GetMapping("/analysis")
    public ResponseEntity<PageResponse<BugSimilarityResult>> getSimilarityAnalysis(
        @PathVariable String projectSlug,
        @RequestParam(defaultValue = "0.4") double threshold,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "similarityScore") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );

    // Mark bug as duplicate
    @PostMapping("/{bugId}/mark-duplicate")
    public ResponseEntity<Void> markAsDuplicate(
        @PathVariable String projectSlug,
        @PathVariable Long bugId,
        @RequestBody MarkDuplicateRequest request
    );

    // Get similarity between two specific bugs
    @GetMapping("/{bugId}/similarity/{otherBugId}")
    public ResponseEntity<BugSimilarityResult> getBugSimilarity(
        @PathVariable String projectSlug,
        @PathVariable Long bugId,
        @PathVariable Long otherBugId
    );
}
```

---

### **3. Database Schema Updates**

**File**: `backend/src/main/resources/db/migration/V25__Add_similarity_analysis_tables.sql`

#### **Required Tables**:

```sql
-- Store similarity analysis results
CREATE TABLE bug_similarity_cache (
    id BIGSERIAL PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    bug_id BIGINT NOT NULL REFERENCES bugs(id),
    similar_bug_id BIGINT NOT NULL REFERENCES bugs(id),
    similarity_score DECIMAL(5,4) NOT NULL,
    algorithm_scores JSONB NOT NULL,
    text_fingerprint TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, bug_id, similar_bug_id)
);

-- Store duplicate relationships
CREATE TABLE bug_duplicates (
    id BIGSERIAL PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    original_bug_id BIGINT NOT NULL REFERENCES bugs(id),
    duplicate_bug_id BIGINT NOT NULL REFERENCES bugs(id),
    marked_by UUID NOT NULL REFERENCES users(id),
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, original_bug_id, duplicate_bug_id)
);

-- Indexes for performance
CREATE INDEX idx_bug_similarity_cache_project_bug ON bug_similarity_cache(project_id, bug_id);
CREATE INDEX idx_bug_similarity_cache_score ON bug_similarity_cache(similarity_score);
CREATE INDEX idx_bug_duplicates_project ON bug_duplicates(project_id);
```

---

### **4. DTO Updates**

**File**: `backend/src/main/java/com/pbm5/bugtracker/dto/`

#### **New DTOs Required**:

```java
// MarkDuplicateRequest.java
public class MarkDuplicateRequest {
    private Long originalBugId;
    private Long duplicateBugId;
    private String reason;
    // getters, setters, validation
}

// SimilarityAnalysisRequest.java
public class SimilarityAnalysisRequest {
    private double threshold;
    private String searchTerm;
    private String sortBy;
    private String sortDirection;
    private int page;
    private int size;
    // getters, setters, validation
}
```

---

## 🎨 **REQUIRED FRONTEND IMPLEMENTATION**

### **1. Replace Mock Data with Real API Calls**

**File**: `frontend/src/pages/SimilarityAnalysisPage.tsx`

#### **Current Issues**:

- ❌ Hardcoded mock data (lines 144-200)
- ❌ No real API integration
- ❌ No error handling for API failures
- ❌ No loading states for API operations

#### **Required Changes**:

```typescript
// Replace mock data with real API call
const loadSimilarBugs = useCallback(async () => {
	if (!projectSlug) return;

	try {
		setState((prev) => ({ ...prev, loading: true, error: null }));

		// TODO: Replace with real API call
		const response = await bugService.getSimilarityAnalysis(
			projectSlug,
			state.similarityThreshold,
			state.searchTerm,
			state.sortBy,
			state.sortDirection
		);

		setState((prev) => ({
			...prev,
			similarBugs: response.data,
			loading: false,
			lastRefresh: new Date(),
		}));
	} catch (error) {
		// Handle API errors properly
		setState((prev) => ({
			...prev,
			loading: false,
			error: error.message,
		}));
	}
}, [
	projectSlug,
	state.similarityThreshold,
	state.searchTerm,
	state.sortBy,
	state.sortDirection,
]);
```

---

### **2. Update BugService**

**File**: `frontend/src/services/bugService.ts`

#### **Add New Methods**:

```typescript
export const bugService = {
	// ... existing methods ...

	// Get similarity analysis for project
	async getSimilarityAnalysis(
		projectSlug: string,
		threshold: number,
		searchTerm?: string,
		sortBy: string = "similarityScore",
		sortDirection: "asc" | "desc" = "desc",
		page: number = 0,
		size: number = 20
	): Promise<ApiResponse<BugSimilarityResult[]>> {
		const params = new URLSearchParams({
			threshold: threshold.toString(),
			sortBy,
			sortDirection,
			page: page.toString(),
			size: size.toString(),
		});

		if (searchTerm) {
			params.append("search", searchTerm);
		}

		const response = await api.get(
			`/projects/${projectSlug}/bugs/similarity/analysis?${params}`
		);
		return response.data;
	},

	// Mark bug as duplicate
	async markAsDuplicate(
		projectSlug: string,
		bugId: number,
		originalBugId: number,
		reason?: string
	): Promise<ApiResponse<void>> {
		const response = await api.post(
			`/projects/${projectSlug}/bugs/similarity/${bugId}/mark-duplicate`,
			{ originalBugId, duplicateBugId: bugId, reason }
		);
		return response.data;
	},
};
```

---

### **3. Implement Real-time Updates**

**File**: `frontend/src/pages/SimilarityAnalysisPage.tsx`

#### **Add Real-time Features**:

```typescript
// Real-time similarity analysis updates
useEffect(() => {
	if (!projectSlug) return;

	// Initial load
	loadSimilarBugs();

	// Set up polling for real-time updates
	const interval = setInterval(() => {
		loadSimilarBugs();
	}, 5 * 60 * 1000); // Every 5 minutes

	return () => clearInterval(interval);
}, [projectSlug, loadSimilarBugs]);

// Optimistic updates for duplicate marking
const handleMarkAsDuplicate = async (bugId: number, originalBugId: number) => {
	try {
		// Optimistically update UI
		setState((prev) => ({
			...prev,
			similarBugs: prev.similarBugs.map((bug) =>
				bug.bugId === bugId ? { ...bug, isAlreadyMarkedDuplicate: true } : bug
			),
		}));

		// Call API
		await bugService.markAsDuplicate(projectSlug, bugId, originalBugId);

		// Show success message
		toast.success("Bug marked as duplicate successfully");

		// Refresh data
		loadSimilarBugs();
	} catch (error) {
		// Revert optimistic update on error
		loadSimilarBugs();
		toast.error("Failed to mark bug as duplicate");
	}
};
```

---

## 🧪 **TESTING REQUIREMENTS**

### **Backend Testing**:

- [ ] Unit tests for BugSimilarityService
- [ ] Integration tests for BugSimilarityController
- [ ] Performance tests for similarity algorithms
- [ ] Database migration tests

### **Frontend Testing**:

- [ ] Unit tests for similarity analysis logic
- [ ] Integration tests for API calls
- [ ] E2E tests for duplicate marking workflow
- [ ] Performance tests for large datasets

---

## 📊 **PERFORMANCE CONSIDERATIONS**

### **Backend Performance**:

- **Similarity Calculation**: Use efficient algorithms and caching
- **Database Queries**: Optimize with proper indexes
- **Batch Processing**: Process similarity analysis in background
- **Caching Strategy**: Cache results to avoid recalculation

### **Frontend Performance**:

- **Virtual Scrolling**: Handle large lists of similar bugs
- **Debounced Search**: Avoid excessive API calls
- **Lazy Loading**: Load similarity data on demand
- **Optimistic Updates**: Provide immediate feedback

---

## 🚀 **IMPLEMENTATION PRIORITY**

### **Phase 1: Backend Foundation (Week 1)**

1. Create database migration for similarity tables
2. Implement BugSimilarityService with basic algorithms
3. Create BugSimilarityController with core endpoints
4. Add basic DTOs and validation

### **Phase 2: Frontend Integration (Week 2)**

1. Update BugService with new API methods
2. Replace mock data with real API calls
3. Implement proper error handling
4. Add loading states and user feedback

### **Phase 3: Advanced Features (Week 3)**

1. Implement real-time updates
2. Add advanced filtering and sorting
3. Optimize performance for large datasets
4. Add comprehensive testing

---

## ⚠️ **RISKS & MITIGATION**

### **Risk 1: Performance Issues**

- **Mitigation**: Implement efficient algorithms and caching
- **Mitigation**: Use background processing for similarity analysis

### **Risk 2: Data Accuracy**

- **Mitigation**: Implement multiple similarity algorithms
- **Mitigation**: Add validation and testing for accuracy

### **Risk 3: User Experience**

- **Mitigation**: Provide immediate feedback with optimistic updates
- **Mitigation**: Implement proper loading states and error handling

---

## 📝 **SUCCESS CRITERIA**

- [ ] All mock data removed from SimilarityAnalysisPage
- [ ] Real API endpoints working for similarity analysis
- [ ] Duplicate marking functionality working
- [ ] Performance acceptable for projects with 1000+ bugs
- [ ] Comprehensive test coverage
- [ ] User experience smooth and responsive

---

## 🔗 **RELATED FILES**

### **Backend Files to Create/Update**:

- `BugSimilarityService.java`
- `BugSimilarityController.java`
- `V25__Add_similarity_analysis_tables.sql`
- `MarkDuplicateRequest.java`
- `SimilarityAnalysisRequest.java`

### **Frontend Files to Update**:

- `SimilarityAnalysisPage.tsx` (remove mock data)
- `bugService.ts` (add new methods)
- `types/similarity.ts` (update types if needed)

---

_This document should be updated as implementation progresses. Last updated: [Current Date]_
