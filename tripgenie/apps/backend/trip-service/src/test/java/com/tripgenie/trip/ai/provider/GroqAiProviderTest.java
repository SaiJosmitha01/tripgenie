package com.tripgenie.trip.ai.provider;

import com.tripgenie.common.exception.BusinessException;
import com.tripgenie.trip.ai.config.AiProperties;
import com.tripgenie.trip.ai.dto.AiGenerationContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqAiProviderTest {

    @Test
    void callsOpenAiCompatibleEndpointInJsonMode() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://groq.test/openai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiProperties properties = new AiProperties(null, "secret-test-key", null, 2,
                Duration.ofSeconds(1), Duration.ofSeconds(1));
        GroqAiProvider provider = new GroqAiProvider(builder.build(), properties);
        server.expect(requestTo("https://groq.test/openai/v1/chat/completions"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret-test-key"))
                .andExpect(jsonPath("$.model").value("llama-3.3-70b-versatile"))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"role":"assistant","content":"{\\"itinerary_days\\":[]}"}}]}
                        """, MediaType.APPLICATION_JSON));

        var response = provider.generateItinerary(context());

        assertThat(response.provider()).isEqualTo("groq");
        assertThat(response.rawContent()).isEqualTo("{\"itinerary_days\":[]}");
        server.verify();
    }

    @Test
    void failsSafelyWhenApiKeyIsMissing() {
        AiProperties properties = new AiProperties(null, "", null, 2,
                Duration.ofSeconds(1), Duration.ofSeconds(1));
        GroqAiProvider provider = new GroqAiProvider(RestClient.create(), properties);
        assertThatThrownBy(() -> provider.generateItinerary(context()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("AI_PROVIDER_NOT_CONFIGURED"));
    }

    private AiGenerationContext context() {
        return new AiGenerationContext(
                "Paris", LocalDate.now(), LocalDate.now(), null, "USD", 1,
                "balanced", List.of(), null, null);
    }
}
