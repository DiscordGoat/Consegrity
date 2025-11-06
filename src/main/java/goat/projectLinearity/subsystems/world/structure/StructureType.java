package goat.projectLinearity.subsystems.world.structure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Groups schematic identifiers under a semantic structure category.
 */
public enum StructureType {
    DESERT_TEMPLE("deserttemple"),
    JUNGLE_TEMPLE("jungletemple"),
    WITCH_HUT("witchhut"),
    WITCH_FESTIVAL("witchfestival"),
    MONASTERY("monastery"),
    HOT_SPRING("hotspring"),
    MONUMENT("monument", "abandonedmonument"),
    JADE_STATUE("jadestatue1", "jadestatue2", "jadestatue3", "jadestatuefinal"),
    BEACON("beacon0", "beacon1", "beacon2", "beacon3", "beacon4"),
    CONDUIT("conduit1", "conduit2", "conduit3", "conduit4"),
    SARCOPHAGUS("sarcophagus0", "sarcophagus1", "sarcophagus2", "sarcophagus3", "sarcophagus4", "sarcophagus5", "sarcophagus6", "sarcophagus7", "sarcophagus8"),
    PILLAGER("pillager"),
    PROSPECT("prospect"),
    HAY_WAGON("haywagon"),
    SEAMINE("seamine");

    private static final Map<String, StructureType> LOOKUP;

    static {
        Map<String, StructureType> map = new HashMap<>();
        for (StructureType type : values()) {
            for (String key : type.structureKeys) {
                map.put(key, type);
            }
        }
        LOOKUP = Collections.unmodifiableMap(map);
    }

    private final Set<String> structureKeys;

    StructureType(String... keys) {
        Set<String> normalized = new HashSet<>();
        if (keys != null) {
            for (String key : keys) {
                if (key == null) continue;
                normalized.add(normalize(key));
            }
        }
        this.structureKeys = Collections.unmodifiableSet(normalized);
    }

    public Set<String> getStructureKeys() {
        return structureKeys;
    }

    public boolean matches(String key) {
        return structureKeys.contains(normalize(key));
    }

    public static StructureType fromKey(String key) {
        if (key == null) {
            return null;
        }
        return LOOKUP.get(normalize(key));
    }

    private static String normalize(String key) {
        return key == null ? "" : key.toLowerCase(Locale.ENGLISH);
    }
}
