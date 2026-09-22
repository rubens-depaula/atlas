package com.atlas.core.spotify;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

public final class SpotifyArtworkService {

    public static final int WIDTH = 96;
    public static final int HEIGHT = 96;

    private final SpotifyClient spotify;
    private final HttpClient httpClient;

    private String cachedUrl;
    private byte[] cachedRgb565;

    public SpotifyArtworkService(
            SpotifyClient spotify
    ) {
        this.spotify = spotify;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(5)
                        )
                        .build();
    }

    public synchronized Optional<byte[]>
    currentArtworkRgb565() {

        SpotifyClient.PlayerSnapshot snapshot =
                spotify.player();

        if (!snapshot.active()
                || !snapshot.artworkAvailable()) {
            return Optional.empty();
        }

        String artworkUrl =
                snapshot.artworkUrl();

        if (
                artworkUrl.equals(cachedUrl)
                && cachedRgb565 != null
        ) {
            return Optional.of(
                    cachedRgb565.clone()
            );
        }

        byte[] imageBytes =
                download(artworkUrl);

        BufferedImage source =
                decode(imageBytes);

        BufferedImage resized =
                resize(
                        source,
                        WIDTH,
                        HEIGHT
                );

        byte[] rgb565 =
                toRgb565(resized);

        cachedUrl =
                artworkUrl;

        cachedRgb565 =
                rgb565;

        return Optional.of(
                rgb565.clone()
        );
    }

    private byte[] download(
            String url
    ) {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(
                                Duration.ofSeconds(10)
                        )
                        .GET()
                        .build();

        try {
            HttpResponse<byte[]> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofByteArray()
                    );

            if (
                    response.statusCode() < 200
                    || response.statusCode() >= 300
            ) {
                throw new IllegalStateException(
                        "Spotify artwork download failed "
                        + "with HTTP "
                        + response.statusCode()
                );
            }

            return response.body();

        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Spotify artwork download interrupted",
                    exception
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Spotify artwork download failed",
                    exception
            );
        }
    }

    private BufferedImage decode(
            byte[] bytes
    ) {
        try {
            BufferedImage image =
                    ImageIO.read(
                            new ByteArrayInputStream(
                                    bytes
                            )
                    );

            if (image == null) {
                throw new IllegalStateException(
                        "Unsupported Spotify artwork format"
                );
            }

            return image;

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not decode Spotify artwork",
                    exception
            );
        }
    }

    private BufferedImage resize(
            BufferedImage source,
            int width,
            int height
    ) {
        BufferedImage output =
                new BufferedImage(
                        width,
                        height,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                output.createGraphics();

        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );

            graphics.setRenderingHint(
                    RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY
            );

            graphics.drawImage(
                    source,
                    0,
                    0,
                    width,
                    height,
                    null
            );

        } finally {
            graphics.dispose();
        }

        return output;
    }

    private byte[] toRgb565(
            BufferedImage image
    ) {
        byte[] output =
                new byte[
                        WIDTH
                        * HEIGHT
                        * 2
                ];

        int offset = 0;

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int rgb =
                        image.getRGB(x, y);

                int red =
                        (rgb >> 16) & 0xFF;

                int green =
                        (rgb >> 8) & 0xFF;

                int blue =
                        rgb & 0xFF;

                int rgb565 =
                        ((red & 0xF8) << 8)
                        | ((green & 0xFC) << 3)
                        | (blue >> 3);

                output[offset++] =
                        (byte)(
                                (rgb565 >> 8)
                                & 0xFF
                        );

                output[offset++] =
                        (byte)(
                                rgb565 & 0xFF
                        );
            }
        }

        return output;
    }
}
