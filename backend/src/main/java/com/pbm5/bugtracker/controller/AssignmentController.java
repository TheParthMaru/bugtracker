package com.pbm5.bugtracker.controller;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pbm5.bugtracker.entity.User;
import com.pbm5.bugtracker.service.AssignmentOrchestrator;

import lombok.RequiredArgsConstructor;

/**
 * Controller for testing and managing auto-assignment operations.
 * This controller provides endpoints to test the complete auto-assignment
 * workflow (teams + users) for existing bugs.
 */
@RestController
@RequestMapping("/api/bugtracker/v1/assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssignmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentController.class);

    private final AssignmentOrchestrator assignmentOrchestrator;

    /**
     * Execute complete auto-assignment for a bug by project ticket number.
     * This endpoint allows testing the auto-assignment workflow on existing bugs.
     * 
     * @param projectId           the project ID
     * @param projectTicketNumber the project ticket number
     * @return the assigned user information
     */
    @PostMapping("/projects/{projectId}/bugs/{projectTicketNumber}/auto-assign")
    public ResponseEntity<?> executeCompleteAutoAssignment(
            @PathVariable UUID projectId,
            @PathVariable Integer projectTicketNumber) {

        logger.info("Executing complete auto-assignment for project: {}, ticket: {}",
                projectId, projectTicketNumber);

        try {
            User assignedUser = assignmentOrchestrator.executeCompleteAutoAssignmentByProjectTicket(
                    projectId, projectTicketNumber);

            if (assignedUser != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Auto-assignment completed successfully",
                        "assignedUser", Map.of(
                                "id", assignedUser.getId(),
                                "firstName", assignedUser.getFirstName(),
                                "lastName", assignedUser.getLastName(),
                                "email", assignedUser.getEmail())));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Teams assigned but no suitable user found",
                        "assignedUser", null));
            }

        } catch (Exception e) {
            logger.error("Error during auto-assignment for project: {}, ticket: {}",
                    projectId, projectTicketNumber, e);

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Auto-assignment failed: " + e.getMessage(),
                    "error", e.getClass().getSimpleName()));
        }
    }
}