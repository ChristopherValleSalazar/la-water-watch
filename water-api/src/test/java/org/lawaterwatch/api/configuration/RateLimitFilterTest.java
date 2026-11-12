package org.lawaterwatch.api.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.controller.WaterController;
import org.lawaterwatch.api.dto.AddressLookupResponse;
import org.lawaterwatch.api.dto.MetaResponse;
import org.lawaterwatch.api.service.MapResponseCache;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//The real CORS + rate limit filters in front of the controller, with tiny limits so they're quick to hit
//Buckets live for the whole test context, so every test uses its own client IP
@WebMvcTest(WaterController.class)
@Import(WebFilterConfig.class)
@TestPropertySource(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.address-per-minute=2",
        "app.rate-limit.default-per-minute=3",
        "app.cors.allowed-origins=http://127.0.0.1:3000,http://localhost:3000"
})
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaterSystemService waterSystemService;

    @MockitoBean
    private MapResponseCache mapResponseCache;

    @BeforeEach
    void stubService() {
        when(waterSystemService.meta()).thenReturn(new MetaResponse(null, "test"));
        when(waterSystemService.addressLookup(anyString())).thenReturn(AddressLookupResponse.notFound());
    }

    private static RequestPostProcessor ip(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private static MockHttpServletRequestBuilder addressLookup() {
        return post("/api/address").contentType(MediaType.APPLICATION_JSON).content("{\"address\": \"1 Main St\"}");
    }

    @Test
    void addressLimitGivesSlim429() throws Exception {
        mockMvc.perform(addressLookup().with(ip("10.0.0.1"))).andExpect(status().isOk());
        mockMvc.perform(addressLookup().with(ip("10.0.0.1"))).andExpect(status().isOk());

        mockMvc.perform(addressLookup().with(ip("10.0.0.1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Too many requests, please try again later."))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void usedUpAddressLimitDoesNotBlockOtherEndpoints() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(addressLookup().with(ip("10.0.0.2")));
        }
        mockMvc.perform(get("/api/meta").with(ip("10.0.0.2"))).andExpect(status().isOk());
    }

    @Test
    void otherEndpointsShareTheDefaultLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/meta").with(ip("10.0.0.3"))).andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/meta").with(ip("10.0.0.3"))).andExpect(status().isTooManyRequests());
    }

    @Test
    void eachIpHasItsOwnBucket() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(addressLookup().with(ip("10.0.0.4")));
        }
        mockMvc.perform(addressLookup().with(ip("10.0.0.5"))).andExpect(status().isOk());
    }

    @Test
    void preflightsAreAnsweredByCorsAndNotCounted() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(options("/api/address").with(ip("10.0.0.6"))
                            .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
        }
        mockMvc.perform(addressLookup().with(ip("10.0.0.6"))).andExpect(status().isOk());
    }

    @Test
    void bothDevOriginsAreAllowedAndOthersRejected() throws Exception {
        mockMvc.perform(get("/api/meta").with(ip("10.0.0.7")).header(HttpHeaders.ORIGIN, "http://127.0.0.1:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:3000"));
        mockMvc.perform(get("/api/meta").with(ip("10.0.0.7")).header(HttpHeaders.ORIGIN, "http://evil.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void the429StillCarriesCorsHeadersSoTheBrowserCanReadIt() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(addressLookup().with(ip("10.0.0.8")).header(HttpHeaders.ORIGIN, "http://localhost:3000"));
        }
        mockMvc.perform(addressLookup().with(ip("10.0.0.8")).header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }
}
