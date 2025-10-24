package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.other.additionalfunctionality.BlessingUtils;
import goat.minecraft.minecraftnew.other.beacon.Catalyst;
import goat.minecraft.minecraftnew.other.beacon.CatalystManager;
import goat.minecraft.minecraftnew.other.beacon.CatalystType;
import goat.minecraft.minecraftnew.other.skilltree.Skill;
import goat.minecraft.minecraftnew.subsystems.brewing.PotionManager;
import goat.minecraft.minecraftnew.subsystems.brewing.PotionEffectPreferences;
import goat.minecraft.minecraftnew.subsystems.combat.HostilityManager;
import goat.minecraft.minecraftnew.other.enchanting.CustomEnchantmentManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetManager;
import goat.minecraft.minecraftnew.subsystems.pets.PetTrait;
import goat.minecraft.minecraftnew.utils.devtools.ItemRegistry;
import goat.minecraft.minecraftnew.subsystems.combat.SpawnMonsters;
import goat.minecraft.minecraftnew.utils.devtools.PlayerMeritManager;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import goat.minecraft.minecraftnew.other.skilltree.SkillTreeManager;
import goat.minecraft.minecraftnew.other.skilltree.Talent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

import static goat.minecraft.minecraftnew.subsystems.fishing.SeaCreature.createDyedLeatherArmor;



public class FishingEvent implements Listener {
    private final MinecraftNew plugin = MinecraftNew.getInstance();
    private final XPManager xpManager = new XPManager(plugin);
    private final Random random = new Random();
    private static final String SEA_CREATURE_METADATA = "SEA_CREATURE";
    private static final double SEA_CREATURE_REFORGE_BONUS = 4.0; // 4% bonus if reforged for sea creatures
    @EventHandler(priority = EventPriority.HIGHEST)

