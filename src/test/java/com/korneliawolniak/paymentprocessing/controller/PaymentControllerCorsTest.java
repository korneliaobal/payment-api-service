package com.korneliawolniak.paymentprocessing.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerCorsTest {
  @Autowired private MockMvc mvc;

  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:4200", "https://obal-flow.up.railway.app"})
  void allowsConfiguredFrontendPreflightForUpload(String origin) throws Exception {
    mvc.perform(
            options("/api/payments/upload")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "POST"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:4200", "https://obal-flow.up.railway.app"})
  void exposesUploadErrorsToTheAllowedFrontend(String origin) throws Exception {
    mvc.perform(multipart("/api/payments/upload").header(HttpHeaders.ORIGIN, origin))
        .andExpect(status().isBadRequest())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
  }

  @Test
  void rejectsAnUnconfiguredOrigin() throws Exception {
    mvc.perform(
            options("/api/payments/upload")
                .header(HttpHeaders.ORIGIN, "https://unconfigured.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }
}
