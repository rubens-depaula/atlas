package com.atlas.core.spotify;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Optional;
import java.util.Set;

public final class SpotifyTokenStore {

    private final Path path;
    private final JsonMapper jsonMapper;

    public SpotifyTokenStore(
            Path path,
            JsonMapper jsonMapper
    ) {
        this.path = path;
        this.jsonMapper = jsonMapper;
    }

    public synchronized Optional<TokenData> load() {
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    jsonMapper.readValue(
                            Files.readString(path),
                            TokenData.class
                    )
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not read Spotify token store",
                    exception
            );
        }
    }

    public synchronized void save(
            TokenData token
    ) {
        try {
            Files.createDirectories(
                    path.getParent()
            );

            Files.writeString(
                    path,
                    jsonMapper.writeValueAsString(token),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {
                Files.setPosixFilePermissions(
                        path,
                        Set.of(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE
                        )
                );
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystem.
            }

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "could not save Spotify token store",
                    exception
            );
        }
    }

    public record TokenData(
            String accessToken,
            String refreshToken,
            long expiresAtEpochSecond
    ) {
    }
}
