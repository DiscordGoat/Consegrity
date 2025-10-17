package goat.projectLinearity.util;

import org.bukkit.ChatColor;

import java.util.*;

/**
 * Registry for all curse types in the Consegrity curse system.
 */
public final class CurseRegistry {

    private static final Map<String, Curse> CURSES = new LinkedHashMap<>();
    private static final List<String> CURSE_NAMES = new ArrayList<>();

    static {
        registerCurses();
    }

    private CurseRegistry() {}

    private static void registerCurses() {
        // Temperature curses
        registerCurse("curse_of_freezing", "Curse of Freezing", ChatColor.RED + "Curse of Freezing", "Temperature decreases");
        registerCurse("curse_of_microwaving", "Curse of Microwaving", ChatColor.RED + "Curse of Microwaving", "Temperature increases");

        // Environmental curses
        registerCurse("curse_of_suffocation", "Curse of Suffocation", ChatColor.RED + "Curse of Suffocation", "Oxygen set to 0");
        registerCurse("curse_of_insomnia", "Curse of Insomnia", ChatColor.RED + "Curse of Insomnia", "Cannot sleep in beds");

        // Equipment curses
        registerCurse("curse_of_shattering", "Curse of Shattering", ChatColor.RED + "Curse of Shattering", "Durability losses quadrupled");
        registerCurse("curse_of_clumsiness", "Curse of Clumsiness", ChatColor.RED + "Curse of Clumsiness", "Damage causes 1 random item to drop");
        registerCurse("curse_of_silence", "Curse of Silence", ChatColor.RED + "Curse of Silence", "Nullifies all sound for the player");

        // Health curses
        registerCurse("curse_of_frailty", "Curse of Frailty", ChatColor.RED + "Curse of Frailty", "Max health set to half");
        registerCurse("curse_of_anorexia", "Curse of Anorexia", ChatColor.RED + "Curse of Anorexia", "Cannot consume food, but food restores health instead of hunger");

        // Status curses
        registerCurse("curse_of_gluttony", "Curse of Gluttony", ChatColor.RED + "Curse of Gluttony", "Hunger 2");
        registerCurse("curse_of_weakness", "Curse of Weakness", ChatColor.RED + "Curse of Weakness", "Weakness 2");
        registerCurse("curse_of_slowness", "Curse of Slowness", ChatColor.RED + "Curse of Slowness", "Slowness 2");
        registerCurse("curse_of_fatigue", "Curse of Fatigue", ChatColor.RED + "Curse of Fatigue", "Mining fatigue 3");
        registerCurse("curse_of_blindness", "Curse of Blindness", ChatColor.RED + "Curse of Blindness", "Darkness effect");

        // Combat curses
        registerCurse("curse_of_peace", "Curse of Peace", ChatColor.RED + "Curse of Peace", "Cannot damage monsters");
    }

    private static void registerCurse(String id, String name, String displayName, String description) {
        Curse curse = new Curse(id, name, displayName, description);
        CURSES.put(id, curse);
        CURSE_NAMES.add(name);
    }

    public static Curse getCurse(String name) {
        return CURSES.get(name.toLowerCase().replace(" ", "_"));
    }

    public static Collection<Curse> getAllCurses() {
        return Collections.unmodifiableCollection(CURSES.values());
    }

    public static List<String> getCurseNames() {
        return Collections.unmodifiableList(CURSE_NAMES);
    }

    public static Optional<Curse> getRandomCurse() {
        if (CURSES.isEmpty()) {
            return Optional.empty();
        }
        List<Curse> curseList = new ArrayList<>(CURSES.values());
        Random random = new Random();
        return Optional.of(curseList.get(random.nextInt(curseList.size())));
    }

    public record Curse(String id, String name, String displayName, String description) {
        public Curse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
        }
    }
}
