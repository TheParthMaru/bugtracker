# Frontend Refactoring Analysis: projectSlug and teamSlug Consistency

## Overview

This document tracks all the places in the frontend codebase that need to be updated to ensure consistency with the backend refactoring from `slug` to `projectSlug` and `teamSlug`.

## Current State Analysis

- Backend has been successfully refactored to use `project_slug` and `team_slug` columns
- Frontend still uses various naming conventions that need alignment
- Need to ensure all API calls, routing, and data handling use consistent naming

## Areas Requiring Changes

### 1. Type Definitions

**File: `src/types/project.ts`**

- [x] `Project` interface: `slug` field should be `projectSlug` (line 26)
- [x] All references to `slug` in project-related types
- [x] API response handling for `projectSlug`

**File: `src/types/team.ts`**

- [x] `Team` interface: `slug` field should be `teamSlug` (line 22)
- [x] All references to `slug` in team-related types
- [x] API response handling for `teamSlug`

**File: `src/types/bug.ts`**

- [x] `Bug` interface: `projectSlug` field exists and is correct (line 50)
- [x] Ensure consistency with backend `BugResponse`

### 2. API Services

**File: `src/services/projectService.ts`**

- [x] All method parameters using `slug` should use `projectSlug` (multiple methods: getProjectBySlug, updateProject, deleteProject, getProjectMembers, etc.)
- [x] API endpoint construction using `projectSlug`
- [x] Response handling for `projectSlug`

**File: `src/services/teamService.ts`**

- [x] All method parameters using `slug` should use `teamSlug` (getTeamBySlug method)
- [x] API endpoint construction using `teamSlug`
- [x] Response handling for `teamSlug`

**File: `src/services/bugService.ts`**

- [x] `projectSlug` usage is correct in all methods
- [x] Consistent with backend API calls

**File: `src/services/cacheService.ts`**

- [x] Update cache key methods to use `projectSlug` and `teamSlug`
- [x] Update method signatures for consistency

### 3. Routing and Navigation

**File: `src/App.tsx`**

- [x] Route parameters: All routes correctly use `:projectSlug` and `:teamSlug`
- [x] All project routes use consistent parameter naming
- [x] Team routes within projects are correctly defined

**File: `src/pages/ProjectDetailPage.tsx`**

- [x] URL parameter extraction uses `projectSlug` correctly
- [x] Navigation to other project-scoped pages needs verification

**File: `src/pages/ProjectTeamsPage.tsx`**

- [x] URL parameter handling: uses `slug` but should use `projectSlug` (line 46)
- [x] API calls using correct parameter names

**File: `src/pages/ProjectTeamDetailPage.tsx`**

- [x] Both `projectSlug` and `teamSlug` parameter handling needs verification
- [x] API calls and navigation

**File: `src/pages/SimilarityAnalysisPage.tsx`**

- [x] Uses `slug` parameter but should use `projectSlug` (line 48)

**File: `src/pages/TeamEditPage.tsx`**

- [x] Uses `slug` parameter but should use `teamSlug` (line 72)

**File: `src/pages/BugsListPage.tsx`**

- [x] Uses `slug: projectSlug` destructuring but should be consistent (line 59)

**File: `src/pages/BugsPage.tsx`**

- [x] Uses `slug: projectSlug` destructuring but should be consistent (line 54)

**File: `src/pages/CreateBugPage.tsx`**

- [x] Uses `slug: urlProjectSlug` destructuring but should be consistent (line 45)

**File: `src/pages/TeamDetailPage.tsx`**

- [x] Uses `slug` parameter but should use `teamSlug` (line 60)

**File: `src/pages/AnalyticsPage.tsx`**

- [x] Uses `slug: projectSlug` destructuring but should be consistent (line 7)

**File: `src/pages/ProjectMembersPage.tsx`**

- [x] Uses `projectSlug` parameter correctly (line 101)
- [x] API calls and navigation use correct parameter names

**File: `src/pages/ProjectEditPage.tsx`**

- [x] Correctly uses `projectSlug` parameter (line 35)

**File: `src/pages/LandingPage.tsx`**

- [x] `project.slug` references updated to `projectSlug` (line 564)

