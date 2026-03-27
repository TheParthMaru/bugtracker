## Backend Mock Data Audit

Scope: Full scan of `backend/` for any mock data, seeded data, dev/test endpoints, or placeholder responses that could surface data not present in DB.

### Summary

- No in-code mock datasets (e.g., hardcoded lists of projects/bugs/users) were found in controllers/services.
- There are intentional seed inserts in Flyway migrations (system labels, default project for migration, default similarity configurations).
- One explicit dev/test endpoint exists.
- Several endpoints return safe fallback responses (empty lists or zeroed analytics) on error; these are not mock data but can look like “data present” when troubleshooting.
- Some debug `System.out.println` statements exist; not mock data but should be switched to structured logging.

### Confirmed Seeded Data (Flyway)

1. `V10__Create_bug_labels_table.sql` – Inserts system labels

   - File: `backend/src/main/resources/db/migration/V10__Create_bug_labels_table.sql`
   - Lines ~15-30: INSERTs for labels like Frontend, Backend, Database, API, UI/UX, Security, etc.

2. ~~`V8__Migrate_teams_to_project_submodule.sql` – Inserts a default project for legacy teams (migration safety)~~ ✅ **COMMENTED OUT BY USER**

   - File: `backend/src/main/resources/db/migration/V8__Migrate_teams_to_project_submodule.sql`
   - Lines ~10-24: INSERT `Legacy Teams Project` if not exists.
   - **Status**: User has commented out these lines as they're not needed for fresh DB presentation

3. `V19__Create_bug_similarity_tables.sql` – Inserts default similarity configurations for existing projects

   - File: `backend/src/main/resources/db/migration/V19__Create_bug_similarity_tables.sql`
   - Lines ~93-101: INSERT default configs for COSINE/JACCARD/LEVENSHTEIN for all active projects.

These are legitimate seed/migration data and not mock runtime data.

### Dev/Test Endpoints

1. General test endpoint
   - File: `backend/src/main/java/com/pbm5/bugtracker/controller/GeneralBugController.java`
   - Method: `GET /api/bugtracker/v1/bugs/test` returns a static message ("GeneralBugController is working!")

### Fallback/Placeholder Responses (Not Mock Data)

These are returned on error/exception paths to avoid leaking details; they are not mock data, but may appear as unexpected non-empty responses.

1. Duplicate analytics and duplicates listing (similarity controller)

   - File: `backend/src/main/java/com/pbm5/bugtracker/controller/BugSimilarityController.java`
   - `getDuplicatesOfBug(...)`: on error returns `ResponseEntity.status(500).body(List.of())` (empty list)
   - `getDuplicateAnalytics(...)`: on error returns `ResponseEntity.status(500).body(new DuplicateAnalyticsResponse(0L, Map.of(), Map.of()))`

2. Similarity service empty returns
   - File: `backend/src/main/java/com/pbm5/bugtracker/service/BugSimilarityService.java`
   - Multiple branches return `Collections.emptyList()` when preconditions aren’t met or caches are empty; these are safeguards, not mock data.

### Hardcoded Defaults (Domain/Config Constants)

These are domain defaults or validation constants; not mock data.

- `BugSimilarityCache.algorithmUsed` default `"COSINE"` (entity field default)
- `SimilarityConfig` default weight/threshold values
- Stop-words, regex patterns, and utility constants in `TextPreprocessor` and validators

### Debug/Dev Logging to Review

1. `System.out.println` usage
   - File: `backend/src/main/java/com/pbm5/bugtracker/service/BugAttachmentService.java` (download path logging)
   - Recommendation: replace with `logger.debug/info` and remove before production if noisy.

### What Was Not Found

- No controllers/services returning fabricated bug/project/team/user lists.
- No Java-side in-memory repositories with hardcoded sample data.
- No `@Profile("dev")` initializers or `@PostConstruct` seeders creating mock rows at runtime.

### Recommendations

- Keep Flyway seeds (labels, config) if desired; otherwise gate under environment or separate “sample data” migrations.
- Remove or guard the `GET /bugs/test` endpoint for production.
- Replace `System.out.println` statements with structured logging.
- For error fallbacks that return empty lists/zeroed analytics, consider also returning an error code/message to avoid confusion during testing.

### Verification Notes

- Scanned all Java files in `backend/src/main/java` and SQL migrations in `backend/src/main/resources/db/migration` for: mock/test indicators, hardcoded lists, inserts, dev-only constructs, and empty-return fallbacks.
- Found only the items listed above.
