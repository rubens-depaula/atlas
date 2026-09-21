package com.atlas.core.device;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ActionKey;
import com.atlas.action.ParameterDescriptor;
import com.atlas.device.Criticality;
import com.atlas.device.Device;
import com.atlas.device.DeviceClass;
import com.atlas.device.DeviceId;
import com.atlas.device.DeviceLifecycle;
import com.atlas.device.DeviceRegistry;
import com.atlas.device.DeviceSource;
import com.atlas.environment.EnvironmentId;
import com.atlas.property.Unit;
import com.atlas.property.ValueType;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.OffsetDateTime;
import java.util.List;

@Configuration
public class YellowBoardDeviceConfiguration {

    @Bean
    public ApplicationRunner registerYellowBoardDevice(
            DeviceRegistry deviceRegistry
    ) {
        return args -> {
            OffsetDateTime now =
                    OffsetDateTime.now();

            DeviceId deviceId =
                    new DeviceId(
                            "dev_atlas_desk_01"
                    );

            ActionDescriptor showMessage =
                    new ActionDescriptor(
                            new ActionKey(
                                    "show_message"
                            ),
                            "Exibir mensagem",
                            List.of(
                                    new ParameterDescriptor(
                                            "text",
                                            "Mensagem",
                                            ValueType.STRING,
                                            Unit.NONE,
                                            null,
                                            true
                                    )
                            ),
                            Criticality.COSMETIC,
                            false,
                            null,
                            null,
                            null,
                            List.of(),
                            null
                    );

            Device device =
                    new Device(
                            deviceId,
                            "ATLAS Desk 01",
                            new EnvironmentId(
                                    "env_apartamento"
                            ),
                            null,
                            null,
                            DeviceClass.CONTROLLER,
                            DeviceLifecycle.ACTIVE,
                            Criticality.COSMETIC,
                            new DeviceSource(
                                    "yellow-board.adapter",
                                    "atlas-desk-01",
                                    now
                            ),
                            List.of(),
                            List.of(
                                    showMessage
                            ),
                            List.of(),
                            null,
                            now,
                            now,
                            null
                    );

            deviceRegistry.register(
                    device
            );
        };
    }
}
