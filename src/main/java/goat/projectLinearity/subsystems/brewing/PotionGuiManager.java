package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.BrewType;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionDefinition;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionIngredient;
import goat.projectLinearity.subsystems.brewing.PotionRegistry.PotionRecipe;
import goat.projectLinearity.subsystems.enchanting.EnchantedManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the potion catalogue menu and the brewing interface.
 */
public final class PotionGuiManager implements Listener {

    private static final String PREFIX = ChatColor.DARK_AQUA + "[Potions] " + ChatColor.GRAY;
    private static final String TITLE_CATALOGUE = ChatColor.DARK_GREEN + "Custom Potion Catalogue";
    private static final String TITLE_BREW = ChatColor.DARK_AQUA + "%s " + ChatColor.GRAY + "(%s)";
    private static final ChatColor UNDISCOVERED_COLOR = ChatColor.DARK_GRAY;

    private static final int CATALOGUE_SIZE = 45;
    private static final int[] CATALOGUE_POTION_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int SLOT_PREV_PAGE = 36;
    private static final int SLOT_PAGE_INFO = 40;
    private static final int SLOT_NEXT_PAGE = 44;
    private static final String ACTION_PREVIOUS_PAGE = "__prev";
    private static final String ACTION_NEXT_PAGE = "__next";

    private static final int BREW_SIZE = 54;
    private static final int SLOT_INFO_PRIMARY = 4;
    private static final int SLOT_INFO_SECONDARY = 40;
    private static final int SLOT_BOTTLE = 10;
    private static final int SLOT_ENZYME = 16;
    private static final int SLOT_MAIN = 22;
    private static final int SLOT_BREW = 49;
    private static final int SLOT_TOGGLE = 53;
    private static final int SLOT_BACK = 45;
    private static final int[] SLOT_BOOKS = {28, 37, 46};
    private static final int[] DECOR_WHITE = {0, 1, 2, 9, 11, 18, 19, 20};
    private static final int[] DECOR_YELLOW = {6, 7, 8, 15, 17, 24, 25, 26};
    private static final int[] DECOR_PURPLE = {12, 13, 14, 21, 23, 30, 31, 32};

    private final JavaPlugin plugin;
    private final EnchantedManager enchantedManager;

    private final ItemStack filler;
    private final ItemStack infoPane;
    private final ItemStack backButton;

