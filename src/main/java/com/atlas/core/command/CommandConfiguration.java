package com.atlas.core.command;

import com.atlas.command.CommandRegistry;
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
}
