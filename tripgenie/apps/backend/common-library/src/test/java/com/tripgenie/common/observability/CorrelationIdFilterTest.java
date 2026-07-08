package com.tripgenie.common.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void usesIncomingCorrelationIdAndAddsResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "request-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertingChain("request-id-123"));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("request-id-123");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, assertingChain(null));

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private FilterChain assertingChain(String expectedCorrelationId) {
        return (request, response) -> {
            String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (expectedCorrelationId == null) {
                assertThat(correlationId).isNotBlank();
            } else {
                assertThat(correlationId).isEqualTo(expectedCorrelationId);
            }
        };
    }
}
