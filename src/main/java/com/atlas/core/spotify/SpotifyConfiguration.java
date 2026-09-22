package com.atlas.core.spotify;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;

@Configuration
public class SpotifyConfiguration {

    @Bean
    public SpotifyProperties spotifyProperties(
            @Value("${SPOTIFY_CLIENT_ID:}")
            String clientId,

            @Value("${SPOTIFY_CLIENT_SECRET:}")
            String clientSecret,

            @Value(
                    "${SPOTIFY_REDIRECT_URI:"
                            + "http://127.0.0.1:8888"
                            + "/api/integrations/spotify/callback}"
            )
            String redirectUri,

            @Value(
                    "${SPOTIFY_TOKEN_FILE:"
                            + "/home/atlas/.local/state/atlas/"
                            + "spotify-token.json}"
            )
            String tokenFile
    ) {
        return new SpotifyProperties(
                clientId,
                clientSecret,
                redirectUri,
                Path.of(tokenFile)
        );
    }

    @Bean
    public SpotifyTokenStore spotifyTokenStore(
            SpotifyProperties properties,
            JsonMapper jsonMapper
    ) {
        return new SpotifyTokenStore(
                properties.tokenFile(),
                jsonMapper
        );
    }

    @Bean
    public SpotifyClient spotifyClient(
            SpotifyProperties properties,
            SpotifyTokenStore tokenStore,
            JsonMapper jsonMapper
    ) {
        return new SpotifyClient(
                properties,
                tokenStore,
                jsonMapper
        );
    }
}
