package goat.projectLinearity.subsystems.fishing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.util.CustomEntityRegistry;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Coordinates lifecycle for custom sea creature Citizens NPCs.
 *
 * <p>Definitions registered here are also exposed through {@code /spawncustommob}
 * for quick developer testing.</p>
 */
public final class SeaCreatureRegistry implements Listener {

    private final ProjectLinearity plugin;
    private final CustomEntityRegistry customEntityRegistry;
    private final Map<String, SeaCreatureDefinition> definitions = new LinkedHashMap<>();
    private final Map<UUID, SeaCreatureEntity> activeCreatures = new ConcurrentHashMap<>();
    private final Map<String, List<DropDefinition>> dropTables = new ConcurrentHashMap<>();
    private NPCRegistry npcRegistry;
    private boolean registeredListener;

    public SeaCreatureRegistry(ProjectLinearity plugin, CustomEntityRegistry customEntityRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.customEntityRegistry = customEntityRegistry;
        registerListener();
    }

    public void register(SeaCreatureDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String key = normalise(definition.id());
        if (definitions.put(key, definition) != null) {
            plugin.getLogger().warning("Overwriting existing sea creature definition for id '" + definition.id() + "'.");
        }
        dropTables.put(key, new ArrayList<>(definition.drops()));
        if (customEntityRegistry != null) {
            List<String> aliases = buildAliases(definition);
            CustomEntityRegistry.CustomEntityEntry entry = new CustomEntityRegistry.CustomEntityEntry(
                    definition.id(),
                    definition.displayName(),
                    "Spawns the " + definition.displayName() + " sea creature.",
                    aliases,
                    (pl, location, sender) -> spawnForCommand(definition, location, sender)
            );
            customEntityRegistry.register(entry);
        }
    }

    public void shutdown() {
        for (SeaCreatureEntity entity : activeCreatures.values()) {
            entity.destroy();
        }
        activeCreatures.clear();
        if (npcRegistry != null) {
            npcRegistry.deregisterAll();
            npcRegistry = null;
        }
        if (registeredListener) {
            HandlerList.unregisterAll(this);
            registeredListener = false;
        }
    }

    void registerActive(SeaCreatureEntity entity) {
        activeCreatures.put(entity.getNpcId(), entity);
    }

    void unregisterActive(UUID npcId) {
        if (npcId == null) {
            return;
        }
        activeCreatures.remove(npcId);
    }

