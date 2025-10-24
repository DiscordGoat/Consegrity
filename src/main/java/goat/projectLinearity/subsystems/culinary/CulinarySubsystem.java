package goat.projectLinearity.subsystems.culinary;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * CulinarySubsystem
 *
 * Handles displaying and cooking custom recipes on campfires.
 * Active cooking sessions are saved to <code>culinary_sessions.yml</code>
 * so they persist through reloads and restarts.
 */
public class CulinarySubsystem implements Listener {
    private JavaPlugin plugin;
    private Logger logger;
    private static CulinarySubsystem instance;

    private final File dataFile;
    private YamlConfiguration dataConfig;
    private boolean isEnabled = false;

    // Active recipe sessions keyed by the campfire location
    private final Map<String, RecipeSession> activeRecipeSessions = new HashMap<>();
    private final Random random = new Random();
    private final Map<UUID, CulinaryRecipe> selectedRecipes = new HashMap<>();
    private CulinaryCatalogueManager catalogueManager;


    private enum FoodGroup {
        FRUITS(ChatColor.DARK_RED),
        GRAINS(ChatColor.GOLD),
        PROTEINS(ChatColor.RED),
        VEGGIES(ChatColor.GREEN),
        SUGARS(ChatColor.LIGHT_PURPLE);

        private final ChatColor color;

        FoodGroup(ChatColor color) {
            this.color = color;
        }

        public ChatColor getColor() {
            return color;
        }
    }
    public List<ItemStack> getAllRecipeItems() {
        List<ItemStack> recipeItems = new ArrayList<>();

        // Iterate through each recipe in the recipe registry
        for (CulinaryRecipe recipe : recipeRegistry) {
            // Create the recipe item for the current recipe
            ItemStack recipeItem = createRecipeItem(recipe);

            // Add the recipe item to the list
            recipeItems.add(recipeItem);
        }

        return recipeItems;
    }
    public List<ItemStack> getAllNonFeastRecipeItems() {
        List<ItemStack> recipeItems = new ArrayList<>();

        // Iterate through each recipe in the recipe registry
        for (CulinaryRecipe recipe : recipeRegistry) {
            // Skip recipes containing "Feast"
            if (recipe.getName().contains("Feast")) {
                continue; // skip to next iteration
            }

            // Create the recipe item for the current non-feast recipe
            ItemStack recipeItem = createRecipeItem(recipe);

            // Add the recipe item to the list
            recipeItems.add(recipeItem);
        }

        return recipeItems;
    }

    public ItemStack getRecipeItemByName(String recipeName) {
        // search the normal recipes
        for (CulinaryRecipe recipe : recipeRegistry) {
            if (recipe.getName().equalsIgnoreCase(recipeName)) {
                return createRecipeItem(recipe);
            }
        }
        // search the bartender-only (oceanic) recipes
        for (CulinaryRecipe recipe : oceanicRecipes) {
            if (recipe.getName().equalsIgnoreCase(recipeName)) {
                return createRecipeItem(recipe);
            }
        }
        // not found
        return null;
    }

    public void setCatalogueManager(CulinaryCatalogueManager manager) {
        this.catalogueManager = manager;
    }

    public void setSelectedRecipe(UUID playerId, CulinaryRecipe recipe) {
        if (playerId == null) {
            return;
        }
        if (recipe == null) {
            selectedRecipes.remove(playerId);
        } else {
            selectedRecipes.put(playerId, recipe);
        }
    }

    public CulinaryRecipe getSelectedRecipe(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return selectedRecipes.get(playerId);
    }

    public List<CulinaryRecipe> getStandardRecipes() {
        List<CulinaryRecipe> combined = new ArrayList<>(recipeRegistry.size() + oceanicRecipes.size());
        for (CulinaryRecipe recipe : recipeRegistry) {
            if (!recipe.isFeast()) {
                combined.add(recipe);
            }
        }
        for (CulinaryRecipe recipe : oceanicRecipes) {
            if (!recipe.isFeast()) {
                combined.add(recipe);
            }
        }
        return Collections.unmodifiableList(combined);
    }

    public List<CulinaryRecipe> getFeastRecipes() {
        List<CulinaryRecipe> feasts = new ArrayList<>();
        for (CulinaryRecipe recipe : recipeRegistry) {
            if (recipe.isFeast()) {
                feasts.add(recipe);
            }
        }
        for (CulinaryRecipe recipe : oceanicRecipes) {
            if (recipe.isFeast()) {
                feasts.add(recipe);
            }
        }
        return Collections.unmodifiableList(feasts);
    }

    public boolean isBartenderRecipe(CulinaryRecipe recipe) {
        return recipe != null && oceanicRecipes.contains(recipe);
    }

    public ItemStack createCataloguePreview(CulinaryRecipe recipe) {
        if (recipe == null) {
            return new ItemStack(Material.PAPER);
        }
        ItemStack preview = createOutputItem(recipe);
        return preview == null ? new ItemStack(Material.PAPER) : preview;
    }

    public boolean beginRecipeSessionFromCatalogue(Player player, String locKey, Location tableLoc, CulinaryRecipe recipe) {
        if (player == null || locKey == null || tableLoc == null || recipe == null) {
            return false;
        }
        if (activeRecipeSessions.containsKey(locKey)) {
            player.sendMessage(ChatColor.RED + "This campfire is already being used for another recipe!");
            return false;
        }
        beginRecipeSession(player, tableLoc, locKey, recipe, false);
        return true;
    }

