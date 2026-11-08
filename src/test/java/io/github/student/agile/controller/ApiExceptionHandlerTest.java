package io.github.student.agile.controller;

import io.github.student.agile.exception.definition.BusinessRuleException;
import io.github.student.agile.exception.definition.ForbiddenOperationException;
import io.github.student.agile.exception.definition.InvalidRequestException;
import io.github.student.agile.exception.definition.NullIdentifierException;
import io.github.student.agile.exception.definition.ResourceNotFoundException;
import io.github.student.agile.resource.controller.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static io.github.student.agile.exception.dictionary.ExceptionMessages.NULL_IDENTIFIER;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void resourceNotFound_returns404() throws Exception {
        expectError(get("/not-found"), 404, "Not Found", "Sprint missing");
    }

    @Test
    void businessRuleViolation_returns409() throws Exception {
        expectError(get("/conflict"), 409, "Conflict", "Sprint finished");
    }

    @Test
    void forbiddenOperation_returns403() throws Exception {
        expectError(get("/forbidden"), 403, "Forbidden", "Not your comment");
    }

    @Test
    void accessDenied_returns403WithoutInternalMessage() throws Exception {
        expectError(get("/access-denied"), 403, "Forbidden", "Access denied");
    }

    @Test
    void invalidRequest_returns400() throws Exception {
        expectError(get("/invalid"), 400, "Bad Request", "from must not be after to");
    }

    @Test
    void nullIdentifier_returns400() throws Exception {
        expectError(get("/null-id"), 400, "Bad Request", NULL_IDENTIFIER);
    }

    @Test
    void pathVariableTypeMismatch_returns400() throws Exception {
        expectError(get("/typed/abc"), 400, "Bad Request", "Invalid value 'abc' for parameter 'id'");
    }

    @Test
    void malformedBody_returns400() throws Exception {
        expectError(post("/body").contentType(MediaType.APPLICATION_JSON).content("{not json"),
                400, "Bad Request", "Malformed request body");
    }

    @Test
    void missingRequestParameter_returns400() throws Exception {
        mvc.perform(get("/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        mvc.perform(post("/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status", is(405)))
                .andExpect(jsonPath("$.error", is("Method Not Allowed")));
    }

    @Test
    void unexpectedException_returns500WithoutInternalDetails() throws Exception {
        expectError(get("/boom"), 500, "Internal Server Error", "Unexpected server error");
    }

    private void expectError(org.springframework.test.web.servlet.RequestBuilder request,
                             int status, String error, String message) throws Exception {
        ResultActions result = mvc.perform(request);
        result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(status)))
                .andExpect(jsonPath("$.error", is(error)))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.path", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Sprint missing");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new BusinessRuleException("Sprint finished");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenOperationException("Not your comment");
        }

        @GetMapping("/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("internal role check details");
        }

        @GetMapping("/invalid")
        void invalid() {
            throw new InvalidRequestException("from must not be after to");
        }

        @GetMapping("/null-id")
        void nullIdentifier() {
            throw new NullIdentifierException();
        }

        @GetMapping("/typed/{id}")
        Integer typed(@PathVariable Integer id) {
            return id;
        }

        @PostMapping("/body")
        Map<String, String> body(@RequestBody Map<String, String> body) {
            return body;
        }

        @GetMapping("/param")
        String param(@RequestParam String value) {
            return value;
        }

        @GetMapping("/boom")
        void boom() {
            throw new NullPointerException("Cannot invoke \"Sprint.getId()\" because \"sprint\" is null");
        }
    }
}
