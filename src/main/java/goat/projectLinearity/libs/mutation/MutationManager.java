package goat.projectLinearity.libs.mutation;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.libs.effects.ParticleEngine;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Entry point for the mutation system. Handles registration and runtime mutations.
 */
public final class MutationManager implements Listener {

    private static final String MUTATION_TAG = "project_linearity_mutation";

    private final ProjectLinearity plugin;
    private final NamespacedKey mutationKey;
    private final NamespacedKey mutationIdKey;
    private final ParticleEngine particleEngine;
    private final Map<EntityType, List<MutationDefinition>> definitions = new EnumMap<>(EntityType.class);
    private final Map<String, MutationDefinition> definitionLookup = new ConcurrentHashMap<>();
    private final Map<UUID, ActiveMutation> activeMutations = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> bossBarTrustedViewers = new ConcurrentHashMap<>();
    private static final boolean DEBUG = true;
    private static final double ELITE_HEALTH_THRESHOLD = 40.0D;
    private static final double BOSSBAR_RANGE_BLOCKS = 10.0D;
    private static final double BOSSBAR_RANGE_SQUARED = BOSSBAR_RANGE_BLOCKS * BOSSBAR_RANGE_BLOCKS;
    private static final double THREE_HEADED_GHAST_BOSSBAR_RANGE_SQUARED = 200 * 200;
    private static final long BOSSBAR_UPDATE_INTERVAL_TICKS = 10L;

