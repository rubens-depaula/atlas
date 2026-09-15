package com.atlas.core.adapter;

import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.adapter.AdapterRegistry;
import com.atlas.adapter.DeviceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AdapterConfiguration {

    @Bean
    public DeviceAdapter demoDeviceAdapter(
            AdapterExecutionSink executionSink
    ) {
        return new DemoDeviceAdapter(
                executionSink
        );
    }

    @Bean
    public AdapterRegistry adapterRegistry(
            List<DeviceAdapter> adapters
    ) {
        return new InMemoryAdapterRegistry(
                adapters
        );
    }
}
