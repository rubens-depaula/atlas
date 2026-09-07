package com.atlas.core.command;

import com.atlas.command.Command;
import com.atlas.command.CommandId;
import com.atlas.command.CommandRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryCommandRegistry
        implements CommandRegistry {

    private final ConcurrentMap<CommandId, Command> commands =
            new ConcurrentHashMap<>();

    @Override
    public void register(Command command) {

        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        Command previous =
                commands.putIfAbsent(
                        command.id(),
                        command
                );

        if (previous != null) {
            throw new IllegalArgumentException(
                    "command already registered: "
                            + command.id().value()
            );
        }
    }

    @Override
    public Optional<Command> findById(
            CommandId id
    ) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "id cannot be null"
            );
        }

        return Optional.ofNullable(
                commands.get(id)
        );
    }

    @Override
    public List<Command> findAll() {
        return commands
                .values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                command ->
                                        command.requestedAt()
                        )
                )
                .toList();
    }
}