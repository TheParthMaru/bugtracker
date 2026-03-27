package com.pbm5.bugtracker.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "team_members", uniqueConstraints = @UniqueConstraint(columnNames = { "team_id", "user_id" }))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamMember {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "team_id", nullable = false)
    @NotNull(message = "Team ID cannot be null")
    private UUID teamId;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID cannot be null")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Role cannot be null")
    @Builder.Default
    private TeamRole role = TeamRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "added_by", nullable = false)
    @NotNull(message = "Added by cannot be null")
    private UUID addedBy;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", insertable = false, updatable = false)
    @JsonIgnore
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by", insertable = false, updatable = false)
    @JsonIgnore
    private User addedByUser;

    // Utility methods
    public boolean isAdmin() {
        return role == TeamRole.ADMIN;
    }

    public boolean isMember() {
        return role == TeamRole.MEMBER;
    }

    public void promoteToAdmin() {
        this.role = TeamRole.ADMIN;
    }

    public void demoteToMember() {
        this.role = TeamRole.MEMBER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TeamMember))
            return false;
        TeamMember that = (TeamMember) o;
        return Objects.equals(id, that.id) ||
                (Objects.equals(teamId, that.teamId) && Objects.equals(userId, that.userId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamId, userId);
    }

    @Override
    public String toString() {
        return "TeamMember{" +
                "id=" + id +
                ", teamId=" + teamId +
                ", userId=" + userId +
                ", role=" + role +
                ", joinedAt=" + joinedAt +
                '}';
    }
}