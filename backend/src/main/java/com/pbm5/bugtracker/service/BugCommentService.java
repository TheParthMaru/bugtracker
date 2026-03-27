package com.pbm5.bugtracker.service;

import com.pbm5.bugtracker.entity.Bug;
import com.pbm5.bugtracker.entity.BugComment;
import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.exception.CommentNotFoundException;
import com.pbm5.bugtracker.repository.BugCommentRepository;
import com.pbm5.bugtracker.repository.BugRepository;
import com.pbm5.bugtracker.repository.UserRepository;
import com.pbm5.bugtracker.service.BugNotificationEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BugCommentService {

    @Autowired
    private BugCommentRepository bugCommentRepository;

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BugNotificationEventListener bugNotificationEventListener;

    // CRUD Operations

    /**
     * Create a new comment
     */
    public BugComment createComment(UUID projectId, Long bugId, String content, UUID authorId, Long parentId) {
        // Validate bug exists
        Bug bug = bugRepository.findByProjectIdAndId(projectId, bugId)
                .orElseThrow(() -> new RuntimeException("Bug not found"));

        // Validate author exists
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found"));

        // Validate parent comment if provided
        BugComment parent = null;
        if (parentId != null) {
            parent = bugCommentRepository.findById(parentId)
                    .orElseThrow(() -> new CommentNotFoundException("Parent comment not found"));
        }

        // Create comment
        BugComment comment = new BugComment(content, author);
        comment.setBug(bug);
        comment.setParent(parent);

        BugComment savedComment = bugCommentRepository.save(comment);

        // Trigger notification for comment (async - won't affect transaction)
        try {
            if (bugNotificationEventListener != null) {
                // Eagerly load assignee and reporter to avoid lazy loading issues in async
                // method
                User assignee = bug.getAssignee();
                User reporter = bug.getReporter();

                // Log the notification attempt for debugging
                System.out.println("Attempting to send comment notification: bug=" + bug.getId() +
                        ", assignee=" + (assignee != null ? assignee.getId() : "null") +
                        ", reporter=" + (reporter != null ? reporter.getId() : "null") +
                        ", commenter=" + author.getId());

                bugNotificationEventListener.onBugCommented(bug, savedComment, author);

                // Note: Mention detection is now handled inside onBugCommented method
                // to ensure it runs in the same async context
            }
        } catch (Exception e) {
            // Log error but don't fail comment creation
            System.err.println("Failed to trigger comment notification: " + e.getMessage());
        }

        return savedComment;
    }

    /**
     * Get comment by ID
     */
    public BugComment getCommentById(UUID projectId, Long bugId, Long commentId) {
        return bugCommentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Comment not found with id: " + commentId));
    }

    /**
     * Update comment
     */
    public BugComment updateComment(UUID projectId, Long bugId, Long commentId, String content, UUID authorId) {
        BugComment comment = getCommentById(projectId, bugId, commentId);

        // Validate author owns the comment
        if (!comment.getAuthor().getId().equals(authorId)) {
            throw new RuntimeException("Only comment author can update the comment");
        }

        comment.setContent(content);
        return bugCommentRepository.save(comment);
    }

    /**
     * Delete comment
     */
    public void deleteComment(UUID projectId, Long bugId, Long commentId, UUID authorId) {
        BugComment comment = getCommentById(projectId, bugId, commentId);

        // Validate author owns the comment or is admin
        if (!comment.getAuthor().getId().equals(authorId)) {
            // TODO: Add admin check
            throw new RuntimeException("Only comment author can delete the comment");
        }

        bugCommentRepository.delete(comment);
    }

    // Query Operations

    /**
     * Get all comments for a bug
     */
    public List<BugComment> getCommentsByBugId(Long bugId) {
        return bugCommentRepository.findByBugIdOrderByCreatedAtAsc(bugId);
    }

    /**
     * Get top-level comments for a bug
     */
    public List<BugComment> getTopLevelComments(Long bugId) {
        return bugCommentRepository.findByBugIdAndParentIsNullOrderByCreatedAtAsc(bugId);
    }

    /**
     * Get replies for a comment
     */
    public List<BugComment> getRepliesByParentId(Long parentId) {
        return bugCommentRepository.findByParentIdOrderByCreatedAtAsc(parentId);
    }

    /**
     * Get comments by author
     */
    public List<BugComment> getCommentsByAuthor(Long projectId, UUID authorId) {
        return bugCommentRepository.findByProjectIdAndAuthorId(projectId, authorId);
    }

    /**
     * Search comments by content
     */
    public List<BugComment> searchCommentsByContent(Long bugId, String searchTerm) {
        return bugCommentRepository.findByBugIdAndContentContaining(bugId, searchTerm);
    }

    /**
     * Search comments by content in project
     */
    public List<BugComment> searchCommentsByContentInProject(Long projectId, String searchTerm) {
        return bugCommentRepository.findByProjectIdAndContentContaining(projectId, searchTerm);
    }

    // Pagination

    /**
     * Get recent comments with pagination
     */
    public Page<BugComment> getRecentComments(Long projectId, Pageable pageable) {
        return bugCommentRepository.findRecentCommentsByProjectId(projectId, pageable);
    }

    /**
     * Get recent comments for bug with pagination
     */
    public Page<BugComment> getRecentCommentsByBug(Long bugId, Pageable pageable) {
        return bugCommentRepository.findRecentCommentsByBugId(bugId, pageable);
    }

    // Analytics

    /**
     * Get comment statistics for project
     */
    public CommentStatistics getCommentStatistics(Long projectId) {
        long totalComments = bugCommentRepository.countByProjectId(projectId);
        long topLevelComments = bugCommentRepository.countTopLevelCommentsByBugId(0L); // This needs to be calculated
                                                                                       // differently
        List<Object[]> mostActiveUsers = bugCommentRepository.findMostActiveUsersByProjectId(projectId);

        return new CommentStatistics(totalComments, topLevelComments, mostActiveUsers);
    }

    /**
     * Get comment statistics for bug
     */
    public CommentStatistics getCommentStatisticsByBug(Long bugId) {
        long totalComments = bugCommentRepository.countByBugId(bugId);
        long topLevelComments = bugCommentRepository.countTopLevelCommentsByBugId(bugId);
        List<Object[]> mostActiveUsers = bugCommentRepository.findMostActiveUsersByProjectId(0L); // This needs to be
                                                                                                  // calculated
                                                                                                  // differently

        return new CommentStatistics(totalComments, topLevelComments, mostActiveUsers);
    }

    /**
     * Get comment activity over time
     */
    public List<Object[]> getCommentActivity(Long projectId, LocalDateTime startDate, LocalDateTime endDate) {
        return bugCommentRepository.findCommentActivityByProjectId(projectId, startDate, endDate);
    }

    /**
     * Get thread depth distribution
     */
    public List<Object[]> getThreadDepthDistribution(Long projectId) {
        return bugCommentRepository.findThreadDepthDistributionByProjectId(projectId);
    }

    /**
     * Get edited comments
     */
    public List<BugComment> getEditedComments(Long projectId) {
        return bugCommentRepository.findEditedCommentsByProjectId(projectId);
    }

    public List<BugComment> getEditedCommentsByBug(Long bugId) {
        return bugCommentRepository.findEditedCommentsByBugId(bugId);
    }

    // Validation Methods

    /**
     * Check if comment exists
     */
    public boolean commentExists(Long bugId, Long commentId) {
        return bugCommentRepository.existsByBugIdAndId(bugId, commentId);
    }

    /**
     * Check if parent comment exists
     */
    public boolean parentCommentExists(Long parentId, Long commentId) {
        return bugCommentRepository.existsByParentIdAndId(parentId, commentId);
    }

    /**
     * Validate comment content
     */
    public boolean isValidCommentContent(String content) {
        return content != null && !content.trim().isEmpty() && content.length() <= 10000;
    }

    // Utility Methods

    /**
     * Get comment depth
     */
    public int getCommentDepth(BugComment comment) {
        int depth = 0;
        BugComment current = comment;
        while (current.getParent() != null) {
            depth++;
            current = current.getParent();
        }
        return depth;
    }

    /**
     * Check if comment is top-level
     */
    public boolean isTopLevelComment(BugComment comment) {
        return comment.getParent() == null;
    }

    /**
     * Check if comment has replies
     */
    public boolean hasReplies(BugComment comment) {
        return !comment.getReplies().isEmpty();
    }

    /**
     * Get reply count for comment
     */
    public long getReplyCount(Long parentId) {
        return bugCommentRepository.countRepliesByParentId(parentId);
    }

    // Statistics DTO
    public static class CommentStatistics {
        private final long totalComments;
        private final long topLevelComments;
        private final List<Object[]> mostActiveUsers;

        public CommentStatistics(long totalComments, long topLevelComments, List<Object[]> mostActiveUsers) {
            this.totalComments = totalComments;
            this.topLevelComments = topLevelComments;
            this.mostActiveUsers = mostActiveUsers;
        }

        // Getters
        public long getTotalComments() {
            return totalComments;
        }

        public long getTopLevelComments() {
            return topLevelComments;
        }

        public List<Object[]> getMostActiveUsers() {
            return mostActiveUsers;
        }

        public long getReplyComments() {
            return totalComments - topLevelComments;
        }

        public double getAverageRepliesPerComment() {
            return topLevelComments > 0 ? (double) getReplyComments() / topLevelComments : 0.0;
        }
    }
}