    public MutationManager(ProjectLinearity plugin, ParticleEngine particleEngine) {
        this.plugin = plugin;
        this.particleEngine = particleEngine;
        this.mutationKey = new NamespacedKey(plugin, "mutation_id");
        this.mutationIdKey = new NamespacedKey(plugin, "mutation_name");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        ActiveMutation mutation = activeMutations.get(event.getEntity().getUniqueId());
        if (mutation == null) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity living) {
            refreshBossBar(living, mutation);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEndermanTargetsEndermite(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof Enderman)) {
            return;
        }
        if (event.getTarget() instanceof Endermite) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, ActiveMutation> entry : activeMutations.entrySet()) {
            entry.getValue().cancelTasks();
            if (particleEngine != null) {
                particleEngine.detachAmbientEffect(entry.getKey());
            }
        }
        activeMutations.clear();
        bossBarTrustedViewers.clear();
    }

    public void registerMutation(String mutationId,
                                 EntityType entityType,
                                 int percentage,
                                 Color armorColor,
                                 String headTexture,
                                 Stats stats,
                                 String name,
                                 MutationBehavior behavior,
                                 ItemStack drop,
                                 int min,
                                 int max,
                                 int dropChancePercentage,
                                 Biome... allowedBiomes) {
        registerMutation(mutationId, entityType, percentage, armorColor, headTexture, stats, name, behavior,
                drop, min, max, dropChancePercentage, false, null, null, null,
                0, 0, 0f, 0f, allowedBiomes);
    }

    public void registerMutation(String mutationId,
                                 EntityType entityType,
                                 int percentage,
                                 Color armorColor,
                                 String headTexture,
                                 Stats stats,
                                 String name,
                                 MutationBehavior behavior,
                                 ItemStack drop,
                                 int min,
                                 int max,
                                 int dropChancePercentage,
                                 boolean ambient,
                                 Color ambientDustColor,
                                 Particle ambientParticle,
                                 Sound ambientSound,
                                 double ambientIntervalSeconds,
                                 int ambientSoundCooldownSeconds,
                                 float ambientSoundVolume,
                                 float ambientSoundPitch,
                                 Biome... allowedBiomes) {
        MutationDefinition definition = new MutationDefinition(
                mutationId,
                entityType,
                clampPercentage(percentage),
                armorColor,
                headTexture,
                stats,
                name,
                behavior,
                drop,
                min,
                max,
                dropChancePercentage,
                allowedBiomes,
                ambient,
                ambientDustColor,
                ambientParticle,
                ambientSound,
                ambientIntervalSeconds,
                ambientSoundCooldownSeconds,
                ambientSoundVolume,
                ambientSoundPitch);
        definitions.computeIfAbsent(entityType, ignored -> new ArrayList<>()).add(definition);
        definitionLookup.put(definition.id(), definition);
        if (ambient && particleEngine != null) {
            Particle particle = ambientParticle != null ? ambientParticle : Particle.DUST;
            Color dustColor = particle == Particle.DUST
                    ? (ambientDustColor != null ? ambientDustColor : Color.fromRGB(78, 168, 255))
                    : null;
            Sound sound = ambientSound != null ? ambientSound : Sound.ENTITY_ENDERMAN_AMBIENT;
            double interval = ambientIntervalSeconds > 0 ? ambientIntervalSeconds : 3.0;
            int soundCooldown = ambientSoundCooldownSeconds > 0 ? ambientSoundCooldownSeconds : 6;
            float volume = ambientSoundVolume > 0 ? ambientSoundVolume : 50f;
            float pitch = ambientSoundPitch > 0 ? ambientSoundPitch : 0.4f;
            particleEngine.registerAmbientParticles(
                    definition.id(),
                    particle,
                    dustColor,
                    interval,
                    ParticleEngine.EffectType.AMBIENT,
                    sound,
                    soundCooldown,
                    volume,
                    pitch
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        List<MutationDefinition> available = definitions.get(entity.getType());
        if (available == null || available.isEmpty()) {
            return;
        }

        if (isMutated(entity)) {
            MutationDefinition definition = resolveDefinitionForEntity(entity);
            if (definition != null) {
                debug(String.format("onCreatureSpawn: rehydrating '%s' for entity %s (%s)", definition.id(), entity.getUniqueId(), entity.getType()));
                rehydrateEntity(entity, definition);
            } else {
                debug(String.format("onCreatureSpawn: entity %s (%s) tagged but definition missing", entity.getUniqueId(), entity.getType()));
            }
            return;
        }

        for (MutationDefinition definition : available) {
            if (!definition.allowedBiomes().isEmpty()) {
                Biome biome = entity.getLocation().getBlock().getBiome();
                if (!definition.allowedBiomes().contains(biome)) {
                    continue;
                }
            }
            if (ThreadLocalRandom.current().nextInt(100) >= definition.percentage()) {
                continue;
            }
            mutateEntity(entity, definition);
            break;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        ActiveMutation mutation = resolveMutation(event.getDamager());
        if (mutation == null) {
            return;
        }
        double multiplier = mutation.damageMultiplier();
        if (multiplier > 0.0 && multiplier != 1.0) {
            event.setDamage(event.getDamage() * multiplier);
        }
        if (mutation.definition().behavior() == MutationBehavior.CHARRED
                && event.getDamager() instanceof Arrow
                && event.getEntity() instanceof Player target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0, true, true, true));
        }

        if (event.getEntity() instanceof LivingEntity victim) {
            ActiveMutation victimMutation = activeMutations.get(victim.getUniqueId());
            if (victimMutation != null && victimMutation.definition().behavior() == MutationBehavior.THREE_HEADED_GHAST) {
                if (event.getDamager() instanceof Fireball fireball) {
                    ProjectileSource shooter = fireball.getShooter();
                    if (shooter instanceof Player player) {
                        grantBossBarVision(victim.getUniqueId(), player.getUniqueId());
                    }
                    event.setDamage(Math.min(event.getDamage(), 25.0));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        ActiveMutation mutation = activeMutations.get(entity.getUniqueId());
        if (mutation == null) {
            return;
        }
        double resistancePercentage = mutation.resistancePercentage();
        if (resistancePercentage <= 0.0) {
            return;
        }
        double reduction = 1.0 - (resistancePercentage / 100.0);
        if (reduction < 0.0) {
            reduction = 0.0;
        }
        event.setDamage(event.getDamage() * reduction);
        if (entity instanceof LivingEntity living) {
            refreshBossBar(living, mutation);
            if (mutation.definition().behavior() == MutationBehavior.THREE_HEADED_GHAST) {
                living.getWorld().playSound(living.getLocation(), Sound.ENTITY_GHAST_SCREAM, 8f, 0.4f);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        ActiveMutation mutation = activeMutations.remove(event.getEntity().getUniqueId());
        if (mutation == null) {
            return;
        }
        mutation.cancelTasks();
        if (particleEngine != null) {
            particleEngine.detachAmbientEffect(event.getEntity().getUniqueId());
        }
        bossBarTrustedViewers.remove(event.getEntity().getUniqueId());

        MutationDefinition definition = mutation.definition();
        ItemStack dropItem = definition.drop();
        if (dropItem == null) {
            return;
        }

        if (definition.dropChancePercentage() <= 0) {
            return;
        }

        int dropChance = definition.dropChancePercentage();
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            ItemStack weapon = killer.getInventory().getItemInMainHand();
            int lootingLevel = weapon != null ? weapon.getEnchantmentLevel(Enchantment.LOOTING) : 0;
            if (lootingLevel > 0) {
                int bonus = Math.min(30, lootingLevel * 10);
                dropChance = Math.min(100, dropChance + bonus);
            }
        }

        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll >= dropChance) {
            return;
        }

        int amount = Math.max(definition.minDrop(), definition.maxDrop());
        if (definition.maxDrop() > definition.minDrop()) {
            amount = ThreadLocalRandom.current().nextInt(definition.maxDrop() - definition.minDrop() + 1) + definition.minDrop();
        }
        if (amount <= 0) {
            return;
        }

        dropItem.setAmount(amount);
        event.getDrops().add(dropItem);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntityMonitor(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager == null) {
            return;
        }
        LivingEntity attacker = null;
        if (damager instanceof LivingEntity living) {
            attacker = living;
        } else if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            attacker = shooter;
        }
        if (attacker == null) {
            return;
        }
        ActiveMutation mutation = activeMutations.get(attacker.getUniqueId());
        if (mutation == null || mutation.definition().behavior() != MutationBehavior.DUST_DEMON) {
            return;
        }
        if (event.getEntity() instanceof Player target) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, true, true, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!isMutated(living)) {
                continue;
            }
            MutationDefinition definition = resolveDefinitionForEntity(living);
            if (definition == null) {
                continue;
            }
            rehydrateEntity(living, definition);
            debug(String.format("EntitiesLoadEvent: rehydrated '%s' for entity %s (%s)", definition.id(), living.getUniqueId(), living.getType()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (projectile == null) {
            return;
        }
        if (!(projectile.getShooter() instanceof LivingEntity shooter)) {
            return;
        }
        ActiveMutation mutation = activeMutations.get(shooter.getUniqueId());
        if (mutation == null) {
            return;
        }
        if (mutation.definition().behavior() != MutationBehavior.SUBSONIC) {
            return;
        }
        projectile.setVelocity(projectile.getVelocity().multiply(4.0));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity source = event.getEntity();
        if (!(source instanceof Creeper creeper)) {
            return;
        }
        ActiveMutation mutation = activeMutations.get(creeper.getUniqueId());
        if (mutation == null || mutation.definition().behavior() != MutationBehavior.VOLTAIC_CREEPER) {
            return;
        }
        Location origin = event.getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        double radius = 6.0;
        world.getNearbyEntities(origin, radius, radius, radius).stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(living -> !living.getUniqueId().equals(creeper.getUniqueId()))
                .forEach(living -> world.strikeLightning(living.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Fireball)) {
            return;
        }
        if (!(projectile.getShooter() instanceof LivingEntity shooter)) {
            return;
        }
        ActiveMutation mutation = activeMutations.get(shooter.getUniqueId());
        if (mutation == null || mutation.definition().behavior() != MutationBehavior.STORMCALLER) {
            return;
        }
        Location strikeLocation;
        if (event.getHitEntity() != null) {
            strikeLocation = event.getHitEntity().getLocation();
        } else if (event.getHitBlock() != null) {
            strikeLocation = event.getHitBlock().getLocation().add(0.5, 0.5, 0.5);
        } else {
            strikeLocation = projectile.getLocation();
        }
        World world = strikeLocation.getWorld();
        if (world == null) {
            return;
        }
        world.strikeLightning(strikeLocation);
    }

    private void mutateEntity(LivingEntity entity, MutationDefinition definition) {
        markMutated(entity, definition);
        applyAppearance(entity, definition);
        applyName(entity, definition);
        ActiveMutation mutation = applyStats(entity, definition);
        startBehavior(entity, mutation);
        activeMutations.put(entity.getUniqueId(), mutation);
        updateBossBarVisionRegistry(entity.getUniqueId(), definition.behavior());
        maybeStartBossBarTracking(entity, mutation);
        if (definition.hasAmbientEffect()) {
            attachAmbient(entity, definition);
        }
        debug(String.format("mutateEntity: applied '%s' to entity %s (%s)", definition.id(), entity.getUniqueId(), entity.getType()));
    }

    public Optional<MutationDefinition> find(String mutationId) {
        if (mutationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitionLookup.get(normalizeId(mutationId)));
    }

    public Optional<LivingEntity> spawnMutation(String mutationId, Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        Optional<MutationDefinition> definitionOpt = find(mutationId);
        if (definitionOpt.isEmpty()) {
            return Optional.empty();
        }
        MutationDefinition definition = definitionOpt.get();
        Entity spawned = location.getWorld().spawnEntity(location, definition.entityType());
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            return Optional.empty();
        }
        mutateEntity(living, definition);
        return Optional.of(living);
    }

    public boolean forceApplyMutation(LivingEntity entity, String mutationId) {
        if (entity == null || mutationId == null || mutationId.isBlank()) {
            return false;
        }
        String normalized = normalizeId(mutationId);
        MutationDefinition definition = definitionLookup.get(normalized);
        if (definition == null || definition.entityType() != entity.getType()) {
            return false;
        }
        mutateEntity(entity, definition);
        return true;
    }

    private void rehydrateEntity(LivingEntity entity, MutationDefinition definition) {
        UUID uuid = entity.getUniqueId();
        ActiveMutation mutation = activeMutations.get(uuid);
        if (mutation == null || mutation.definition() != definition) {
            mutation = createActiveMutation(definition);
            activeMutations.put(uuid, mutation);
            debug(String.format("rehydrateEntity: created new ActiveMutation for '%s' entity %s (%s)", definition.id(), uuid, entity.getType()));
        }
        markMutated(entity, definition);
        startBehavior(entity, mutation);
        maybeStartBossBarTracking(entity, mutation);
        updateBossBarVisionRegistry(entity.getUniqueId(), definition.behavior());
        applyAppearance(entity, definition);
        applyName(entity, definition);
        if (definition.hasAmbientEffect()) {
            attachAmbient(entity, definition);
        }
    }

    public void rehydrateExistingMutations() {
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getLivingEntities()) {
                if (!isMutated(entity)) {
                    continue;
                }
                UUID uuid = entity.getUniqueId();
                if (activeMutations.containsKey(uuid)) {
                    continue;
                }
                MutationDefinition definition = resolveDefinitionForEntity(entity);
                if (definition == null) {
                    continue;
                }
                markMutated(entity, definition);
                ActiveMutation mutation = createActiveMutation(definition);
                activeMutations.put(uuid, mutation);
                startBehavior(entity, mutation);
                applyAppearance(entity, definition);
                applyName(entity, definition);
                updateBossBarVisionRegistry(entity.getUniqueId(), definition.behavior());
                if (definition.hasAmbientEffect()) {
                    attachAmbient(entity, definition);
                }
                debug(String.format("rehydrateExistingMutations: restored '%s' for entity %s (%s)", definition.id(), uuid, entity.getType()));
            }
        }
    }

    public List<String> mutationIds() {
        return definitionLookup.keySet().stream()
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void startBehavior(LivingEntity entity, ActiveMutation mutation) {
        MutationBehavior behavior = mutation.definition().behavior();
        if (behavior == null || behavior == MutationBehavior.NONE) {
            return;
        }
        BukkitTask task = behavior.start(plugin, entity, mutation);
        mutation.behaviorTask(task);
    }

    private ActiveMutation applyStats(LivingEntity entity, MutationDefinition definition) {
        Stats stats = definition.stats();
        stats.get(StatType.HEALTH).ifPresent(health -> {
            AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double targetHealth = Math.max(1.0, health);
            if (maxHealth != null) {
                maxHealth.setBaseValue(targetHealth);
                entity.setHealth(Math.min(targetHealth, maxHealth.getValue()));
            } else {
                entity.setHealth(targetHealth);
            }
        });

        stats.get(StatType.SPEED).ifPresent(multiplier -> {
            if (multiplier <= 0) {
                return;
            }
            AttributeInstance groundSpeed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (groundSpeed != null) {
                groundSpeed.setBaseValue(groundSpeed.getBaseValue() * multiplier);
            }
            AttributeInstance flyingSpeed = entity.getAttribute(Attribute.GENERIC_FLYING_SPEED);
            if (flyingSpeed != null) {
                flyingSpeed.setBaseValue(flyingSpeed.getBaseValue() * multiplier);
            }
        });

        return createActiveMutation(definition);
    }

    private ActiveMutation createActiveMutation(MutationDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        Stats stats = definition.stats();
        double damageMultiplier = stats.getOrDefault(StatType.DAMAGE, 1.0D);
        double resistance = stats.getOrDefault(StatType.RESISTANCE, 0.0D);
        return new ActiveMutation(definition, damageMultiplier, resistance);
    }

    private void applyAppearance(LivingEntity entity, MutationDefinition definition) {
        var equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }

        ItemStack helmet = equipment.getHelmet();
        Color color = definition.armorColor();
        String texture = definition.headTexture();

        if (texture != null && texture.startsWith("ey")) {
            ItemStack customHead = MutationHeadUtil.createHead(texture);
            if (customHead != null) {
                helmet = customHead;
            }
        }

        if (color != null) {
            helmet = ensureDyedPiece(helmet, color, EquipmentSlot.HEAD);
            equipment.setChestplate(ensureDyedPiece(equipment.getChestplate(), color, EquipmentSlot.CHEST));
            equipment.setLeggings(ensureDyedPiece(equipment.getLeggings(), color, EquipmentSlot.LEGS));
            equipment.setBoots(ensureDyedPiece(equipment.getBoots(), color, EquipmentSlot.FEET));
        }

        if (helmet != null) {
            equipment.setHelmet(helmet);
        }

        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
    }

    private void applyName(LivingEntity entity, MutationDefinition definition) {
        String customName = definition.name();
        if (customName == null || customName.isEmpty()) {
            return;
        }
        entity.setCustomName(customName);
        entity.setCustomNameVisible(true);
    }

    private ItemStack ensureDyedPiece(ItemStack current, Color color, EquipmentSlot slot) {
        ItemStack piece = current;
        if (piece == null || piece.getType() == org.bukkit.Material.AIR) {
            piece = switch (slot) {
                case HEAD -> new ItemStack(org.bukkit.Material.LEATHER_HELMET);
                case CHEST -> new ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE);
                case LEGS -> new ItemStack(org.bukkit.Material.LEATHER_LEGGINGS);
                case FEET -> new ItemStack(org.bukkit.Material.LEATHER_BOOTS);
                default -> null;
            };
        }
        if (piece == null) {
            return null;
        }
        if (piece.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            piece.setItemMeta(meta);
        }
        return piece;
    }

    private MutationDefinition resolveDefinitionForEntity(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        String storedId = getStoredMutationId(entity);
        if (storedId != null) {
            MutationDefinition definition = definitionLookup.get(storedId);
            if (definition != null) {
                return definition;
            }
        }
        String customName = entity.getCustomName();
        if (customName != null) {
            for (MutationDefinition definition : definitionLookup.values()) {
                String definedName = definition.name();
                if (definedName != null && definedName.equals(customName)) {
                    return definition;
                }
            }
        }
        return null;
    }

    private String getStoredMutationId(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (container.has(mutationIdKey, PersistentDataType.STRING)) {
            String raw = container.get(mutationIdKey, PersistentDataType.STRING);
            if (raw != null) {
                String normalized = raw.trim().toLowerCase(Locale.ENGLISH);
                if (!normalized.isEmpty()) {
                    return normalized;
                }
            }
        }
        PersistentDataContainer metaContainer = entity.getPersistentDataContainer();
        if (metaContainer.has(mutationKey, PersistentDataType.BYTE)) {
            String stripped = ChatColor.stripColor(entity.getCustomName() == null ? "" : entity.getCustomName());
            if (stripped != null && !stripped.isBlank()) {
                String normalized = stripped.trim().toLowerCase(Locale.ENGLISH).replace(' ', '_');
                if (!normalized.isEmpty() && definitionLookup.containsKey(normalized)) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private void markMutated(LivingEntity entity, MutationDefinition definition) {
        entity.addScoreboardTag(MUTATION_TAG);
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(mutationKey, PersistentDataType.BYTE, (byte) 1);
        if (definition != null) {
            container.set(mutationIdKey, PersistentDataType.STRING, definition.id());
            debug(String.format("markMutated: entity %s labelled '%s'", entity.getUniqueId(), definition.id()));
        }
    }

    private boolean isMutated(LivingEntity entity) {
        if (entity.getScoreboardTags().contains(MUTATION_TAG)) {
            return true;
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        return container.has(mutationKey, PersistentDataType.BYTE)
                || container.has(mutationIdKey, PersistentDataType.STRING);
    }

    private ActiveMutation resolveMutation(Entity damager) {
        if (damager instanceof LivingEntity living) {
            return activeMutations.get(living.getUniqueId());
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return activeMutations.get(shooter.getUniqueId());
        }
        return null;
    }

    private void attachAmbient(LivingEntity entity, MutationDefinition definition) {
        if (particleEngine == null || entity == null || definition == null || !definition.hasAmbientEffect()) {
            return;
        }
        String identifier = definition.id();
        debug(String.format("attachAmbientEffect: '%s' -> %s (%s)", identifier, entity.getUniqueId(), entity.getType()));
        particleEngine.attachAmbientEffect(entity, identifier);
    }

    private void updateBossBarVisionRegistry(UUID entityId, MutationBehavior behavior) {
        if (entityId == null) {
            return;
        }
        if (behavior == MutationBehavior.THREE_HEADED_GHAST) {
            bossBarTrustedViewers.computeIfAbsent(entityId, ignored -> ConcurrentHashMap.newKeySet());
        } else {
            bossBarTrustedViewers.remove(entityId);
        }
    }

    private void grantBossBarVision(UUID entityId, UUID playerId) {
        if (entityId == null || playerId == null) {
            return;
        }
        bossBarTrustedViewers
                .computeIfAbsent(entityId, ignored -> ConcurrentHashMap.newKeySet())
                .add(playerId);
    }

    private void maybeStartBossBarTracking(LivingEntity entity, ActiveMutation mutation) {
        if (entity == null || mutation == null) {
            return;
        }
        if (mutation.healthBossBar() != null) {
            return;
        }
        double maxHealth = resolveMaxHealth(entity);
        if (maxHealth <= ELITE_HEALTH_THRESHOLD) {
            return;
        }
        BossBar bossBar = plugin.getServer().createBossBar(
                buildBossBarTitle(entity, mutation, entity.getHealth(), maxHealth),
                BarColor.PURPLE,
                BarStyle.SEGMENTED_10
        );
        mutation.healthBossBar(bossBar);
        refreshBossBar(entity, mutation);
        BukkitTask tracker = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> refreshBossBar(entity, mutation),
                BOSSBAR_UPDATE_INTERVAL_TICKS,
                BOSSBAR_UPDATE_INTERVAL_TICKS
        );
        mutation.bossBarTask(tracker);
    }

    private void refreshBossBar(LivingEntity entity, ActiveMutation mutation) {
        BossBar bossBar = mutation.healthBossBar();
        if (bossBar == null) {
            return;
        }
        if (entity == null || entity.isDead() || !entity.isValid()) {
            mutation.cancelBossBar();
            return;
        }
        double maxHealth = Math.max(1.0D, resolveMaxHealth(entity));
        double currentHealth = Math.max(0.0D, Math.min(maxHealth, entity.getHealth()));
        double progress = Math.max(0.0D, Math.min(1.0D, currentHealth / maxHealth));
        bossBar.setProgress(progress);
        bossBar.setTitle(buildBossBarTitle(entity, mutation, currentHealth, maxHealth));
        syncBossBarViewers(entity, bossBar, mutation);
    }

    private String buildBossBarTitle(LivingEntity entity, ActiveMutation mutation, double health, double maxHealth) {
        String displayName = mutation.definition().name();
        if (displayName == null || displayName.isBlank()) {
            displayName = formatEntityTypeName(entity);
        }
        int current = (int) Math.ceil(health);
        int max = (int) Math.ceil(maxHealth);
        return ChatColor.DARK_PURPLE + displayName
                + ChatColor.GRAY + " [" + ChatColor.RED + current
                + ChatColor.GRAY + "/" + ChatColor.WHITE + max + ChatColor.GRAY + "]";
    }

    private String formatEntityTypeName(LivingEntity entity) {
        if (entity == null) {
            return "Mutant";
        }
        String typeName = entity.getType().name().toLowerCase(Locale.ENGLISH).replace('_', ' ');
        return capitalizeWords(typeName);
    }

    private String capitalizeWords(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String[] parts = input.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ENGLISH));
            }
            builder.append(' ');
        }
        return builder.toString().trim();
    }

    private void syncBossBarViewers(LivingEntity entity, BossBar bossBar, ActiveMutation mutation) {
        if (entity == null || bossBar == null || entity.getWorld() == null) {
            return;
        }
        Location origin = entity.getLocation();
        List<Player> worldPlayers = entity.getWorld().getPlayers();
        List<Player> currentViewers = new ArrayList<>(bossBar.getPlayers());
        Set<UUID> viewerIds = new HashSet<>();
        boolean isThreeHeaded = mutation != null && mutation.definition().behavior() == MutationBehavior.THREE_HEADED_GHAST;
        double currentHealth = entity.getHealth();
        double maxHealth = resolveMaxHealth(entity);
        double healthThreshold = maxHealth <= 0 ? 0 : maxHealth - 50.0; // below 450 (500 max) => max-50
        boolean broadcastMode = isThreeHeaded && currentHealth <= healthThreshold;
        Set<UUID> trustedViewers = isThreeHeaded ? bossBarTrustedViewers.get(entity.getUniqueId()) : null;
        for (Player viewer : currentViewers) {
            UUID viewerId = viewer.getUniqueId();
            viewerIds.add(viewerId);
            double allowedRange = BOSSBAR_RANGE_SQUARED;
            if (isThreeHeaded) {
                if (broadcastMode) {
                    allowedRange = THREE_HEADED_GHAST_BOSSBAR_RANGE_SQUARED;
                } else if (trustedViewers != null && trustedViewers.contains(viewerId)) {
                    allowedRange = THREE_HEADED_GHAST_BOSSBAR_RANGE_SQUARED;
                }
            }
            boolean remove = !viewer.isOnline()
                    || viewer.isDead()
                    || viewer.getWorld() != entity.getWorld()
                    || viewer.getLocation().distanceSquared(origin) > allowedRange;
            if (remove) {
                bossBar.removePlayer(viewer);
                viewerIds.remove(viewerId);
            }
        }
        for (Player player : worldPlayers) {
            if (!player.isOnline() || player.isDead()) {
                continue;
            }
            double allowedRange = BOSSBAR_RANGE_SQUARED;
            if (isThreeHeaded) {
                if (broadcastMode) {
                    allowedRange = THREE_HEADED_GHAST_BOSSBAR_RANGE_SQUARED;
                } else if (trustedViewers != null && trustedViewers.contains(player.getUniqueId())) {
                    allowedRange = THREE_HEADED_GHAST_BOSSBAR_RANGE_SQUARED;
                }
            }
            if (player.getLocation().distanceSquared(origin) <= allowedRange
                    && viewerIds.add(player.getUniqueId())) {
                bossBar.addPlayer(player);
            }
        }
    }

    private double resolveMaxHealth(LivingEntity entity) {
        if (entity == null) {
            return 0.0D;
        }
        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            return attribute.getValue();
        }
        return entity.getHealth();
    }

    private int clampPercentage(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(100, value);
    }

    private String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ENGLISH).replace(' ', '_');
    }

    private void debug(String message) {
        if (!DEBUG || message == null || message.isBlank()) {
            return;
        }
        plugin.getLogger().info("[MutationManager] " + message);
    }
}
