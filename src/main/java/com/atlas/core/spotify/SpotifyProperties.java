package com.atlas.core.spotify;

import java.nio.file.Path;

public record SpotifyProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        Path tokenFile
) {

    public boolean configured() {
        return clientId != null
                && !clientId.isBlank()
                && clientSecret != null
                && !clientSecret.isBlank()
                && redirectUri != null
                && !redirectUri.isBlank();
    }
}