    public void onPlayerFish(PlayerFishEvent e) {
        //Bukkit.broadcastMessage("Luck chance = " + (0.05 + ((double) getLuckOfTheSeaLevel(e.getPlayer().getItemInUse()) /100)));
        if (e.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            Entity caught = e.getCaught();
            if (caught != null && caught.hasMetadata(SEA_CREATURE_METADATA)) {
                handleSeaCreatureCatch(caught, e.getPlayer());
            }
        } else if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            handleRegularFishCatch(e);
        }
    }

    /**
     * Handles the logic when a player catches a sea creature.
     *
     * @param caught The caught sea creature entity.
     * @param player The player who caught the sea creature.
     */
    private void handleSeaCreatureCatch(Entity caught, Player player) {
        String creatureName = caught.getMetadata(SEA_CREATURE_METADATA).get(0).asString();
        Optional<SeaCreature> optionalSeaCreature = SeaCreatureRegistry.getSeaCreatureByName(creatureName);
        if (!optionalSeaCreature.isPresent()) return;

        SeaCreature seaCreature = optionalSeaCreature.get();
        int level = xpManager.getPlayerLevel(player, "Fishing"); // Get player's fishing level

        // Award base XP
        xpManager.addXP(player, "Fishing", 20);

        // Award boosted XP based on sea creature rarity
        int boostedXP = getBoostedXP(seaCreature.getRarity());
        xpManager.addXP(player, "Fishing", boostedXP);

        player.sendMessage(ChatColor.AQUA + "You caught a " + seaCreature.getColoredDisplayName() + " (" + seaCreature.getRarity().name() + ")!");
        player.sendMessage(ChatColor.GREEN + "Bonus Fishing XP: " + boostedXP);
        player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);

        ItemStack rod = player.getInventory().getItemInMainHand();
        int diamondLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.DIAMOND_HOOK);
        if (diamondLevel > 0 && caught instanceof LivingEntity living) {
            // Kill the sea creature while crediting the player as the killer so
            // that normal loot logic still runs in the death event handler.
            living.damage(living.getHealth() * 2, player);
        } else {
            caught.remove();
        }

        applyReelInUpgrades(player, true, rod);

        // Grant Luck effect
        grantLuck(player, "Fishing");
    }
    private void handleRegularFishCatch(PlayerFishEvent e) {
        Player player = e.getPlayer();
        int fishingLevel = xpManager.getPlayerLevel(player, "Fishing"); // Get player's fishing level
        double seaCreatureChance = 0;
        

        // Talent: Angler's Instinct grants additional sea creature chance
        int scc1 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_I);
        int scc2 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_II);
        int scc3 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_III);
        int scc4 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_IV);
        int scc5 = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SEA_CREATURE_CHANCE_V);
        seaCreatureChance += scc1 * 0.5;
        seaCreatureChance += scc2 * 1.0;
        seaCreatureChance += scc3 * 1.5;
        seaCreatureChance += scc4 * 2.0;
        seaCreatureChance += scc5 * 2.5;

        // Add "Call of the Void" enchantment bonus
        int callOfTheVoidLevel = CustomEnchantmentManager.getEnchantmentLevel(player.getInventory().getItemInMainHand(), "Call of the Void");
        seaCreatureChance += callOfTheVoidLevel;

        if(PotionManager.isActive("Potion of Fountains", player)
                && PotionEffectPreferences.isEnabled(player, "Potion of Fountains")){
            seaCreatureChance += 10;
        }
        if(SkillTreeManager.getInstance().hasTalent(player, Talent.FOUNTAIN_MASTERY)){
            seaCreatureChance += 5;
        }

        CatalystManager catalystManager = CatalystManager.getInstance();
        if (catalystManager != null && catalystManager.isNearCatalyst(player.getLocation(), CatalystType.DEPTH)) {
            // Player is near a Depth catalyst - apply additional chance based on tier
            Catalyst nearestDepthCatalyst = catalystManager.findNearestCatalyst(player.getLocation(), CatalystType.DEPTH);
            if (nearestDepthCatalyst != null) {
                int catalystTier = catalystManager.getCatalystTier(nearestDepthCatalyst);
                final double BASE_SEA_CREATURE_CHANCE_INCREASE = 0.05; // 5% base increase
                final double PER_TIER_SEA_CREATURE_INCREASE = 0.01;    // 1% per tier
                // Calculate bonus: 5% + (tier * 1%)
                double seaCreatureBonus = BASE_SEA_CREATURE_CHANCE_INCREASE + (catalystTier * PER_TIER_SEA_CREATURE_INCREASE);
                seaCreatureChance += seaCreatureBonus;
            }

        }
        // Check for reforged items for sea creatures
        if (isReforgedForSeaCreatures(player.getInventory().getItemInMainHand())) {
            seaCreatureChance += 5; // +4% if the item is reforged
        }
        PlayerMeritManager playerMeritManager = PlayerMeritManager.getInstance(plugin);
        if(BlessingUtils.hasFullSetBonus(player, "Fathmic Iron")){
            seaCreatureChance -= 20;
        }

        ItemStack rod = player.getInventory().getItemInMainHand();
        int sonarLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.SONAR);
        seaCreatureChance += sonarLevel;
        // Check active pet perks
        PetManager petManager = PetManager.getInstance(plugin);
        PetManager.Pet activePet = petManager.getActivePet(player);
        if (activePet != null) {

            int anglerTalent = 0;
            if (SkillTreeManager.getInstance() != null) {
                anglerTalent = SkillTreeManager.getInstance().getTalentLevel(player.getUniqueId(), Skill.TAMING, Talent.ANGLER);
            }

            if (activePet.hasPerk(PetManager.PetPerk.ANGLER) || anglerTalent > 0) {
                int anglerBonus = 5; // base bonus
                anglerBonus *= (1 + anglerTalent * 0.5); // talent increases bonus by 50% per level
                seaCreatureChance += anglerBonus;
            }

            if (activePet.hasPerk(PetManager.PetPerk.HEART_OF_THE_SEA)) {
                int heartOfTheSeaBonus = 10; // Scales up to +10% at level 100
                seaCreatureChance += heartOfTheSeaBonus;
            }

            if (activePet.hasPerk(PetManager.PetPerk.BUDDY_SYSTEM)) {
                for (Player other : player.getWorld().getPlayers()) {
                    if (!other.equals(player) && other.getLocation().distance(player.getLocation()) <= 20) {
                        seaCreatureChance += 5;
                        break;
                    }
                }
            }

            if (activePet.hasPerk(PetManager.PetPerk.BAIT)) {
                seaCreatureChance += (double) activePet.getLevel() / 10;; // +10% from Heart of the Sea perk
            }
            if(activePet.getTrait().equals(PetTrait.NAUTICAL)){
                seaCreatureChance += (activePet.getTrait().getValueForRarity(activePet.getTraitRarity()));
            }
        }

        seaCreatureChance += SeaCreatureDeathEvent.getMawBonus(player);

        // Convert to a decimal for probability
        seaCreatureChance /= 100.0;

        // Award base XP
        xpManager.addXP(player, "Fishing", 20);

        // Determine if a sea creature should replace this catch
        if (random.nextDouble() <= seaCreatureChance) {
            spawnAndLaunchSeaCreature(player, e.getHook().getLocation(), rod);
            applyReelInUpgrades(player, true, rod);
        } else {
            // Proceed with regular fish catch
            awardRegularFish(player, fishingLevel);
            player.sendMessage(ChatColor.DARK_AQUA + "Sea Creature Chance: " + Math.round(seaCreatureChance * 100) + "%");
            applyReelInUpgrades(player, false, rod);
        }

        // Implement the enhanced treasure system
        rollForTreasure(player);

        // Grant Luck effect based on fishing level
        grantLuck(player, "Fishing");
    }

    private boolean isReforgedForSeaCreatures(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            for (String lore : meta.getLore()) {
                if (ChatColor.stripColor(lore).equals("Talisman: Sea Creature Chance")) {
                    //Bukkit.broadcastMessage("WORKS");
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Awards a regular fish to the player and handles double fish logic.
     *
     * @param player The player who caught the fish.
     * @param level  The player's fishing level.
     */
    private void awardRegularFish(Player player, int level) {
        // Chance to receive double fish based on player's fishing level
        int rollDoubleFish = random.nextInt(100) + 1;
        if (rollDoubleFish <= level) { // Higher fishing level increases chance
            Material fishType = random.nextBoolean() ? Material.COD : Material.SALMON;
            ItemStack fish = new ItemStack(fishType); // Randomly choose between COD and SALMON
            player.getInventory().addItem(fish);
            player.sendMessage(ChatColor.AQUA + "You caught an extra fish!");
            player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);
        }

        SkillTreeManager mgr = SkillTreeManager.getInstance();
        if (mgr != null) {
            int sponge = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.MONUMENTAL);
            if (sponge > 0 && random.nextDouble() < sponge * 0.01) {
                player.getInventory().addItem(new ItemStack(Material.SPONGE));
                player.sendMessage(ChatColor.GOLD + "You fished up a Sponge!");
            }

            double gfChance = 0.0;
            gfChance += mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SNACK_THAT_SMILES_BACK_I) * 0.25;
            gfChance += mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SNACK_THAT_SMILES_BACK_II) * 0.25;
            gfChance += mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SNACK_THAT_SMILES_BACK_III) * 0.25;
            gfChance += mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SNACK_THAT_SMILES_BACK_IV) * 0.25;
            gfChance += mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.SNACK_THAT_SMILES_BACK_V) * 0.25;
            if (random.nextDouble() < gfChance / 100.0) {
                player.getInventory().addItem(ItemRegistry.getGoldenFish());
                player.sendMessage(ChatColor.GOLD + "You caught a Golden Fish!");
            }

            int library = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.LOST_LIBRARY);
            if (library > 0 && random.nextDouble() < library * 0.01) {
                ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                org.bukkit.inventory.meta.EnchantmentStorageMeta bm = (org.bukkit.inventory.meta.EnchantmentStorageMeta) book.getItemMeta();
                Enchantment[] values = Enchantment.values();
                Enchantment ench = values[random.nextInt(values.length)];
                bm.addStoredEnchant(ench, 1, true);
                book.setItemMeta(bm);
                player.getInventory().addItem(book);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "You found a mysterious book!");
            }
        }
    }
    private int getAdjustedSeaCreatureLevel(int baseLevel, int hostilityLevel) {
        double multiplier = 1.0 + (hostilityLevel / 10.0);
        return (int) Math.round(baseLevel * multiplier);
    }
    /**
     * Spawns a sea creature and launches it towards the player.
     *
     * @param player         The player towards whom the sea creature will be launched.
     * @param bobberLocation The location of the fishing bobber.
     */
    private void spawnAndLaunchSeaCreature(Player player, Location bobberLocation, ItemStack rod) {
        PetManager petManager = PetManager.getInstance(plugin);
        Optional<SeaCreature> optionalSeaCreature = SeaCreatureRegistry.getRandomSeaCreature();
        Optional<SeaCreature> optionalRareSeaCreature = SeaCreatureRegistry.getRandomRareSeaCreature();
        SeaCreature seaCreature;
        if (!optionalSeaCreature.isPresent()) return;

        if (BlessingUtils.hasFullSetBonus(player, "Fathmic Iron")) {
            seaCreature = optionalRareSeaCreature.get();
        } else {
            seaCreature = optionalSeaCreature.get();
        }
        EntityType entityType = seaCreature.getEntityType();


        // Log sea creature stats to the console
        Bukkit.getLogger().info("Sea Creature Stats:");
        Bukkit.getLogger().info("Name: " + seaCreature.getDisplayName());
        Bukkit.getLogger().info("Rarity: " + seaCreature.getRarity());
        Bukkit.getLogger().info("Level: " + seaCreature.getLevel());

        // Spawn the sea creature at the bobber's location
        Entity spawnedEntity = player.getWorld().spawnEntity(bobberLocation, entityType);
        LivingEntity livingEntity = (LivingEntity) spawnedEntity;
        // Retrieve the creature's custom base64 texture
        String skullName = seaCreature.getSkullName();
        ItemStack helmet = petManager.getSkullForPet(seaCreature.getSkullName());
        ItemStack chest = createDyedLeatherArmor(Material.LEATHER_CHESTPLATE, seaCreature.getArmorColor());
        ItemStack legs = createDyedLeatherArmor(Material.LEATHER_LEGGINGS, seaCreature.getArmorColor());
        ItemStack boots = createDyedLeatherArmor(Material.LEATHER_BOOTS, seaCreature.getArmorColor());

        livingEntity.getEquipment().setHelmet(helmet);
        livingEntity.getEquipment().setChestplate(chest);
        livingEntity.getEquipment().setLeggings(legs);
        livingEntity.getEquipment().setBoots(boots);

        livingEntity.getEquipment().setHelmetDropChance(0);
        livingEntity.getEquipment().setChestplateDropChance(0);
        livingEntity.getEquipment().setLeggingsDropChance(0);
        livingEntity.getEquipment().setBootsDropChance(0);
        ((LivingEntity) spawnedEntity).getEquipment().setItemInOffHandDropChance(1);
        if (seaCreature.getSkullName().equals("Pirate")) {
            ((LivingEntity) spawnedEntity).getEquipment().setItemInOffHand(ItemRegistry.getRandomTreasure());
            ((LivingEntity) spawnedEntity).getEquipment().setItemInOffHandDropChance(1);
        }
        if (seaCreature.getSkullName().equals("Poseidon")) {
            ((LivingEntity) spawnedEntity).getEquipment().setItemInOffHand(ItemRegistry.getTrident());

        }
        livingEntity.setSwimming(true);
        livingEntity.setSilent(true);

        HostilityManager hostilityManager = HostilityManager.getInstance(plugin);
        int hostilityLevel = hostilityManager.getPlayerDifficultyTier(player);
        plugin.getLogger().info("Hostility Level for player " + player.getName() + ": " + hostilityLevel);

        SpawnMonsters spawnMonsters = SpawnMonsters.getInstance(xpManager);
        int baseLevel = seaCreature.getLevel();
        int biggerLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.BIGGER_FISH);
        int adjustedLevel = (int) Math.max(1, Math.round(baseLevel * (1.0 - biggerLevel * 0.10)));
        plugin.getLogger().info("Base Level of Sea Creature: " + baseLevel);

        spawnMonsters.applyMobAttributes(livingEntity, adjustedLevel);

        spawnedEntity.setCustomName(ChatColor.AQUA + "[Lvl " + adjustedLevel + "] " + seaCreature.getColoredDisplayName());
        spawnedEntity.setCustomNameVisible(true);
        // Attach metadata with the sea creature's name
        spawnedEntity.setMetadata("SEA_CREATURE", new FixedMetadataValue(MinecraftNew.getInstance(), seaCreature.getDisplayName()));
        if (spawnedEntity.hasMetadata("SEA_CREATURE")) {
            Bukkit.getLogger().info("Metadata successfully applied to entity: " + spawnedEntity.getName());
        } else {
            Bukkit.getLogger().warning("Metadata application failed for entity: " + spawnedEntity.getName());
        }

        // Calculate the direction vector from sea creature to player
        Vector direction = player.getLocation().subtract(bobberLocation).toVector().normalize();

        // Base vertical force used for most creatures
        double verticalBoost = 0.2;

        if (spawnedEntity.getType() == EntityType.SQUID || spawnedEntity.getType() == EntityType.GLOW_SQUID) {
            // Squids teleport above the player instead of being launched
            Location abovePlayer = player.getLocation().clone().add(0, 2, 0);
            spawnedEntity.teleport(abovePlayer);
        } else {
            // If the player is significantly above the water, give the creature extra lift
            double heightDifference = player.getLocation().getY() - bobberLocation.getY();
            if (heightDifference >= 4) {
                verticalBoost += heightDifference * 0.1; // scale boost with difference
            }

            // Apply vertical boost
            direction.setY(verticalBoost);

            // Multiply the vector to control overall speed towards the player
            direction.multiply(2); // Adjust speed multiplier as needed

            // Set the sea creature's velocity towards the player, including upward motion
            spawnedEntity.setVelocity(direction);
        }

        player.sendMessage("A " + seaCreature.getColoredDisplayName() + " is approaching!");
        playSplashSound(player, seaCreature.getRarity());

        // Play particles at the bobber location
        bobberLocation.getWorld().spawnParticle(Particle.FISHING, bobberLocation, 30, 0.5, 0.5, 0.5, 0.1);
        bobberLocation.getWorld().playSound(bobberLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1.0f, 1.0f);

        ((LivingEntity) spawnedEntity).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false));

        int krakenLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.KRAKEN);
        if (krakenLevel > 0 && random.nextDouble() < 0.05 * krakenLevel) {
            spawnSpecificSeaCreature(player, bobberLocation, seaCreature, adjustedLevel, rod);
            player.sendMessage(ChatColor.DARK_PURPLE + "Kraken awakens! Another sea creature appears.");
        }
        if (seaCreature instanceof Monster monster) {
            monster.setTarget(player);
        }
    }
    private void spawnSpecificSeaCreature(Player player, Location bobberLocation, SeaCreature seaCreature, int level, ItemStack rod) {
        Entity spawned = player.getWorld().spawnEntity(bobberLocation, seaCreature.getEntityType());
        LivingEntity living = (LivingEntity) spawned;
        PetManager petManager = PetManager.getInstance(plugin);

        ItemStack helmet = petManager.getSkullForPet(seaCreature.getSkullName());
        ItemStack chest = createDyedLeatherArmor(Material.LEATHER_CHESTPLATE, seaCreature.getArmorColor());
        ItemStack legs = createDyedLeatherArmor(Material.LEATHER_LEGGINGS, seaCreature.getArmorColor());
        ItemStack boots = createDyedLeatherArmor(Material.LEATHER_BOOTS, seaCreature.getArmorColor());

        living.getEquipment().setHelmet(helmet);
        living.getEquipment().setChestplate(chest);
        living.getEquipment().setLeggings(legs);
        living.getEquipment().setBoots(boots);
        living.getEquipment().setHelmetDropChance(0);
        living.getEquipment().setChestplateDropChance(0);
        living.getEquipment().setLeggingsDropChance(0);
        living.getEquipment().setBootsDropChance(0);

        SpawnMonsters spawnMonsters = SpawnMonsters.getInstance(xpManager);
        spawnMonsters.applyMobAttributes(living, level);
        spawned.setCustomName(ChatColor.AQUA + "[Lvl " + level + "] " + seaCreature.getColoredDisplayName());
        spawned.setCustomNameVisible(true);
        spawned.setMetadata("SEA_CREATURE", new FixedMetadataValue(MinecraftNew.getInstance(), seaCreature.getDisplayName()));

        // Launch the sea creature towards the player using the same logic as
        // the initial spawn to keep behavior consistent.
        Vector direction = player.getLocation().subtract(bobberLocation).toVector().normalize();

        // Base vertical force for non-squid creatures
        double verticalBoost = 0.2;

        if (spawned.getType() == EntityType.SQUID || spawned.getType() == EntityType.GLOW_SQUID) {
            // Squids teleport directly above the player
            Location abovePlayer = player.getLocation().clone().add(0, 2, 0);
            spawned.teleport(abovePlayer);
        } else {
            // Extra lift if the player is well above the water
            double heightDifference = player.getLocation().getY() - bobberLocation.getY();
            if (heightDifference >= 4) {
                verticalBoost += heightDifference * 0.1;
            }

            // Apply vertical boost and speed multiplier
            direction.setY(verticalBoost);
            direction.multiply(2);
            spawned.setVelocity(direction);
        }
        ((LivingEntity) spawned).addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, false));
    }


    private void playSplashSound(Player player, Rarity rarity) {
        // Adjusted to include more sounds based on rarity
        Sound sound;
        float volume = 1.0f;
        float pitch = 1.0f;

        switch (rarity) {
            case COMMON, UNCOMMON:
                sound = Sound.ENTITY_DOLPHIN_SPLASH;
                break;
            case RARE:
                sound = Sound.ENTITY_DOLPHIN_SPLASH;
                break;
            case EPIC:
                sound = Sound.ENTITY_ELDER_GUARDIAN_AMBIENT;
                volume = 1.5f;
                pitch = 0.8f;
                break;
            case LEGENDARY:
                sound = Sound.ENTITY_ELDER_GUARDIAN_AMBIENT;
                volume = 2.0f;
                pitch = 0.6f;
                break;
            case MYTHIC:
                sound = Sound.ENTITY_ELDER_GUARDIAN_AMBIENT;
                volume = 2.0f;
                pitch = 0.5f;
                break;
            default:
                sound = Sound.ENTITY_ELDER_GUARDIAN_AMBIENT;
                break;
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private int getBoostedXP(Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return 100;
            case UNCOMMON:
                return 200;
            case RARE:
                return 400;
            case EPIC:
                return 800;
            case LEGENDARY:
                return 1600;
            case MYTHIC:
                return 3200;
            default:
                return 10;
        }
    }

    /**
     * Grants a Luck potion effect to the player based on their fishing level.
     *
     * @param player The player to receive the effect.
     * @param skill  The skill name (e.g., "Fishing").
     */
    private void grantLuck(Player player, String skill) {
        int level = xpManager.getPlayerLevel(player, skill);
        ItemStack rod = player.getInventory().getItemInMainHand();
        int charmed = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.CHARMED);
        int rabbitsFoot = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.RABBITS_FOOT);
        int goodDay = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.GOOD_DAY);

        double luckChance = 0.10 + charmed * 0.15;
        if (random.nextDouble() <= luckChance) {
            int luckDuration = level * 5 + goodDay * 15;
            luckDuration *= 20;
            player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, luckDuration, rabbitsFoot));
            player.sendMessage(ChatColor.GREEN + "You feel lucky!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1.0f, 1.0f);
        }
    }
    /**
     * Implements the enhanced treasure system.
     *
     * @param player The player who is fishing.
     */
    private void rollForTreasure(Player player) {
        PetManager petManager = PetManager.getInstance(plugin);
        double treasureChance = 0.05; // Base chance of 5%
        ItemStack rod = player.getInventory().getItemInMainHand();
        int upgradeLevel = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.TREASURE_HUNTER);
        treasureChance += upgradeLevel / 100.0;
        SkillTreeManager mgr = SkillTreeManager.getInstance();
        if (mgr != null) {
            int treasury = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.TREASURY);
            treasureChance += treasury / 100.0;
        }
        int petLevel = 1;
        if(petManager.getActivePet(player) != null) {
            petLevel = petManager.getActivePet(player).getLevel();
        }
        // Check if the player has the Treasure Hunter perk
        if (petManager.getActivePet(player) != null && petManager.getActivePet(player).hasPerk(PetManager.PetPerk.TREASURE_HUNTER)) {
            treasureChance += (petLevel * 0.0010); // Scale to add 0.45 at pet level 100
        }
        if(PotionManager.isActive("Potion of Liquid Luck", player)
                && PotionEffectPreferences.isEnabled(player, "Potion of Liquid Luck")){
            treasureChance += 0.1;
        }

        // Add bonus from "Piracy" enchantment
        int piracyLevel = CustomEnchantmentManager.getEnchantmentLevel(player.getInventory().getItemInMainHand(), "Piracy");
        treasureChance += piracyLevel / 100.0; // 1% bonus per level of Piracy

        // Roll for treasure
        if (random.nextDouble() <= treasureChance) {
            ItemStack chest = ItemRegistry.getTreasureChest();
            player.getInventory().addItem(chest);
            int motherlode = 0;
            if (mgr != null) {
                motherlode = mgr.getTalentLevel(player.getUniqueId(), Skill.FISHING, Talent.MOTHERLODE);
            }
            if (motherlode > 0 && random.nextDouble() < motherlode * 0.05) {
                player.getInventory().addItem(ItemRegistry.getTreasureChest());
            }
            player.sendMessage(ChatColor.GOLD + "You fished up a treasure chest!");
            player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.0f);

            // Play particle effect
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 30, 1, 1, 1, 0.1);
        }
    }


    /**
     * Returns a random treasure item from the treasure loot table.
     *
     * @return An ItemStack representing the treasure item.
     */
    public ItemStack getRandomTreasure(Player player) {
        // Define a loot table with items and their weights
        List<LootItem> lootTable = Arrays.asList(
                new LootItem(new ItemStack(Material.NAUTILUS_SHELL, 8), 10),
                new LootItem(new ItemStack(Material.SADDLE), 10),
                new LootItem(new ItemStack(Material.DIAMOND, random.nextInt(10) + 1), 10),
                new LootItem(new ItemStack(Material.EMERALD, 64), 10),
                new LootItem(new ItemStack(Material.ANCIENT_DEBRIS), 7),
                new LootItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), 3),
                new LootItem(new ItemStack(Material.TOTEM_OF_UNDYING), 13),
                new LootItem(new ItemStack(Material.HEART_OF_THE_SEA), 5),
                new LootItem(new ItemStack(Material.SHULKER_SHELL), 10),
                new LootItem(new ItemStack(Material.SPONGE), 8),
                new LootItem(new ItemStack(Material.TURTLE_SCUTE), 7),
                new LootItem(new ItemStack(Material.WITHER_SKELETON_SKULL), 4),
                new LootItem(new ItemStack(Material.CREEPER_HEAD), 7),
                new LootItem(new ItemStack(Material.ZOMBIE_HEAD), 7),
                new LootItem(new ItemStack(Material.SKELETON_SKULL), 7),
                new LootItem(new ItemStack(Material.NETHER_WART, random.nextInt(3) + 1), 10),
                new LootItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 64), 7),
                new LootItem(new ItemStack(Material.MUSIC_DISC_13), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_CAT), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_BLOCKS), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_CHIRP), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_FAR), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_MALL), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_MELLOHI), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_STAL), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_STRAD), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_WARD), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_11), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_WAIT), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_OTHERSIDE), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_RELIC), 2),
                new LootItem(new ItemStack(Material.MUSIC_DISC_5), 2),
                new LootItem(getRandomSeaCreatureAlchemyItem(), 1),
                new LootItem(getRandomRareSapling(), 10)
        );


        // Calculate the total weight
        int totalWeight = lootTable.stream().mapToInt(LootItem::getWeight).sum();

        // Roll for treasure
        int roll = random.nextInt(totalWeight);
        int cumulativeWeight = 0;

        for (LootItem lootItem : lootTable) {
            cumulativeWeight += lootItem.getWeight();
            if (roll < cumulativeWeight) {
                return lootItem.getItem();
            }
        }

        return null; // Should not reach here
    }

    /**
     * Returns a random sea creature alchemy item.
     *
     * @return An ItemStack representing the alchemy item.
     */
    private ItemStack getRandomSeaCreatureAlchemyItem() {
        // Get all sea creatures
        List<SeaCreature> seaCreatures = SeaCreatureRegistry.getSeaCreatures();
        if (seaCreatures.isEmpty()) return null;

        // Choose a random sea creature
        SeaCreature randomCreature = seaCreatures.get(random.nextInt(seaCreatures.size()));

        // Get the drops from the random sea creature
        List<ItemStack> drops = randomCreature.getDrops();

        // If there are no drops, return null
        if (drops.isEmpty()) return null;

        // Return a random item from the drops
        return drops.get(random.nextInt(drops.size()));
    }


    /**
     * Returns a random rare sapling.
     *
     * @return An ItemStack representing the sapling.
     */
    private ItemStack getRandomRareSapling() {
        Material[] rareSaplings = {
                Material.ACACIA_SAPLING,
                Material.DARK_OAK_SAPLING,
                Material.JUNGLE_SAPLING,
                Material.BIRCH_SAPLING,
                Material.SPRUCE_SAPLING,
                Material.OAK_SAPLING
        };
        Material saplingType = rareSaplings[random.nextInt(rareSaplings.length)];
        return new ItemStack(saplingType, random.nextInt(3) + 1);
    }

    private void applyReelInUpgrades(Player player, boolean seaCreature, ItemStack rod) {
        if (rod == null || rod.getType() != Material.FISHING_ROD) return;

        if (!seaCreature) {
            int nemo = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.FINDING_NEMO);
            if (nemo > 0 && random.nextDouble() < nemo * 0.15) {
                player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(Material.TROPICAL_FISH));
                player.sendMessage(ChatColor.AQUA + "Finding Nemo activates! Extra tropical fish found.");
            }
        }

        int passion = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.PASSION);
        if (passion > 0 && random.nextDouble() < 0.15) {
            double max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            player.setHealth(max);
            player.sendMessage(ChatColor.RED + "Passion surges through you and restores your health!");
        }

        int feed = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.FEED);
        if (feed > 0 && random.nextDouble() < 0.15) {
            player.setFoodLevel(20);
            player.setSaturation(20f);
            player.sendMessage(ChatColor.GOLD + "Feed satisfies your hunger!");
        }

        int rainDance = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.RAIN_DANCE);
        if (rainDance > 0 && player.getWorld().hasStorm()) {
            boolean triggered = false;
            for (int i = 0; i < rainDance; i++) {
                if (random.nextDouble() < 0.05) {
                    World w = player.getWorld();
                    w.setWeatherDuration(w.getWeatherDuration() + 200);
                    w.setThunderDuration(w.getThunderDuration() + 200);
                    triggered = true;
                }
            }
            if (triggered) {
                player.sendMessage(ChatColor.BLUE + "Raindance extends the storm!");
            }
        }

        handlePayout(player, rod);
    }

    private void handlePayout(Player player, ItemStack rod) {
        int level = FishingUpgradeSystem.getUpgradeLevel(rod, FishingUpgradeSystem.UpgradeType.PAYOUT);
        if (level <= 0 || random.nextDouble() >= 0.5) return;
        Inventory inv = player.getInventory();
        int emeralds = 0;
        emeralds += removeFishGroups(inv, Material.COD, Material.SALMON, 4);
        emeralds += removeUnenchanted(inv, Material.PUFFERFISH, level);
        emeralds += removeUnenchanted(inv, Material.TROPICAL_FISH, level);
        if (emeralds > 0) {
            inv.addItem(new ItemStack(Material.EMERALD, emeralds));
            player.sendMessage(ChatColor.GREEN + "Sold fish for " + emeralds + " emeralds!");
        }
    }

    private int countMaterial(Inventory inv, Material mat) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == mat) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Removes as many full “groups” of unenchanted m1/m2 as possible,
     * and returns the number of groups removed.
     */
    private int removeFishGroups(Inventory inv, Material m1, Material m2, int group) {
        int total = countUnenchanted(inv, m1) + countUnenchanted(inv, m2);
        int groups = total / group;
        if (groups <= 0) return 0;

        int need = groups * group;
        // first remove from m1
        int removed = removeUnenchanted(inv, m1, Math.min(need, countUnenchanted(inv, m1)));
        need -= removed;
        // then from m2 if still needed
        if (need > 0) {
            removeUnenchanted(inv, m2, need);
        }
        return groups;
    }

    /** Count only those stacks of the given material that have NO enchantments. */
    private int countUnenchanted(Inventory inv, Material material) {
        int count = 0;
        for (ItemStack item : inv.getContents()) {
            if (item != null
                    && item.getType() == material
                    && item.getEnchantments().isEmpty()) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * Remove up to `amount` items of the given material,
     * but skip any enchanted stacks.
     * Returns how many were actually removed.
     */
    private int removeUnenchanted(Inventory inventory, Material material, int amount) {
        int remaining = amount;
        int removed = 0;

        for (ItemStack item : inventory.getContents()) {
            if (remaining <= 0) break;
            if (item == null) continue;

            // skip enchanted fish
            if (item.getType() == material && item.getEnchantments().isEmpty()) {
                int stackSize = item.getAmount();

                if (stackSize <= remaining) {
                    inventory.removeItem(item);
                    removed += stackSize;
                    remaining -= stackSize;
                } else {
                    item.setAmount(stackSize - remaining);
                    removed += remaining;
                    remaining = 0;
                }
            }
        }

        return removed;
    }

    /**
     * Returns a random spawn egg.
     *
     * @return An ItemStack representing the spawn egg.
     */
    

    /**
     * Helper class to represent a loot item and its weight.
     */
    public static class LootItem {
        private final ItemStack item;
        private final int weight;

        public LootItem(ItemStack item, int weight) {
            this.item = item;
            this.weight = weight;
        }

        public ItemStack getItem() {
            return item;
        }

        public int getWeight() {
            return weight;
        }
    }

}