    public static CulinarySubsystem getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new CulinarySubsystem(plugin);
            instance.onEnable();
        }
        return instance;
    }

    public void onEnable() {
        if (!isEnabled) {
            loadAllSessions();
            isEnabled = true;
        }
    }

    public void onDisable() {
        saveAllSessions();
    }

    // Recipe registry
    public static List<CulinaryRecipe> recipeRegistry = new ArrayList<>();
    // recipes that only the Bartender can craft
    public static List<CulinaryRecipe> oceanicRecipes = new ArrayList<>();


    static {
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.HONEY_BOTTLE,
                "Tidal Shot",
                Arrays.asList("Rum", "Gunpowder", "Ice", "Sea Salt"),
                1000,
                FoodGroup.SUGARS,
                false
        ));
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.HONEY_BOTTLE,
                "Coral Cooler",
                Arrays.asList("Rum", "Ice", "Prismarine Shard", "Sea Pickle", "Sea Salt"),
                1000,
                FoodGroup.FRUITS,
                false
        ));
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.HONEY_BOTTLE,
                "Prismarita",
                Arrays.asList("Rum", "Lime", "Sugar", "Ice", "Prismarine Shard", "Sea Salt"),
                1000,
                FoodGroup.SUGARS,
                false
        ));
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.HONEY_BOTTLE,
                "Kelp Mojito",
                Arrays.asList("Rum", "Lime", "Sugar", "Ice", "Kelp", "Sea Salt"),
                1000,
                FoodGroup.FRUITS,
                false
        ));
        // (Water Breathing)
        // Bananas Split is bartender-only
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.MELON_SLICE,
                "Banana Split",
                Arrays.asList("Banana", "Snowball", "Chocolate", "Milk Bucket", "Sea Salt"),
                1000,
                FoodGroup.SUGARS,
                false
        ));
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.HONEY_BOTTLE,
                "Pina Colada",
                Arrays.asList("Milk Bucket", "Rum", "Ice", "Pineapple", "Coconut", "Sea Salt"),
                1000,
                FoodGroup.FRUITS,
                false
        ));
        oceanicRecipes.add(new CulinaryRecipe(
                Material.PAPER,
                Material.PUMPKIN_PIE,
                "Key Lime Pie",
                Arrays.asList("Lime", "Sugar", "Egg", "Milk Bucket", "Sea Salt"),
                1000,
                FoodGroup.FRUITS,
                false
        ));

    }
    static {
        // Example recipes
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKED_BEEF,
                "Salted Steak",
                Arrays.asList("Cooked Beef", "Sea Salt"),
                500,
                FoodGroup.PROTEINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKED_CHICKEN,
                "Chicken Tenders",
                Arrays.asList("Cooked Chicken", "Bread", "Sea Salt"),
                500,
                FoodGroup.PROTEINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.YELLOW_DYE,
                "Slice of Cheese",
                Arrays.asList("Milk Bucket", "Sea Salt"),
                500,
                FoodGroup.PROTEINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Ham and Cheese Sandwich",
                Arrays.asList("Slice of Cheese", "Cooked Porkchop", "Bread", "Sea Salt"),
                500,
                FoodGroup.GRAINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Toast",
                Arrays.asList("Bread", "Butter", "Sea Salt"),
                500,
                FoodGroup.GRAINS,
                false
        ));



        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.PUMPKIN_PIE,
                "Sweet Feast",
                Arrays.asList("Sugar", "Pumpkin", "Egg", "Wheat", "Sea Salt"),
                1000,
                FoodGroup.SUGARS,
                true
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.CARROT,
                "Vegetarian Feast",
                Arrays.asList("Carrot", "Potato", "Golden Carrot", "Beetroot", "Sea Salt"),
                1000,
                FoodGroup.VEGGIES,
                true
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKED_RABBIT,
                "Meatlovers Feast",
                Arrays.asList("Cooked Beef", "Cooked Chicken", "Butter", "Sea Salt", "Cooked Mutton", "Cooked Rabbit", "Cooked Porkchop"),
                1000,
                FoodGroup.PROTEINS,
                true
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.DRIED_KELP,
                "Seafood Feast",
                Arrays.asList("Dried Kelp Block", "Cod", "Salmon", "Tropical Fish", "Calamari", "Sea Salt"),
                1000,
                FoodGroup.PROTEINS,
                true
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKED_SALMON,
                "Grilled Salmon",
                Arrays.asList("Cooked Salmon", "Sea Salt"),
                500,
                FoodGroup.PROTEINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Mushroom Soup",
                Arrays.asList("Red Mushroom", "Brown Mushroom", "Sea Salt"),
                500,
                FoodGroup.VEGGIES,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BAKED_POTATO,
                "Loaded Baked Potato",
                Arrays.asList("Baked Potato", "Butter", "Slice of Cheese", "Cooked Porkchop", "Sea Salt"),
                500,
                FoodGroup.VEGGIES,
                false
        ));
        // Additional recipes to fill food groups
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.APPLE,
                "Apple Tart",
                Arrays.asList("Apple", "Sugar", "Wheat", "Sea Salt"),
                500,
                FoodGroup.FRUITS,
                false
        ));

        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.MELON_SLICE,
                "Fruit Salad",
                Arrays.asList("Melon", "Apple", "Sweet Berries", "Sea Salt"),
                500,
                FoodGroup.FRUITS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.SWEET_BERRIES,
                "Berry Pie",
                Arrays.asList("Sweet Berries", "Sugar", "Egg", "Sea Salt"),
                500,
                FoodGroup.FRUITS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.POTION,
                "Tropical Smoothie",
                Arrays.asList("Lime", "Pineapple", "Milk Bucket", "Sea Salt"),
                500,
                FoodGroup.FRUITS,
                false
        ));

        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.GOLDEN_APPLE,
                "Fruit Feast",
                Arrays.asList("Apple", "Melon", "Sweet Berries", "Sugar", "Sea Salt"),
                1000,
                FoodGroup.FRUITS,
                true
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.APPLE,
                "Exotic Fruit Feast",
                Arrays.asList("Golden Carrot", "Melon", "Golden Apple", "Sugar", "Sea Salt"),
                1000,
                FoodGroup.FRUITS,
                true
        ));

        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Wheat Bread",
                Arrays.asList("Wheat", "Butter", "Sea Salt"),
                500,
                FoodGroup.GRAINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BOWL,
                "Oatmeal",
                Arrays.asList("Wheat", "Milk Bucket", "Sugar", "Sea Salt"),
                500,
                FoodGroup.GRAINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Pasta Bowl",
                Arrays.asList("Wheat", "Egg", "Sea Salt"),
                500,
                FoodGroup.GRAINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Granola Bar",
                Arrays.asList("Wheat", "Honey Bottle", "Chocolate", "Sea Salt"),
                500,
                FoodGroup.GRAINS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.PUMPKIN_PIE,
                "Grain Feast",
                Arrays.asList("Bread", "Wheat", "Sugar", "Egg", "Sea Salt"),
                1000,
                FoodGroup.GRAINS,
                true
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.BREAD,
                "Hearty Grain Feast",
                Arrays.asList("Bread", "Wheat", "Oatmeal (Culinary)", "Sea Salt", "Butter"),
                1000,
                FoodGroup.GRAINS,
                true
        ));

        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.CARROT,
                "Garden Salad",
                Arrays.asList("Carrot", "Beetroot", "Potato", "Sea Salt"),
                500,
                FoodGroup.VEGGIES,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.CARROT,
                "Roasted Veggies",
                Arrays.asList("Potato", "Carrot", "Beetroot", "Sea Salt"),
                500,
                FoodGroup.VEGGIES,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.CARROT,
                "Veggie Stir Fry",
                Arrays.asList("Carrot", "Potato", "Dried Kelp", "Sea Salt"),
                500,
                FoodGroup.VEGGIES,
                false
        ));

        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKIE,
                "Chocolate Cake",
                Arrays.asList("Cocoa Beans", "Sugar", "Egg", "Milk Bucket", "Wheat", "Chocolate", "Sea Salt"),
                500,
                FoodGroup.SUGARS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKIE,
                "Cookie Platter",
                Arrays.asList("Cookie", "Sugar", "Milk Bucket", "Sea Salt"),
                500,
                FoodGroup.SUGARS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.COOKIE,
                "Ice Cream Cone",
                Arrays.asList("Milk Bucket", "Sugar", "Snowball", "Sea Salt"),
                500,
                FoodGroup.SUGARS,
                false
        ));
        recipeRegistry.add(new CulinaryRecipe(
                Material.PAPER,
                Material.APPLE,
                "Candied Apple",
                Arrays.asList("Apple", "Sugar", "Sea Salt"),
                500,
                FoodGroup.SUGARS,
                false
        ));
    }

    private CulinarySubsystem(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        dataFile = new File(plugin.getDataFolder(), "culinary_sessions.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        Bukkit.getLogger().info("[CulinarySubsystem] Registering events...");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getLogger().info("[CulinarySubsystem] Events registered.");
    }

    /**
     * Called from the main plugin's onDisable(). Saves all sessions so they can
     * be restored on the next startup.
     */
    public void finalizeAllSessionsOnShutdown() {
        for (RecipeSession session : activeRecipeSessions.values()) {
            if (session.cookTask != null) {
                session.cookTask.cancel();
            }
            for (BukkitTask t : session.ingredientSpinTasks.values()) {
                if (t != null) t.cancel();
            }
            if (session.particleTask != null) {
                session.particleTask.cancel();
            }
        }
        saveAllSessions();
        activeRecipeSessions.clear();
        logger.info("[CulinarySubsystem] Sessions saved and cleared for shutdown.");
    }


    public List<ItemStack> getAllRecipePapers() {
        List<ItemStack> recipePapers = new ArrayList<>();

        // Iterate through each recipe in the recipe registry
        for (CulinaryRecipe recipe : recipeRegistry) {
            // Create the recipe item for the current recipe
            ItemStack recipePaper = createRecipeItem(recipe);

            // Add the recipe item to the list
            recipePapers.add(recipePaper);
        }

        return recipePapers;
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (event.getClickedBlock().getType() != Material.CAMPFIRE) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand().clone();
        Location tableLoc = event.getClickedBlock().getLocation().clone();
        String locKey = toLocKey(tableLoc);
        RecipeSession session = activeRecipeSessions.get(locKey);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            boolean holdingRecipePaper = player.getInventory().getItemInMainHand().getType() == Material.PAPER && isRecipeItem(hand);

            if (session != null && holdingRecipePaper) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "This campfire is already being used for another recipe!");
                return;
            }

            if (session == null) {
                if (holdingRecipePaper) {
                    CulinaryRecipe recipe = parseRecipeFromItem(hand);
                    if (recipe == null) {
                        event.setCancelled(true);
                        player.sendMessage(ChatColor.RED + "This recipe is invalid or not recognized.");
                        logger.warning("[CulinarySubsystem] Player " + player.getName() + " attempted to use an unregistered recipe item.");
                        return;
                    }
                    event.setCancelled(true);
                    beginRecipeSession(player, tableLoc, locKey, recipe, true);
                    return;
                }

                if (catalogueManager != null) {
                    event.setCancelled(true);
                    catalogueManager.openCatalogue(player, locKey, tableLoc);
                    return;
                }
                return;
            }

            // -- 2) Placing an ingredient on an ACTIVE recipe session --
            if (session != null) {
                if (!session.finalized && hand.getType() == Material.AIR) {
                    event.setCancelled(true);
                    clearRecipeSession(session, player, true);
                    return;
                }

                if (session.readyForPickup) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.YELLOW + "LEFT CLICK to claim " + session.recipe.getName() + "!");
                    return;
                } else if (session.finalized) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This recipe is already cooking!");
                    return;
                }
                CulinaryRecipe recipe = session.recipe;
                // Identify if the held item is a needed ingredient and hasn't been placed yet
                String ingredientName = matchIngredient(hand, recipe.getIngredients(), session.placedIngredientsStands.keySet());
                if (ingredientName != null) {
                    event.setCancelled(true);
                    logger.info("[CulinarySubsystem] Player " + player.getName() + " placing ingredient " + ingredientName);

                    consumeItem(player, hand, 1);

                    UUID standUUID = spawnIngredientAboveTableRandom(tableLoc, hand.getType(), hand);
                    session.placedIngredientsStands.put(ingredientName, standUUID);
                    ItemStack copy = hand.clone();
                    copy.setAmount(1);
                    session.placedIngredientItems.put(ingredientName, copy);

                    // Remove this ingredient’s label stand
                    UUID labelStandUUID = session.ingredientLabelStands.get(ingredientName);
                    removeEntityByUUID(labelStandUUID);
                    session.ingredientLabelStands.remove(ingredientName);

                    // Start spinning
                    BukkitTask spinTask = startSpinning(standUUID);
                    session.ingredientSpinTasks.put(ingredientName, spinTask);

                    // Re-lay out the remaining ingredient labels with NO gaps:
                    updateIngredientLabels(session);
                    // Check if all ingredients are now placed
                    if (session.placedIngredientsStands.size() == session.recipe.getIngredients().size()) {
                        // All placed! Update the main armor stand's name to "[LEFT CLICK] To Combine!"
                        ArmorStand mainStand = (ArmorStand) Bukkit.getEntity(session.mainArmorStandUUID);
                        if (mainStand != null && mainStand.isValid()) {
                            mainStand.setCustomName(ChatColor.GREEN + "[LEFT CLICK] To Combine!");
                        }
                    }

                    player.sendMessage(ChatColor.GREEN + ingredientName + " placed.");
                    return;
                } else {
                    player.sendMessage(ChatColor.RED + "This item is not required or already placed.");
                    logger.info("[CulinarySubsystem] Irrelevant item or all ingredients placed.");
                }
            }
        }

        // -- 3) Finalizing the recipe (LEFT_CLICK_BLOCK) remains the same --
        else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (session != null) {
                if (session.readyForPickup) {
                    event.setCancelled(true);
                    claimRecipe(session, player);
                } else if (session.finalized) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "This recipe is already cooking!");
                } else if (session.placedIngredientsStands.size() == session.recipe.getIngredients().size()) {
                    event.setCancelled(true);
                    logger.info("[CulinarySubsystem] Finalizing recipe " + session.recipe.getName());
                    startCooking(session, player);
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "Not all ingredients are placed yet!");
                    logger.info("[CulinarySubsystem] Not all ingredients placed for " + session.recipe.getName());
                }
            } else {
                logger.info("[CulinarySubsystem] No active recipe session for campfire at " + tableLoc);
            }
        }
    }

    private void beginRecipeSession(Player player, Location tableLoc, String locKey, CulinaryRecipe recipe, boolean consumeHeldItem) {
        if (player == null || tableLoc == null || locKey == null || recipe == null) {
            return;
        }
        logger.info("[CulinarySubsystem] Displaying recipe " + recipe.getName() + " at " + tableLoc);

        RecipeSession session = new RecipeSession(locKey, recipe, tableLoc);
        activeRecipeSessions.put(locKey, session);

        if (consumeHeldItem) {
            consumeItem(player, player.getInventory().getItemInMainHand(), 1);
        }

        Location mainLoc = tableLoc.clone().add(0.5, 0.7, 0.5);
        UUID mainStand = spawnInvisibleArmorStand(
                mainLoc,
                ChatColor.GOLD + recipe.getName(),
                Arrays.asList(ChatColor.YELLOW + "Ingredients:", "Sea Salt"),
                true
        );
        session.mainArmorStandUUID = mainStand;
        updateIngredientLabels(session);

        player.sendMessage(ChatColor.GREEN + "Recipe " + recipe.getName() + " displayed! Right-click with ingredients to place them, left-click to finalize.");
        player.sendMessage(ChatColor.DARK_GRAY + "Tip: Right-click with an empty hand to cancel before cooking.");
        if (!consumeHeldItem) {
            player.sendMessage(ChatColor.YELLOW + "Loaded directly from your catalogue selection.");
        }
    }

    private void clearRecipeSession(RecipeSession session, Player player, boolean dropIngredients) {
        if (session == null) {
            return;
        }

        if (session.cookTask != null) {
            session.cookTask.cancel();
            session.cookTask = null;
        }
        if (session.particleTask != null) {
            session.particleTask.cancel();
            session.particleTask = null;
        }
        if (session.resultSpinTask != null) {
            session.resultSpinTask.cancel();
            session.resultSpinTask = null;
        }

        for (BukkitTask task : session.ingredientSpinTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        session.ingredientSpinTasks.clear();

        for (UUID uuid : session.placedIngredientsStands.values()) {
            removeEntityByUUID(uuid);
        }
        session.placedIngredientsStands.clear();

        for (UUID uuid : session.ingredientLabelStands.values()) {
            removeEntityByUUID(uuid);
        }
        session.ingredientLabelStands.clear();

        removeEntityByUUID(session.timerStandUUID);
        session.timerStandUUID = null;
        removeEntityByUUID(session.resultStandUUID);
        session.resultStandUUID = null;
        removeEntityByUUID(session.mainArmorStandUUID);
        session.mainArmorStandUUID = null;

        if (dropIngredients && !session.placedIngredientItems.isEmpty()) {
            Location dropLocation = session.tableLocation.clone().add(0.5, 0.75, 0.5);
            World world = session.tableLocation.getWorld();
            if (world != null) {
                for (ItemStack stack : session.placedIngredientItems.values()) {
                    if (stack != null && stack.getType() != Material.AIR) {
                        world.dropItemNaturally(dropLocation, stack.clone());
                    }
                }
            }
        }
        session.placedIngredientItems.clear();

        activeRecipeSessions.remove(session.locationKey);
        session.readyForPickup = false;
        session.finalized = false;

        if (player != null) {
            player.sendMessage(ChatColor.YELLOW + "Cleared the active recipe and returned any ingredients.");
        }
    }


    public static ItemStack createRecipeItem(CulinaryRecipe recipe) {
        ItemStack item = new ItemStack(recipe.getRecipeItem(), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + recipe.getName() + " Recipe");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Ingredients:");
        for (String ing : recipe.getIngredients()) {
            lore.add(ChatColor.GRAY + "- " + ing);
        }
        lore.add(ChatColor.DARK_PURPLE + "Culinary Recipe");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    /**
     * Re-lays out the label stands (the named invisible armor stands)
     * so they appear top-to-bottom without gaps for unplaced ingredients.
     */
    private void updateIngredientLabels(RecipeSession session) {
        // Remove all existing label stands for this recipe session
        for (UUID standUUID : session.ingredientLabelStands.values()) {
            removeEntityByUUID(standUUID);
        }
        session.ingredientLabelStands.clear();

        // We'll anchor them relative to the main stand's location or the table location
        Location mainLoc = session.tableLocation.clone().add(0.5, 0.5, 0.5);

        // Start offset so labels appear under the main stand
        double offsetY = -0.25;

        // Loop in the recipe's original ingredient order
        for (String ing : session.recipe.getIngredients()) {
            // If it is NOT already placed, spawn a label for it
            if (!session.placedIngredientsStands.containsKey(ing)) {
                offsetY -= 0.3;
                Location ingLoc = mainLoc.clone().add(0, offsetY, 0);
                UUID ingStand = spawnInvisibleArmorStand(
                        ingLoc,
                        ChatColor.GRAY + ing,
                        null,  // no lore
                        false
                );
                session.ingredientLabelStands.put(ing, ingStand);
            }
        }
    }

    private ItemStack createOutputItem(CulinaryRecipe recipe) {
        ItemStack item = new ItemStack(recipe.getOutputMaterial());
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if(recipe.getName().equals("Slice of Cheese")){
            meta.setDisplayName(ChatColor.GOLD + recipe.getName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Crafted with:");
            for (String ing : recipe.getIngredients()) {
                lore.add(ChatColor.GRAY + "- " + ing);
            }
            lore.add(ChatColor.DARK_PURPLE + "Culinary Ingredient");
            meta.setLore(lore);
            item.setItemMeta(meta);

            return item;
        }
        meta.setDisplayName(ChatColor.GOLD + recipe.getName() + " (Culinary)");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Crafted with:");
        for (String ing : recipe.getIngredients()) {
            lore.add(ChatColor.GRAY + "- " + ing);
        }
        lore.add(ChatColor.DARK_PURPLE + "Culinary Delight");
        ChatColor color = recipe.getGroup().getColor();
        int amt = recipe.isFeast() ? 20 : 10;
        String groupName = recipe.getGroup().name().substring(0,1) + recipe.getGroup().name().substring(1).toLowerCase();
        String groupLine = color + "+" + amt + " " + groupName;
        lore.add(groupLine);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isRecipeItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        for (String line : item.getItemMeta().getLore()) {
            if (ChatColor.stripColor(line).equalsIgnoreCase("Ingredients:")) {
                return true;
            }
        }
        return false;
    }

    private CulinaryRecipe parseRecipeFromItem(ItemStack item) {
        logger.info("[CulinarySubsystem] parseRecipeFromItem: Parsing from item.");
        String recipeName = ChatColor.stripColor(item.getItemMeta().getDisplayName()).replace(" Recipe", "");
        CulinaryRecipe found = getRecipeByName(recipeName);
        if (found != null) {
            logger.info("[CulinarySubsystem] Found recipe '" + recipeName + "' in registry.");
            return found;
        }

        logger.warning("[CulinarySubsystem] Recipe '" + recipeName + "' not found in registry.");
        return null;
    }

    private CulinaryRecipe getRecipeByName(String name) {
        for (CulinaryRecipe r : recipeRegistry) {
            if (r.getName().equalsIgnoreCase(name)) return r;
        }
        for (CulinaryRecipe r : oceanicRecipes) {
            if (r.getName().equalsIgnoreCase(name)) return r;
        }
        return null;
    }

    private String matchIngredient(ItemStack item, List<String> neededIngredients, Set<String> alreadyPlaced) {
        if (item == null || item.getType() == Material.AIR) return null;

        // Get the name of the item in hand
        String handName = (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) ?
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).toLowerCase() :
                item.getType().toString().toLowerCase().replace("_", " ");

        // Iterate through the needed ingredients
        for (String ing : neededIngredients) {
            // Skip if the ingredient is already placed
            if (alreadyPlaced.contains(ing)) continue;

            // Normalize the ingredient name for comparison
            String ingLower = ing.toLowerCase();

            // Check for exact match or specific conditions
            if (handName.equals(ingLower)) {
                return ing;
            }

            // Handle cases like "Golden Carrot" vs "Carrot"
            if (ingLower.equals("carrot") && handName.equals("golden carrot")) {
                continue; // Skip if the ingredient is "Carrot" but the item is "Golden Carrot"
            }
        }

        return null; // No match found
    }

    /**
     * Attempts to fetch an ingredient from nearby shelves when the Portal Pantry
     * talent triggers.
     */
    private void consumeItem(Player player, ItemStack item, int amount) {
        player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
    }

    private void startCooking(RecipeSession session, Player player) {
        session.finalized = true;
        // Clean up ingredient stands and tasks
        for (UUID u : session.placedIngredientsStands.values()) {
            removeEntityByUUID(u);
        }
        for (BukkitTask t : session.ingredientSpinTasks.values()) {
            t.cancel();
        }
        for (UUID u : session.ingredientLabelStands.values()) {
            removeEntityByUUID(u);
        }
        session.placedIngredientsStands.clear();
        session.ingredientSpinTasks.clear();
        session.ingredientLabelStands.clear();
        session.placedIngredientItems.clear();

        int cookTime = session.recipe.getIngredients().size() * 10;
        if (session.recipe.getName().toLowerCase().contains("feast")) {
            cookTime += 20;
        }

        session.cookTimeRemaining = cookTime;

        ArmorStand mainStand = (ArmorStand) Bukkit.getEntity(session.mainArmorStandUUID);
        if (mainStand != null) {
            mainStand.setCustomName(ChatColor.GOLD + session.recipe.getName());
        }

        Location timerLoc = session.tableLocation.clone().add(0.5, 2.0, 0.5);
        ArmorStand timer = (ArmorStand) timerLoc.getWorld().spawnEntity(timerLoc, EntityType.ARMOR_STAND);
        timer.setInvisible(true);
        timer.setCustomNameVisible(true);
        timer.setGravity(false);
        timer.setInvulnerable(true);
        timer.setMarker(true);
        timer.setCustomName(ChatColor.YELLOW + "" + session.cookTimeRemaining + "s");
        session.timerStandUUID = timer.getUniqueId();

        session.particleTask = startCookingParticles(session.tableLocation);

        session.cookTask = new BukkitRunnable() {
            @Override
            public void run() {
                session.cookTimeRemaining--;
                Entity ent = Bukkit.getEntity(session.timerStandUUID);
                if (ent instanceof ArmorStand) {
                    ((ArmorStand) ent).setCustomName(ChatColor.YELLOW + "" + session.cookTimeRemaining + "s");
                }
                if (session.cookTimeRemaining <= 0) {
                    cancel();
                    finalizeRecipe(session, player);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void finalizeRecipe(RecipeSession session, Player player) {
        logger.info("[CulinarySubsystem] finalizeRecipe: " + session.recipe.getName() + " at " + session.tableLocation);

        if (session.cookTask != null) {
            session.cookTask.cancel();
        }

        for (BukkitTask task : session.ingredientSpinTasks.values()) {
            task.cancel();
        }

        for (UUID u : session.placedIngredientsStands.values()) {
            removeEntityByUUID(u);
        }
        for (UUID u : session.ingredientLabelStands.values()) {
            removeEntityByUUID(u);
        }

        if (session.particleTask != null) {
            session.particleTask.cancel();
        }

        removeEntityByUUID(session.timerStandUUID);
        session.placedIngredientItems.clear();

        ItemStack result = createOutputItem(session.recipe);

        ArmorStand mainStand = (ArmorStand) Bukkit.getEntity(session.mainArmorStandUUID);
        if (mainStand != null) {
            mainStand.setCustomName(ChatColor.GREEN + "LEFT CLICK to claim " + session.recipe.getName() + "!");
        }

        UUID resultStand = spawnResultAboveTable(session.tableLocation, result);
        session.resultStandUUID = resultStand;
        session.resultSpinTask = startSpinning(resultStand);
        session.readyForPickup = true;
        logger.info("[CulinarySubsystem] finalizeRecipe: Dish ready for pickup.");
    }

    private UUID spawnInvisibleArmorStand(Location loc, String displayName, List<String> lore, boolean marker) {
        logger.info("[CulinarySubsystem] spawnInvisibleArmorStand: Spawning stand at " + loc + ", Name=" + displayName);
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setCustomNameVisible(true);
        stand.setCustomName(displayName);
        stand.setGravity(false);
        stand.setMarker(marker);
        stand.setInvulnerable(true);
        return stand.getUniqueId();
    }

    private UUID spawnIngredientAboveTableRandom(Location tableLoc, Material mat, ItemStack ingredient) {
        if (ingredient == null || ingredient.getType() == Material.AIR) {
            logger.warning("[CulinarySubsystem] spawnIngredientAboveTableRandom: Invalid ingredient. Material: " + mat);
            return null;
        }
        double offsetX = (Math.random() - 0.5) * 0.6;
        double offsetZ = (Math.random() - 0.5) * 0.6;
        Location itemLoc = tableLoc.clone().add(0.5 + offsetX, 0.5, 0.5 + offsetZ);
        logger.info("[CulinarySubsystem] spawnIngredientAboveTableRandom: Spawning ingredient stand for " + mat + " at " + itemLoc);
        ArmorStand stand = (ArmorStand) itemLoc.getWorld().spawnEntity(itemLoc, EntityType.ARMOR_STAND);

        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setArms(true);
        ItemStack ingredientCopy = ingredient.clone();
        ingredientCopy.setAmount(1);

        stand.getEquipment().setItemInMainHand(ingredientCopy);
        stand.setCustomNameVisible(false);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));

        UUID uuid = stand.getUniqueId();
        Bukkit.getLogger().info("[CulinarySubsystem] spawnIngredientAboveTableRandom: Spawned item stand UUID=" + uuid);
        return uuid;
    }

    private UUID spawnResultAboveTable(Location tableLoc, ItemStack result) {
        Location itemLoc = tableLoc.clone().add(0.5, 1.0, 0.5);
        ArmorStand stand = (ArmorStand) itemLoc.getWorld().spawnEntity(itemLoc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setInvulnerable(true);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setArms(true);
        ItemStack copy = result.clone();
        copy.setAmount(1);
        stand.getEquipment().setItemInMainHand(copy);
        stand.setCustomNameVisible(false);
        stand.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
        return stand.getUniqueId();
    }

    private void claimRecipe(RecipeSession session, Player player) {
        if (!session.readyForPickup) return;

        if (session.resultSpinTask != null) {
            session.resultSpinTask.cancel();
        }

        removeEntityByUUID(session.resultStandUUID);
        removeEntityByUUID(session.mainArmorStandUUID);

        ItemStack result = createOutputItem(session.recipe);
        int yield = 1 + random.nextInt(3);
        for (int i = 0; i < yield; i++) {
            session.tableLocation.getWorld().dropItem(session.tableLocation.clone().add(0.5, 1, 0.5), result.clone());
        }

        if (player != null) {
            player.sendMessage(ChatColor.GREEN + "You cooked " + session.recipe.getName() + "!");
        }

        activeRecipeSessions.remove(session.locationKey);
        session.readyForPickup = false;
    }

    private BukkitTask startSpinning(UUID standUUID) {
        logger.info("[CulinarySubsystem] startSpinning: Starting spin task for stand " + standUUID);
        return new BukkitRunnable() {
            double angle = 0.0;

            @Override
            public void run() {
                Entity e = Bukkit.getEntity(standUUID);
                if (e == null || !(e instanceof ArmorStand) || !e.isValid()) {
                    cancel();
                    logger.warning("[CulinarySubsystem] Spinning task cancelled: Stand " + standUUID + " not found or invalid.");
                    return;
                }
                ArmorStand stand = (ArmorStand) e;
                angle += 5.0;
                if (angle > 360.0) angle -= 360.0;
                Location loc = stand.getLocation();
                loc.setYaw((float) angle);
                stand.teleport(loc);
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private BukkitTask startCookingParticles(Location loc) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                loc.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                        loc.clone().add(0.5, 1.0, 0.5),
                        4, 0.2, 0.2, 0.2, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    public List<ItemStack> getOceanicRecipeItems() {
        List<ItemStack> items = new ArrayList<>();
        for (CulinaryRecipe r : oceanicRecipes) {
            items.add(createRecipeItem(r));
        }
        return items;
    }
    /**
     * Returns the *crafted* output ItemStack for the given recipe name,
     * searching both the normal and the exclusive (oceanic) registries.
     */
    public ItemStack getRecipeOutputByName(String recipeName) {
        // search the public recipes
        for (CulinaryRecipe r : recipeRegistry) {
            if (r.getName().equalsIgnoreCase(recipeName)) {
                return createOutputItem(r);
            }
        }
        // search the bartender-only recipes
        for (CulinaryRecipe r : oceanicRecipes) {
            if (r.getName().equalsIgnoreCase(recipeName)) {
                return createOutputItem(r);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Persistence helpers
    // ------------------------------------------------------------------
    private String toLocKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private Location fromLocKey(String key) {
        String[] p = key.split(":");
        World w = Bukkit.getWorld(p[0]);
        int x = Integer.parseInt(p[1]);
        int y = Integer.parseInt(p[2]);
        int z = Integer.parseInt(p[3]);
        return new Location(w, x, y, z);
    }

    private void loadAllSessions() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : dataConfig.getKeys(false)) {
            String recipeName = dataConfig.getString(key + ".recipe", null);
            int timeLeft = dataConfig.getInt(key + ".timeLeft", 0);
            boolean finalized = dataConfig.getBoolean(key + ".finalized", false);
            ConfigurationSection ingSec = dataConfig.getConfigurationSection(key + ".placedIngredients");
            Map<String, ItemStack> placed = new HashMap<>();
            if (ingSec != null) {
                for (String ing : ingSec.getKeys(false)) {
                    ItemStack it = ingSec.getItemStack(ing);
                    if (it != null) placed.put(ing, it);
                }
            }
            CulinaryRecipe recipe = getRecipeByName(recipeName);
            if (recipe == null) continue;
            Location loc = fromLocKey(key);
            RecipeSession session = new RecipeSession(key, recipe, loc);
            session.cookTimeRemaining = timeLeft;
            session.finalized = finalized;
            session.placedIngredientItems.putAll(placed);

            if (session.finalized) {
                summonCookingStands(session);
                // Always resume cooking for finalized sessions so that the
                // timer task can properly finalize dishes even after a reload.
                resumeCooking(session);
            } else {
                summonSessionStands(session);
            }
            activeRecipeSessions.put(key, session);
        }
        logger.info("[CulinarySubsystem] Loaded " + activeRecipeSessions.size() + " cooking session(s).");
    }

    private void saveAllSessions() {
        for (String key : dataConfig.getKeys(false)) {
            dataConfig.set(key, null);
        }
        for (Map.Entry<String, RecipeSession> e : activeRecipeSessions.entrySet()) {
            RecipeSession s = e.getValue();
            dataConfig.set(e.getKey() + ".recipe", s.recipe.getName());
            dataConfig.set(e.getKey() + ".timeLeft", s.cookTimeRemaining);
            dataConfig.set(e.getKey() + ".finalized", s.finalized);
            for (Map.Entry<String, ItemStack> pi : s.placedIngredientItems.entrySet()) {
                dataConfig.set(e.getKey() + ".placedIngredients." + pi.getKey(), pi.getValue());
            }
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void summonSessionStands(RecipeSession session) {
        UUID main = spawnInvisibleArmorStand(session.tableLocation.clone().add(0.5, 0.7, 0.5),
                ChatColor.GOLD + session.recipe.getName(),
                Arrays.asList(ChatColor.YELLOW + "Ingredients:", "Sea Salt"),
                true);
        session.mainArmorStandUUID = main;
        for (Map.Entry<String, ItemStack> e : session.placedIngredientItems.entrySet()) {
            UUID stand = spawnIngredientAboveTableRandom(session.tableLocation, e.getValue().getType(), e.getValue());
            session.placedIngredientsStands.put(e.getKey(), stand);
            BukkitTask spin = startSpinning(stand);
            session.ingredientSpinTasks.put(e.getKey(), spin);
        }
        updateIngredientLabels(session);
    }

    private void summonCookingStands(RecipeSession session) {
        UUID main = spawnInvisibleArmorStand(session.tableLocation.clone().add(0.5, 0.7, 0.5),
                ChatColor.GOLD + session.recipe.getName(),
                Arrays.asList(ChatColor.YELLOW + "Cooking...", "Sea Salt"),
                true);
        session.mainArmorStandUUID = main;
    }

    private void resumeCooking(RecipeSession session) {
        session.finalized = true;
        // If we are resuming after a reload and the timer has already
        // elapsed, force at least 1 second so finalizeRecipe is invoked.
        if (session.cookTimeRemaining <= 0) {
            session.cookTimeRemaining = 1;
        }
        Location timerLoc = session.tableLocation.clone().add(0.5, 2.0, 0.5);
        ArmorStand timer = (ArmorStand) timerLoc.getWorld().spawnEntity(timerLoc, EntityType.ARMOR_STAND);
        timer.setInvisible(true);
        timer.setCustomNameVisible(true);
        timer.setGravity(false);
        timer.setInvulnerable(true);
        timer.setMarker(true);
        timer.setCustomName(ChatColor.YELLOW + "" + session.cookTimeRemaining + "s");
        session.timerStandUUID = timer.getUniqueId();

        session.particleTask = startCookingParticles(session.tableLocation);

        session.cookTask = new BukkitRunnable() {
            @Override
            public void run() {
                session.cookTimeRemaining--;
                Entity ent = Bukkit.getEntity(session.timerStandUUID);
                if (ent instanceof ArmorStand) {
                    ((ArmorStand) ent).setCustomName(ChatColor.YELLOW + "" + session.cookTimeRemaining + "s");
                }
                if (session.cookTimeRemaining <= 0) {
                    cancel();
                    finalizeRecipe(session, null);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }


    private void removeEntityByUUID(UUID uuid) {
        if (uuid == null) return;
        logger.info("[CulinarySubsystem] removeEntityByUUID: Removing entity " + uuid);
        Entity e = Bukkit.getEntity(uuid);
        if (e != null) {
            e.remove();
            logger.info("[CulinarySubsystem] removeEntityByUUID: Entity " + uuid + " removed.");
        } else {
            logger.warning("[CulinarySubsystem] removeEntityByUUID: Entity " + uuid + " not found.");
        }
    }

    // Placeholder Classes

    public static class CulinaryRecipe {
        private final Material recipeItem;
        private final Material outputMaterial;
        private final String name;
        private final List<String> ingredients;
        private final int xpReward;
        private final FoodGroup group;
        private final boolean feast;

        public CulinaryRecipe(Material recipeItem, Material outputMaterial, String name,
                              List<String> ingredients, int xpReward,
                              FoodGroup group, boolean feast) {
            this.recipeItem = recipeItem;
            this.outputMaterial = outputMaterial;
            this.name = name;
            this.ingredients = ingredients;
            this.xpReward = xpReward;
            this.group = group;
            this.feast = feast;
        }

        public Material getRecipeItem() { return recipeItem; }
        public Material getOutputMaterial() { return outputMaterial; }
        public String getName() { return name; }
        public List<String> getIngredients() { return ingredients; }
        public int getXpReward() { return xpReward; }
        public FoodGroup getGroup() { return group; }
        public ChatColor getGroupColor() { return group.getColor(); }
        public String getGroupDisplayName() {
            String base = group.name().toLowerCase(Locale.ENGLISH);
            return Character.toUpperCase(base.charAt(0)) + base.substring(1);
        }
        public boolean isFeast() { return feast; }
    }

    public static class RecipeSession {
        public final String locationKey;
        public CulinaryRecipe recipe;
        public Location tableLocation;

        public UUID mainArmorStandUUID;
        public Map<String, UUID> ingredientLabelStands = new HashMap<>();
        public Map<String, UUID> placedIngredientsStands = new HashMap<>();
        public Map<String, ItemStack> placedIngredientItems = new HashMap<>();
        public Map<String, BukkitTask> ingredientSpinTasks = new HashMap<>();
        public int cookTimeRemaining = 0;
        public BukkitTask cookTask;
        public UUID timerStandUUID;
        public BukkitTask particleTask;
        public boolean finalized = false;

        // New fields for completed dishes waiting to be claimed
        public boolean readyForPickup = false;
        public UUID resultStandUUID;
        public BukkitTask resultSpinTask;

        public RecipeSession(String locKey, CulinaryRecipe recipe, Location tableLocation) {
            this.locationKey = locKey;
            this.recipe = recipe;
            this.tableLocation = tableLocation.clone();
        }
    }
}