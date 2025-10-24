package goat.minecraft.minecraftnew.subsystems.fishing;

import goat.minecraft.minecraftnew.utils.devtools.ItemRegistry;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class SeaCreatureRegistry implements Listener {
    private static final List<SeaCreature> SEA_CREATURES = new ArrayList<>();
    private static final Map<Rarity, Double> RARITY_WEIGHTS = new HashMap<>();
    private static final Map<Rarity, Integer> RARITY_LEVELS = new HashMap<>();
    private static final Random RANDOM = new Random();



    static {
        // Define weights for each rarity (sum should be 1.0)
        RARITY_WEIGHTS.put(Rarity.COMMON, 0.50);
        RARITY_WEIGHTS.put(Rarity.UNCOMMON, 0.30);
        RARITY_WEIGHTS.put(Rarity.RARE, 0.12);
        RARITY_WEIGHTS.put(Rarity.EPIC, 0.06);
        RARITY_WEIGHTS.put(Rarity.LEGENDARY, 0.02);


        // Assign levels based on rarity
        RARITY_LEVELS.put(Rarity.COMMON, 10);
        RARITY_LEVELS.put(Rarity.UNCOMMON, 30);
        RARITY_LEVELS.put(Rarity.RARE, 60);
        RARITY_LEVELS.put(Rarity.EPIC, 100);
        RARITY_LEVELS.put(Rarity.LEGENDARY, 150);



        //COMMON
        List<SeaCreature.DropItem> turtleDrops = new ArrayList<>();
        turtleDrops.add(new SeaCreature.DropItem(ItemRegistry.getShallowShell(), 1, 1, 1)); // Common drop for a cod fish
        SEA_CREATURES.add(new SeaCreature(
                "Shallow Turtle", // Name of the sea creature
                Rarity.COMMON, // Rarity of the sea creature
                EntityType.TURTLE, // Entity type for the cod fish
                turtleDrops, // Drops for the cod fish
                Color.fromRGB(162, 162, 162), // Light gray color to represent a cod fish
                "Fish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.COMMON) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> codFishDrops = new ArrayList<>();
        codFishDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 1, 1, 4)); // Common drop for a cod fish
        codFishDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 1, 1, 3)); // Common drop for a cod fish
        SEA_CREATURES.add(new SeaCreature(
                "Fish", // Name of the sea creature
                Rarity.COMMON, // Rarity of the sea creature
                EntityType.COD, // Entity type for the cod fish
                codFishDrops, // Drops for the cod fish
                Color.fromRGB(162, 162, 162), // Light gray color to represent a cod fish
                "Fish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.COMMON) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> salmonDrops = new ArrayList<>();
        salmonDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 1, 1, 4)); // Common drop for a salmon
        salmonDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 1, 1, 2)); // Common drop for a salmon
        SEA_CREATURES.add(new SeaCreature(
                "Salmon", // Name of the sea creature
                Rarity.COMMON, // Rarity of the sea creature
                EntityType.SALMON, // Entity type for the salmon
                salmonDrops, // Drops for the salmon
                Color.fromRGB(255, 105, 180), // Pink color to represent a salmon
                "Salmon", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.COMMON) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> pufferfishDrops = new ArrayList<>();
        pufferfishDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 1, 1, 4)); // Common drop for a pufferfish
        pufferfishDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 1, 1, 2)); // Common drop for a pufferfish
        SEA_CREATURES.add(new SeaCreature(
                "Pufferfish", // Name of the sea creature
                Rarity.COMMON, // Rarity of the sea creature
                EntityType.PUFFERFISH, // Entity type for the pufferfish
                pufferfishDrops, // Drops for the pufferfish
                Color.fromRGB(255, 165, 0), // Orange color to represent a pufferfish
                "Pufferfish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.COMMON) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> tropicalFishDrops = new ArrayList<>();
        tropicalFishDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 1, 1, 1)); // Common drop for a tropical fish
        tropicalFishDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 1, 1, 2)); // Common drop for a tropical fish
        SEA_CREATURES.add(new SeaCreature(
                "Tropical Fish", // Name of the sea creature
                Rarity.COMMON, // Rarity of the sea creature
                EntityType.TROPICAL_FISH, // Entity type for the tropical fish
                tropicalFishDrops, // Drops for the tropical fish
                Color.fromRGB(255, 215, 0), // Gold color to represent a tropical fish
                "Tropical_Fish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.COMMON) // Rarity level for common creatures
        ));

        //UNCOMMON
        List<SeaCreature.DropItem> turdleDrops = new ArrayList<>();
        turdleDrops.add(new SeaCreature.DropItem(ItemRegistry.getShell(), 1, 1, 1)); // Common drop for a cod fish
        SEA_CREATURES.add(new SeaCreature(
                "Sea Turtle", // Name of the sea creature
                Rarity.UNCOMMON, // Rarity of the sea creature
                EntityType.TURTLE, // Entity type for the cod fish
                turdleDrops, // Drops for the cod fish
                Color.fromRGB(162, 162, 162), // Light gray color to represent a cod fish
                "Fish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.UNCOMMON) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> luminescentDrownedDrops = new ArrayList<>();
        luminescentDrownedDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 1, 1, 2)); // Common drop for a luminescent drowned
        luminescentDrownedDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 1, 1, 1));
        luminescentDrownedDrops.add(new SeaCreature.DropItem(ItemRegistry.getAquaAffinity(), 1, 1, 12));
        SEA_CREATURES.add(new SeaCreature(
                "Luminescent Drowned", // Name of the sea creature
                Rarity.UNCOMMON, // Rarity of the sea creature
                EntityType.DROWNED, // Entity type for the luminescent drowned
                luminescentDrownedDrops, // Drops for the luminescent drowned
                Color.fromRGB(50, 150, 255), // Light blue color to represent a luminescent drowned
                "Luminescent_Drowned", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.UNCOMMON) // Rarity level for uncommon creatures
        ));
        List<SeaCreature.DropItem> squidDrops = new ArrayList<>();
        squidDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 1, 1, 1)); // Luminescent Ink drop
        squidDrops.add(new SeaCreature.DropItem(ItemRegistry.getCalamari(), 1, 1, 1)); // Luminescent Ink drop
        SEA_CREATURES.add(new SeaCreature(
                "Squid", // Name of the sea creature
                Rarity.UNCOMMON, // Rarity of the sea creature
                EntityType.SQUID, // Entity type for the luminescent drowned
                squidDrops, // Drops for the luminescent drowned
                Color.fromRGB(50, 150, 255), // Light blue color to represent a luminescent drowned
                "Luminescent_Drowned", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.UNCOMMON) // Rarity level for uncommon creatures
        ));

        //RARE
        List<SeaCreature.DropItem> deepTurtleDrops = new ArrayList<>();
        deepTurtleDrops.add(new SeaCreature.DropItem(ItemRegistry.getDeepShell(), 2, 1, 2)); // Common drop for a cod fish
        SEA_CREATURES.add(new SeaCreature(
                "Deep Sea Turtle", // Name of the sea creature
                Rarity.RARE, // Rarity of the sea creature
                EntityType.TURTLE, // Entity type for the cod fish
                deepTurtleDrops, // Drops for the cod fish
                Color.fromRGB(162, 162, 162), // Light gray color to represent a cod fish
                "Fish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.RARE) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> poseidonDrops = new ArrayList<>();
        poseidonDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 6, 1, 3));
        poseidonDrops.add(new SeaCreature.DropItem(ItemRegistry.getTrident(), 1, 1, 4));
        poseidonDrops.add(new SeaCreature.DropItem(ItemRegistry.getRiptide(), 1, 1, 3));
        poseidonDrops.add(new SeaCreature.DropItem(ItemRegistry.getChanneling(), 1, 1, 6));
        poseidonDrops.add(new SeaCreature.DropItem(ItemRegistry.getLoyaltyContract(), 1, 1, 5));
        SEA_CREATURES.add(new SeaCreature(
                "Poseidon",
                Rarity.RARE,
                EntityType.DROWNED,
                poseidonDrops,
                Color.fromRGB(0, 0, 255),
                "Poseidon",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.RARE)
        ));
        List<SeaCreature.DropItem> sharkDrops = new ArrayList<>();
        sharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 12, 1, 5));
        sharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 6, 1, 5));
        // Guaranteed Fish Bait drop
        sharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getBait(), 1, 1, 1));
        SEA_CREATURES.add(new SeaCreature(
                "Shark",
                Rarity.RARE,
                EntityType.ZOMBIE,
                sharkDrops,
                Color.fromRGB(47, 47, 47),
                "Shark",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.RARE)
        ));
        List<SeaCreature.DropItem> pirateDrops = new ArrayList<>();
        pirateDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 6, 1, 5));
        pirateDrops.add(new SeaCreature.DropItem(ItemRegistry.getSeaSalt(), 6, 1, 5));
        pirateDrops.add(new SeaCreature.DropItem(ItemRegistry.getImpaling(), 1, 1, 2));
        pirateDrops.add(new SeaCreature.DropItem(ItemRegistry.getLuck(), 1, 1, 2));

        SEA_CREATURES.add(new SeaCreature(
                "Pirate",
                Rarity.RARE,
                EntityType.SKELETON,
                null,
                Color.fromRGB(0, 0, 0),
                "Pirate",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.RARE)
        ));




        //EPIC
        List<SeaCreature.DropItem> abyssalTurtleDrops = new ArrayList<>();
        abyssalTurtleDrops.add(new SeaCreature.DropItem(ItemRegistry.getAbyssalShell(), 3, 1, 2)); // Common drop for a cod fish
        abyssalTurtleDrops.add(new SeaCreature.DropItem(ItemRegistry.getLuck(), 1, 1, 1)); // Common drop for a cod fish
        SEA_CREATURES.add(new SeaCreature(
                "Abyssal Sea Turtle", // Name of the sea creature
                Rarity.EPIC, // Rarity of the sea creature
                EntityType.TURTLE, // Entity type for the cod fish
                abyssalTurtleDrops, // Drops for the cod fish
                Color.fromRGB(162, 162, 162), // Light gray color to represent a cod fish
                "Fish", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.EPIC) // Rarity level for common creatures
        ));
        List<SeaCreature.DropItem> waterSpiderDrops = new ArrayList<>();
        waterSpiderDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 14, 1, 5));
        waterSpiderDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 14, 1, 4));
        SEA_CREATURES.add(new SeaCreature(
                "Water Spider",
                Rarity.EPIC,
                EntityType.CAVE_SPIDER,
                waterSpiderDrops,
                Color.fromRGB(0, 51, 102),
                "waterspider",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.EPIC)
        ));
        List<SeaCreature.DropItem> greatWhiteSharkDrops = new ArrayList<>();
        greatWhiteSharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 14, 1, 5));
        greatWhiteSharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 14, 1, 4));
        greatWhiteSharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getRespiration(), 2, 1, 2));
        // Guaranteed Fish Bait drop
        greatWhiteSharkDrops.add(new SeaCreature.DropItem(ItemRegistry.getBait(), 1, 1, 1));
        SEA_CREATURES.add(new SeaCreature(
                "Great White Shark",
                Rarity.EPIC,
                EntityType.ZOMBIE,
                greatWhiteSharkDrops,
                Color.fromRGB(0, 51, 102),
                "Great_White_Shark",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.EPIC)
        ));
        List<SeaCreature.DropItem> leviathanDrops = new ArrayList<>();
        leviathanDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 30, 1, 4));
        leviathanDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 30, 1, 4));
        leviathanDrops.add(new SeaCreature.DropItem(ItemRegistry.getLeviathanHeart(), 1, 1, 10)); // Add Leviathan heart drop
        leviathanDrops.add(new SeaCreature.DropItem(ItemRegistry.getSweepingEdge(), 1, 1, 3)); // Add Leviathan heart drop
        SEA_CREATURES.add(new SeaCreature(
                "Leviathan", // Changed name to Leviathan
                Rarity.EPIC,
                EntityType.ZOMBIE, // You can change this to a more appropriate entity type if needed
                leviathanDrops,
                Color.fromRGB(0, 100, 100), // Dark aqua color
                "Leviathan", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.EPIC)
        ));
        List<SeaCreature.DropItem> yetiDrops = new ArrayList<>();
        yetiDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 20, 1, 3)); // Example drop
        yetiDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 20, 1, 3)); // Example drop
        yetiDrops.add(new SeaCreature.DropItem(ItemRegistry.getInfernalSharpness(), 1, 1, 6)); // Yeti Fur drop
        SEA_CREATURES.add(new SeaCreature(
                "Yeti", // Name of the sea creature
                Rarity.EPIC, // Rarity of the sea creature
                EntityType.ZOMBIE, // Entity type for the Yeti (you can change this to a more appropriate entity type if needed)
                yetiDrops, // Drops for the Yeti
                Color.fromRGB(255, 255, 255), // White color to represent a Yeti
                "Yeti", // Unique texture for this creature
                RARITY_LEVELS.get(Rarity.EPIC) // Rarity level for epic creatures
        ));






        //LEGENDARY
        List<SeaCreature.DropItem> bioluminescentGuardianDrops = new ArrayList<>();
        bioluminescentGuardianDrops.add(new SeaCreature.DropItem(ItemRegistry.getAbyssalInk(), 2, 1, 2));
        SEA_CREATURES.add(new SeaCreature(
                "Bioluminescent Guardian",
                Rarity.LEGENDARY,
                EntityType.IRON_GOLEM,
                bioluminescentGuardianDrops,
                Color.fromRGB(211, 211, 211),
                "Megalodon",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.LEGENDARY)
        ));
        List<SeaCreature.DropItem> megalodonDrops = new ArrayList<>();
        megalodonDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 30, 1, 4));
        megalodonDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 30, 1, 4));
        megalodonDrops.add(new SeaCreature.DropItem(ItemRegistry.getInfernalLure(), 1, 1, 7));
        SEA_CREATURES.add(new SeaCreature(
                "Megalodon",
                Rarity.LEGENDARY,
                EntityType.ZOMBIE,
                megalodonDrops,
                Color.fromRGB(211, 211, 211),
                "Megalodon",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.LEGENDARY)
        ));
        List<SeaCreature.DropItem> abominationDrops = new ArrayList<>();
        abominationDrops.add(new SeaCreature.DropItem(ItemRegistry.getTooth(), 64, 1, 4));
        abominationDrops.add(new SeaCreature.DropItem(ItemRegistry.getFishBone(), 64, 1, 4));
        abominationDrops.add(new SeaCreature.DropItem(ItemRegistry.getInfernalUnbreaking(), 1, 1, 2));
        abominationDrops.add(new SeaCreature.DropItem(ItemRegistry.getSwiftSneak(), 1, 1, 3));
        SEA_CREATURES.add(new SeaCreature(
                "Abomination",
                Rarity.LEGENDARY,
                EntityType.ZOMBIE,
                abominationDrops,
                Color.fromRGB(250, 10, 20),
                "Abomination",  // unique texture for this creature
                RARITY_LEVELS.get(Rarity.LEGENDARY)
        ));
        List<SeaCreature.DropItem> midasDrops = new ArrayList<>();
        midasDrops.add(new SeaCreature.DropItem(ItemRegistry.getVerdantRelicTreasury(), 1, 1, 1));

        SEA_CREATURES.add(new SeaCreature(
                "Midas",
                Rarity.LEGENDARY,
                EntityType.ZOMBIE,  // Choose an appropriate EntityType; DROWNED is used here for an aquatic theme.
                midasDrops,
                Color.fromRGB(212, 175, 55),  // A gold-like color.
                "Midas",  // Unique texture for this creature (ensure your texture key/method is set up accordingly).
                RARITY_LEVELS.get(Rarity.LEGENDARY)
        ));

        //MYTHIC

    }

    /**
     * Retrieves all registered sea creatures.
     *
     * @return An unmodifiable list of all sea creatures.
     */
    public static List<SeaCreature> getSeaCreatures() {
        return Collections.unmodifiableList(SEA_CREATURES);
    }

    /**
     * Retrieves a sea creature by its display name.
     *
     * @param name The display name of the sea creature.
     * @return An Optional containing the sea creature if found.
     */
    public static Optional<SeaCreature> getSeaCreatureByName(String name) {
        return SEA_CREATURES.stream()
                .filter(creature -> creature.getDisplayName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Retrieves a random sea creature from the registry based on rarity.
     *
     * @return An Optional containing a random sea creature if available.
     */
    public static Optional<SeaCreature> getRandomSeaCreature() {
        if (SEA_CREATURES.isEmpty()) return Optional.empty();

        // Step 1: Select a rarity based on weights
        double rand = RANDOM.nextDouble();
        double cumulative = 0.0;
        Rarity selectedRarity = Rarity.COMMON; // Default

        for (Map.Entry<Rarity, Double> entry : RARITY_WEIGHTS.entrySet()) {
            cumulative += entry.getValue();
            if (rand <= cumulative) {
                selectedRarity = entry.getKey();
                break;
            }
        }

        // Step 2: Filter creatures by selected rarity
        List<SeaCreature> filtered = new ArrayList<>();
        for (SeaCreature creature : SEA_CREATURES) {
            if (creature.getRarity() == selectedRarity) {
                filtered.add(creature);
            }
        }

        // Step 3: Select a random creature from the filtered list
        if (filtered.isEmpty()) return Optional.empty();

        SeaCreature selectedCreature = filtered.get(RANDOM.nextInt(filtered.size()));
        return Optional.of(selectedCreature);
    }

    /**
     * Applies a full set of dyed armor and a player head to the sea creature.
     *
     * @param creature The sea creature entity to which armor and head will be applied.
     */

    /**
     * Creates a dyed leather armor piece with the specified color.
     *
     * @param material The type of leather armor.
     * @param color    The color to dye the armor.
     * @return The dyed leather armor ItemStack.
     */
    static ItemStack createDyedLeatherArmor(Material material, Color color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(color);
        armor.setItemMeta(meta);
        return armor;
    }
    public static Optional<SeaCreature> getRandomRareSeaCreature() {
        // Filter out COMMON and UNCOMMON
        List<SeaCreature> rares = SEA_CREATURES.stream()
                .filter(c -> {
                    Rarity r = c.getRarity();
                    return r != Rarity.COMMON && r != Rarity.UNCOMMON;
                })
                .collect(Collectors.toList());
        if (rares.isEmpty()) return Optional.empty();
        return Optional.of(rares.get(RANDOM.nextInt(rares.size())));
    }

    /**
     * Creates a player head with the specified display name.
     *
     * @param displayName The display name for the head.
     * @return The player head ItemStack.
     */
}
