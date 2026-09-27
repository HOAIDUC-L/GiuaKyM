package com.course.server.service;

import com.course.common.dto.SearchCourseRequestDto;
import com.course.common.model.Course;
import com.course.common.protocol.Action;
import com.course.common.protocol.ErrorCode;
import com.course.common.protocol.Response;
import com.course.server.network.ProtocolHandler;
import com.course.server.repository.CourseRepository;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service managing course catalog queries and searches.
 */
public class CourseService implements ProtocolHandler.CourseDelegate {
    private static final Logger LOGGER = Logger.getLogger(CourseService.class.getName());

    private final CourseRepository courseRepository;

    public CourseService() {
        this.courseRepository = new CourseRepository();
    }

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public Response getCourses(String requestId) {
        try {
            List<Course> courses = courseRepository.findAll();
            return Response.success(Action.GET_COURSES_RESPONSE, requestId, "Lấy danh sách học phần thành công.", courses);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to retrieve courses", e);
            return Response.error(Action.GET_COURSES_RESPONSE, requestId, ErrorCode.DATABASE_ERROR, "Không thể tải danh sách học phần.");
        }
    }

    @Override
    public Response searchCourses(SearchCourseRequestDto req, String requestId) {
        try {
            String keyword = req != null ? req.getKeyword() : "";
            SearchCourseRequestDto.SearchType type = req != null && req.getSearchType() != null ? req.getSearchType() : SearchCourseRequestDto.SearchType.ALL;

            List<Course> courses = courseRepository.search(keyword, type);
            return Response.success(Action.SEARCH_COURSES_RESPONSE, requestId, "Tìm kiếm học phần thành công.", courses);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to search courses with keyword: " + (req != null ? req.getKeyword() : null), e);
            return Response.error(Action.SEARCH_COURSES_RESPONSE, requestId, ErrorCode.DATABASE_ERROR, "Lỗi khi tìm kiếm học phần.");
        }
    }
}
