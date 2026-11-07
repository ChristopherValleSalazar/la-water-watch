package org.lawaterwatch.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.service.MapResponseCache;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//GET /api/systems/map picks the gzip or plain bytes and answers repeat visits with 304
@WebMvcTest(WaterController.class)
class MapControllerTest {

    private static final byte[] JSON = "{\"type\":\"FeatureCollection\",\"features\":[]}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] GZIP = {1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaterSystemService waterSystemService;

    @MockitoBean
    private MapResponseCache mapResponseCache;

    @BeforeEach
    void cachedBody() {
        when(mapResponseCache.get()).thenReturn(new MapResponseCache.Body(JSON, GZIP, "abc", "abc-gz"));
    }

    @Test
    void browserThatAcceptsGzipGetsGzipBytes() throws Exception {
        mockMvc.perform(get("/api/systems/map").header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br, zstd"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_ENCODING, "gzip"))
                .andExpect(header().string(HttpHeaders.ETAG, "\"abc-gz\""))
                //CORS adds its own Vary values first
                .andExpect(header().stringValues(HttpHeaders.VARY, hasItem("Accept-Encoding")))
                .andExpect(content().contentType("application/json"))
                .andExpect(content().bytes(GZIP));
    }

    @Test
    void clientWithoutGzipGetsPlainJson() throws Exception {
        mockMvc.perform(get("/api/systems/map"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_ENCODING))
                .andExpect(header().string(HttpHeaders.ETAG, "\"abc\""))
                .andExpect(content().bytes(JSON));
    }

    @Test
    void repeatVisitWithSameEtagGets304WithoutBody() throws Exception {
        mockMvc.perform(get("/api/systems/map")
                        .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                        .header(HttpHeaders.IF_NONE_MATCH, "\"abc-gz\""))
                .andExpect(status().isNotModified())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void staleEtagAfterDataReloadGetsTheNewMap() throws Exception {
        mockMvc.perform(get("/api/systems/map")
                        .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                        .header(HttpHeaders.IF_NONE_MATCH, "\"old-gz\""))
                .andExpect(status().isOk())
                .andExpect(content().bytes(GZIP));
    }
}
