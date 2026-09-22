package com.atlas.core.spotify;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/integrations/spotify")
public class SpotifyController {

    private final SpotifyClient spotify;

    public SpotifyController(
            SpotifyClient spotify
    ) {
        this.spotify = spotify;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "configured",
                true,
                "connected",
                spotify.connected()
        );
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        URI location =
                spotify.authorizationUri();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    @GetMapping("/callback")
    public Map<String, String> callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        spotify.authorize(
                code,
                state
        );

        return Map.of(
                "status",
                "CONNECTED",
                "message",
                "Spotify connected to ATLAS"
        );
    }

    @GetMapping("/player")
    public SpotifyClient.PlayerSnapshot player() {
        return spotify.player();
    }

    @PostMapping("/play-pause")
    public Map<String, String> playPause() {
        spotify.playPause();

        return Map.of(
                "status",
                "OK"
        );
    }

    @PostMapping("/next")
    public Map<String, String> next() {
        spotify.next();

        return Map.of(
                "status",
                "OK"
        );
    }

    @PostMapping("/previous")
    public Map<String, String> previous() {
        spotify.previous();

        return Map.of(
                "status",
                "OK"
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleFailure(
            IllegalStateException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(
                        Map.of(
                                "status",
                                "ERROR",
                                "message",
                                exception.getMessage()
                        )
                );
    }
}
