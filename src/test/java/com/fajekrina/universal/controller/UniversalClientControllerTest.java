package com.fajekrina.universal.controller;

import com.fajekrina.universal.model.response.ActionResultResponseBuilder;
import com.fajekrina.universal.model.response.PublishedPageResponseBuilder;
import com.fajekrina.universal.service.GetPublishedPageService;
import com.fajekrina.universal.service.GetSigningKeyService;
import com.fajekrina.universal.service.GetSigningKeysService;
import com.fajekrina.universal.service.SubmitActionService;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniversalClientControllerTest {

    @InjectMocks
    UniversalClientController universalClientController;

    @Mock GetPublishedPageService getPublishedPageService;
    @Mock GetSigningKeysService getSigningKeysService;
    @Mock GetSigningKeyService getSigningKeyService;
    @Mock SubmitActionService submitActionService;

    MockMvc mockMvc;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(universalClientController)
                .setControllerAdvice(new GlobalErrorHandler())
                .build();
    }

    @Test
    void servesPayloadWithEtag() throws Exception {
        when(getPublishedPageService.execute(any())).thenReturn(PublishedPageResponseBuilder.builder()
                .contentHash("abc").payload("{\"pageKey\":\"home\"}").build());
        mockMvc.perform(MockMvcRequestBuilders.get("/universal/v1/client/page/m/home"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.header().string("ETag", "\"abc\""))
                .andExpect(MockMvcResultMatchers.jsonPath("$.pageKey").value("home"));
    }

    @Test
    void notModifiedHasNoBody() throws Exception {
        when(getPublishedPageService.execute(any())).thenReturn(PublishedPageResponseBuilder.builder()
                .notModified(true).contentHash("abc").build());
        mockMvc.perform(MockMvcRequestBuilders.get("/universal/v1/client/page/m/home").header("If-None-Match", "\"abc\""))
                .andExpect(MockMvcResultMatchers.status().isNotModified());
    }

    @Test
    void faultsRenderTheErrorContract() throws Exception {
        when(getPublishedPageService.execute(any())).thenThrow(new UniversalException(UniversalFault.PUBLISHED_PAGE_NOT_FOUND, "nope"));
        mockMvc.perform(MockMvcRequestBuilders.get("/universal/v1/client/page/m/x"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorDesc").value("PUBLISHED_PAGE_NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorCode").value("SDUI-002"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.errorMessage").value("nope"));
    }

    @Test
    void failedActionIsBadRequest() throws Exception {
        when(submitActionService.execute(any())).thenReturn(ActionResultResponseBuilder.builder()
                .action("submit").success(false).error("Form data is empty").build());
        mockMvc.perform(MockMvcRequestBuilders.post("/universal/v1/client/action/submit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"submit\"}"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Form data is empty"));
    }
}
