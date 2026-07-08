package com.tripgenie.trip.location.provider;

import com.tripgenie.trip.location.config.MapsProperties;
import com.tripgenie.trip.location.dto.PlaceResolution;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GoogleMapsProviderTest {

    @Test
    void resolvePlaceMapsGoogleTextSearchResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://maps.example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://maps.example.test/maps/api/place/textsearch/json?query=Louvre%20Museum&key=test-key"))
                .andExpect(queryParam("query", "Louvre%20Museum"))
                .andExpect(queryParam("key", "test-key"))
                .andRespond(withSuccess("""
                        {
                          "status": "OK",
                          "results": [{
                            "name": "Musee du Louvre",
                            "formatted_address": "Rue de Rivoli, 75001 Paris, France",
                            "place_id": "google-place-id",
                            "rating": 4.7,
                            "geometry": {
                              "location": {
                                "lat": 48.8606111,
                                "lng": 2.337644
                              }
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));
        GoogleMapsProvider provider = new GoogleMapsProvider(builder.build(), new MapsProperties(
                "https://maps.example.test",
                "test-key",
                2,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        ));

        Optional<PlaceResolution> resolution = provider.resolvePlace("Louvre Museum");

        assertThat(resolution).isPresent();
        assertThat(resolution.get().placeName()).isEqualTo("Musee du Louvre");
        assertThat(resolution.get().formattedAddress()).isEqualTo("Rue de Rivoli, 75001 Paris, France");
        assertThat(resolution.get().latitude()).isEqualByComparingTo("48.8606111");
        assertThat(resolution.get().longitude()).isEqualByComparingTo("2.337644");
        assertThat(resolution.get().googlePlaceId()).isEqualTo("google-place-id");
        assertThat(resolution.get().rating()).isEqualByComparingTo("4.7");
        server.verify();
    }

    @Test
    void resolvePlaceReturnsEmptyWhenApiKeyMissing() {
        GoogleMapsProvider provider = new GoogleMapsProvider(RestClient.builder().build(), new MapsProperties(
                "https://maps.example.test",
                "",
                2,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        ));

        assertThat(provider.resolvePlace("Louvre Museum")).isEmpty();
    }

    @Test
    void retriesTransientServerFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://maps.example.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://maps.example.test/maps/api/place/textsearch/json?query=Louvre%20Museum&key=test-key"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(requestTo("https://maps.example.test/maps/api/place/textsearch/json?query=Louvre%20Museum&key=test-key"))
                .andRespond(withSuccess("""
                        {"status":"OK","results":[{"name":"Louvre","formatted_address":"Paris"}]}
                        """, MediaType.APPLICATION_JSON));
        GoogleMapsProvider provider = new GoogleMapsProvider(builder.build(), new MapsProperties(
                "https://maps.example.test",
                "test-key",
                2,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        ));

        Optional<PlaceResolution> resolution = provider.resolvePlace("Louvre Museum");

        assertThat(resolution).isPresent();
        server.verify();
    }
}
