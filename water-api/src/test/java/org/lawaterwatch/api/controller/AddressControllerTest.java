package org.lawaterwatch.api.controller;

import org.junit.jupiter.api.Test;
import org.lawaterwatch.api.dto.AddressLookupResponse;
import org.lawaterwatch.api.service.WaterSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//POST /api/address body binding and status codes
@WebMvcTest(WaterController.class)
class AddressControllerTest {

    private static final String ADDRESS = "1572 Ricardo St, Los Angeles, CA, 90033, USA";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaterSystemService waterSystemService;

    @Test
    void addressInBodyIsLookedUp() throws Exception {
        when(waterSystemService.addressLookup(ADDRESS)).thenReturn(new AddressLookupResponse(
                "1572 RICARDO ST, LOS ANGELES, CA, 90033", 34.061, -118.197, "CA1910067"));

        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\": \"" + ADDRESS + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchedAddress").value("1572 RICARDO ST, LOS ANGELES, CA, 90033"))
                .andExpect(jsonPath("$.lat").value(34.061))
                .andExpect(jsonPath("$.lng").value(-118.197))
                .andExpect(jsonPath("$.pwsid").value("CA1910067"))
                .andExpect(jsonPath("$.inServiceArea").value(true));
    }

    @Test
    void blankAddressIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"address\": \"  \"}"))
                .andExpect(status().isBadRequest());

        verify(waterSystemService, never()).addressLookup(anyString());
    }

    @Test
    void missingAddressIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getIsNotAllowed() throws Exception {
        //Addresses must never be accepted in the query string
        mockMvc.perform(get("/api/address").param("address", ADDRESS))
                .andExpect(status().isMethodNotAllowed());
    }
}
