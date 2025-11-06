package goat.projectLinearity.libs.mutation;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Simple container mapping {@link StatType}s to their configured values.
 */
public final class Stats {

    private final EnumMap<StatType, Double> values = new EnumMap<>(StatType.class);

    private Stats() {
    }

    public static Stats empty() {
        return new Stats();
    }

    public static Stats of(Stat... stats) {
        Stats result = new Stats();
        if (stats == null) {
            return result;
        }
        for (Stat stat : stats) {
            if (stat == null || stat.type() == null) {
                continue;
            }
            result.values.put(stat.type(), stat.value());
        }
        return result;
    }

    public OptionalDouble get(StatType type) {
        Double value = values.get(type);
        if (value == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(value);
    }

    public double getOrDefault(StatType type, double defaultValue) {
        return values.getOrDefault(type, defaultValue);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<StatType, Double> asMap() {
        return Map.copyOf(values);
    }
}
