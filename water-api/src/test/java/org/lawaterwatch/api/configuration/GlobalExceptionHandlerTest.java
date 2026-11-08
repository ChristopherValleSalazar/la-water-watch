package org.lawaterwatch.api.configuration;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.controller.WaterController;
import org.lawaterwatch.api.exception.PwsidNotFoundException;
import org.lawaterwatch.api.service.MapResponseCache;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Every error comes back as {status, error, message, timestamp}, except 429 which is {status, message}
@WebMvcTest(WaterController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaterSystemService waterSystemService;

    @MockitoBean
    private MapResponseCache mapResponseCache;

    @Test
    void unknownPwsidIs404WithItsMessage() throws Exception {
        when(waterSystemService.infoPanel("NOPE")).thenThrow(new PwsidNotFoundException("NOPE"));

        mockMvc.perform(get("/api/systems/info-panel/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("No water system with PWSID 'NOPE'")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void badEnumValueListsTheAllowedOnes() throws Exception {
        mockMvc.perform(get("/api/systems/leaderboard").param("sort", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid value 'NOPE' for 'sort'")))
                .andExpect(jsonPath("$.message").value(containsString("WATERSCORE")));
    }

    @Test
    void malformedJsonBodyIs400() throws Exception {
        mockMvc.perform(post("/api/address").contentType(MediaType.APPLICATION_JSON).content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void unknownRouteIs404() throws Exception {
        mockMvc.perform(get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No endpoint at GET /api/nope"));
    }

    @Test
    void wrongMethodIs405() throws Exception {
        mockMvc.perform(delete("/api/meta"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value(containsString("DELETE")));
    }

    @Test
    void censusOutageIs502WithItsReason() throws Exception {
        when(waterSystemService.addressLookup(anyString())).thenThrow(new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "The address service (U.S. Census geocoder) is unavailable, please try again later"));

        mockMvc.perform(post("/api/address").contentType(MediaType.APPLICATION_JSON).content("{\"address\": \"1 Main St\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value(containsString("Census geocoder")));
    }

    @Test
    void unexpectedErrorIs500WithOnlyTheRootCause() throws Exception {
        when(waterSystemService.meta()).thenThrow(new IllegalStateException("query failed",
                new SQLException("database is locked")));

        mockMvc.perform(get("/api/meta"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("SQLException: database is locked"))
                //No stack trace frames leak out
                .andExpect(content().string(not(containsString("at org."))));
    }

    @Test
    void tooManyRequestsHidesEverythingButStatusAndMessage() throws Exception {
        when(waterSystemService.meta()).thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "bucket empty for 1.2.3.4"));

        mockMvc.perform(get("/api/meta"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many requests, please try again later."))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(content().string(not(containsString("1.2.3.4"))));
    }
}
