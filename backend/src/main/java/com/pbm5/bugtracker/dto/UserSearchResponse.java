package com.pbm5.bugtracker.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.pbm5.bugtracker.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user search response with pagination support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {

    private List<User> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Create UserSearchResponse from a Spring Data Page
     */
    public static UserSearchResponse fromPage(Page<User> page) {
        return UserSearchResponse.builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}