**File: `src/pages/ProjectsListPage.tsx`**

- [x] Multiple `project.slug` references updated to `projectSlug` (lines 377, 399, 419, 429)

**File: `src/pages/TeamsPage.tsx`**

- [x] Multiple `team.slug` and `project.slug` references updated (lines 359, 373, 383, 587)

### 4. Component Props and Data Flow

**File: `src/components/projects/ProjectCard.tsx`**

- [x] Props interface using `slug` vs `projectSlug` needs verification
- [x] Navigation calls using correct field names needs verification

**File: `src/components/teams/TeamCard.tsx`**

- [x] Props interface using `slug` vs `teamSlug` needs verification
- [x] Navigation calls using correct field names needs verification

**File: `src/components/teams/AddMemberModal.tsx`**

- [x] `projectSlug` prop usage is correct (line 78)
- [x] API calls using correct parameter names needs verification

**File: `src/components/ui/project-picker.tsx`**

- [x] Interface uses `slug` but should use `projectSlug` (line 9)
- [x] Props correctly use `selectedProjectSlug` and `onProjectSelect(projectSlug)`

**File: `src/components/ui/breadcrumb.tsx`**

- [x] `ProjectBreadcrumb` correctly uses `projectSlug` (line 138)
- [x] `TeamBreadcrumb` correctly uses `teamSlug` (line 190)

**File: `src/components/teams/TeamMemberList.tsx`**

- [x] Correctly uses `teamSlug` and `projectSlug` props (lines 68-69)

**File: `src/components/bugs/SimilarityConfigPanel.tsx`**

- [x] Correctly uses `projectSlug` prop (line 73)

**File: `src/components/bugs/TeamAssignmentSection.tsx`**

- [x] Correctly uses `projectSlug` prop (line 38)

**File: `src/components/bugs/DuplicateManagementInterface.tsx`**

- [x] Correctly uses `projectSlug` prop (line 22)

**File: `src/components/bugs/DuplicateStatusBadge.tsx`**

- [x] Correctly uses `projectSlug` prop (line 8)

**File: `src/components/analytics/AnalyticsDashboard.tsx`**

- [x] Correctly uses `projectSlug` prop (line 52)

**File: `src/components/bugs/BugDetailPage.tsx`**

- [x] Multiple `project.slug` references updated to `projectSlug` (lines 301, 317, 331, 338, 355)

**File: `src/components/projects/ProjectTeamCard.tsx`**

- [x] Multiple `team.slug` references updated to `teamSlug` (lines 55, 61, 67, 118)

**File: `src/components/teams/CreateTeamModal.tsx`**

- [x] `project.slug` references updated to `projectSlug` (lines 153, 170)

**File: `src/components/teams/TeamActionMenu.tsx`**

- [x] `team.slug` references updated to `teamSlug` (line 98)

### 5. Form Handling and Data Submission

**File: `src/components/projects/CreateProjectModal.tsx`**

- [x] Form data structure for `projectSlug` needs verification
- [x] API submission using correct field names needs verification

**File: `src/components/teams/CreateTeamModal.tsx`**

- [x] Form data structure correctly uses `projectSlug` (line 85)
- [x] API submission correctly includes `projectSlug` (line 166)

**File: `src/components/projects/CreateProjectTeamModal.tsx`**

- [x] Correctly uses `projectSlug` prop (line 294)

**File: `src/components/projects/PendingRequestsModal.tsx`**

- [x] Correctly uses `projectSlug` prop (line 49)

### 6. Utility Functions and Helpers

**File: `src/utils/logger.ts`**

- [x] No logging references to slug fields found
- [x] Field naming is consistent

**File: `src/hooks/useSearchParams.ts`**

- [x] No specific slug field handling found
- [x] Search and filter logic is generic

**File: `src/hooks/useDebouncedSimilarityCheck.ts`**

- [x] Correctly uses `projectSlug` parameter (line 9)

**File: `src/hooks/useDuplicateAnalytics.ts`**

- [x] Correctly uses `projectSlug` parameter (line 6)

**File: `src/hooks/useDuplicateInfo.ts`**

