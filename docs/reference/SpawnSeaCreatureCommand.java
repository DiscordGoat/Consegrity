package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.MinecraftNew;
import goat.minecraft.minecraftnew.subsystems.pets.PetManager;
import goat.minecraft.minecraftnew.utils.devtools.ItemRegistry;
import goat.minecraft.minecraftnew.subsystems.combat.SpawnMonsters;
import goat.minecraft.minecraftnew.utils.devtools.XPManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Optional;

public class SpawnSeaCreatureCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("continuity.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        // Check if the player provided a sea creature name
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /spawnseacreature <creature_name>");
            return true;
        }

        String creatureName = args[0].replace("_", " ");

        // Get the sea creature by name
        Optional<SeaCreature> optionalSeaCreature = SeaCreatureRegistry.getSeaCreatureByName(creatureName);
        if (!optionalSeaCreature.isPresent()) {
            player.sendMessage(ChatColor.RED + "SeaCreature with name '" + creatureName + "' not found!");
            return true;
        }

        SeaCreature seaCreature = optionalSeaCreature.get();

        // Spawn the sea creature at the player's location

        Entity spawnedEntity = player.getWorld().spawnEntity(player.getLocation(), seaCreature.getEntityType());
        if (spawnedEntity instanceof org.bukkit.entity.LivingEntity) {
            org.bukkit.entity.LivingEntity livingEntity = (org.bukkit.entity.LivingEntity) spawnedEntity;
            applySeaCreatureEquipment(livingEntity, seaCreature);
            SpawnMonsters spawnMonsters = SpawnMonsters.getInstance(new XPManager(MinecraftNew.getInstance()));
            spawnMonsters.applyMobAttributes(livingEntity, seaCreature.getLevel());
        }

        spawnedEntity.setCustomName(ChatColor.AQUA + "[Lvl " + seaCreature.getLevel() + "] " + seaCreature.getColoredDisplayName());
        spawnedEntity.setCustomNameVisible(true);
        spawnedEntity.setMetadata("SEA_CREATURE", new FixedMetadataValue(Bukkit.getPluginManager().getPlugin("MinecraftNew"), seaCreature.getDisplayName()));

        // Apply attributes and equipment

        player.sendMessage(ChatColor.GREEN + "Spawned " + seaCreature.getColoredDisplayName() + " at your location!");
        Bukkit.getLogger().info("Sea Creature Stats:");
        Bukkit.getLogger().info("Name: " + seaCreature.getDisplayName());
        Bukkit.getLogger().info("Rarity: " + seaCreature.getRarity());
        Bukkit.getLogger().info("Level: " + seaCreature.getLevel());
        return true;
    }

    /**
     * Applies attributes to the sea creature based on its level.
     *
     * @param entity The sea creature entity.
     * @param level  The level of the sea creature.
     */


    /**
     * Applies a full set of dyed armor and a player head to the sea creature.
     *
     * @param creature The sea creature entity to which armor and head will be applied.
     */
    private void applySeaCreatureEquipment(org.bukkit.entity.LivingEntity creature, SeaCreature seaCreature) {
        PetManager petManager = PetManager.getInstance(MinecraftNew.getInstance());
        org.bukkit.inventory.EntityEquipment equipment = creature.getEquipment();
        if (equipment == null) return;

        // Create and set the armor
        org.bukkit.Color armorColor = seaCreature.getArmorColor();
        equipment.setHelmet(SeaCreatureRegistry.createDyedLeatherArmor(org.bukkit.Material.LEATHER_HELMET, armorColor));
        equipment.setChestplate(SeaCreatureRegistry.createDyedLeatherArmor(org.bukkit.Material.LEATHER_CHESTPLATE, armorColor));
        equipment.setLeggings(SeaCreatureRegistry.createDyedLeatherArmor(org.bukkit.Material.LEATHER_LEGGINGS, armorColor));
        equipment.setBoots(SeaCreatureRegistry.createDyedLeatherArmor(org.bukkit.Material.LEATHER_BOOTS, armorColor));

        equipment.setBootsDropChance(0);
        equipment.setChestplateDropChance(0);
        equipment.setHelmetDropChance(0);
        equipment.setLeggingsDropChance(0);

        equipment.setItemInOffHandDropChance(1.0f);

        if(seaCreature.getSkullName().equals("Pirate")){
            equipment.setItemInOffHand(ItemRegistry.getRandomTreasure());
            equipment.setItemInOffHandDropChance(1);
        }
        // Create and set the player head
        String playerHeadName = seaCreature.getSkullName();
        ItemStack helmet = petManager.getSkullForPet(seaCreature.getSkullName());
        equipment.setHelmet(helmet);
    }
}