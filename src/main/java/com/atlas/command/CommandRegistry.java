package com.atlas.command;

import java.util.List;
import java.util.Optional;

public interface CommandRegistry {

    void register(Command command);

    void save(Command command);

    Optional<Command> findById(CommandId id);

    List<Command> findAll();
}
