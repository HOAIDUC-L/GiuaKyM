package com.course.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchCourseRequestDto implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum SearchType {
        ALL,
        COURSE_NAME,
        SUBJECT_CODE,
        LECTURER
    }

    private String keyword;
    private SearchType searchType = SearchType.ALL;

    public SearchCourseRequestDto() {
    }

    public SearchCourseRequestDto(String keyword, SearchType searchType) {
        this.keyword = keyword;
        this.searchType = searchType != null ? searchType : SearchType.ALL;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }
}
