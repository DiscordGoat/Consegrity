package goat.minecraft.minecraftnew.subsystems.pets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import goat.minecraft.minecraftnew.utils.stats.DefenseManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import goat.minecraft.minecraftnew.subsystems.pets.PetRegistry;
import goat.minecraft.minecraftnew.subsystems.pets.PetTrait;
import goat.minecraft.minecraftnew.subsystems.pets.TraitRarity;
import goat.minecraft.minecraftnew.subsystems.pets.UniqueTrait;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.bukkit.scheduler.BukkitTask;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import goat.minecraft.minecraftnew.utils.stats.StrengthManager;
import goat.minecraft.minecraftnew.other.skilltree.Skill;
import goat.minecraft.minecraftnew.other.skilltree.SkillTreeManager;
import goat.minecraft.minecraftnew.other.skilltree.Talent;
import goat.minecraft.minecraftnew.other.health.HealthManager;
import goat.minecraft.minecraftnew.other.beacon.BeaconPassivesGUI;
import goat.minecraft.minecraftnew.other.additionalfunctionality.BlessingUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PetManager implements Listener {
    private JavaPlugin plugin;
    private XPManager xpManager;
    private static PetManager instance;
    public PetManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadPets();
    }

    public void setXPManager(XPManager xpManager) {
        this.xpManager = xpManager;
    }

    private Map<UUID, Horse> summonedHorses = new HashMap<>();
    // Tracks the last summoned pet for each player
    private final Map<UUID, String> lastActivePet = new HashMap<>();
    // Stores the location a player was at before entering Spectral mode
    private final Map<UUID, Location> ghostPreloc = new HashMap<>();

    // Instead of using IDs, we now store base64 textures directly.
    // You must populate these with actual base64 textures for each pet.
    // The keys must match the pet names used elsewhere in your code.
    private static final Map<String, String> PET_TEXTURES = new HashMap<>();

    // Pool of traits used when rolling a UNIQUE rarity trait. Modify as needed.
    private static final PetTrait[] UNIQUE_TRAITS = PetTrait.values();
    // Pool of perks that can be granted when rolling a UNIQUE trait.
    private static final UniqueTrait[] UNIQUE_PERKS = UniqueTrait.values();

    static {
        // Example placeholders, replace with actual base64 textures.
        // You can find these by searching for "custom head textures" online.
        PET_TEXTURES.put("Allay", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGY1ZGU5NDBiZmU0OTljNTllZThkYWM5ZjljMzkxOWU3NTM1ZWZmM2E5YWNiMTZmNDg0MmJmMjkwZjRjNjc5ZiJ9fX0=");
        PET_TEXTURES.put("Armadillo", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTg1MmIzM2JhMjk0ZjU2MDA5MDc1MmQxMTNmZTcyOGNiYzdkZDA0MjAyOWEzOGQ1MzgyZDY1YTIxNDYwNjhiNyJ9fX0=");
        PET_TEXTURES.put("Axolotl", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDU5YjI4OTFhYjJiYzU5NGEwNjkxMDg3MTgyZDI0NDdhZWZkMjE3YzgzNDRiZTdiMzMwMjM1NjdkNzhhMGY5NSJ9fX0=");
        PET_TEXTURES.put("Cat", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDllMTliN2I1YWE4NjliY2JlYTE5ZTVjOWQ3MWJiYmU3NDFmOGQ2Yjg5NGY4Y2VmNTM3OTE2MTJjMTllYjUwMCJ9fX0=");
        PET_TEXTURES.put("Cow", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODdlNzE5M2EwYzNmZjgyYzE4OTI3ZDVhNzMwMTVmMDU3ZDA4N2ZmOTJjNGJjZTE1NTdiZTQ2MjNkMzA5NTBmZiJ9fX0=");
        PET_TEXTURES.put("Horse", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTMzNWUzMTk2MTcxMzM1M2E3NjQwMWUwMGMzNDU0YjdjYTg4NWI3Nzg0ZDUyODhkMzIyNzIyMmQ5YjQ4ZDM5MyJ9fX0=");
        PET_TEXTURES.put("Glow Squid", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2U5NGExYmIxY2IwMGFhYTE1M2E3NGRhZjRiMGVlYTIwYjg5NzQ1MjJmZTk5MDFlYjU1YWVmNDc4ZWJlZmYwZCJ9fX0=");
        PET_TEXTURES.put("Parrot", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWRmNGIzNDAxYTRkMDZhZDY2YWM4YjVjNGQxODk2MThhZTYxN2Y5YzE0MzA3MWM4YWMzOWE1NjNjZjRlNDIwOCJ9fX0=");
        PET_TEXTURES.put("Sheep", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODRlNWNkYjBlZGIzNjJjYjQ1NDU4NmQxZmQwZWJlOTcxNDIzZjAxNWIwYjFiZmM5NWY4ZDVhZjhhZmU3ZTgxMCJ9fX0=");
        PET_TEXTURES.put("Turtle", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjEyYjU4Yzg0MWIzOTQ4NjNkYmNjNTRkZTFjMmFkMjY0OGFmOGYwM2U2NDg5ODhjMWY5Y2VmMGJjMjBlZTIzYyJ9fX0=");
        PET_TEXTURES.put("Villager", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDBjYzBhMTI1Nzc4OThlNzE2N2M0M2NmOWZlOGNiMjg5NTYzOTIxYjcwOTQ2MDEyYTYwODkzY2FiNzZlNTQ5In19fQ==");
        PET_TEXTURES.put("Squirrel", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzc2OTU5YTMxMWJiMzRiNGRjMTRhNmEwMzA3MTg1ZTdlZDIyYzk5ZmJhOGVmZGM1NGY3N2Y3NzZlMjk3MzFlIn19fQ==");
        PET_TEXTURES.put("Leviathan", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDBjZDA2ODI5NDZhNThlYTA1ZDEwODdjYzczMGY1ZmVlNzIzNDQzZDJlOWQxOWRmNGM3OTFmYjUxOTBmNTJmYSJ9fX0=");
        PET_TEXTURES.put("Dolphin", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGU5Njg4Yjk1MGQ4ODBiNTViN2FhMmNmY2Q3NmU1YTBmYTk0YWFjNmQxNmY3OGU4MzNmNzQ0M2VhMjlmZWQzIn19fQ==");
        PET_TEXTURES.put("Fish", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWZmNDQ0MjhkYzc0MzAwMDY3YzhjYTJlYTQyMGRmYjJlNzcyOGZiMzA3NDljMGRmYmY1YzQ3YTIwOGVjYzJlNyJ9fX0=");
        PET_TEXTURES.put("Golden Steve", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWYxMGMxNzlmNzNhZWVjZTg0N2EwNTg4MTVhMDlmZjQ3MTUzY2M4NzY1MmFhZGRlYzdhODBiZGVkYjkwMzRlNCJ9fX0=");
        PET_TEXTURES.put("Stray", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWUzOTFjNmU1MzVmN2FhNWEyYjZlZTZkMTM3ZjU5ZjJkN2M2MGRlZjg4ODUzYmE2MTFjZWIyZDE2YTdlN2M3MyJ9fX0=");
        PET_TEXTURES.put("Bat", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjYxNjI4MTExM2ZmMzNkZjhlYjNiMDE1NjBhZjViODRmNWJjY2U3MDc4OWYyYjA2M2E1NGM2YWYzYWMxZTg2NSJ9fX0=");
        PET_TEXTURES.put("Chicken", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTk2Y2Q1YzVjNTQ3NDdlMjk4YmExYzNiZjJhMmU3NjNjZWU2NThmOTc3MzY1YTlkMjMxYTM0M2Y5YjhkYzM2ZCJ9fX0=");
        PET_TEXTURES.put("Mooshroom", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzNhY2VlZjVkNDM1YTNjYTVkMjc2OGMwM2NlN2M3NDdjYWU4YWE1MDkxODE0NWUyOGQ5MzgwYjQ4N2UzYjI5MiJ9fX0=");
        PET_TEXTURES.put("Pig", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2FlZGZkYTE0NTQ0ZmEzNmM3NzE5NjYxODExZmRhOGY2YWRmZjViYzk1YWU3ZjlhMTgyNDExOTkxNmI1OTkzMCJ9fX0=");
        PET_TEXTURES.put("Scarecrow", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWQ2MjZiY2QyMTQ3ZGY4ZDdhMDllNDhkYThjYzg2OTU5YWJkNzZiYzllYTYyNTYxZGMyOWYwNTg5OWEzNjA0OSJ9fX0=");
        PET_TEXTURES.put("Killer Rabbit", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzc0ZDgyOTg3OTdlNzEyYmIxZjc1YWQ2ZmZhNzczNGFjNDIzN2VhNjliZTFkYjkyZjBlNDExMTVhMmMxNzBjZiJ9fX0=");
        PET_TEXTURES.put("Baron", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzEyYTk3MWYxY2U1OWUzNTk3MGE3Y2Q3Y2Y4YzllMjY5MjQ4ZWJmYzQ5NjUxOTBhMDM0NThmZWY2MjRjYjVmIn19fQ==");
        PET_TEXTURES.put("Mole", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzRiNTRmN2Y1NTkzYTMyM2I2NTUyMWU2MTA2MTZmZGM5OTEwZjI5ZTI3YWUzMTkxNTExNjIzZTgxOGQ4ODM0OCJ9fX0=");
        PET_TEXTURES.put("Yeti", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjM0MjUzODAxMjFjOGVjMWUwYmY0OTVhOTgzNGMxMGNiODNkOTRkMTlkZTk5NDJiMTg5NDAyM2E4Zjg3MiJ9fX0=");
        PET_TEXTURES.put("Iron Golem", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDI3MTkxM2EzZmM4ZjU2YmRmNmI5MGE0YjRlZDZhMDVjNTYyY2UwMDc2YjUzNDRkNDQ0ZmIyYjA0MGFlNTdkIn19fQ==");
        PET_TEXTURES.put("Dwarf", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTU5YWNjZDY1NzAzNzIyY2ZlYjlmNjUwOTk5M2VlNzJiYWRmNGNiYWVhZmM4ODA4MzYyZDRlOWE4MjA3ODdhNCJ9fX0=");
        PET_TEXTURES.put("Piglin Brute", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzMDBlOTAyNzM0OWM0OTA3NDk3NDM4YmFjMjllM2E0Yzg3YTg0OGM1MGIzNGMyMTI0MjcyN2I1N2Y0ZTFjZiJ9fX0=");
        PET_TEXTURES.put("Vindicator", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGFlZWQ5ZDhlZDE3NjllNzdlM2NmZTExZGMxNzk2NjhlZDBkYjFkZTZjZTI5ZjFjOGUwZDVmZTVlNjU3M2I2MCJ9fX0=");
        PET_TEXTURES.put("Guardian", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTBiZjM0YTcxZTc3MTViNmJhNTJkNWRkMWJhZTVjYjg1Zjc3M2RjOWIwZDQ1N2I0YmZjNWY5ZGQzY2M3Yzk0In19fQ==");
        PET_TEXTURES.put("Zombie Pigman", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDYxYWMyZTQ3NWFkODA3MmZlM2Q4OGIxYzRmYzZkYTRmYTYyYjJiZDE0MTc4MWQ2NmU4ZThkMGY5NzY0MDQ4OCJ9fX0=");
        PET_TEXTURES.put("Zombie", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzgzYWFhZWUyMjg2OGNhZmRhYTFmNmY0YTBlNTZiMGZkYjY0Y2QwYWVhYWJkNmU4MzgxOGMzMTJlYmU2NjQzNyJ9fX0=");
        PET_TEXTURES.put("Skeleton", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDgyYjc4ZGE2ZWU3MTNkNWFjZmU1ZmNiMDc1NGVlNTY5MDA4MzFhNTA5ODMxMzA2NDEwOGRlNmU3ZTQwNjgzOSJ9fX0=");
        PET_TEXTURES.put("Warden", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYWYwMGMzNDI5YTYzZjMzZjcyM2RlY2MzNThkMTcyM2U2ZDBjMzkyYTI2N2QwYzFmZTM2YTEzNjE3NTA0N2U2ZSJ9fX0=");
        PET_TEXTURES.put("Wither Skeleton", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODg2ZGMwY2ZjYWVlY2ZlMWFiNjkxNDZlNGQ0ZjExOTA4MzcwNzZhNjdkZWMxMzVmYWJkYTYyNzFmMzc1ZDAxZiJ9fX0=");
        PET_TEXTURES.put("Blaze", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTgwNDZkMzhhOTdjOTFmNTk5NDllYTc0MmVmZDc0ODI3Y2NlZGVmZTk4NTI4NTUyY2QzMjdiNGI2MWMzOWI1ZiJ9fX0=");
        PET_TEXTURES.put("Enderman", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGYyNDc2N2M4MTM4YjNkZmVjMDJmNzdiZDE1MTk5NGQ0ODBkNGU4Njk2NjRjZTA5YTI2YjE5Mjg5MjEyMTYyYiJ9fX0=");
        PET_TEXTURES.put("Drowned", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzNmN2NjZjYxZGJjM2Y5ZmU5YTYzMzNjZGUwYzBlMTQzOTllYjJlZWE3MWQzNGNmMjIzYjNhY2UyMjA1MSJ9fX0=");
        PET_TEXTURES.put("Raccoon", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmU5ZGFhOTllZTQ1YzAxMWVkMDc5ZDhmYzhhN2ViNzFiNTk2Yjg2NDQxNzA4YWQwOTU5ODNiNjAxZGIyMTE0YiJ9fX0=");
        PET_TEXTURES.put("Monkey", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDAxNjkzODM2ZjIwZjA0MWM2YjZhZDhhMDE4N2E0OTU2N2QwYzU3ZTM0MGEwMmU1NjFkNGQxNmU1NjkxZWI5YSJ9fX0=");
        PET_TEXTURES.put("Ent", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2JiZTFjNmEyNjg0Zjg1NGJjMmU1YjczNDA1NDRlOGVhZDM2NWY3YWQyY2M0NDJkMWZjOGY5NTE3MDNjM2QwNSJ9fX0=");
        PET_TEXTURES.put("Wither", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzRmMzI4ZjUwNDQxMjliNWQxZjk2YWZmZDFiOGMwNWJjZGU2YmQ4ZTc1NmFmZjVjNTAyMDU4NWVlZjhhM2RhZiJ9fX0=");
        PET_TEXTURES.put("Spider", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzM4ODE3M2Y0ZjgyYTE0MTUzZTA4NmJmMTM3OTA3MjU2ZTUxMmIyMTczMWYwNDcwMDQ3YmYyZDQ1MzU0NWQyMSJ9fX0=");
        PET_TEXTURES.put("Wolf", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2MyNWQ1ZGMwYjViZDkxMjI2YTNhZWYzYzJkNjdhYjVlNTcyNjkxMDVkZTUxYjM5OWJlMzhiYzc1N2Y0MWQifX19");
        PET_TEXTURES.put("Phoenix", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmRmZTU4ZGNhNDQ2ODg4MDlhNDRjMjczNjdmY2MyODgwODk3NzkyYWY4ODM4ZjkzZjJiNTg1YWZjYmU4ZjEzNyJ9fX0=");

        PET_TEXTURES.put("Imprint", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjMyZDgxMjRkZmRiYTczMzU4YWMxNDVkZmFkNTljOTc5NDkyNDBlMGUwZmI5ZTZlZDUzYzlkMjA5MmI4NGQwMiJ9fX0=");
        PET_TEXTURES.put("Spirit", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGZkOTk5NjdjY2Y5MzgyOTUwODA3NGEyNjZiMTBkNGQ2YmNiOWMxYTFmYjJiZjZkNTk0ZmIxN2I3YjE2OTBjNCJ9fX0=");
        PET_TEXTURES.put("Banshee", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjc2OTkwYjYzZWJjMzVhZDdkZmVmMzM1Y2IyNGM1N2JmZDY5NGZjODg0MjdjNzczNjJlMWI0ZTYxYzc4OTAzZSJ9fX0=");
        PET_TEXTURES.put("Wraith", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGE1NGRmMDNlMzY1ODkyMDUyODYwNTNkOWNhNzg1MGNlMjU2Y2M2ZDQ0ZDcwZmRmOWVkNTkwOTllY2Q2OWFmYyJ9fX0=");
        PET_TEXTURES.put("Revenant", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQ5YjMyOTQ3Zjc2ZWM3ZmI0YjMxMTczZWFlNDc2YTZmZWQ4Y2Y4NTY3M2MwZTMxMGJlODM3NTlhOThkNTJlNiJ9fX0=");

        //sea creature textures...
        PET_TEXTURES.put("Shark", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2QyMTEzZmJhNTFiODM2NTIyOWUwZmYxZjIwMWY1MzMxNDgzMzcxZjE4NjA1N2JhNzQyMDMyOTRkMDYxMDgyZiJ9fX0=");
        PET_TEXTURES.put("Great_White_Shark", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGUyZTU4NjU0MjlkZDI1MjBiYmM3MDNlNGE5ZjJmMWFiZDVlMWNjNWQzMWI4YTlhY2JmNzRiN2E5N2M5MzdhYSJ9fX0=");
        PET_TEXTURES.put("Megalodon", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzE2MzZhZTZlNTU4Y2Y4MzVlN2E0M2JjZmQwYmQ0YTI3N2Q1NmQ2NmE1ZjRjOGJjYzBhMDNlZDY2MzI1MTAifX19");
        PET_TEXTURES.put("Pirate", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjY1M2JkN2FiMjQyYzVhNDE2NzUyNGE3Nzk1OWYxMjZmYTc5MGY0YjNiODE1Mjc2MDIwM2MxZDIxOTc2OTdmNiJ9fX0=");
        PET_TEXTURES.put("Abomination", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ3ZmNlZGI2NzczZDNmN2NmNjYxN2FiMmQ1OWYyNzc1YmI1OWU5YjU1NGNmNDY4OTViYjg2ODRiMmJkMjYwOCJ9fX0=");
        PET_TEXTURES.put("Luminescent_Drowned", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjQ4MDBlNWY1ZTM3NTJkNGY2OWI1MjVjYzAwZGNjYTg2ODdhZThjYTBmYjYyYzQ1NzE5ZDlmY2U0NTFlYTQ1YSJ9fX0=");
        PET_TEXTURES.put("Poseidon", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmVjYjJhYTNhNTE0MTQ4ODU3NzI4OGEyOGZmMzk0NmI0MzQyOWY1NmJkMjIzMDFkOTFmYTM3MWU3NjVmM2I4YSJ9fX0=");
        PET_TEXTURES.put("waterspider", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzZhZjM5N2Q5N2I0NjcwMTQ5NmRmMmJkNmFmNGU0ZDdjZjYxMGY3MzcyMzlmZDMzYzlhMGE5MzMxOWUwODBmYiJ9fX0=");
        PET_TEXTURES.put("Midas", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjZjNWVlY2QyMTljNWUyM2Q4MjFjZmZkYWNiNTk3ZWQyM2M2MzI3YTI3MjM2Yzk4MjFjZGQ5NjgyNmQ5Y2E0MyJ9fX0=");
        PET_TEXTURES.put("Witch", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2U3MWE2ZWIzMDNhYjdlNmY3MGVkNTRkZjkxNDZhODBlYWRmMzk2NDE3Y2VlOTQ5NTc3M2ZmYmViZmFkODg3YyJ9fX0=");
        //mutations
        PET_TEXTURES.put("diver", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTc4MzBjMWQ4Mjg0NWM5MWI4MzQyOWY5ZGM1OTczMTc4NDE1MzhlMTRkNGZiZWQ2MWFlMWEzYjBlYjdjY2QifX19");
        PET_TEXTURES.put("Reuben", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTMyMGNmZDNiYjMyYmU1ZDk1NWY2Nzc1Y2Y3ZWJiNmJmYmNiMDVkMzk4YTg3YTRiMWEyYmVhYmJkMTVhNTcwOCJ9fX0=");
        PET_TEXTURES.put("Ghost", "");
    }

    public static PetManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new PetManager(plugin);
        }
        return instance;
    }

    // Helper method to create a custom skull from a base64 texture
    public ItemStack getCustomSkull(String base64Json) {
        // fallback if no texture
        if (base64Json == null || base64Json.isEmpty()) {
            return new ItemStack(Material.NAME_TAG);
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        try {
            // 1) Decode the JSON payload
            byte[] decoded = Base64.getDecoder().decode(base64Json);
            String json   = new String(decoded, StandardCharsets.UTF_8);

            // 2) Extract the URL field
            JsonObject root   = JsonParser.parseString(json).getAsJsonObject();
            String    urlText = root
                    .getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url")
                    .getAsString();

            // 3) Build a fresh PlayerProfile + PlayerTextures
            PlayerProfile   profile  = Bukkit.createPlayerProfile(UUID.randomUUID());
            PlayerTextures  textures = profile.getTextures();
            textures.setSkin(new URL(urlText), PlayerTextures.SkinModel.CLASSIC);
            profile.setTextures(textures);

            // 4) Apply it to the SkullMeta
            meta.setOwnerProfile(profile);
            skull.setItemMeta(meta);
            return skull;

        } catch (Exception e) {
            e.printStackTrace();
            // if anything goes wrong, fallback
            return new ItemStack(Material.NAME_TAG);
        }
    }


    public ItemStack getSkullForPet(String petName) {
        String texture = PET_TEXTURES.get(petName);
        if (!isValidBase64Texture(texture)) {
            // If no valid base64 texture is found, return a NAME_TAG
            return new ItemStack(Material.NAME_TAG);
        }
        return getCustomSkull(texture);
    }

    /**
     * Checks if the given texture string is likely a valid base64-encoded texture.
     *
     * This basic check ensures:
     * - It's not null or empty.
     * - It doesn't start with known placeholders like "BASE64_TEXTURE_FOR_".
     * - It's not a direct URL (e.g., "http://...").
     * - Starts with a known base64 JSON prefix often used for custom heads.
     *
     * Note: You can improve this method to decode and verify the JSON structure if you want more robust checks.
     */
    private boolean isValidBase64Texture(String texture) {
        if (texture == null || texture.isEmpty()) {
            return false;
        }

        // Check if it's still using a placeholder or a direct URL
        if (texture.startsWith("BASE64_TEXTURE_FOR_")) {
            return false;
        }
        return true;
    }

    // ====================
    // Trait rolling utils
    // ====================

    /**
     * Randomly selects a TraitRarity using the weight defined in {@link TraitRarity#getWeight()}.
     */
    private TraitRarity getRandomRarityWeighted(Player player) {
        int level = 0;
        if (SkillTreeManager.getInstance() != null) {
            level = SkillTreeManager.getInstance()
                    .getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.NATURAL_SELECTION);
        }

        List<TraitRarity> pool = new ArrayList<>(Arrays.asList(TraitRarity.values()));
        if (level >= 1) pool.remove(TraitRarity.COMMON);
        if (level >= 2) pool.remove(TraitRarity.UNCOMMON);
        if (level >= 3) pool.remove(TraitRarity.RARE);
        if (level >= 4) pool.remove(TraitRarity.EPIC);
        if (level >= 5) pool.remove(TraitRarity.LEGENDARY);

        double total = 0;
        for (TraitRarity r : pool) {
            total += r.getWeight();
        }
        double rand = Math.random() * total;
        for (TraitRarity r : pool) {
            rand -= r.getWeight();
            if (rand <= 0) {
                return r;
            }
        }
        return TraitRarity.MYTHIC;
    }

    /**
     * Picks a random trait for the given rarity. Unique rarity pulls from {@link #UNIQUE_TRAITS}.
     */
    private PetTrait getRandomTraitForRarity(TraitRarity rarity) {
        PetTrait[] pool = (rarity == TraitRarity.UNIQUE) ? UNIQUE_TRAITS : PetTrait.values();
        return pool[new Random().nextInt(pool.length)];
    }


    /**
     * Applies the tick speed effect to the player.
     * (Implementation depends on how tick speed affects your plugin/game mechanics.)
     *
     * @param player    The player to apply the tick speed to.
     * @param tickSpeed The tick speed value.
     */
    private Map<UUID, Map<String, Pet>> playerPets = new HashMap<>();
    private Map<UUID, Pet> activePets = new HashMap<>();
    private Map<UUID, BukkitTask> petTasks = new HashMap<>();

    private Set<UUID> guiCooldown = new HashSet<>();
    private final long COOLDOWN_TIME = 20L; // 1 second (20 ticks)

    public void addPet(Player player, Pet pet) {
        if(player == null){
            return;
        }
        UUID playerId = player.getUniqueId();
        Map<String, Pet> pets = playerPets.computeIfAbsent(playerId, k -> new HashMap<>());

        if (pets.containsKey(pet.getName())) {
            // Player already owns a pet with this name
            return;
        }

        pets.put(pet.getName(), pet);
        savePets();
        player.sendMessage(ChatColor.GREEN + "You have received a new pet: " + pet.getName());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 2.0f);
    }

    public Pet getPet(Player player, String petName) {
        return playerPets.getOrDefault(player.getUniqueId(), new HashMap<>()).get(petName);
    }

    private void summonHorseForPlayer(Player player, Pet pet) {
        World world = player.getWorld();
        Location location = player.getLocation();

        // Spawn a horse
        Horse horse = (Horse) world.spawnEntity(location, EntityType.HORSE);

        // Set horse properties
        horse.setTamed(true);
        horse.setOwner(player);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));

        // Scale stats based on pet level
        double speed = 0.2 + (pet.getLevel() * 0.02); // Example scaling
        double jumpStrength = 0.5 + (pet.getLevel() * 0.03); // Example scaling

        horse.setJumpStrength(Math.min(2.0, jumpStrength));
        horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(Math.min(0.5, speed));

        // Store the summoned horse
        summonedHorses.put(player.getUniqueId(), horse);

        player.sendMessage(ChatColor.GREEN + "Summoned your horse pet!");
    }

    /**
     * Summons a pet for a player. If another pet is active, it will be despawned first.
     *
     * @param player  The player.
     * @param petName The name of the pet to summon.
     */
    public void summonPet(Player player, String petName) {
        Pet pet = getPet(player, petName);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        if (pet != null) {
            // Check if the pet is already active
            if (pet.equals(activePets.get(player.getUniqueId()))) {
                despawnPet(player); // Despawn if already active
            } else {
                despawnPet(player); // Despawn any currently active pet
                activePets.put(player.getUniqueId(), pet);

                // Check if this is the Horse pet
                if ("Horse".equalsIgnoreCase(pet.getName())) {
                    summonHorseForPlayer(player, pet);
                } else {
                    spawnPetParticle(player, pet);
                }

                goat.minecraft.minecraftnew.subsystems.pets.traits.PetTraitEffects.getInstance().applyTraits(player);

                if ("Ghost".equalsIgnoreCase(pet.getName())) {
                    ghostPreloc.put(player.getUniqueId(), player.getLocation());
                    player.setGameMode(GameMode.SPECTATOR);
                }

                // Remember last summoned pet name
                lastActivePet.put(player.getUniqueId(), pet.getName());

            }
        }
    }

    private void saveHorseStats(Player player, Pet pet, Horse horse) {
        File file = new File(plugin.getDataFolder(), "pets.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = player.getUniqueId().toString() + "." + pet.getName() + ".horse";
        config.set(path + ".jumpStrength", horse.getJumpStrength());
        config.set(path + ".speed", horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());

        ItemStack armor = horse.getInventory().getArmor();
        if (armor != null) {
            config.set(path + ".armor", armor.getType().toString());
        } else {
            config.set(path + ".armor", null);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHorseStats(Player player, Pet pet, Horse horse) {
        File file = new File(plugin.getDataFolder(), "pets.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = player.getUniqueId().toString() + "." + pet.getName() + ".horse";
        double jumpStrength = config.getDouble(path + ".jumpStrength", 0.7); // Default
        double speed = config.getDouble(path + ".speed", 0.2); // Default

        horse.setJumpStrength(jumpStrength);
        horse.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);

        String armorType = config.getString(path + ".armor");
        if (armorType != null) {
            Material material = Material.getMaterial(armorType);
            if (material != null) {
                horse.getInventory().setArmor(new ItemStack(material));
            }
        }
    }

    /**
     * Writes the player's last summoned pet name to pets.yml
     */
    private void cacheLastActivePet(Player player) {
        String last = lastActivePet.get(player.getUniqueId());
        if (last == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), "pets.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set(player.getUniqueId().toString() + ".lastActive", last);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void despawnPet(Player player) {
        Pet pet = activePets.remove(player.getUniqueId());
        if (pet != null) {
            if ("Horse".equalsIgnoreCase(pet.getName())) {
                Horse horse = summonedHorses.remove(player.getUniqueId());
                if (horse != null) {
                    saveHorseStats(player, pet, horse);
                    horse.remove();
                }
            } else {
                removePetParticle(player);
            }
            if ("Ghost".equalsIgnoreCase(pet.getName())) {
                Location pre = ghostPreloc.remove(player.getUniqueId());
                if (pre != null) {
                    player.teleport(pre);
                }
                player.setGameMode(GameMode.SURVIVAL);
            }
            player.sendMessage(ChatColor.YELLOW + "Your pet '" + pet.getName() + "' has been despawned.");
            removePetParticle(player);
            goat.minecraft.minecraftnew.subsystems.pets.traits.PetTraitEffects.getInstance().removeTraits(player);
        }
    }

    /**
     * Retrieves the active pet for a player.
     *
     * @param player The player.
     * @return The active pet if any, else null.
     */
    public Pet getActivePet(Player player) {
        if (player == null) {
            return null;
        } else {
            return activePets.get(player.getUniqueId());
        }
    }

    public Map<String, Pet> getPlayerPets(Player player) {
        return new HashMap<>(playerPets.getOrDefault(player.getUniqueId(), new HashMap<>()));
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public String getLastActivePetName(UUID playerId) {
        return lastActivePet.get(playerId);
    }

    /**
     * Clears all pets from a player's collection.
     *
     * @param player The player whose pets are to be cleared.
     */
    public void clearPets(Player player) {
        UUID playerId = player.getUniqueId();

        // Remove active pet if any
        if (activePets.containsKey(playerId)) {
            Pet pet = activePets.get(playerId);
            removePetParticle(player);
            activePets.remove(playerId);
            player.sendMessage(ChatColor.YELLOW + "Your pet '" + pet.getName() + "' has been despawned.");
        }

        // Remove all pets from the collection
        if (playerPets.containsKey(playerId)) {
            playerPets.remove(playerId);
            savePets();
        }
    }

    // ==========================
    // Pet Particle Methods
    // ==========================

    /**
     * Spawns the particle effect associated with a pet, following the player.
     *
     * @param player The player.
     * @param pet    The pet.
     */
    private void spawnPetParticle(Player player, Pet pet) {
        Particle particleType = pet.getParticle();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                removePetParticle(player);
                return;
            }

            Location location = player.getLocation().add(0, 1.5, 0);
            player.getWorld().spawnParticle(particleType, location, 1, 0.3, 0.3, 0.3, 0.05);

        }, 0L, 100L); // every 5 seconds

        petTasks.put(player.getUniqueId(), task);
    }

    /**
     * Removes the particle effect associated with a player's active pet.
     *
     * @param player The player.
     */
    private void removePetParticle(Player player) {
        BukkitTask task = petTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // ==========================
    // Event Handlers
    // ==========================

    @EventHandler
    public void onPlayerGainXP(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        Pet activePet = getActivePet(player);
        if (activePet != null) {
            double xpGained = event.getAmount();
            double petXP = xpGained;
            SkillTreeManager mgr = SkillTreeManager.getInstance();
            if (mgr != null) {
                double chance = 0.0;
                chance += mgr.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.BONUS_PET_XP_I) * 0.02;
                chance += mgr.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.BONUS_PET_XP_II) * 0.04;
                chance += mgr.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.BONUS_PET_XP_III) * 0.06;
                chance += mgr.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.BONUS_PET_XP_IV) * 0.08;
                chance += mgr.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.BONUS_PET_XP_V) * 0.10;
                if (chance > 0 && Math.random() < chance) {
                    petXP *= 2;
                }
            }
            int before = activePet.getLevel();
            activePet.addXP(petXP);
            if (xpManager != null) {
                if(activePet.getLevel() < 100) {
                    xpManager.addXP(player, "Taming", petXP/4);
                    if (before < 100 && activePet.getLevel() >= 100) {
                        xpManager.addXP(player, "Taming", 2500);
                    }
                }
            }
            //player.sendMessage(ChatColor.AQUA + activePet.getName() + " gained " + xpGained + " XP!");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            Map<String, Pet> pets = playerPets.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            if (!pets.containsKey("Ghost")) {
                Pet ghost = PetRegistry.getPetByName("Ghost", this);
                if (ghost != null) {
                    addPet(player, ghost);
                }
            }
        }
        String lastPet = lastActivePet.get(player.getUniqueId());
        if (lastPet != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> summonPet(player, lastPet), 20L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Cache the last summoned pet name for this player
        cacheLastActivePet(player);
        despawnPet(player);
    }

    public void openPetGUI(Player player) {
        openPetGUI(player, 1);
    }

    public void openPetGUI(Player player, int page) {
        Map<String, Pet> petsMap = playerPets.getOrDefault(player.getUniqueId(), new HashMap<>());
        List<Pet> pets = new ArrayList<>(petsMap.values());

        int totalPages = (int) Math.ceil(pets.size() / 45.0);
        if (totalPages == 0) totalPages = 1;
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.DARK_GREEN + "Your Pets: Page " + page + "/" + totalPages);

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.setDisplayName(" ");
        pane.setItemMeta(pm);
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, pane.clone());
        }

        if (page > 1) {
            ItemStack prev = new ItemStack(Material.PAPER);
            ItemMeta meta = prev.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Previous Page");
            prev.setItemMeta(meta);
            gui.setItem(0, prev);
        }

        if (page < totalPages) {
            ItemStack next = new ItemStack(Material.PAPER);
            ItemMeta meta = next.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Next Page");
            next.setItemMeta(meta);
            gui.setItem(8, next);
        }

        Pet active = activePets.get(player.getUniqueId());
        if (active != null) {
            ItemStack activeIcon = getSkullForPet(active.getName());
            if (activeIcon == null) activeIcon = new ItemStack(Material.NAME_TAG);
            ItemMeta aMeta = activeIcon.getItemMeta();
            if (aMeta != null) {
                ChatColor rarityColor = active.getRarity().getColor();
                aMeta.setDisplayName(rarityColor + "[Lvl " + active.getLevel() + "] " + active.getName());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "Currently Summoned");
                aMeta.setLore(lore);
                activeIcon.setItemMeta(aMeta);
            }
            gui.setItem(4, activeIcon);
        }

        int startIndex = (page - 1) * 45;
        int endIndex = Math.min(pets.size(), startIndex + 45);
        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            Pet pet = pets.get(i);
            ItemStack petIcon = getSkullForPet(pet.getName());
            if (petIcon == null) {
                petIcon = new ItemStack(Material.NAME_TAG); // Fallback item
            }

            ItemMeta meta = petIcon.getItemMeta();
            if (meta != null) {
                ChatColor rarityColor = pet.getRarity().getColor();
                meta.setDisplayName(rarityColor + "[Lvl " + pet.getLevel() + "] " + pet.getName());

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GREEN + "XP: " + (int) pet.getXp() + "/" + (int) pet.getXPForNextLevel());
                lore.add(ChatColor.GRAY + " ");
                for (PetPerk perk : pet.getPerks()) {
                    lore.add(ChatColor.GOLD + perk.getDisplayName());
                    lore.add(ChatColor.GRAY + getDynamicPerkEffectDescription(perk, pet.getLevel()));
                    lore.add(ChatColor.GRAY + " ");
                }
                if (pet.getUniqueTrait() != null) {
                    lore.add(ChatColor.DARK_RED + "Unique Trait: " + pet.getUniqueTrait().getDisplayName());
                    lore.add(ChatColor.GRAY + getDynamicPerkEffectDescription(pet.getUniqueTrait().getPerk(), pet.getLevel()));
                    lore.add(ChatColor.GRAY + " ");
                } else {
                    lore.add(ChatColor.GRAY + "Trait: "
                            + pet.getTraitRarity().getColor()
                            + "[" + pet.getTraitRarity().getDisplayName() + "] "
                            + pet.getTraitRarity().getColor() + pet.getTrait().getDisplayName());
                    double traitValue = pet.getTrait().getValueForRarity(pet.getTraitRarity());
                    if (pet.getTrait() == PetTrait.STRONG) {
                        SkillTreeManager stm = SkillTreeManager.getInstance();
                        if (stm != null) {
                            int q = stm.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.QUIRKY);
                            traitValue *= (1 + q * 0.20);
                        }
                        int strengthBonus = (int) Math.round(traitValue);
                        lore.add(ChatColor.GRAY + "Grants " + StrengthManager.COLOR + "+" + strengthBonus + " " + StrengthManager.DISPLAY_NAME);
                    } else if (pet.getTrait() == PetTrait.HEALTHY) {
                        SkillTreeManager stm = SkillTreeManager.getInstance();
                        if (stm != null) {
                            int q = stm.getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.QUIRKY);
                            traitValue *= (1 + q * 0.20);
                        }
                        double base = 20.0;
                        double talent = 0.0;
                        SkillTreeManager mgr = SkillTreeManager.getInstance();
                        if (mgr != null) {
                            talent += mgr.getTalentLevel(player.getUniqueId(), Skill.PLAYER, Talent.HEALTH_I);
                            talent += mgr.getTalentLevel(player.getUniqueId(), Skill.PLAYER, Talent.HEALTH_II);
                            talent += mgr.getTalentLevel(player.getUniqueId(), Skill.PLAYER, Talent.HEALTH_III);
                            talent += mgr.getTalentLevel(player.getUniqueId(), Skill.PLAYER, Talent.HEALTH_IV);
                            talent += mgr.getTalentLevel(player.getUniqueId(), Skill.PLAYER, Talent.HEALTH_V);
                        }
                        base += talent;
                        if (BeaconPassivesGUI.hasBeaconPassives(player)
                                && BeaconPassivesGUI.hasPassiveEnabled(player, "mending")) {
                            base += 20.0;
                        }
                        if (BlessingUtils.hasFullSetBonus(player, "Monolith")) {
                            base += 20.0;
                        }
                        double petBonus = Math.floor((base * traitValue / 100.0) / 2) * 2;
                        lore.add(ChatColor.GRAY + "Grants " + HealthManager.COLOR + "+" + (int) petBonus + " " + HealthManager.DISPLAY_NAME);
                    } else {
                        lore.add(pet.getTrait().getColor() + pet.getTrait().getDescription() + ": "
                                + pet.getTrait().getColor() + "+" + traitValue + "%");
                    }
                }
                if (pet.equals(active)) {
                    lore.add(ChatColor.GREEN + "Currently Active");
                }
                meta.setLore(lore);
                petIcon.setItemMeta(meta);
            }

            gui.setItem(slot++, petIcon);
        }

        for (; slot < gui.getSize(); slot++) {
            gui.setItem(slot, pane.clone());
        }

        player.openInventory(gui);
    }

    private String getDynamicPerkEffectDescription(PetPerk perk, int level) {

        switch (perk) {
            case ROCK_EATER:
                int requiredMaterials = Math.max(256 - (level - 1) * (256 - 64) / 99, 64);
                return ChatColor.GRAY + "Compacts " + requiredMaterials + " stone into " + ChatColor.DARK_BLUE + "Compact Stone" + ChatColor.GRAY + " every ore break.";
            case WATERLOGGED:
                return ChatColor.GRAY + "Grants Breath " + ChatColor.AQUA + "Regeneration below Y=44.";
            case ASPECT_OF_THE_END:
                return ChatColor.BLUE + "Right Click: " + ChatColor.DARK_PURPLE + "Warp forward.";
            case BLAZE:
                return "";
            case SECRET_LEGION:
                return "Getting hit " + ChatColor.GRAY + "summons a " + ChatColor.YELLOW + "Secret Legion Ally " + ChatColor.GRAY + "to defend you. Costs " + ChatColor.GREEN + "2 Hunger.";
            case DECAY:
                return ChatColor.DARK_GRAY + "Rapidly deteriorates hit mobs, " + ChatColor.GRAY + "making them " + ChatColor.DARK_GRAY + "succumb faster.";
            case BLACKLUNG:
                return "Transplants " + ChatColor.RED + "lungs " + ChatColor.GRAY + "to operate with " + ChatColor.DARK_GRAY + "smoke " + ChatColor.GRAY + "instead of air.";
            case SUPERIOR_ENDURANCE:
                return "Gain " + ChatColor.GREEN + "eternal sustenance " + ChatColor.GRAY + "when grinding " + ChatColor.YELLOW + "crops.";
            case ANTIDOTE:
                return "Makes you immune to " + ChatColor.DARK_GRAY + "negative potion effects";
            case CULTIVATION:
                return "Gain " + ChatColor.GREEN + "+" + (level * 1) + "% Chance " + ChatColor.GRAY + "to harvest " + ChatColor.GREEN + "double crops.";
            case GREEN_THUMB:
                return "Grows " + ChatColor.YELLOW + "crops within " + (10 + level) + ChatColor.GRAY + " blocks every minute.";
            case GREED:
                return "Gain up to " + ChatColor.YELLOW + "4% chance " + ChatColor.GRAY + "to drop " + ChatColor.GREEN + "1-" + (level * 0.32) + " Emeralds " + ChatColor.GRAY + "when killing monsters.";
            case SKEPTICISM:
                return "Gain " + ChatColor.AQUA + "+2 Forest Spirit Chance.";
            case CHALLENGE:
                return "Gain " + ChatColor.AQUA + "+5 Forest Spirit Chance.";
            case FLOAT:
                return "Gain " + ChatColor.YELLOW + "Permanent Slow Falling.";
            case COLLECTOR:
                return ChatColor.YELLOW + "Automatically picks up " + ChatColor.GRAY + "nearby items" + ChatColor.GRAY + " less than "+ Math.min(50.0, 15.0 + level) + " Blocks away.";
            case LULLABY:
                return "Prevents " + ChatColor.DARK_GRAY + "monsters from spawning " + ChatColor.GRAY + "within " + ChatColor.YELLOW + ((level * 4) + 40) + " blocks.";
            case FLIGHT:
                return "Gain the ability to " + ChatColor.YELLOW + "Fly for " + level * 0.01 + " km";
            case BROOMSTICK:
                return "Fly endlessly but your health drops to 1 while flying.";
            case EMERALD_SEEKER:
                return "Gain a " + ChatColor.GREEN + "4% chance " + ChatColor.GRAY + "to mine an " + ChatColor.GREEN + "Emerald.";
            case MITHRIL_MINER:
                return "Gain a " + ChatColor.GREEN + (level * 0.05) + " chance " + ChatColor.GRAY + "to mine a " + ChatColor.YELLOW + "Mithril Chunk.";
            case ECHOLOCATION:
                return "Gain " + ChatColor.AQUA + "Night Vision.";
            case X_RAY:
                return ChatColor.YELLOW + "Find ores " + ChatColor.GRAY + "through stone.";
            case DIGGING_CLAWS:
                return "Gain " + ChatColor.YELLOW + "Haste II " + ChatColor.GRAY + "when mining blocks.";
            case ELITE:
                return ChatColor.GRAY + "Grants " + StrengthManager.COLOR + "+" + Math.min(level * 0.5, 25) + " " + StrengthManager.DISPLAY_NAME;
            case WALKING_FORTRESS:
                return "Gain " + ChatColor.AQUA + " +25 " + DefenseManager.DISPLAY_NAME;
            case REBIRTH:
                return "Revive after dying " + ChatColor.GRAY + "at " + ChatColor.GREEN + "full health " + ChatColor.GRAY + "at the cost of your entire hunger bar.";
            case SECOND_WIND:
                return "Gain " + ChatColor.GREEN + "Regeneration " + ChatColor.GRAY + "for " + ChatColor.GREEN + (level * 1) + ChatColor.GRAY + " seconds after taking damage.";
            case BLIZZARD:
                return "Gain " + ChatColor.DARK_GRAY + "+" + (level * 0.5) + "% Damage Reduction " + ChatColor.GRAY + "near snow.";
            case ASPECT_OF_THE_FROST:
                return ChatColor.DARK_GRAY + "Slows enemies " + ChatColor.GRAY + "for " + ChatColor.DARK_GRAY + (level * 1) + " seconds " + ChatColor.GRAY + "on hit.";
            case FIREPROOF:
                return "Gain " + ChatColor.RED + "immunity to Fire Damage.";
            case DEVOUR:
                return "Gain " + ChatColor.GREEN + "+1 Hunger " + ChatColor.GRAY + "when damaging mobs.";
            case BONE_PLATING_WEAK:
                return ChatColor.DARK_GRAY + "Reduces damage " + ChatColor.GRAY + "by " + ChatColor.DARK_GRAY + "20% " + ChatColor.GRAY + "for " + ChatColor.DARK_GRAY + (level * 1) + " seconds.";
            case LASER_BEAM:
                return "Has a " + ChatColor.AQUA + (level * 1) + "% chance " + ChatColor.GRAY + "to " + ChatColor.DARK_AQUA + "assist ranged attacks.";
            case SHOTCALLING:
                return "Gain " + ChatColor.RED + "+" + (level * 0.5) + "% Arrow Damage.";
            case HEADLESS_HORSEMAN:
                return "+" + ChatColor.RED + level*0.25 + "% Double Wheat Tally.";
            case ORANGE:
                return "+" + ChatColor.RED + level*0.25 + "% Double Carrot Tally.";
            case BEETS_ME:
                return "+" + ChatColor.RED + level*0.25 + "% Double Beetroot Tally.";
            case BLOODTHIRSTY:
                return "+" + ChatColor.RED + level*0.25 + "% Double Potato Tally.";
            case BONE_COLD:
                return "Provides " + ChatColor.AQUA + "immunity to freeze damage.";
            case TIPPED_SLOWNESS:
                return "Augments arrows with " + ChatColor.DARK_GRAY + "slowness " + ChatColor.GRAY + "for " + ChatColor.DARK_GRAY + (level * 1) + " seconds.";
            case QUICK_DRAW:
                return ChatColor.BLUE + "LEFT CLICK: " + ChatColor.GRAY + "To instantly fire arrows at reduced damage.";
            case RECOVERY:
                return "Gain a " + ChatColor.YELLOW + (level * 1) + "% chance " + ChatColor.GRAY + "to " + ChatColor.YELLOW + "recover arrows.";
            case BONE_PLATING:
                return ChatColor.DARK_GRAY + "Reduces damage " + ChatColor.GRAY + "by " + ChatColor.DARK_GRAY + level * 0.5 + "%";
            case STRONG_SWIMMER:
                return "Grants " + ChatColor.AQUA + "Dolphins Grace " + ChatColor.GRAY + "when swimming.";
            case COMFORTABLE:
                double bonusHealth = Math.round(level * 0.1 * 100.0) / 100.0;
                return "Grants " + ChatColor.GREEN + bonusHealth + " bonus health " + ChatColor.GRAY + "when eating.";
            case ANGLER:
                int anglerBonus = 5;
                return "Grants " + ChatColor.AQUA + anglerBonus + "% Sea Creature Chance.";
            case BUDDY_SYSTEM:
                return "Grants " + ChatColor.AQUA + "+5 Sea Creature Chance " + ChatColor.GRAY + "near other players.";
            case HEART_OF_THE_SEA:
                int heartBonus = 10;
                return "Grants " + ChatColor.AQUA + heartBonus + "% Sea Creature Chance.";
            case TERROR_OF_THE_DEEP:
                return "Grants you a combat buff when you strike a " + ChatColor.AQUA + "Sea Creature" + ChatColor.GRAY + " for " + level + " seconds.";
            case TREASURE_HUNTER:
                return ChatColor.AQUA + "Bonus Treasure Chance: " + ChatColor.GOLD + (level * 0.1) + "%";
            case PRACTICE:
                return ChatColor.YELLOW + "Triples Bartering XP from trades while the Villager pet is active.";
            case HAGGLE:
                double discount;
                if (level >= 100) {
                    discount = 25;
                } else if (level >= 75) {
                    discount = 20;
                } else if (level >= 50) {
                    discount = 15;
                } else if (level >= 25) {
                    discount = 10;
                } else if (level >= 1) {
                    discount = 5;
                } else {
                    discount = 0;
                }
                return ChatColor.GRAY + "Villager trade discount: " + ChatColor.YELLOW + discount + "%";
            case LEAP:
                return ChatColor.YELLOW + "Enables the ability to leap forward.";
            case SOFT_PAW:
                return ChatColor.YELLOW + "Reduces fall damage " + ChatColor.GRAY + "by " + ChatColor.YELLOW + (level * 1) + "%.";
            case SPEED_BOOST:
                return "Increases " + ChatColor.YELLOW + "speed " + ChatColor.GRAY + "by " + ChatColor.YELLOW + (level * 0.4f) + "%.";
            case CLAW:
                return ChatColor.GRAY + "Grants " + StrengthManager.COLOR + "+" + Math.min(level * 0.5, 10) + " " + StrengthManager.DISPLAY_NAME;
            case NO_HIBERNATION:
                return "Increases " + ChatColor.RED + "Natural Regeneration " + ChatColor.GRAY + "by " + ChatColor.RED + (level) + "%.";
            case GROOT:
                int requiredMaterialsWood = Math.max(256 - (level - 1) * (256 - 64) / 99, 64);
                return ChatColor.GRAY + "Compacts " + requiredMaterialsWood + " wood into " + ChatColor.GREEN + "Compact Logs" + ChatColor.GRAY + " when breaking wood";
            case COMPOSTER:
                int requiredMaterialsOrganic = Math.max(256 - (level - 1) * (256 - 64) / 99, 64);
                return ChatColor.GRAY + "Compacts " + requiredMaterialsOrganic + " crops into " + ChatColor.GREEN + "Organic Soil";
            case HARVEST_FESTIVAL:
                return ChatColor.GOLD + "Sacrifices crops for Harvest progress. "
                            + ChatColor.YELLOW + "Every 2000 crops" + ChatColor.GRAY + ": "
                            + ChatColor.GREEN + "+100 Harvest Count";
            case LUMBERJACK:
                return "Drops " + ChatColor.GREEN + "+2 logs " + ChatColor.GRAY + "when chopping trees.";
            case EARTHWORM:
                return "Grants " + ChatColor.YELLOW + "Haste 8 " + ChatColor.GRAY + "for " + ChatColor.WHITE + "0.5s " + ChatColor.GRAY + "when mining grass/dirt/gravel/sand";
            case ALPHA:
                return "Getting hit summons a temporary wolf ally.";
            case FETCH:
                double catchChance = Math.min(level * 4, 100);
                return "Has a " + ChatColor.YELLOW + catchChance + "%" + ChatColor.GRAY + " chance to catch incoming arrows.";
            case SPIDER_STEVE:
                return "Allows the player to " + ChatColor.GOLD + "Scale Walls " + ChatColor.GRAY + "by " + ChatColor.YELLOW + "rapidly clicking them.";
            case PARKOUR_ROLL:
                // scale from 10→30 blocks over levels 1→100
                int maxFallDistance = 10 + Math.round((level - 1) * 20f / 99f);
                return "Take no "
                        + ChatColor.RED + "fall damage "
                        + ChatColor.GRAY + "from up to "
                        + ChatColor.RED + maxFallDistance
                        + ChatColor.GRAY + " blocks.";
            case OBSESSION:
                return "10% chance to gain " + ChatColor.YELLOW + "+1 Hunger " + ChatColor.GRAY + "when " + ChatColor.YELLOW + "placing blocks.";
            case PHOENIX_REBIRTH:
                double rebirthHealth = 25 + (level * 0.5);
                int immunityDuration = 10 + (level / 10);
                return "When you die, " + ChatColor.GOLD + "resurrect " + ChatColor.GRAY + "at " + ChatColor.GREEN + rebirthHealth + "% health " + ChatColor.GRAY + "with " + ChatColor.RED + "5 minute cooldown";
            case FLAME_TRAIL:
                double flameDamage = 3.0 + (level * 0.08);
                return "Creates " + ChatColor.RED + "explosive fire bursts " + ChatColor.GRAY + "while moving that deal " + ChatColor.RED + String.format("%.1f", flameDamage) + " damage " + ChatColor.GRAY + "to monsters within " + ChatColor.YELLOW + "8 blocks" + ChatColor.GRAY + ". Distance reduces damage.";
            case ENDLESS_WARP:
                return ChatColor.DARK_PURPLE + "Grants +100 stacks of Warp for the Warp enchant.";
            case SPLASH_POTION:
                return "Reduces brew time by " + ChatColor.YELLOW + level/2 + "%" + ChatColor.GRAY + ".";
            case EXPERIMENTATION:
                return "Potions last " + ChatColor.YELLOW + (3 * level) + "s" + ChatColor.GRAY + " longer.";
            case MICROWAVE:
                return ChatColor.GRAY + "Reduces cooking time by 50%.";
            case TRASH_CAN:
                return ChatColor.GRAY + "Increases Delight yield by +2.";
            case MEMORY:
                return ChatColor.GRAY + "+0.01 " + ChatColor.AQUA + "Grave Chance";
            case HAUNTING:
                return ChatColor.GRAY + "+0.02 " + ChatColor.AQUA + "Grave Chance";
            case SCREAM:
                return ChatColor.GRAY + "+0.04 " + ChatColor.AQUA + "Grave Chance";
            case COLD:
                return ChatColor.GRAY + "+0.05 " + ChatColor.AQUA + "Grave Chance";
            case MALIGNANCE:
                return ChatColor.GRAY + "+0.10 " + ChatColor.AQUA + "Grave Chance";
            case REVENANT:
                return ChatColor.GRAY + "Return from death after 2 minutes";
            case SPECTRAL:
                return ChatColor.GRAY + "Enter " + ChatColor.DARK_PURPLE + "Spectator" + ChatColor.GRAY + " mode for scouting.";
            default:
                return ChatColor.GRAY + "Static effect or undefined scaling.";

        }

    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        String rawTitle = ChatColor.stripColor(event.getView().getTitle());
        if (rawTitle.startsWith("Your Pets")) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            int currentPage = 1;
            if (rawTitle.contains("Page")) {
                try {
                    String pagePart = rawTitle.substring(rawTitle.indexOf("Page") + 5);
                    currentPage = Integer.parseInt(pagePart.split("/")[0]);
                } catch (Exception ignored) {}
            }

            if (clicked != null && clicked.hasItemMeta()) {
                String displayName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
                if (displayName.equalsIgnoreCase("Next Page")) {
                    openPetGUI(player, currentPage + 1);
                    return;
                }
                if (displayName.equalsIgnoreCase("Previous Page")) {
                    openPetGUI(player, currentPage - 1);
                    return;
                }

                String petName = displayName.replaceFirst("\\[Lvl \\d+\\] ", "");
                Pet pet = getPet(player, petName);
                if (pet != null) {
                    if (event.getClick().isRightClick()) {
                        if (player.getLevel() < 1) {
                            player.sendMessage(ChatColor.RED + "You need at least 1 XP level to roll a trait.");
                            return;
                        }
                        player.setLevel(player.getLevel() - 1);
                        // Rerolling a trait removes any current unique trait
                        pet.setUniqueTrait(null);
                        TraitRarity rarity = getRandomRarityWeighted(player);
                        if (rarity == TraitRarity.UNIQUE) {
                            UniqueTrait uTrait = UNIQUE_PERKS[new Random().nextInt(UNIQUE_PERKS.length)];
                            pet.setUniqueTrait(uTrait);
                            savePets();
                            player.sendMessage(ChatColor.GREEN + "Your pet " + pet.getName() + " gained the "
                                    + rarity.getColor() + "[" + rarity.getDisplayName() + "] "
                                    + uTrait.getDisplayName() + ChatColor.GREEN + " trait!");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.1f);
                            openPetGUI(player, currentPage);
                        } else {
                            PetTrait trait = getRandomTraitForRarity(rarity);
                            pet.setTrait(trait, rarity);
                            savePets();
                            player.sendMessage(ChatColor.GREEN + "Your pet " + pet.getName() + " gained the "
                                    + rarity.getColor() + "[" + rarity.getDisplayName() + "] "
                                    + trait.getDisplayName() + ChatColor.GREEN + " trait!");
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                            openPetGUI(player, currentPage);
                        }
                    } else {
                        summonPet(player, petName);
                        openPetGUI(player);
                    }
                }
            }
        }
    }

    /**
     * Creates a new pet with specified properties and adds it to the player's collection.
     *
     * @param player    The player.
     * @param name      The name of the pet.
     * @param rarity    The rarity of the pet.
     * @param maxLevel  The maximum level the pet can reach.
     * @param particle  The particle effect associated with the pet.
     * @param perks     The perks granted by the pet.
     */
    public void createPet(Player player, String name, Rarity rarity, int maxLevel, Particle particle, PetPerk... perks) {
        ItemStack icon = getSkullForPet(name);
        List<PetPerk> perkList = Arrays.asList(perks);
        Pet newPet = new Pet(name, rarity, maxLevel, icon, particle, perkList,
                PetTrait.HEALTHY, TraitRarity.COMMON);
        addPet(player, newPet);
    }

    public void savePets() {
        File file = new File(plugin.getDataFolder(), "pets.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (UUID playerId : playerPets.keySet()) {
            Map<String, Pet> pets = playerPets.get(playerId);
            for (String petName : pets.keySet()) {
                Pet pet = pets.get(petName);
                String path = playerId.toString() + "." + petName;
                config.set(path + ".rarity", pet.getRarity().toString());
                config.set(path + ".level", pet.getLevel());
                config.set(path + ".xp", pet.getXp());
                config.set(path + ".icon", pet.getIcon().getType().toString());
                config.set(path + ".particle", pet.getParticle().toString());

                // We no longer save skullId (no SkullsAPI). Instead, we rely solely on textures.
                // If needed, you could store the base64 texture here as well.
                // config.set(path + ".texture", PET_TEXTURES.get(pet.getName()));

                List<String> perkNames = pet.getPerks().stream().map(Enum::name).collect(Collectors.toList());
                config.set(path + ".perks", perkNames);
                if (pet.getUniqueTrait() != null) {
                    config.set(path + ".uniqueTrait", pet.getUniqueTrait().name());
                } else {
                    config.set(path + ".uniqueTrait", null);
                }
                config.set(path + ".trait", pet.getTrait().name());
                config.set(path + ".traitRarity", pet.getTraitRarity().name());
            }
            // Save last active pet if known
            if (lastActivePet.containsKey(playerId)) {
                config.set(playerId.toString() + ".lastActive", lastActivePet.get(playerId));
            }
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves all cached last active pets to disk.
     */
    public void saveLastActivePets() {
        File file = new File(plugin.getDataFolder(), "pets.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (Map.Entry<UUID, String> entry : lastActivePet.entrySet()) {
            config.set(entry.getKey().toString() + ".lastActive", entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPets() {
        File file = new File(plugin.getDataFolder(), "pets.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String playerIdString : config.getKeys(false)) {
            UUID playerId = UUID.fromString(playerIdString);
            Map<String, Pet> pets = new HashMap<>();
            ConfigurationSection playerSection = config.getConfigurationSection(playerIdString);

            for (String petName : playerSection.getKeys(false)) {
                if ("lastActive".equals(petName)) {
                    String last = config.getString(playerIdString + ".lastActive");
                    if (last != null) {
                        lastActivePet.put(playerId, last);
                    }
                    continue;
                }
                String path = playerIdString + "." + petName;
                String rarityString = config.getString(path + ".rarity");
                int level = config.getInt(path + ".level");
                double xp = config.getDouble(path + ".xp");
                String particleName = config.getString(path + ".particle");
                String traitString = config.getString(path + ".trait");
                String traitRarityString = config.getString(path + ".traitRarity");
                String uniqueTraitString = config.getString(path + ".uniqueTrait");
                List<String> perkNames = config.getStringList(path + ".perks");

                List<PetPerk> perks = perkNames.stream().map(PetPerk::valueOf).collect(Collectors.toList());
                Rarity rarity = Rarity.valueOf(rarityString);
                PetTrait trait;
                TraitRarity traitRarity;
                try {
                    trait = PetTrait.valueOf(traitString);
                } catch (Exception e) {
                    trait = PetTrait.HEALTHY;
                }
                try {
                    traitRarity = TraitRarity.valueOf(traitRarityString);
                } catch (Exception e) {
                    traitRarity = TraitRarity.COMMON;
                }

                UniqueTrait uniqueTrait = null;
                if (uniqueTraitString != null) {
                    try {
                        uniqueTrait = UniqueTrait.valueOf(uniqueTraitString);
                    } catch (Exception ignored) {
                    }
                }

                // Retrieve icon from the pet name using the textures we have
                ItemStack icon = getSkullForPet(petName);

                Particle particle;
                try {
                    particle = Particle.valueOf(particleName);
                } catch (IllegalArgumentException e) {
                    particle = Particle.FLAME;
                }

                Pet pet = new Pet(petName, rarity, 100, icon, particle, perks, trait, traitRarity);
                pet.setLevel(level);
                pet.setXp(xp);
                pet.setUniqueTrait(uniqueTrait);

                pets.put(petName, pet);
            }

            playerPets.put(playerId, pets);
        }
    }

    public class Pet {
        private String name;
        private Rarity rarity;
        private int maxLevel;
        private int level;
        private double xp;
        private ItemStack icon;
        private Particle particle;
        private List<PetPerk> perks;
        private PetTrait trait;
        private TraitRarity traitRarity;
        private UniqueTrait uniqueTrait;

        public Pet(String name, Rarity rarity, int maxLevel, ItemStack icon, Particle particle, List<PetPerk> perks,
                    PetTrait trait, TraitRarity traitRarity) {
            this.name = name;
            this.rarity = rarity;
            this.maxLevel = maxLevel;
            this.icon = icon;
            this.particle = particle;
            this.perks = perks;
            this.level = 1;
            this.xp = 0;
            this.trait = trait;
            this.traitRarity = traitRarity;
            this.uniqueTrait = null;
        }

        public String getName() {
            return name;
        }

        public Rarity getRarity() {
            return rarity;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public int getLevel() {
            return level;
        }

        public double getXp() {
            return xp;
        }

        public ItemStack getIcon() {
            return icon.clone();
        }

        public Particle getParticle() {
            return particle;
        }

        public List<PetPerk> getPerks() {
            return perks;
        }

        public PetTrait getTrait() {
            return trait;
        }

        public TraitRarity getTraitRarity() {
            return traitRarity;
        }

        public void setTrait(PetTrait trait, TraitRarity rarity) {
            this.trait = trait;
            this.traitRarity = rarity;
            // Acquiring a normal trait removes any unique trait
            this.uniqueTrait = null;
        }

        public UniqueTrait getUniqueTrait() {
            return uniqueTrait;
        }

        public void setUniqueTrait(UniqueTrait uniqueTrait) {
            this.uniqueTrait = uniqueTrait;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public void setXp(double xp) {
            this.xp = xp;
        }

        public boolean hasPerk(PetPerk perk) {
            return perks.contains(perk);
        }

        public boolean hasUniqueTraitPerk(PetPerk perk) {
            return uniqueTrait != null && uniqueTrait.getPerk() == perk;
        }

        public void addXP(double amount) {
            this.xp += amount;
            while (this.xp >= getXPForNextLevel() && this.level < maxLevel) {
                levelUp();
            }
        }

        private void levelUp() {
            this.xp -= getXPForNextLevel();
            this.level++;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (playerPets.getOrDefault(player.getUniqueId(), new HashMap<>()).containsKey(name)) {
                    player.sendMessage(ChatColor.GREEN + name + " has leveled up to level " + level + "!");
                }
            }
        }

        public double getXPForNextLevel() {
            // Base XP required for level 2
            double baseXP = 20;

            // Calculate the XP required for the next level
            // Each level requires 2 more XP than the previous level
            return baseXP + (level - 1) * 14;
        }
    }

    public enum PetPerk {
        WATERLOGGED("Waterlogged", ""),
        ASPECT_OF_THE_END("Aspect of the End", "Right click to warp forward."),
        BLAZE("Blaze", "Lights hit mobs on fire."),
        SECRET_LEGION("Secret Legion", "Summons a Zombified Piglin minion to defend you."),
        DECAY("Decay","Rapidly Deteriorates hit mobs."),
        BLACKLUNG("Blacklung","Transplants a nether-attuned lung."),
        SUPERIOR_ENDURANCE("Superior Endurance", "Gain +1 saturation when breaking crops."),
        ELITE("Elite", "+50 Strength ⚔"),
        GREED("Greed","4% chance to drop 1-16 emeralds on killing a monster."),
        BONE_COLD("Bone Cold","Grants immunity to freeze damage."),
        TIPPED_SLOWNESS("Tipped Slowness","Arrows you fire apply slowness."),
        X_RAY("X-Ray", "Helps you locate ores."),
        EMERALD_SEEKER("Emerald Seeker","1% chance to mine emeralds."),
        FIREPROOF("Fireproof","Grants permanent fire resistance."),
        LASER_BEAM("Laser Beam","25% chance to assist ranged attacks."),
        SKEPTICISM("Skepticism","Increases Forest Spirit spawn chance by 2%"),
        CHALLENGE("Challenge","Increases Forest Spirit spawn chance by 5%."),
        MITHRIL_MINER("Mithril Miner","1% chance to find Mithril Chunks while mining."),
        WALKING_FORTRESS("Walking Fortress","-20% movement speed, +60% defense."),
        BLIZZARD("Blizzard","Slows enemies for [Lvl] ticks."),
        ASPECT_OF_THE_FROST("Aspect Of The Frost","Take 50% less damage near snow."),
        CULTIVATION("Cultivation","Extra crops drop chance based on level."),
        FLOAT("Float","Grants Slow Falling while shifting in the air."),
        ECHOLOCATION("Echolocation","Grants Night Vision."),
        QUICK_DRAW("Quick Draw","Left Click to instantly fire Arrows! (Ignores Infinity)"),
        RECOVERY("Recovery","[Lvl] % chance to recover arrow."),
        TREASURE_HUNTER("Treasure Hunter","Triples Treasure Chance while fishing."),
        BAIT("Bait","Gains 1 Sea Creature Chance per 10 Lvls."),
        STRONG_SWIMMER("Strong Swimmer","Activates Dolphins Grace in water."),
        TERROR_OF_THE_DEEP("Terror Of The Deep","Grants a combat buff when hitting Sea Creatures."),
        HEART_OF_THE_SEA("Heart Of The Sea","+10 Sea Creature Chance."),
        GREEN_THUMB("Green Thumb","Increases tick speed while equipped."),
        HAGGLE("Haggle","Provides up to 25% discount on villager transactions."),
        PRACTICE("Practice","Triples Bartering XP when the Villager pet is active"),
        COMFORTABLE("Comfortable","Grants you with 1 bonus health per 10 levels when eating."),
        LULLABY("Lullaby","Lulls monsters back to sleep, preventing their spawning."),
        ANGLER("Angler","+5 Sea Creature Chance"),
        BUDDY_SYSTEM("Buddy System", "+5 Sea Creature Chance near other players"),
        ANTIDOTE("Antidote","Cures a random negative potion effect on eating."),
        LEAP("Leap","Enables you to leap forward when shifting."),
        SOFT_PAW("Soft Paw","Reduces fall damage."),
        CLAW("Claws", "+0.5 Strength ⚔ per level (max +10)"),
        REBIRTH("Rebirth","Cheat Death at the cost of your entire hunger bar."),
        DEVOUR("Devour", "Provides hunger when damaging mobs."),
        BONE_PLATING_WEAK("Weak Bone Plating", ChatColor.GOLD + "Provides minor damage resistance on taking damage."),
        DIGGING_CLAWS("Digging Claws", ChatColor.GOLD + "Provides moderate mining speed when mining blocks."),
        BONE_PLATING("Bone Plating", ChatColor.GOLD + "Provides moderate damage resistance for Lvl seconds."),
        COLLECTOR("Collector", ChatColor.GOLD + "Automatically collects nearby items upon walking nearby."),
        LUMBERJACK("Lumberjack", ChatColor.GOLD + "Grants 2 extra logs when chopping trees."),
        FLIGHT("Flight", ChatColor.GOLD + "Grants the ability to fly."),
        BROOMSTICK("Broomstick", ChatColor.GOLD + "Infinite flight but health is reduced to 1 while flying."),
        SPEED_BOOST("Speed Boost", ChatColor.GOLD + "Increases your walking speed by 100%."),
        SECOND_WIND("Second Wind", ChatColor.GOLD + "Provides [Lvl] seconds of regeneration when taking damage."),
        SHOTCALLING("Shotcalling", ChatColor.GOLD + "Increases arrow damage by 1% per [Lvl]"),
        ROCK_EATER("Ore Magnet", ChatColor.GOLD + ""),
        NO_HIBERNATION("No Hibernation", ChatColor.GOLD + ""),
        GROOT("Groot", ChatColor.GOLD + ""),
        COMPOSTER("Composter", ChatColor.GOLD + ""),
        HARVEST_FESTIVAL(
                "Harvest Festival",
                ChatColor.GOLD + "Sacrifices crops for Harvest progress. "
                        + ChatColor.YELLOW + "Every 2000 crops" + ChatColor.GRAY + ": "
                        + ChatColor.GREEN + "+100 Harvest Count " + ChatColor.GRAY + "to your dominant crop."
        ),
        HEADLESS_HORSEMAN("Headless Horseman", ChatColor.GOLD + "+[Lvl]% Bonus Wheat Cropcount."),
        ORANGE("Orange", ChatColor.GOLD + "+[Lvl]% Bonus Carrot Cropcount."),
        BEETS_ME("Beets me", ChatColor.GOLD + "+[Lvl]% Bonus Beetroot Cropcount."),
        BLOODTHIRSTY("Bloodthirsty", ChatColor.GOLD + "+[Lvl]% Bonus Potato Cropcount."),
        OBSESSION("Obsession", ChatColor.GOLD + ""),
        SPIDER_STEVE("Spider Steve", ChatColor.GOLD + ""),
        ALPHA("Alpha", ChatColor.GOLD + ""),
        FETCH("Fetch", ChatColor.GOLD + ""),
        PARKOUR_ROLL("Parkour Roll", ChatColor.GOLD + ""),
        EARTHWORM("Earthworm", ChatColor.GOLD + ""),
        PHOENIX_REBIRTH("Phoenix Rebirth", ChatColor.GOLD + ""),
        FLAME_TRAIL("Flame Trail", ChatColor.GOLD + ""),
        ENDLESS_WARP("Endless Warp", ChatColor.GOLD + ""),
        SPLASH_POTION("Splash Potion", ChatColor.GOLD + ""),
        EXPERIMENTATION("Experimentation", ChatColor.GOLD + ""),
        MICROWAVE("Microwave", ChatColor.GOLD + "Reduces cooking time by 50%"),
        TRASH_CAN("Trash Can, Not Can't", ChatColor.GOLD + "Increases Delight Yield by +2."),
        MEMORY("Memory", ChatColor.GOLD + "+0.01 " + ChatColor.AQUA + "Grave Chance"),
        HAUNTING("Haunting", ChatColor.GOLD + "+0.02 " + ChatColor.AQUA + "Grave Chance"),
        SCREAM("Scream", ChatColor.GOLD + "+0.04 " + ChatColor.AQUA + "Grave Chance"),
        COLD("Cold", ChatColor.GOLD + "+0.05 " + ChatColor.AQUA + " Grave Chance"),
        MALIGNANCE("Malignance", ChatColor.GOLD + "+0.10 " + ChatColor.AQUA + " Grave Chance"),
        REVENANT("Revenant", ChatColor.GOLD + "Return from the dead after 2 minutes"),
        SPECTRAL("Spectral", ChatColor.GOLD + "Allows scouting in Spectator mode");

        private final String displayName;
        private final String description;

        PetPerk(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Rarity {
        COMMON(ChatColor.WHITE),
        UNCOMMON(ChatColor.GREEN),
        RARE(ChatColor.BLUE),
        EPIC(ChatColor.DARK_PURPLE),
        LEGENDARY(ChatColor.GOLD),
        ADMIN(ChatColor.DARK_RED);

        private final ChatColor color;

        Rarity(ChatColor color) {
            this.color = color;
        }

        public ChatColor getColor() {
            return color;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.hasItemMeta()) {
            if (item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Pet Menu")) {
                Player player = event.getPlayer();
                UUID playerId = player.getUniqueId();
                if (guiCooldown.contains(playerId)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Please wait a moment before opening the Pet Menu again.");
                    return;
                }

                guiCooldown.add(playerId);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    guiCooldown.remove(playerId);
                }, COOLDOWN_TIME);

                openPetGUI(player);
                event.setCancelled(true);
            }
        }
    }
}
