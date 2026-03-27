package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.BugStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateBugStatusRequest {

    @NotNull(message = "Status is required")
    private BugStatus status;

    public UpdateBugStatusRequest() {
    }

    public UpdateBugStatusRequest(BugStatus status) {
        this.status = status;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }
}