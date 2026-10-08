package com.lacivita.turnos.shared.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTests.FailingController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTests.FailingController.class})
class GlobalExceptionHandlerTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void unexpectedErrorsReturnProblemDetailsWithoutInternalDetails() {
        var result = mvc.get().uri("/test/failure").exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR).hasContentType("application/problem+json");
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Error interno");
        assertThat(result).bodyText().doesNotContain("secreto interno");
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/failure")
        String fail() {
            throw new IllegalStateException("secreto interno");
        }
    }
}