    Optional<SeaCreatureDefinition> findDefinition(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(normalise(id)));
    }

    Optional<NPCRegistry> resolveNpcRegistry() {
        if (npcRegistry != null) {
            return Optional.of(npcRegistry);
        }
        if (!CitizensAPI.hasImplementation()) {
            return Optional.empty();
        }
        npcRegistry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
        return Optional.of(npcRegistry);
    }

    private CustomEntityRegistry.SpawnResult spawnForCommand(SeaCreatureDefinition definition, Location location, CommandSender sender) {
        if (location == null || location.getWorld() == null) {
            return CustomEntityRegistry.SpawnResult.failure("Cannot spawn sea creature at an invalid location.");
        }
        Player caster = resolveCaster(location, sender);
        Optional<SeaCreatureEntity> entityOpt = spawnSeaCreature(definition, location, caster);
        if (entityOpt.isEmpty()) {
            return CustomEntityRegistry.SpawnResult.failure("Failed to spawn sea creature. Check console for details.");
        }
        Location spawnLocation = entityOpt.get().getSpawnLocation();
        String worldName = spawnLocation.getWorld() != null ? spawnLocation.getWorld().getName() : "unknown";
        String message = String.format(
                "Spawned %s at %s x=%.1f y=%.1f z=%.1f",
                definition.displayName(),
                worldName,
                spawnLocation.getX(),
                spawnLocation.getY(),
                spawnLocation.getZ());
        return CustomEntityRegistry.SpawnResult.success(message);
    }

    private Player resolveCaster(Location location, CommandSender sender) {
        if (sender instanceof Player player && player.isOnline() && player.getWorld().equals(location.getWorld())) {
            return player;
        }
        return findNearestPlayer(location);
    }

    private Player findNearestPlayer(Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        double bestDistance = Double.MAX_VALUE;
        Player bestPlayer = null;
        for (Player player : location.getWorld().getPlayers()) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(location);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestPlayer = player;
            }
        }
        return bestPlayer;
    }

    private void registerListener() {
        if (registeredListener) {
            return;
        }
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(this, plugin);
        registeredListener = true;
    }

    private static String normalise(String key) {
        return key.toLowerCase(Locale.ROOT);
    }

    private static List<String> buildAliases(SeaCreatureDefinition definition) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(normalise(definition.id()));
        String noUnderscore = normalise(definition.id()).replace("_", "");
        if (!noUnderscore.isBlank()) {
            aliases.add(noUnderscore);
        }
        String dashed = normalise(definition.id()).replace('_', '-');
        if (!dashed.isBlank()) {
            aliases.add(dashed);
        }
        String display = normalise(definition.displayName()).replace(' ', '_');
        if (!display.isBlank()) {
            aliases.add(display);
        }
        aliases.removeIf(String::isBlank);
        return List.copyOf(aliases);
    }

    @EventHandler
    public void onNpcDespawn(NPCDespawnEvent event) {
        SeaCreatureEntity entity = activeCreatures.remove(event.getNPC().getUniqueId());
        if (entity != null) {
            entity.destroy();
        }
    }

    @EventHandler
    public void onNpcDeath(NPCDeathEvent event) {
        SeaCreatureEntity entity = activeCreatures.remove(event.getNPC().getUniqueId());
        if (entity != null) {
            handleDrops(entity, event.getEvent());
            entity.destroy();
        }
    }

    public Optional<SeaCreatureDefinition> randomDefinition() {
        Collection<SeaCreatureDefinition> values = definitions.values();
        if (values.isEmpty()) {
            return Optional.empty();
        }
        int index = ThreadLocalRandom.current().nextInt(values.size());
        Iterator<SeaCreatureDefinition> iterator = values.iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        return Optional.of(iterator.next());
    }

    public Optional<SeaCreatureEntity> spawnSeaCreature(SeaCreatureDefinition definition, Location location, Player caster) {
        Objects.requireNonNull(definition, "definition");
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        Optional<NPCRegistry> registryOpt = resolveNpcRegistry();
        if (registryOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<SeaCreatureEntity> entityOpt = SeaCreatureEntity.spawn(plugin, this, registryOpt.get(), definition, location, caster);
        entityOpt.ifPresent(this::registerActive);
        return entityOpt;
    }

    public Optional<SeaCreatureEntity> spawnSeaCreature(String id, Location location, Player caster) {
        return findDefinition(id).flatMap(def -> spawnSeaCreature(def, location, caster));
    }

    public boolean registerLoot(String creatureId, String materialName, int minAmount, int maxAmount, int numerator, int denominator) {
        if (creatureId == null || materialName == null) {
            return false;
        }
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            plugin.getLogger().warning("Unknown material '" + materialName + "' for sea creature loot '" + creatureId + "'.");
            return false;
        }
        return registerLoot(creatureId, () -> new ItemStack(material), minAmount, maxAmount, numerator, denominator);
    }

    public boolean registerLoot(String creatureId, Supplier<ItemStack> itemFactory, int minAmount, int maxAmount, int numerator, int denominator) {
        if (creatureId == null || itemFactory == null) {
            return false;
        }
        if (findDefinition(creatureId).isEmpty()) {
            plugin.getLogger().warning("Cannot register loot for unknown sea creature id '" + creatureId + "'.");
            return false;
        }
        String key = normalise(creatureId);
        DropDefinition dropDefinition = new DropDefinition(itemFactory, minAmount, maxAmount, numerator, denominator);
        dropTables.computeIfAbsent(key, ignored -> new ArrayList<>()).add(dropDefinition);
        return true;
    }

    private void handleDrops(SeaCreatureEntity entity, org.bukkit.event.entity.EntityDeathEvent deathEvent) {
        if (deathEvent == null) {
            return;
        }
        String key = normalise(entity.getDefinition().id());
        List<DropDefinition> drops = dropTables.get(key);
        if (drops == null || drops.isEmpty()) {
            return;
        }
        java.util.List<ItemStack> eventDrops = deathEvent.getDrops();
        Location location = deathEvent.getEntity().getLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (DropDefinition drop : drops) {
            if (!drop.rollsSuccessful(rng)) {
                continue;
            }
            ItemStack stack = drop.createStack(rng);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (eventDrops != null) {
                eventDrops.add(stack);
            } else if (location.getWorld() != null) {
                location.getWorld().dropItemNaturally(location, stack);
            }
        }
    }

    public enum BehaviorTrait {
        DIVER
    }

    public record SeaCreatureDefinition(
            String id,
            String displayName,
            String skinValue,
            String skinSignature,
            BehaviorTrait behaviorTrait,
            double maxHealth,
            double baseDamage,
            List<DropDefinition> drops
    ) {
        public SeaCreatureDefinition {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(skinValue, "skinValue");
            Objects.requireNonNull(skinSignature, "skinSignature");
            Objects.requireNonNull(behaviorTrait, "behaviorTrait");
            Objects.requireNonNull(drops, "drops");
            maxHealth = Math.max(1.0, maxHealth);
            drops = List.copyOf(drops);
        }
    }

    public record DropDefinition(
            Supplier<ItemStack> itemFactory,
            int minAmount,
            int maxAmount,
            int chanceNumerator,
            int chanceDenominator
    ) {
        public DropDefinition {
            Objects.requireNonNull(itemFactory, "itemFactory");
            minAmount = Math.max(0, minAmount);
            maxAmount = Math.max(minAmount, maxAmount);
            chanceNumerator = Math.max(0, chanceNumerator);
            chanceDenominator = Math.max(1, chanceDenominator);
        }

        boolean rollsSuccessful(ThreadLocalRandom rng) {
            if (chanceNumerator >= chanceDenominator) {
                return true;
            }
            if (chanceNumerator <= 0) {
                return false;
            }
            return rng.nextInt(chanceDenominator) < chanceNumerator;
        }

        ItemStack createStack(ThreadLocalRandom rng) {
            ItemStack base = itemFactory.get();
            if (base == null || base.getType().isAir()) {
                return null;
            }
            int amount = minAmount;
            if (maxAmount > minAmount) {
                amount += rng.nextInt(maxAmount - minAmount + 1);
            }
            if (amount <= 0) {
                return null;
            }
            ItemStack stack = base.clone();
            int maxStack = stack.getMaxStackSize();
            stack.setAmount(Math.min(amount, maxStack));
            return stack;
        }
    }
}
