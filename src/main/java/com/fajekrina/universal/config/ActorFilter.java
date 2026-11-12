package com.fajekrina.universal.config;

import com.fajekrina.universal.utils.constants.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Binds the request actor (header {@code X-SDUI-Actor}; merchant API defaults to "builder"). */
@Component
public class ActorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(Constants.ACTOR_HEADER);
        String actor = header != null && !header.isBlank() ? header.trim()
                : request.getRequestURI().contains("/merchant/") ? Constants.DEFAULT_ACTOR : Constants.SYSTEM_ACTOR;
        ActorContext.set(actor);
        try {
            chain.doFilter(request, response);
        } finally {
            ActorContext.clear();
        }
    }
}
