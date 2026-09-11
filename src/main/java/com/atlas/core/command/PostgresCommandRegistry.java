package com.atlas.core.command;

import com.atlas.action.ActionKey;
import com.atlas.command.CausalityRef;
import com.atlas.command.CausalityRefType;
import com.atlas.command.Command;
import com.atlas.command.CommandActor;
import com.atlas.command.CommandActorType;
import com.atlas.command.CommandCausality;
import com.atlas.command.CommandErrorCode;
import com.atlas.command.CommandId;
import com.atlas.command.CommandIdempotencyKey;
import com.atlas.command.CommandOrigin;
import com.atlas.command.CommandOriginType;
import com.atlas.command.CommandOutcome;
import com.atlas.command.CommandRegistry;
import com.atlas.command.CommandResult;
import com.atlas.command.CommandStatus;
import com.atlas.command.CommandStatusEntry;
import com.atlas.command.CommandTarget;
import com.atlas.device.Criticality;
import com.atlas.device.DeviceId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PostgresCommandRegistry
        implements CommandRegistry {

    private static final TypeReference<Map<String, Object>>
            PARAMETERS_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public PostgresCommandRegistry(
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper
    ) {
        if (jdbcTemplate == null) {
            throw new IllegalArgumentException(
                    "jdbcTemplate cannot be null"
            );
        }

        if (jsonMapper == null) {
            throw new IllegalArgumentException(
                    "jsonMapper cannot be null"
            );
        }

        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    @Transactional
    public void register(Command command) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        try {
            insertCommand(command);
            insertStatusHistory(command);
        } catch (DuplicateKeyException exception) {
            throw new IllegalStateException(
                    "command already registered: "
                            + command.id().value(),
                    exception
            );
        }
    }

    @Override
    public Optional<Command> findById(CommandId id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "id cannot be null"
            );
        }

        List<Command> commands = jdbcTemplate.query(
                """
                SELECT *
                FROM atlas_command
                WHERE id = ?
                """,
                (rs, rowNum) -> mapCommand(rs),
                id.value()
        );

        return commands.stream().findFirst();
    }

    @Override
    public List<Command> findAll() {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM atlas_command
                ORDER BY requested_at, id
                """,
                (rs, rowNum) -> mapCommand(rs)
        );
    }

    private void insertCommand(Command command) {
        String parametersJson =
                jsonMapper.writeValueAsString(
                        command.parameters()
                );

        CommandResult result = command.result();

        CausalityRef cause =
                command.causality().cause();

        jdbcTemplate.update(
                """
                INSERT INTO atlas_command (
                    id,
                    actor_type,
                    actor_id,
                    origin_type,
                    origin_id,
                    device_id,
                    action,
                    parameters,
                    criticality,
                    requested_at,
                    dispatched_at,
                    executing_since,
                    expected_completion_at,
                    completed_at,
                    timeout_ms,
                    idempotency_value,
                    idempotency_window_ms,
                    trace_id,
                    cause_type,
                    cause_id,
                    depth,
                    result_outcome,
                    result_error_code,
                    result_error_message,
                    result_adapter_message_id,
                    result_at
                )
                VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    CAST(? AS jsonb),
                    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?
                )
                """,
                command.id().value(),
                command.actor().type().name(),
                command.actor().id(),
                command.origin().type().name(),
                command.origin().id(),
                command.target().deviceId().value(),
                command.target().action().value(),
                parametersJson,
                command.criticality().name(),
                command.requestedAt(),
                command.dispatchedAt(),
                command.executingSince(),
                command.expectedCompletionAt(),
                command.completedAt(),
                command.timeout().toMillis(),
                command.idempotencyKey().value(),
                command.idempotencyKey()
                        .window()
                        .toMillis(),
                command.causality().traceId(),
                cause == null
                        ? null
                        : cause.type().name(),
                cause == null
                        ? null
                        : cause.id(),
                command.causality().depth(),
                result == null
                        ? null
                        : result.outcome().name(),
                result == null
                        || result.errorCode() == null
                        ? null
                        : result.errorCode().name(),
                result == null
                        ? null
                        : result.errorMessage(),
                result == null
                        ? null
                        : result.adapterMessageId(),
                result == null
                        ? null
                        : result.at()
        );
    }

    private void insertStatusHistory(Command command) {
        List<CommandStatusEntry> history =
                command.statusHistory();

        for (int sequence = 0;
             sequence < history.size();
             sequence++) {

            CommandStatusEntry entry =
                    history.get(sequence);

            jdbcTemplate.update(
                    """
                    INSERT INTO atlas_command_status_history (
                        command_id,
                        sequence_no,
                        status,
                        at,
                        detail
                    )
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    command.id().value(),
                    sequence,
                    entry.status().name(),
                    entry.at(),
                    entry.detail()
            );
        }
    }

    private Command mapCommand(ResultSet rs)
            throws SQLException {

        CommandId id =
                new CommandId(
                        rs.getString("id")
                );

        CommandActor actor =
                new CommandActor(
                        CommandActorType.valueOf(
                                rs.getString(
                                        "actor_type"
                                )
                        ),
                        rs.getString("actor_id")
                );

        CommandOrigin origin =
                new CommandOrigin(
                        CommandOriginType.valueOf(
                                rs.getString(
                                        "origin_type"
                                )
                        ),
                        rs.getString("origin_id")
                );

        CommandTarget target =
                new CommandTarget(
                        new DeviceId(
                                rs.getString("device_id")
                        ),
                        new ActionKey(
                                rs.getString("action")
                        )
                );

        Map<String, Object> parameters =
                readParameters(
                        rs.getString("parameters")
                );

        Criticality criticality =
                Criticality.valueOf(
                        rs.getString("criticality")
                );

        OffsetDateTime requestedAt =
                rs.getObject(
                        "requested_at",
                        OffsetDateTime.class
                );

        Duration timeout =
                Duration.ofMillis(
                        rs.getLong("timeout_ms")
                );

        CommandIdempotencyKey idempotencyKey =
                new CommandIdempotencyKey(
                        rs.getString(
                                "idempotency_value"
                        ),
                        Duration.ofMillis(
                                rs.getLong(
                                        "idempotency_window_ms"
                                )
                        )
                );

        CommandCausality causality =
                new CommandCausality(
                        rs.getString("trace_id"),
                        readCause(rs),
                        rs.getInt("depth")
                );

        List<CommandStatusEntry> statusHistory =
                readStatusHistory(id);

        return Command.rehydrate(
                id,
                actor,
                origin,
                target,
                parameters,
                criticality,
                requestedAt,
                timeout,
                idempotencyKey,
                causality,
                statusHistory,
                getOffsetDateTime(
                        rs,
                        "dispatched_at"
                ),
                getOffsetDateTime(
                        rs,
                        "executing_since"
                ),
                getOffsetDateTime(
                        rs,
                        "expected_completion_at"
                ),
                getOffsetDateTime(
                        rs,
                        "completed_at"
                ),
                readResult(rs)
        );
    }

    private List<CommandStatusEntry> readStatusHistory(
            CommandId id
    ) {
        return jdbcTemplate.query(
                """
                SELECT status, at, detail
                FROM atlas_command_status_history
                WHERE command_id = ?
                ORDER BY sequence_no
                """,
                (rs, rowNum) ->
                        new CommandStatusEntry(
                                CommandStatus.valueOf(
                                        rs.getString(
                                                "status"
                                        )
                                ),
                                rs.getObject(
                                        "at",
                                        OffsetDateTime.class
                                ),
                                rs.getString("detail")
                        ),
                id.value()
        );
    }

    private CausalityRef readCause(
            ResultSet rs
    ) throws SQLException {

        String causeType =
                rs.getString("cause_type");

        if (causeType == null) {
            return null;
        }

        return new CausalityRef(
                CausalityRefType.valueOf(
                        causeType
                ),
                rs.getString("cause_id")
        );
    }

    private CommandResult readResult(
            ResultSet rs
    ) throws SQLException {

        String outcome =
                rs.getString("result_outcome");

        if (outcome == null) {
            return null;
        }

        String errorCode =
                rs.getString(
                        "result_error_code"
                );

        return new CommandResult(
                CommandOutcome.valueOf(outcome),
                errorCode == null
                        ? null
                        : CommandErrorCode.valueOf(
                                errorCode
                        ),
                rs.getString(
                        "result_error_message"
                ),
                rs.getString(
                        "result_adapter_message_id"
                ),
                rs.getObject(
                        "result_at",
                        OffsetDateTime.class
                )
        );
    }

    private Map<String, Object> readParameters(
            String json
    ) {
        return jsonMapper.readValue(
                json,
                PARAMETERS_TYPE
        );
    }

    private OffsetDateTime getOffsetDateTime(
            ResultSet rs,
            String column
    ) throws SQLException {
        return rs.getObject(
                column,
                OffsetDateTime.class
        );
    }
}
