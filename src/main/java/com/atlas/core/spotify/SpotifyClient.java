package com.atlas.core.spotify;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class SpotifyClient {

    private static final String AUTH_URL =
            "https://accounts.spotify.com/authorize";

    private static final String TOKEN_URL =
            "https://accounts.spotify.com/api/token";

    private static final String API_URL =
            "https://api.spotify.com/v1";

    private static final String SCOPES =
            "user-read-playback-state "
            + "user-modify-playback-state";

    private final SpotifyProperties properties;
    private final SpotifyTokenStore tokenStore;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;

    private final AtomicReference<String> pendingState =
            new AtomicReference<>();

    public SpotifyClient(
            SpotifyProperties properties,
            SpotifyTokenStore tokenStore,
            JsonMapper jsonMapper
    ) {
        this.properties = properties;
        this.tokenStore = tokenStore;
        this.jsonMapper = jsonMapper;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();
    }

    public boolean connected() {
        return tokenStore.load().isPresent();
    }

    public URI authorizationUri() {
        requireConfigured();

        String state =
                randomState();

        pendingState.set(state);

        String query =
                "client_id="
                + encode(properties.clientId())
                + "&response_type=code"
                + "&redirect_uri="
                + encode(properties.redirectUri())
                + "&scope="
                + encode(SCOPES)
                + "&state="
                + encode(state);

        return URI.create(
                AUTH_URL + "?" + query
        );
    }

    public void authorize(
            String code,
            String state
    ) {
        requireConfigured();

        String expected =
                pendingState.getAndSet(null);

        if (expected == null
                || !expected.equals(state)) {
            throw new IllegalStateException(
                    "invalid Spotify OAuth state"
            );
        }

        String body =
                "grant_type=authorization_code"
                + "&code="
                + encode(code)
                + "&redirect_uri="
                + encode(properties.redirectUri());

        JsonNode response =
                sendTokenRequest(body);

        saveTokenResponse(
                response,
                null
        );
    }

    public PlayerSnapshot player() {
        HttpResponse<String> response =
                authorizedRequest(
                        "GET",
                        "/me/player"
                );

        if (response.statusCode() == 204) {
            return new PlayerSnapshot(
                    true,
                    false,
                    false,
                    null,
                    null,
                    null,
                    null,
                    0,
                    0,
                    null,
                    null
            );
        }

        requireSuccess(
                response,
                200,
                "read Spotify player"
        );

        try {
            JsonNode root =
                    jsonMapper.readTree(
                            response.body()
                    );

            JsonNode item =
                    root.path("item");

            JsonNode device =
                    root.path("device");

            String track =
                    item.isMissingNode()
                            || item.isNull()
                            ? null
                            : nullableText(
                                    item.path("name")
                            );

            String artist = null;

            JsonNode albumNode =
                    item.path("album");

            String album =
                    albumNode.isMissingNode()
                            || albumNode.isNull()
                            ? null
                            : nullableText(
                                    albumNode.path("name")
                            );

            String artworkUrl =
                    selectArtworkUrl(
                            albumNode.path("images")
                    );

            JsonNode artists =
                    item.path("artists");

            if (artists.isArray()
                    && !artists.isEmpty()) {
                artist =
                        nullableText(
                                artists.get(0)
                                        .path("name")
                        );
            }

            String deviceName =
                    device.isMissingNode()
                            || device.isNull()
                            ? null
                            : nullableText(
                                    device.path("name")
                            );

            Integer volume =
                    device.isMissingNode()
                            || device.isNull()
                            || device.path(
                                    "volume_percent"
                            ).isMissingNode()
                            || device.path(
                                    "volume_percent"
                            ).isNull()
                            ? null
                            : device.path(
                                    "volume_percent"
                            ).asInt();

            return new PlayerSnapshot(
                    true,
                    true,
                    root.path(
                            "is_playing"
                    ).asBoolean(false),
                    track,
                    artist,
                    album,
                    artworkUrl,
                    root.path(
                            "progress_ms"
                    ).asLong(0),
                    item.path(
                            "duration_ms"
                    ).asLong(0),
                    deviceName,
                    volume
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "could not parse Spotify player response",
                    exception
            );
        }
    }

    public void playPause() {
        PlayerSnapshot snapshot =
                player();

        if (snapshot.playing()) {
            executeControl(
                    "PUT",
                    "/me/player/pause"
            );
        } else {
            executeControl(
                    "PUT",
                    "/me/player/play"
            );
        }
    }

    public void next() {
        executeControl(
                "POST",
                "/me/player/next"
        );
    }

    public void previous() {
        executeControl(
                "POST",
                "/me/player/previous"
        );
    }

    private void executeControl(
            String method,
            String path
    ) {
        HttpResponse<String> response =
                authorizedRequest(
                        method,
                        path
                );

        requireSuccess(
                response,
                204,
                "control Spotify player"
        );
    }

    private HttpResponse<String> authorizedRequest(
            String method,
            String path
    ) {
        String accessToken =
                accessToken(false);

        HttpResponse<String> response =
                sendApiRequest(
                        method,
                        path,
                        accessToken
                );

        if (response.statusCode() == 401) {
            accessToken =
                    accessToken(true);

            response =
                    sendApiRequest(
                            method,
                            path,
                            accessToken
                    );
        }

        return response;
    }

    private HttpResponse<String> sendApiRequest(
            String method,
            String path,
            String accessToken
    ) {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        API_URL + path
                                )
                        )
                        .timeout(
                                Duration.ofSeconds(5)
                        )
                        .header(
                                "Authorization",
                                "Bearer "
                                        + accessToken
                        )
                        .method(
                                method,
                                HttpRequest
                                        .BodyPublishers
                                        .noBody()
                        )
                        .build();

        return send(request);
    }

    private String accessToken(
            boolean forceRefresh
    ) {
        SpotifyTokenStore.TokenData token =
                tokenStore
                        .load()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Spotify is not connected"
                                        )
                        );

        long now =
                Instant.now()
                        .getEpochSecond();

        if (!forceRefresh
                && token.accessToken() != null
                && !token.accessToken()
                        .isBlank()
                && token.expiresAtEpochSecond()
                        > now + 30) {
            return token.accessToken();
        }

        return refresh(token)
                .accessToken();
    }

    private SpotifyTokenStore.TokenData refresh(
            SpotifyTokenStore.TokenData current
    ) {
        if (current.refreshToken() == null
                || current.refreshToken()
                        .isBlank()) {
            throw new IllegalStateException(
                    "Spotify refresh token is unavailable"
            );
        }

        String body =
                "grant_type=refresh_token"
                + "&refresh_token="
                + encode(
                        current.refreshToken()
                );

        JsonNode response =
                sendTokenRequest(body);

        return saveTokenResponse(
                response,
                current.refreshToken()
        );
    }

    private JsonNode sendTokenRequest(
            String body
    ) {
        String credentials =
                properties.clientId()
                + ":"
                + properties.clientSecret();

        String basic =
                Base64.getEncoder()
                        .encodeToString(
                                credentials.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        );

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(TOKEN_URL)
                        )
                        .timeout(
                                Duration.ofSeconds(10)
                        )
                        .header(
                                "Authorization",
                                "Basic " + basic
                        )
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded"
                        )
                        .POST(
                                HttpRequest
                                        .BodyPublishers
                                        .ofString(body)
                        )
                        .build();

        HttpResponse<String> response =
                send(request);

        requireSuccess(
                response,
                200,
                "exchange Spotify token"
        );

        try {
            return jsonMapper.readTree(
                    response.body()
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "could not parse Spotify token response",
                    exception
            );
        }
    }

    private SpotifyTokenStore.TokenData saveTokenResponse(
            JsonNode response,
            String fallbackRefreshToken
    ) {
        String accessToken =
                response.path(
                        "access_token"
                ).asText();

        String refreshToken =
                nullableText(
                        response.path(
                                "refresh_token"
                        )
                );

        if (refreshToken == null) {
            refreshToken =
                    fallbackRefreshToken;
        }

        long expiresIn =
                response.path(
                        "expires_in"
                ).asLong(3600);

        SpotifyTokenStore.TokenData token =
                new SpotifyTokenStore.TokenData(
                        accessToken,
                        refreshToken,
                        Instant.now()
                                .getEpochSecond()
                                + expiresIn
                );

        tokenStore.save(token);

        return token;
    }

    private HttpResponse<String> send(
            HttpRequest request
    ) {
        try {
            return httpClient.send(
                    request,
                    HttpResponse.BodyHandlers
                            .ofString()
            );

        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Spotify request interrupted",
                    exception
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Spotify request failed",
                    exception
            );
        }
    }

    private void requireSuccess(
            HttpResponse<String> response,
            int expected,
            String operation
    ) {
        if (response.statusCode()
                != expected) {
            throw new IllegalStateException(
                    operation
                    + " failed with HTTP "
                    + response.statusCode()
                    + ": "
                    + response.body()
            );
        }
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new IllegalStateException(
                    "Spotify integration is not configured"
            );
        }
    }

    private String randomState() {
        byte[] bytes =
                new byte[24];

        new SecureRandom()
                .nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String encode(
            String value
    ) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private String selectArtworkUrl(
            JsonNode images
    ) {
        if (images == null
                || !images.isArray()
                || images.isEmpty()) {
            return null;
        }

        String bestUrl = null;
        int bestWidth = Integer.MAX_VALUE;

        for (JsonNode image : images) {
            String url =
                    nullableText(
                            image.path("url")
                    );

            if (url == null) {
                continue;
            }

            int width =
                    image.path("width")
                            .asInt(0);

            if (
                    width >= 96
                    && width < bestWidth
            ) {
                bestWidth = width;
                bestUrl = url;
            }
        }

        if (bestUrl != null) {
            return bestUrl;
        }

        return nullableText(
                images.get(0).path("url")
        );
    }

    private String nullableText(
            JsonNode node
    ) {
        if (node == null
                || node.isMissingNode()
                || node.isNull()) {
            return null;
        }

        String value =
                node.asText();

        return value.isBlank()
                ? null
                : value;
    }

    public record PlayerSnapshot(
            boolean connected,
            boolean active,
            boolean playing,
            String track,
            String artist,
            String album,
            String artworkUrl,
            long progressMs,
            long durationMs,
            String device,
            Integer volumePercent
    ) {

        public boolean artworkAvailable() {
            return artworkUrl != null
                    && !artworkUrl.isBlank();
        }
    }
}
