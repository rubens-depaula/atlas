package com.atlas.core.adapter;

import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.adapter.DeviceAdapter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

public final class YellowBoardDeviceAdapter
        implements DeviceAdapter {

    public static final String INSTANCE_ID =
            "yellow-board.adapter";

    private final AdapterExecutionSink executionSink;
    private final JsonMapper jsonMapper;
    private final HttpClient httpClient;
    private final URI commandsUri;

    public YellowBoardDeviceAdapter(
            AdapterExecutionSink executionSink,
            JsonMapper jsonMapper,
            URI baseUri
    ) {
        if (executionSink == null) {
            throw new IllegalArgumentException(
                    "executionSink cannot be null"
            );
        }

        if (jsonMapper == null) {
            throw new IllegalArgumentException(
                    "jsonMapper cannot be null"
            );
        }

        if (baseUri == null) {
            throw new IllegalArgumentException(
                    "baseUri cannot be null"
            );
        }

        this.executionSink = executionSink;
        this.jsonMapper = jsonMapper;

        this.httpClient =
                HttpClient.newBuilder()
                        .connectTimeout(
                                Duration.ofSeconds(1)
                        )
                        .build();

        String base =
                baseUri.toString()
                        .replaceAll("/+$", "");

        this.commandsUri =
                URI.create(
                        base + "/atlas/v1/commands"
                );
    }

    @Override
    public String instanceId() {
        return INSTANCE_ID;
    }

    @Override
    public AdapterDispatchReceipt dispatch(
            AdapterCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        String requestBody;

        try {
            requestBody =
                    jsonMapper.writeValueAsString(
                            Map.of(
                                    "commandId",
                                    command.commandId(),
                                    "action",
                                    command.action(),
                                    "parameters",
                                    command.parameters()
                            )
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "could not serialize Yellow Board command",
                    exception
            );
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(commandsUri)
                        .timeout(
                                Duration.ofSeconds(3)
                        )
                        .header(
                                "Content-Type",
                                "application/json"
                        )
                        .POST(
                                HttpRequest.BodyPublishers
                                        .ofString(requestBody)
                        )
                        .build();

        HttpResponse<String> response;

        try {
            response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Yellow Board request interrupted",
                    exception
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Yellow Board request failed",
                    exception
            );
        }

        if (response.statusCode() != 202) {
            throw new IllegalStateException(
                    "Yellow Board returned HTTP "
                            + response.statusCode()
            );
        }

        YellowBoardResponse payload;

        try {
            payload =
                    jsonMapper.readValue(
                            response.body(),
                            YellowBoardResponse.class
                    );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "could not parse Yellow Board response",
                    exception
            );
        }

        if (!command.commandId().equals(
                payload.commandId()
        )) {
            throw new IllegalStateException(
                    "Yellow Board returned unexpected commandId"
            );
        }

        if (!payload.accepted()) {
            throw new IllegalStateException(
                    "Yellow Board rejected command"
            );
        }

        if (!"COMPLETED".equals(
                payload.status()
        )) {
            throw new IllegalStateException(
                    "Yellow Board returned unexpected status: "
                            + payload.status()
            );
        }

        return new AdapterDispatchReceipt(
                "yellow_board_"
                        + command.commandId(),
                OffsetDateTime.now()
        );
    }

    @Override
    public void afterAcknowledged(
            AdapterCommand command,
            AdapterDispatchReceipt receipt
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        if (receipt == null) {
            throw new IllegalArgumentException(
                    "receipt cannot be null"
            );
        }

        OffsetDateTime observedAt =
                receipt.acknowledgedAt();

        executionSink.executionStarted(
                command.commandId(),
                observedAt,
                observedAt.plusSeconds(1)
        );

        executionSink.executionCompleted(
                command.commandId(),
                receipt.adapterMessageId(),
                OffsetDateTime.now()
        );
    }

    private record YellowBoardResponse(
            String commandId,
            String deviceId,
            boolean accepted,
            String status
    ) {
    }
}
