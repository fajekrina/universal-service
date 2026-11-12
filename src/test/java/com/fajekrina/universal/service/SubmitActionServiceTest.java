package com.fajekrina.universal.service;

import com.fajekrina.universal.model.request.SubmitActionRequestBuilder;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SubmitActionServiceTest {

    @InjectMocks
    SubmitActionService submitActionService;

    @Test
    void submitNavigatesToParamsTarget() {
        var result = submitActionService.execute(SubmitActionRequestBuilder.builder()
                .action("submit").data(Map.of("name", "Budi")).params(Map.of("target", "home")).build());
        assertThat(result.success()).isTrue();
        assertThat(result.navigateTo()).isEqualTo("home");
    }

    @Test
    void submitWithoutDataFails() {
        var result = submitActionService.execute(SubmitActionRequestBuilder.builder().action("submit").build());
        assertThat(result.success()).isFalse();
        assertThat(result.error()).isEqualTo("Form data is empty");
    }

    @Test
    void seatSelectionRequiresSeatId() {
        var result = submitActionService.execute(SubmitActionRequestBuilder.builder().action("SEAT_SELECTION").data(Map.of()).build());
        assertThat(result.success()).isFalse();
    }

    @Test
    void blankActionIsRejected() {
        assertThatThrownBy(() -> submitActionService.execute(SubmitActionRequestBuilder.builder().action(" ").build()))
                .isInstanceOf(UniversalException.class)
                .satisfies(e -> assertThat(((UniversalException) e).getFault()).isEqualTo(UniversalFault.ACTION_REQUIRED));
    }
}
