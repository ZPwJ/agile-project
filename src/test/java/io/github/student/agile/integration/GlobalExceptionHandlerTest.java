package io.github.student.agile.integration;

import io.github.student.agile.exception.definition.InvalidDateRangeException;
import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.exception.definition.ProjectAccessDeniedException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.exception.handler.GlobalExceptionHandler;
import io.github.student.agile.resource.controller.ProjectRestController;
import io.github.student.agile.resource.service.ProjectService;
import io.github.student.agile.security.JwtAuthConverter;
import io.github.student.agile.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.ACTION_NOT_ALLOWED;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.INVALID_DATE_RANGE;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.NULL_IDENTIFIER;
import static io.github.student.agile.exception.dictionary.ExceptionMessages.PROJECT_DOES_NOT_EXISTS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProjectRestController.class, GlobalExceptionHandlerTest.TestErrorController.class})
@Import({SecurityConfig.class, JwtAuthConverter.class, GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestErrorController.class})
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @RestController
    @RequestMapping("/api/test-errors")
    static class TestErrorController {

        @GetMapping("/invalid-date-range")
        public void throwInvalidDateRange() {
            throw new InvalidDateRangeException(INVALID_DATE_RANGE);
        }

        @GetMapping("/null-id")
        public void throwNullIdentifier() {
            throw new NullIdentifierException();
        }

        @GetMapping("/date-param")
        public void parseDateParam(@RequestParam ZonedDateTime from) {
        }

        @GetMapping("/unexpected")
        public void throwUnexpected() {
            throw new RuntimeException("Sensitive internal database details");
        }
    }

    @Test
    public void unauthenticatedRequest_returns401JsonWithoutStackTrace() throws Exception {
        mvc.perform(get("/api/scrum-project/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication required")))
                .andExpect(jsonPath("$.path", is("/api/scrum-project/1")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void invalidJwtToken_returns401JsonWithoutStackTrace() throws Exception {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("Invalid JWT"));

        mvc.perform(get("/api/scrum-project/1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Authentication required")))
                .andExpect(jsonPath("$.path", is("/api/scrum-project/1")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void nonExistentProject_returns404ApiErrorResponse() throws Exception {
        when(projectService.getProjectDetails(999))
                .thenThrow(new ResourceNotFoundException(PROJECT_DOES_NOT_EXISTS));

        mvc.perform(get("/api/scrum-project/999").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", is(PROJECT_DOES_NOT_EXISTS)))
                .andExpect(jsonPath("$.path", is("/api/scrum-project/999")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void missingRequiredRole_returns403ApiErrorResponse() throws Exception {
        mvc.perform(post("/api/scrum-project")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_dev")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Project\",\"description\":\"Desc\",\"projectGoal\":\"Goal\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is("Access Denied")))
                .andExpect(jsonPath("$.path", is("/api/scrum-project")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void projectAccessDenied_returns403ApiErrorResponse() throws Exception {
        doThrow(new ProjectAccessDeniedException(ACTION_NOT_ALLOWED))
                .when(projectService).associateUserWithProject("user@example.com", 10);

        mvc.perform(post("/api/scrum-project/user-association")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_sm")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"projectID\":10}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", is(ACTION_NOT_ALLOWED)))
                .andExpect(jsonPath("$.path", is("/api/scrum-project/user-association")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void invalidDateRange_returns400ApiErrorResponse() throws Exception {
        mvc.perform(get("/api/test-errors/invalid-date-range").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is(INVALID_DATE_RANGE)))
                .andExpect(jsonPath("$.path", is("/api/test-errors/invalid-date-range")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void nullIdentifier_returns400ApiErrorResponse() throws Exception {
        mvc.perform(get("/api/test-errors/null-id").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is(NULL_IDENTIFIER)))
                .andExpect(jsonPath("$.path", is("/api/test-errors/null-id")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void invalidDateParamFormat_returns400ApiErrorResponse() throws Exception {
        mvc.perform(get("/api/test-errors/date-param")
                        .param("from", "not-a-valid-date")
                        .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("from")))
                .andExpect(jsonPath("$.path", is("/api/test-errors/date-param")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void missingRequiredParam_returns400ApiErrorResponse() throws Exception {
        mvc.perform(get("/api/test-errors/date-param").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("from")))
                .andExpect(jsonPath("$.path", is("/api/test-errors/date-param")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void malformedJsonBody_returns400ApiErrorResponse() throws Exception {
        mvc.perform(post("/api/scrum-project")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_sm")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", is("Malformed request body")))
                .andExpect(jsonPath("$.path", is("/api/scrum-project")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    public void unexpectedException_returns500WithoutInternalDetailsOrStackTrace() throws Exception {
        mvc.perform(get("/api/test-errors/unexpected").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")))
                .andExpect(jsonPath("$.path", is("/api/test-errors/unexpected")))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
