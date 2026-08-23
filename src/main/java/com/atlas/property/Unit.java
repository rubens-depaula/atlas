package com.atlas.property;

public enum Unit {

    NONE(Dimension.NONE, "", true),

    CELSIUS(Dimension.TEMPERATURE, "°C", true),
    FAHRENHEIT(Dimension.TEMPERATURE, "°F", false),
    KELVIN(Dimension.TEMPERATURE, "K", false),

    PERCENT(Dimension.RATIO, "%", true),
    RATIO_UNIT(Dimension.RATIO, "", false),

    WATT(Dimension.POWER, "W", true),
    KILOWATT(Dimension.POWER, "kW", false),

    WATT_HOUR(Dimension.ENERGY, "Wh", false),
    KILOWATT_HOUR(Dimension.ENERGY, "kWh", true),

    VOLT(Dimension.VOLTAGE, "V", true),
    AMPERE(Dimension.CURRENT, "A", true),

    LUX(Dimension.ILLUMINANCE, "lx", true),

    HECTOPASCAL(Dimension.PRESSURE, "hPa", true),
    BAR(Dimension.PRESSURE, "bar", false),
    PSI(Dimension.PRESSURE, "psi", false),

    LITERS_PER_MINUTE(Dimension.FLOW_RATE, "L/min", true),
    CUBIC_METERS_PER_HOUR(Dimension.FLOW_RATE, "m³/h", false),

    LITER(Dimension.VOLUME, "L", true),
    CUBIC_METER(Dimension.VOLUME, "m³", false),

    METER(Dimension.LENGTH, "m", true),
    CENTIMETER(Dimension.LENGTH, "cm", false),
    MILLIMETER(Dimension.LENGTH, "mm", false),

    SECONDS(Dimension.DURATION, "s", true),
    MINUTES(Dimension.DURATION, "min", false),
    HOURS(Dimension.DURATION, "h", false),

    METERS_PER_SECOND(Dimension.SPEED, "m/s", true),
    KILOMETERS_PER_HOUR(Dimension.SPEED, "km/h", false),

    PPM(Dimension.CONCENTRATION, "ppm", true),
    DECIBEL_MILLIWATT(Dimension.SIGNAL, "dBm", true),

    MIRED(Dimension.COLOR_TEMPERATURE, "mired", false),
    KELVIN_COLOR(Dimension.COLOR_TEMPERATURE, "K", true);

    private final Dimension dimension;
    private final String symbol;
    private final boolean canonical;

    Unit(Dimension dimension, String symbol, boolean canonical) {
        this.dimension = dimension;
        this.symbol = symbol;
        this.canonical = canonical;
    }

    public Dimension dimension() {
        return dimension;
    }

    public String symbol() {
        return symbol;
    }

    public boolean isCanonical() {
        return canonical;
    }
}