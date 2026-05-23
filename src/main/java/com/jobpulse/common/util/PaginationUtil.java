package com.jobpulse.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pagination utility for converting request parameters to Spring Data Pageable.
 */
public class PaginationUtil {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Create a Pageable from page and size parameters
     */
    public static Pageable getPageable(Integer pageNumber, Integer pageSize, String sortBy, String sortDirection) {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(page, size);
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    /**
     * Create a Pageable with default sorting (createdAt DESC)
     */
    public static Pageable getPageable(Integer pageNumber, Integer pageSize) {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int size = pageSize == null ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}

