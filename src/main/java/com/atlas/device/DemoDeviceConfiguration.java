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
import com.atlas.property.FreshnessPolicy;
import com.atlas.property.PropertyDescriptor;
import com.atlas.property.PropertyKey;
import com.atlas.property.SemanticType;
import com.atlas.property.Unit;
import com.atlas.property.ValueDomain;
import com.atlas.property.ValueType;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Configuration
public class DemoDeviceConfiguration {

    @Bean
    public ApplicationRunner registerDemoDevice(
            DeviceRegistry deviceRegistry
    ) {
        return args -> {
            OffsetDateTime now = OffsetDateTime.now();

            DeviceId deviceId =
                    new DeviceId("dev_demo_spot_01");

            ActionKey setBrightness =
                    new ActionKey("set_brightness");

            PropertyDescriptor powerState =
                    new PropertyDescriptor(
                            deviceId,
                            new PropertyKey("power_state"),
                            "Ligado",
                            SemanticType.POWER_STATE,
                            ValueType.BOOLEAN,
                            Unit.NONE,
                            null,
                            true,
                            null,
                            FreshnessPolicy.OnChange.withoutHeartbeat()
                    );

            PropertyDescriptor brightness =
                    new PropertyDescriptor(
                            deviceId,
                            new PropertyKey("brightness"),
                            "Intensidade",
                            SemanticType.BRIGHTNESS,
                            ValueType.INTEGER,
                            Unit.PERCENT,
                            new ValueDomain(
                                    BigDecimal.ZERO,
                                    new BigDecimal("100"),
                                    BigDecimal.ONE,
                                    null,
                                    false
                            ),
                            false,
                            setBrightness,
                            FreshnessPolicy.OnChange.withoutHeartbeat()
                    );

            ActionDescriptor turnOn =
                    new ActionDescriptor(
                            new ActionKey("turn_on"),
                            "Ligar",
                            List.of(),
                            Criticality.COSMETIC,
                            false,
                            null,
                            null,
                            null,
                            List.of(
                                    new PropertyKey("power_state")
                            ),
                            null
                    );

            ActionDescriptor turnOff =
                    new ActionDescriptor(
                            new ActionKey("turn_off"),
                            "Desligar",
                            List.of(),
                            Criticality.COSMETIC,
                            false,
                            null,
                            null,
                            null,
                            List.of(
                                    new PropertyKey("power_state")
                            ),
                            null
                    );

            ActionDescriptor changeBrightness =
                    new ActionDescriptor(
                            setBrightness,
                            "Definir intensidade",
                            List.of(
                                    new ParameterDescriptor(
                                            "value",
                                            "Intensidade",
                                            ValueType.INTEGER,
                                            Unit.PERCENT,
                                            new ValueDomain(
                                                    BigDecimal.ZERO,
                                                    new BigDecimal("100"),
                                                    BigDecimal.ONE,
                                                    null,
                                                    false
                                            ),
                                            true
                                    )
                            ),
                            Criticality.COSMETIC,
                            false,
                            null,
                            null,
                            null,
                            List.of(
                                    new PropertyKey("brightness")
                            ),
                            null
                    );

            Device device = new Device(
                    deviceId,
                    "Spot da bancada 01",
                    new EnvironmentId("env_apartamento"),
                    null,
                    null,
                    DeviceClass.LIGHT,
                    DeviceLifecycle.ACTIVE,
                    Criticality.COSMETIC,
                    new DeviceSource(
                            "demo.adapter",
                            "spot_bancada_01",
                            now
                    ),
                    List.of(
                            powerState,
                            brightness
                    ),
                    List.of(
                            turnOn,
                            turnOff,
                            changeBrightness
                    ),
                    List.of(),
                    null,
                    now,
                    now,
                    null
            );

            deviceRegistry.register(device);
        };
    }
}