- [x] Correctly uses `projectSlug` parameter (line 7)

**File: `src/hooks/useDuplicateDetection.ts`**

- [x] Correctly uses `projectSlug` parameter (line 55)

**File: `src/hooks/useVersionedParams.ts`** - ✅ **COMPLETED** - All parameter handling uses correct naming conventions

### 7. Breadcrumb and Navigation Components

**File: `src/components/ui/breadcrumb.tsx`**

- [x] `ProjectBreadcrumb` component correctly uses `projectSlug`
- [x] `TeamBreadcrumb` component correctly uses `teamSlug`
- [x] Navigation logic uses correct field names

## Summary of Required Changes

### High Priority Changes (Critical for functionality)

#### Type Definitions

1. **`src/types/project.ts`** - ✅ **COMPLETED** - `projectSlug` field is correctly defined
2. **`src/types/team.ts`** - ✅ **COMPLETED** - `teamSlug` field is correctly defined

#### API Services

3. **`src/services/projectService.ts`** - ✅ **COMPLETED** - All methods correctly use `projectSlug` parameter
4. **`src/services/teamService.ts`** - ✅ **COMPLETED** - All methods correctly use `teamSlug` parameter
5. **`src/services/cacheService.ts`** - ✅ **COMPLETED** - All methods correctly use `projectSlug` and `teamSlug` parameters

#### Page Components

6. **`src/pages/ProjectTeamsPage.tsx`** - ✅ **COMPLETED** - Correctly uses `useParams<{ projectSlug: string }>()`
7. **`src/pages/SimilarityAnalysisPage.tsx`** - ✅ **COMPLETED** - Now correctly uses `useParams<{ projectSlug: string }>()`
8. **`src/pages/TeamEditPage.tsx`** - ✅ **COMPLETED** - Correctly uses `teamSlug` parameter
9. **`src/pages/TeamDetailPage.tsx`** - ✅ **COMPLETED** - Correctly uses `teamSlug` parameter
10. **`src/pages/ProjectMembersPage.tsx`** - ✅ **COMPLETED** - Correctly uses `projectSlug` parameter

#### Component Props

11. **`src/components/ui/project-picker.tsx`** - ✅ **COMPLETED** - Now correctly imports Project type from @/types/project

### Medium Priority Changes (Important for consistency)

#### Component Props and Data Flow

12. **`src/components/projects/ProjectCard.tsx`** - ✅ **COMPLETED** - Props interface and navigation calls verified
13. **`src/components/teams/TeamCard.tsx`** - ✅ **COMPLETED** - Props interface and navigation calls verified
14. **`src/components/teams/AddMemberModal.tsx`** - ✅ **COMPLETED** - All props and API calls verified
15. **`src/components/ui/project-picker.tsx`** - ✅ **COMPLETED** - Interface and props verified
16. **`src/components/ui/breadcrumb.tsx`** - ✅ **COMPLETED** - All breadcrumb components verified
17. **`src/components/teams/TeamMemberList.tsx`** - ✅ **COMPLETED** - Props usage verified
18. **`src/components/bugs/*`** - ✅ **COMPLETED** - All bug-related components verified
19. **`src/components/analytics/AnalyticsDashboard.tsx`** - ✅ **COMPLETED** - Props usage verified

**File: `src/components/bugs/BugDetailPage.tsx`** - ✅ **COMPLETED** - All `project.slug` references updated to `projectSlug`

**File: `src/components/projects/ProjectTeamCard.tsx`** - ✅ **COMPLETED** - All `team.slug` references updated to `teamSlug`

**File: `src/components/teams/CreateTeamModal.tsx`** - ✅ **COMPLETED** - All `project.slug` references updated to `projectSlug`

**File: `src/components/teams/TeamActionMenu.tsx`** - ✅ **COMPLETED** - All `team.slug` references updated to `teamSlug`

#### Form Handling and Data Submission

20. **`src/components/projects/CreateProjectModal.tsx`** - ✅ **COMPLETED** - Form data structure and API submission verified
21. **`src/components/teams/CreateTeamModal.tsx`** - ✅ **COMPLETED** - Form data structure and API submission verified
22. **`src/components/projects/CreateProjectTeamModal.tsx`** - ✅ **COMPLETED** - Props usage verified
23. **`src/components/projects/PendingRequestsModal.tsx`** - ✅ **COMPLETED** - Props usage verified

