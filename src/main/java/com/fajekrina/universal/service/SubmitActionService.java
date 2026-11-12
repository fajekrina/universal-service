package com.fajekrina.universal.service;

import com.fajekrina.universal.model.request.SubmitActionRequest;
import com.fajekrina.universal.model.response.ActionResultResponse;
import com.fajekrina.universal.model.response.ActionResultResponseBuilder;
import com.fajekrina.universal.utils.enums.UniversalFault;
import com.fajekrina.universal.utils.exception.UniversalException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/** Demo action handler behind POST client/action/submit (form submit, seat, checkout, payment). */
@Service
public class SubmitActionService {

    public ActionResultResponse execute(SubmitActionRequest request) {
        if (request.action() == null || request.action().isBlank()) throw new UniversalException(UniversalFault.ACTION_REQUIRED);
        String action = request.action();
        Map<String, Object> data = request.data();
        Map<String, Object> params = request.params();
        var result = ActionResultResponseBuilder.builder().action(action).receivedAt(LocalDateTime.now().toString());

        if ("submit".equalsIgnoreCase(action) || "SUBMIT_FORM".equalsIgnoreCase(action) || "FORM_SUBMIT".equalsIgnoreCase(action)) {
            if (data == null || data.isEmpty()) return result.success(false).error("Form data is empty").build();
            String target = params != null && params.get("target") != null ? params.get("target").toString()
                    : data.get("target") != null ? data.get("target").toString() : "checkout";
            return result.success(true).message("Form submitted successfully").data(data).target(target).navigateTo(target).build();
        }
        if ("SEAT_SELECTION".equalsIgnoreCase(action) || "selectSeat".equalsIgnoreCase(action)) {
            Object seatId = data != null ? data.get("seatId") : null;
            if (seatId == null || seatId.toString().isBlank()) return result.success(false).error("seatId is required").build();
            return result.success(true).message("Seat " + seatId + " reserved").target("checkout").navigateTo("checkout").build();
        }
        if ("CHECKOUT".equalsIgnoreCase(action)) {
            return result.success(true).message("Checkout submitted").data(data).target("home").navigateTo("home").build();
        }
        if ("PAYMENT".equalsIgnoreCase(action)) {
            return result.success(true).message("Payment processed").target("home").navigateTo("home").build();
        }
        String target = params != null && params.get("target") != null ? params.get("target").toString() : null;
        return result.success(true).message("Action '" + action + "' processed (universal)").data(data).params(params)
                .target(target).navigateTo(target).build();
    }
}
