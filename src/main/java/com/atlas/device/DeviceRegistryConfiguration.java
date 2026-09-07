package com.atlas.core.device;

import com.atlas.device.DeviceRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeviceRegistryConfiguration {

    @Bean
    public DeviceRegistry deviceRegistry() {
        return new InMemoryDeviceRegistry();
    }
}