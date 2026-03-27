package com.pbm5.bugtracker.dto;

import com.pbm5.bugtracker.entity.Role;
import lombok.Data;

import java.util.Set;

@Data
public class UserResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private Set<String> skills;

    // constructors
    public UserResponse(String id, String firstName, String lastName, String email, Role role, Set<String> skills) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.skills = skills;
    }

    public UserResponse(com.pbm5.bugtracker.entity.User user) {
        this.id = user.getId().toString();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.skills = user.getSkills();
    }
}
