package com.its.gestioneordinirestclient.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId == null) correlationId = java.util.UUID.randomUUID().toString();

        // Who called: prefer authenticated user, fallback to IP
        String caller = request.getHeader("Auth-User-Id");         // set by gateway
        if (caller == null) caller = request.getRemoteAddr();   // fallback to IP

        // Put everything into MDC — flows into every subsequent log line
        MDC.put("correlationId", correlationId);
        MDC.put("caller", caller);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());

        // Explicit log line for the incoming request
        log.info("Incoming request: {} {} from [{}]",
                request.getMethod(),
                request.getRequestURI(),
                caller);

        return true; // continue the chain
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // Log response status + clear MDC to avoid thread pool leakage
        log.info("Completed: {} {} → status={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus());

        MDC.clear();
    }
}