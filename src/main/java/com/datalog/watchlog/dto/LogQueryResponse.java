package com.datalog.watchlog.dto;

import com.datalog.watchlog.document.LogDocument;

import java.util.List;

/**
 * Paginated wrapper around Elasticsearch log hits.
 *
 * @param logs      the matching documents for the requested page
 * @param totalHits total number of matches across all pages
 * @param page      the zero-based page that was returned
 */
public record LogQueryResponse(
        List<LogDocument> logs,
        long totalHits,
        int page) {
}
