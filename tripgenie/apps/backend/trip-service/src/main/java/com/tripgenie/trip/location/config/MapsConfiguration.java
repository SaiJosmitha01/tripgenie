package com.tripgenie.trip.location.config;

import com.tripgenie.trip.location.provider.GoogleMapsProvider;
import com.tripgenie.trip.location.provider.MapsProvider;
import com.tripgenie.trip.location.provider.NoOpMapsProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(MapsProperties.class)
public class MapsConfiguration {

    @Bean
    RestClient googleMapsRestClient(MapsProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    MapsProvider mapsProvider(@Qualifier("googleMapsRestClient") RestClient googleMapsRestClient,
                              MapsProperties properties) {
        if (properties.hasApiKey()) {
            return new GoogleMapsProvider(googleMapsRestClient, properties);
        }
        return new NoOpMapsProvider();
    }
}