    public PotionGuiManager(ProjectLinearity plugin, EnchantedManager enchantedManager) {
        this.plugin = plugin;
        this.enchantedManager = enchantedManager;
        this.filler = pane(Material.BLACK_STAINED_GLASS_PANE, " ");
        this.infoPane = pane(Material.LIGHT_BLUE_STAINED_GLASS_PANE, ChatColor.AQUA + "Recipe Guide");
        this.backButton = pane(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Back to Catalogue",
                ChatColor.GRAY + "Drop ingredients safely and return.");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // --- Catalogue ---

    public void openCatalogue(Player player) {
        openCatalogue(player, 0);
    }

    private void openCatalogue(Player player, int page) {
        if (player == null) return;

        List<PotionDefinition> definitions = new ArrayList<>(PotionRegistry.getAll());
        int perPage = CATALOGUE_POTION_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil(definitions.size() / (double) perPage));
        int clampedPage = Math.max(0, Math.min(page, totalPages - 1));

        CatalogueHolder holder = new CatalogueHolder(player.getUniqueId(), clampedPage, totalPages);
        Inventory inventory = Bukkit.createInventory(holder, CATALOGUE_SIZE, TITLE_CATALOGUE);
        fill(inventory, filler);

        int start = clampedPage * perPage;
        int end = Math.min(start + perPage, definitions.size());

        for (int index = start; index < end; index++) {
            PotionDefinition definition = definitions.get(index);
            int slot = CATALOGUE_POTION_SLOTS[index - start];
            ItemStack icon = createCatalogueIcon(player, definition);
            inventory.setItem(slot, icon);
            holder.slotMapping.put(slot, definition.getId());
        }

        if (totalPages > 1) {
            if (clampedPage > 0) {
                ItemStack previous = buildPageButton(false, clampedPage, totalPages);
                inventory.setItem(SLOT_PREV_PAGE, previous);
                holder.slotMapping.put(SLOT_PREV_PAGE, ACTION_PREVIOUS_PAGE);
            }
            if (clampedPage < totalPages - 1) {
                ItemStack next = buildPageButton(true, clampedPage, totalPages);
                inventory.setItem(SLOT_NEXT_PAGE, next);
                holder.slotMapping.put(SLOT_NEXT_PAGE, ACTION_NEXT_PAGE);
            }
        }
        inventory.setItem(SLOT_PAGE_INFO, buildPageIndicator(clampedPage, totalPages));

        player.openInventory(inventory);
        player.sendMessage(PREFIX + "Select a potion recipe. " + ChatColor.GRAY + "Page "
                + ChatColor.YELLOW + (clampedPage + 1) + ChatColor.GRAY + "/" + ChatColor.YELLOW + totalPages
                + ChatColor.GRAY + ". Left-click for Overworld, right-click for Nether.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
    }

    private ItemStack createCatalogueIcon(Player player, PotionDefinition definition) {
        boolean discovered = PotionRegistry.hasDiscovered(player, definition);
        String color = discovered ? definition.getAccentColor() : UNDISCOVERED_COLOR.toString();
        ItemStack icon = new ItemStack(Material.POTION);
        ItemMeta baseMeta = icon.getItemMeta();
        PotionMeta meta = baseMeta instanceof PotionMeta ? (PotionMeta) baseMeta : null;
        if (meta != null) {
            Color tint = discovered ? definition.getAccentBukkitColor() : Color.fromRGB(10, 10, 10);
            meta.setColor(tint);
            meta.clearCustomEffects();
            meta.setDisplayName(color + definition.getDisplayName());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Status: " + (discovered ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Unknown"),
                    ChatColor.YELLOW + "Left-click: " + ChatColor.GRAY + "Overworld recipe",
                    ChatColor.RED + "Right-click: " + ChatColor.GRAY + "Nether recipe",
                    ChatColor.DARK_GRAY + "Hint: Brew once to reveal its true color."
            ));
            icon.setItemMeta(meta);
        } else if (baseMeta != null) {
            baseMeta.setDisplayName(color + definition.getDisplayName());
            baseMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Status: " + (discovered ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Unknown"),
                    ChatColor.YELLOW + "Left-click: " + ChatColor.GRAY + "Overworld recipe",
                    ChatColor.RED + "Right-click: " + ChatColor.GRAY + "Nether recipe",
                    ChatColor.DARK_GRAY + "Hint: Brew once to reveal its true color."
            ));
            icon.setItemMeta(baseMeta);
        }
        return icon;
    }

    // --- Brewing ---

    private void openBrewing(Player player, PotionDefinition definition, BrewType type, int originPage) {
        if (player == null || definition == null || type == null) return;
        BrewingHolder holder = new BrewingHolder(player.getUniqueId(), definition, type, originPage);
        String title = String.format(TITLE_BREW, definition.getDisplayName(), type == BrewType.NETHER ? "Nether" : "Overworld");
        Inventory inventory = Bukkit.createInventory(holder, BREW_SIZE, title);
        fill(inventory, filler);

        PotionRecipe recipe = definition.getRecipe(type);
        decorate(inventory, recipe);

        inventory.setItem(SLOT_INFO_PRIMARY, recipeGuide(definition, type));
        inventory.setItem(SLOT_INFO_SECONDARY, enchantGuide());

        inventory.setItem(SLOT_TOGGLE, buildSplashToggle(holder.isSplash()));
        inventory.setItem(SLOT_BREW, buildBrewButton());
        inventory.setItem(SLOT_BACK, backButton.clone());

        clearFunctionalSlots(inventory);
        player.openInventory(inventory);
        player.sendMessage(PREFIX + "Brewing " + definition.getAccentColor() + definition.getDisplayName()
                + ChatColor.GRAY + " [" + type.displayName() + ChatColor.GRAY + "]");
        player.sendMessage(PREFIX + ChatColor.GRAY + "Place exactly one ingredient per slot. Invalid items are refused.");
        player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.6f, 1.15f);
    }

    private void decorate(Inventory inventory, PotionRecipe recipe) {
        applyDecor(inventory, DECOR_WHITE, Material.WHITE_STAINED_GLASS_PANE,
                ChatColor.WHITE + "Bottle Slot",
                ChatColor.GRAY + "Requires: " + recipe.getBottle().friendlyName());
        applyDecor(inventory, DECOR_YELLOW, Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.GOLD + "Enzyme Slot",
                ChatColor.GRAY + "Requires: " + recipe.getEnzyme().friendlyName());
        applyDecor(inventory, DECOR_PURPLE, Material.PURPLE_STAINED_GLASS_PANE,
                ChatColor.LIGHT_PURPLE + "Core Slot",
                ChatColor.GRAY + "Requires: " + recipe.getMain().friendlyName());
    }

    private void clearFunctionalSlots(Inventory inventory) {
        inventory.setItem(SLOT_BOTTLE, null);
        inventory.setItem(SLOT_ENZYME, null);
        inventory.setItem(SLOT_MAIN, null);
        for (int slot : SLOT_BOOKS) {
            inventory.setItem(slot, null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBrewingStandInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.BREWING_STAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("consegrity.potions")) {
            player.sendMessage(PREFIX + ChatColor.RED + "You need permission to operate the experimental brewing station.");
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> openCatalogue(player));
    }

    private ItemStack recipeGuide(PotionDefinition def, BrewType type) {
        ItemStack item = infoPane.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + def.getDisplayName());
            meta.setLore(PotionRegistry.describeRecipe(def.getRecipe(type)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack enchantGuide() {
        ItemStack item = pane(Material.BOOKSHELF, ChatColor.LIGHT_PURPLE + "Enchantment Boost",
                ChatColor.GRAY + "Drop up to three custom enchanted books",
                ChatColor.GRAY + "to pre-upgrade the potion.");
        return item;
    }

    private ItemStack buildBrewButton() {
        return pane(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Brew Potion",
                ChatColor.GRAY + "Consumes valid ingredients from the slots.",
                ChatColor.DARK_GRAY + "Outputs directly to your inventory.");
    }

    private ItemStack buildSplashToggle(boolean enabled) {
        String status = enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
        return pane(Material.ORANGE_STAINED_GLASS_PANE, ChatColor.GOLD + "Toggle Splash Variant",
                ChatColor.GRAY + "Current: " + status,
                ChatColor.DARK_GRAY + "Splash variants excel at support.");
    }

    private ItemStack buildPageButton(boolean forward, int currentPage, int totalPages) {
        int targetPage = forward ? currentPage + 1 : currentPage - 1;
        Material material = forward ? Material.SPECTRAL_ARROW : Material.ARROW;
        String title = forward ? ChatColor.GREEN + "Next Page" : ChatColor.GREEN + "Previous Page";
        String descriptor = forward ? "next" : "previous";
        return pane(material, title,
                ChatColor.GRAY + "Go to page " + ChatColor.YELLOW + (targetPage + 1)
                        + ChatColor.GRAY + "/" + ChatColor.YELLOW + totalPages,
                ChatColor.DARK_GRAY + "View the " + descriptor + " recipes.");
    }

    private ItemStack buildPageIndicator(int currentPage, int totalPages) {
        return pane(Material.PAPER, ChatColor.AQUA + "Catalogue Page",
                ChatColor.GRAY + "Viewing page " + ChatColor.YELLOW + (currentPage + 1)
                        + ChatColor.GRAY + "/" + ChatColor.YELLOW + totalPages);
    }

    private ItemStack pane(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void fill(Inventory inventory, ItemStack item) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, item.clone());
        }
    }

    private void applyDecor(Inventory inventory, int[] slots, Material material, String title, String lore) {
        ItemStack pane = pane(material, title, lore);
        for (int slot : slots) {
            inventory.setItem(slot, pane.clone());
        }
    }

    // --- Event Handling ---

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (holder instanceof BrewingHolder brewingHolder) {
            Player player = (Player) event.getPlayer();
            returnResidualItems(player, top, brewingHolder);
        }
    }

    private void returnResidualItems(Player player, Inventory inventory, BrewingHolder holder) {
        boolean droppedAny = false;
        for (int slot : new int[]{SLOT_BOTTLE, SLOT_ENZYME, SLOT_MAIN}) {
            droppedAny |= returnStack(player, inventory.getItem(slot));
            inventory.setItem(slot, null);
        }
        for (int slot : SLOT_BOOKS) {
            droppedAny |= returnStack(player, inventory.getItem(slot));
            inventory.setItem(slot, null);
        }
        if (droppedAny) {
            player.sendMessage(PREFIX + "Unspent ingredients returned.");
        }
    }

    private boolean returnStack(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        PlayerInventory inv = player.getInventory();
        Map<Integer, ItemStack> overflow = inv.addItem(stack.clone());
        if (!overflow.isEmpty()) {
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
        player.updateInventory();
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (holder instanceof BrewingHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < top.getSize()) {
                    event.setCancelled(true);
                    ((Player) event.getWhoClicked()).sendMessage(PREFIX + "Drag actions are disabled inside the brewing slots.");
                    return;
                }
            }
        } else if (holder instanceof CatalogueHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < top.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        InventoryHolder holder = top.getHolder();

        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (holder instanceof CatalogueHolder catalogueHolder) {
            handleCatalogueClick(event, player, catalogueHolder);
        } else if (holder instanceof BrewingHolder brewingHolder) {
            handleBrewingClick(event, player, brewingHolder);
        }
    }

    private void handleCatalogueClick(InventoryClickEvent event, Player player, CatalogueHolder holder) {
        InventoryView view = event.getView();
        int rawSlot = event.getRawSlot();
        if (rawSlot >= view.getTopInventory().getSize()) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (!holder.isOwner(player.getUniqueId())) {
            return;
        }
        String action = holder.slotMapping.get(rawSlot);
        if (action == null) {
            return;
        }
        if (ACTION_PREVIOUS_PAGE.equals(action)) {
            openCatalogue(player, holder.getPage() - 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
            return;
        }
        if (ACTION_NEXT_PAGE.equals(action)) {
            openCatalogue(player, holder.getPage() + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.8f);
            return;
        }

        PotionDefinition definition = PotionRegistry.getById(action).orElse(null);
        if (definition == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "That recipe is unavailable.");
            return;
        }

        if (event.getClick() == ClickType.LEFT) {
            openBrewing(player, definition, BrewType.OVERWORLD, holder.getPage());
        } else if (event.getClick() == ClickType.RIGHT) {
            openBrewing(player, definition, BrewType.NETHER, holder.getPage());
        } else {
            player.sendMessage(PREFIX + ChatColor.RED + "Use left-click or right-click to choose a recipe variant.");
        }
    }

    private void handleBrewingClick(InventoryClickEvent event, Player player, BrewingHolder holder) {
        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();
        int rawSlot = event.getRawSlot();
        int topSize = top.getSize();

        if (!holder.isOwner(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(PREFIX + ChatColor.RED + "You're peeking in someone else's station!");
            return;
        }

        if (rawSlot < topSize) {
            event.setCancelled(true);
            if (rawSlot == SLOT_BREW) {
                attemptBrew(player, view, holder);
                return;
            }
            if (rawSlot == SLOT_TOGGLE) {
                holder.toggleSplash();
                top.setItem(SLOT_TOGGLE, buildSplashToggle(holder.isSplash()));
                player.sendMessage(PREFIX + "Splash variant " + (holder.isSplash() ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled") + ChatColor.GRAY + ".");
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, holder.isSplash() ? 1.4f : 0.8f);
                return;
            }
            if (rawSlot == SLOT_BACK) {
                returnAndReopenCatalogue(player, top, holder);
                return;
            }
            if (isIngredientSlot(rawSlot)) {
                handleIngredientSlotClick(event, player, top, holder, rawSlot);
                return;
            }
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.2f, 0.6f);
            return;
        }

        if (event.isShiftClick()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) {
                event.setCancelled(true);
                return;
            }
            event.setCancelled(true);
            shiftIntoBrewingSlots(player, view, holder, clicked, event.getSlot());
        }
    }

    private void returnAndReopenCatalogue(Player player, Inventory top, BrewingHolder holder) {
        returnResidualItems(player, top, holder);
        Bukkit.getScheduler().runTask(plugin, () -> openCatalogue(player, holder.getCataloguePage()));
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.3f);
    }

    private void shiftIntoBrewingSlots(Player player, InventoryView view, BrewingHolder holder, ItemStack stack, int sourceSlot) {
        PotionRecipe recipe = holder.definition.getRecipe(holder.brewType);
        Inventory top = view.getTopInventory();
        ItemStack single = stack.clone();
        single.setAmount(1);

        int targetSlot = -1;
        if (recipe.getBottle().matches(stack) && isSlotEmpty(top, SLOT_BOTTLE)) {
            targetSlot = SLOT_BOTTLE;
        } else if (recipe.getEnzyme().matches(stack) && isSlotEmpty(top, SLOT_ENZYME)) {
            targetSlot = SLOT_ENZYME;
        } else if (recipe.getMain().matches(stack) && isSlotEmpty(top, SLOT_MAIN)) {
            targetSlot = SLOT_MAIN;
        } else if (isEnchantBook(stack)) {
            targetSlot = firstEmptyBookSlot(top);
        }

        if (targetSlot == -1) {
            player.sendMessage(PREFIX + ChatColor.RED + "That item doesn't fit any open slots.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.2f);
            return;
        }

        top.setItem(targetSlot, single);
        decrementSourceStack(player, sourceSlot);
        player.updateInventory();
        player.sendMessage(PREFIX + ChatColor.GREEN + "Shift-click moved 1x " + describe(single) + ChatColor.GRAY + " into the station.");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.5f, 1.4f);
    }

    private void decrementSourceStack(Player player, int slot) {
        PlayerInventory inv = player.getInventory();
        ItemStack source = inv.getItem(slot);
        if (source == null || source.getType() == Material.AIR) {
            return;
        }
        if (source.getAmount() <= 1) {
            inv.setItem(slot, null);
        } else {
            source.setAmount(source.getAmount() - 1);
            inv.setItem(slot, source);
        }
    }

    private void handleIngredientSlotClick(InventoryClickEvent event, Player player, Inventory top, BrewingHolder holder, int slot) {
        PotionRecipe recipe = holder.definition.getRecipe(holder.brewType);
        boolean bookSlot = isBookSlot(slot);
        PotionIngredient expected = bookSlot ? null : expectedIngredient(recipe, slot);
        if (!bookSlot && expected == null) {
            player.sendMessage(PREFIX + ChatColor.RED + "That slot is not available.");
            return;
        }

        ItemStack current = top.getItem(slot);
        ItemStack cursor = event.getCursor();

        if ((cursor == null || cursor.getType() == Material.AIR) && current != null && current.getType() != Material.AIR) {
            top.setItem(slot, null);
            event.getView().setCursor(current);
            player.sendMessage(PREFIX + "Removed " + describe(current) + ChatColor.GRAY + " from slot.");
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.5f, 1.0f);
            return;
        }

        if (cursor == null || cursor.getType() == Material.AIR) {
            return;
        }

        ItemStack single = cursor.clone();
        single.setAmount(1);

        boolean matches = bookSlot ? isEnchantBook(single) : expected.matches(single);

        if (!matches) {
            String requirement = bookSlot ? "custom enchanted books" : expected.friendlyName();
            player.sendMessage(PREFIX + ChatColor.RED + "This slot accepts only " + requirement + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.2f);
            return;
        }

        if (current != null && current.getType() != Material.AIR) {
            player.sendMessage(PREFIX + ChatColor.RED + "Remove the existing item first.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.1f);
            return;
        }

        top.setItem(slot, single);
        adjustCursor(event, player, cursor);
        player.sendMessage(PREFIX + ChatColor.GREEN + "Placed 1x " + describe(single) + ChatColor.GRAY + ".");
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.5f, 1.4f);
    }

    private void adjustCursor(InventoryClickEvent event, Player player, ItemStack cursor) {
        int amount = cursor.getAmount();
        if (amount <= 1) {
            event.getView().setCursor(null);
        } else {
            cursor.setAmount(amount - 1);
            event.getView().setCursor(cursor);
        }
        player.updateInventory();
    }

    private PotionIngredient expectedIngredient(PotionRecipe recipe, int slot) {
        if (slot == SLOT_BOTTLE) return recipe.getBottle();
        if (slot == SLOT_ENZYME) return recipe.getEnzyme();
        if (slot == SLOT_MAIN) return recipe.getMain();
        return null;
    }

    private void attemptBrew(Player player, InventoryView view, BrewingHolder holder) {
        Inventory top = view.getTopInventory();
        PotionRecipe recipe = holder.definition.getRecipe(holder.brewType);

        ItemStack bottle = top.getItem(SLOT_BOTTLE);
        ItemStack enzyme = top.getItem(SLOT_ENZYME);
        ItemStack main = top.getItem(SLOT_MAIN);

        if (!recipe.getBottle().matches(bottle)) {
            warnInvalid(player, "bottle", recipe.getBottle());
            return;
        }
        if (!recipe.getEnzyme().matches(enzyme)) {
            warnInvalid(player, "enzyme", recipe.getEnzyme());
            return;
        }
        if (!recipe.getMain().matches(main)) {
            warnInvalid(player, "core", recipe.getMain());
            return;
        }

        int enchantCount = 0;
        for (int slot : SLOT_BOOKS) {
            ItemStack book = top.getItem(slot);
            if (book == null || book.getType() == Material.AIR) continue;
            if (!isEnchantBook(book)) {
                player.sendMessage(PREFIX + ChatColor.RED + "Only custom enchanted books work here.");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 0.8f);
                return;
            }
            enchantCount++;
        }

        top.setItem(SLOT_BOTTLE, null);
        top.setItem(SLOT_ENZYME, null);
        top.setItem(SLOT_MAIN, null);
        for (int slot : SLOT_BOOKS) {
            top.setItem(slot, null);
        }

        ItemStack result = PotionRegistry.createResultItem(holder.definition, holder.brewType, holder.isSplash(), enchantCount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(result);
        overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.updateInventory();

        PotionRegistry.markDiscovered(player, holder.definition, holder.brewType);

        player.sendMessage(PREFIX + ChatColor.GREEN + "Brewed " + holder.definition.getAccentColor()
                + holder.definition.getDisplayName() + ChatColor.GRAY + " (" + holder.brewType.displayName() + ChatColor.GRAY + ").");
        if (enchantCount > 0) {
            player.sendMessage(PREFIX + ChatColor.LIGHT_PURPLE + "Enchantment boost: " + enchantCount + " book(s) consumed.");
        }
        if (holder.isSplash()) {
            player.sendMessage(PREFIX + ChatColor.GOLD + "Splash variant crafted.");
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        Bukkit.getScheduler().runTask(plugin, player::closeInventory);
    }

    private void warnInvalid(Player player, String slotName, PotionIngredient requirement) {
        player.sendMessage(PREFIX + ChatColor.RED + "The " + slotName + " slot requires " + requirement.friendlyName() + ".");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 0.6f);
    }

    private boolean isIngredientSlot(int slot) {
        if (slot == SLOT_BOTTLE || slot == SLOT_ENZYME || slot == SLOT_MAIN) return true;
        for (int bookSlot : SLOT_BOOKS) {
            if (bookSlot == slot) return true;
        }
        return false;
    }

    private boolean isEnchantBook(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return enchantedManager.isCustomEnchantedBook(stack);
    }

    private int firstEmptyBookSlot(Inventory top) {
        for (int slot : SLOT_BOOKS) {
            ItemStack item = top.getItem(slot);
            if (item == null || item.getType() == Material.AIR) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isSlotEmpty(Inventory top, int slot) {
        ItemStack item = top.getItem(slot);
        return item == null || item.getType() == Material.AIR;
    }

    private String describe(ItemStack stack) {
        if (stack == null) return "item";
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }
        return stack.getType().name().toLowerCase().replace('_', ' ');
    }

    private boolean isBookSlot(int slot) {
        for (int candidate : SLOT_BOOKS) {
            if (candidate == slot) {
                return true;
            }
        }
        return false;
    }

    // --- Holder types ---

    private static final class CatalogueHolder implements InventoryHolder {
        private final UUID owner;
        private final int page;
        private final int totalPages;
        private final Map<Integer, String> slotMapping = new HashMap<>();

        private CatalogueHolder(UUID owner, int page, int totalPages) {
            this.owner = owner;
            this.page = page;
            this.totalPages = totalPages;
        }

        private boolean isOwner(UUID uuid) {
            return owner.equals(uuid);
        }

        private int getPage() {
            return page;
        }

        @SuppressWarnings("unused")
        private int getTotalPages() {
            return totalPages;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class BrewingHolder implements InventoryHolder {
        private final UUID owner;
        private final PotionDefinition definition;
        private final BrewType brewType;
        private final int cataloguePage;
        private boolean splash;

        private BrewingHolder(UUID owner, PotionDefinition definition, BrewType brewType, int cataloguePage) {
            this.owner = owner;
            this.definition = definition;
            this.brewType = brewType;
            this.cataloguePage = cataloguePage;
        }

        private boolean isOwner(UUID uuid) {
            return owner.equals(uuid);
        }

        private void toggleSplash() {
            splash = !splash;
        }

        private boolean isSplash() {
            return splash;
        }

        private int getCataloguePage() {
            return cataloguePage;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
