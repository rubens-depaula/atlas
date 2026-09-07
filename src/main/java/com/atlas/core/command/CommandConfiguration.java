package com.atlas.core.command;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}