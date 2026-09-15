package com.atlas.core.command;

import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.adapter.AdapterRegistry;
import com.atlas.command.CommandRegistry;
import com.atlas.device.DeviceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class CommandConfiguration {

    @Bean
    public ActionCommandService actionCommandService() {
        return new ActionCommandService();
    }

    @Bean
    public CommandIdGenerator commandIdGenerator() {
        return new CommandIdGenerator();
    }

    @Bean
    public CommandRegistry commandRegistry(
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper
    ) {
        return new PostgresCommandRegistry(
                jdbcTemplate,
                jsonMapper
        );
    }

    @Bean
    public CommandDispatcher commandDispatcher(
            DeviceRegistry deviceRegistry,
            CommandRegistry commandRegistry,
            AdapterRegistry adapterRegistry
    ) {
        return new CommandDispatcher(
                deviceRegistry,
                commandRegistry,
                adapterRegistry
        );
    }

    @Bean
    public CommandExecutionService commandExecutionService(
            CommandRegistry commandRegistry
    ) {
        return new CommandExecutionService(
                commandRegistry
        );
    }

    @Bean
    public AdapterExecutionSink adapterExecutionSink(
            CommandExecutionService commandExecutionService
    ) {
        return new CoreAdapterExecutionSink(
                commandExecutionService
        );
    }

}
