package com.jobpulse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Used for all list endpoints to provide consistent pagination information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean isLast;
    private boolean isFirst;

    public static <T> PageResponse<T> of(
            List<T> content,
            int pageNumber,
            int pageSize,
            long totalElements) {
        return PageResponse.<T>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageSize))
                .isLast(pageNumber >= Math.ceil((double) totalElements / pageSize) - 1)
                .isFirst(pageNumber == 0)
                .build();
    }
}

