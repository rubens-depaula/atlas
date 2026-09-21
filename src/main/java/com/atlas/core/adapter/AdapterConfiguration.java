package com.atlas.core.adapter;

import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.adapter.AdapterRegistry;
import com.atlas.adapter.DeviceAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AdapterConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService demoAdapterExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean
    public DeviceAdapter demoDeviceAdapter(
            AdapterExecutionSink executionSink,
            @Qualifier("demoAdapterExecutor")
            ScheduledExecutorService executor
    ) {
        return new DemoDeviceAdapter(
                executionSink,
                executor
        );
    }

    @Bean
    public DeviceAdapter yellowBoardDeviceAdapter(
            AdapterExecutionSink executionSink,
            JsonMapper jsonMapper,
            @Value(
                    "${atlas.yellow-board.base-url:"
                            + "http://192.168.15.7}"
            )
            String baseUrl
    ) {
        return new YellowBoardDeviceAdapter(
                executionSink,
                jsonMapper,
                URI.create(baseUrl)
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