#### Utility Functions and Helpers

24. **`src/utils/logger.ts`** - ✅ **COMPLETED** - No slug field references found
25. **`src/hooks/*`** - ✅ **COMPLETED** - All hooks correctly use projectSlug parameter

### Medium Priority Changes (Important for consistency)

#### Parameter Destructuring (make consistent)

12. **`src/pages/BugsListPage.tsx`** - ✅ **COMPLETED** - All parameter destructuring uses correct naming
13. **`src/pages/BugsPage.tsx`** - ✅ **COMPLETED** - All parameter destructuring uses correct naming
14. **`src/pages/CreateBugPage.tsx`** - ✅ **COMPLETED** - All parameter destructuring uses correct naming
15. **`src/pages/AnalyticsPage.tsx`** - ✅ **COMPLETED** - All parameter destructuring uses correct naming

### Files That Are Already Correct

- `src/types/bug.ts` - `projectSlug` field is correct
- `src/services/bugService.ts` - All methods use `projectSlug` correctly
- `src/App.tsx` - All routes use correct parameter names
- `src/pages/ProjectDetailPage.tsx` - Uses `projectSlug` correctly
- `src/pages/ProjectEditPage.tsx` - Uses `projectSlug` correctly
- Most component props are already using correct field names
- All hooks are using correct parameter names

## Priority Levels

### High Priority (Critical for functionality)

- Type definitions in `src/types/`
- API service method signatures
- Route parameter handling
- Component prop interfaces

### Medium Priority (Important for consistency)

- Form data structures
- Navigation logic
- Breadcrumb components
- Utility functions

### Low Priority (Cosmetic/cleanup)

- Logging messages
- Error messages
- Documentation strings

## Testing Strategy

1. **Unit Tests**: Update any existing tests to use new field names
2. **Integration Tests**: Verify API calls work with new parameter names
3. **Manual Testing**: Test all navigation flows and form submissions
4. **Cross-browser Testing**: Ensure consistent behavior across browsers

## Migration Steps

1. Update type definitions first
2. Update API service interfaces
3. Update component props and interfaces
4. Update routing and navigation logic
5. Update form handling
6. Update utility functions
7. Test thoroughly
8. Update documentation

## Notes

- This refactoring should be done incrementally to avoid breaking changes
- Each change should be tested before moving to the next
- Consider creating a feature branch for this refactoring
- Document any breaking changes for team communication

## Status

- [x] Analysis complete ✅
- [x] Type definitions updated ✅
- [x] API services updated ✅
- [x] Components updated ✅
- [x] Routing updated (App.tsx is correct) ✅
- [x] Component props and data flow verified ✅
- [x] Form handling and data submission verified ✅
- [x] Utility functions and helpers verified ✅
- [x] Routing and navigation verified ✅
- [x] All page components verified ✅
- [x] Missing component files have been fixed (build errors resolved)
- [ ] Testing completed
- [x] Documentation updated ✅

## Analysis Results

- **Total files requiring changes**: 25
- **High priority changes**: 11 (critical for functionality) - **ALL COMPLETED** (100%)
- **Medium priority changes**: 14 (important for consistency) - **ALL COMPLETED** (100%)
- **Files already correct**: 25+ (no changes needed)
- **Estimated effort**: **ZERO** - All files are now completed!

## Next Steps

1. ✅ **COMPLETED**: Type definitions (Project and Team interfaces)
2. ✅ **COMPLETED**: API service method signatures
3. ✅ **COMPLETED**: Component props interfaces
4. ✅ **COMPLETED**: All page components have been verified and use correct parameters
5. ✅ **COMPLETED**: cacheService.ts has been verified and uses correct `projectSlug` and `teamSlug` parameters
6. ✅ **COMPLETED**: All component files have been verified and use correct naming conventions
7. ✅ **COMPLETED**: All redundant parameter destructuring patterns have been resolved
8. **TESTING**: Ready for comprehensive testing of all navigation and API calls
