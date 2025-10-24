package goat.projectLinearity.subsystems.culinary;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CulinaryCatalogueManager implements Listener {

    private static final String PREFIX = ChatColor.GOLD + "[Culinary] " + ChatColor.GRAY;
    private static final String TITLE = ChatColor.DARK_GREEN + "Culinary Catalogue";

    private static final int SIZE = 54;
    private static final int SLOT_PAGE_INFO = 4;
    private static final int SLOT_FEAST_LABEL = 49;
    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_NEXT = 53;
    private static final int[] STANDARD_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int[] FEAST_SLOTS = {46, 47, 48, 50, 51, 52};

    private final CulinarySubsystem subsystem;
    private final ItemStack filler;
    private final ItemStack pageInfoTemplate;
    private final String pageTitleFormat;
    private final ItemStack feastLabel;
    private final ItemStack previousPage;
    private final ItemStack nextPage;
    private final ItemStack disabledButton;

    public CulinaryCatalogueManager(ProjectLinearity plugin, CulinarySubsystem subsystem) {
        this.subsystem = subsystem;
        this.filler = pane(Material.GRAY_STAINED_GLASS_PANE, " ");
        this.pageInfoTemplate = pane(Material.BOOK, ChatColor.AQUA + "Page %s",
                ChatColor.GRAY + "Select a recipe to bind it to your campfire.");
        ItemMeta pageMeta = this.pageInfoTemplate.getItemMeta();
        this.pageTitleFormat = pageMeta != null && pageMeta.hasDisplayName()
                ? pageMeta.getDisplayName()
                : ChatColor.AQUA + "Page %s";
        this.feastLabel = pane(Material.GOLDEN_APPLE, ChatColor.LIGHT_PURPLE + "Feast Recipes",
                ChatColor.GRAY + "Large, celebratory meals for the whole settlement.");
        this.previousPage = pane(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                ChatColor.GRAY + "Browse earlier recipes.");
        this.nextPage = pane(Material.ARROW, ChatColor.YELLOW + "Next Page",
                ChatColor.GRAY + "Browse more recipes.");
        this.disabledButton = pane(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "No Page",
                ChatColor.GRAY + "You're already at the edge of the list.");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openCatalogue(Player player) {
        openCatalogue(player, null, null);
    }

    public void openCatalogue(Player player, String campfireKey, Location campfireLocation) {
        if (player == null) {
            return;
        }
        List<CulinarySubsystem.CulinaryRecipe> standards = subsystem.getStandardRecipes();
        List<CulinarySubsystem.CulinaryRecipe> feasts = subsystem.getFeastRecipes();
        CatalogueHolder holder = new CatalogueHolder(player.getUniqueId(), standards, feasts, campfireKey, campfireLocation);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, TITLE);
        populateCatalogue(inventory, holder, player);
        player.openInventory(inventory);
        player.sendMessage(PREFIX + "Select a recipe to bind it to your next campfire session.");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
    }

    private void populateCatalogue(Inventory inventory, CatalogueHolder holder, Player viewer) {
        fill(inventory, filler);
        holder.standardMapping.clear();
        holder.feastMapping.clear();

        int totalPages = holder.totalPages();
        CulinarySubsystem.CulinaryRecipe selected = subsystem.getSelectedRecipe(viewer.getUniqueId());
        inventory.setItem(SLOT_PAGE_INFO, buildPageInfo(holder.page, totalPages, selected, holder.hasCampfireContext()));

        inventory.setItem(SLOT_PREVIOUS, holder.hasPreviousPage() ? previousPage.clone() : disabledButton.clone());
        inventory.setItem(SLOT_NEXT, holder.hasNextPage() ? nextPage.clone() : disabledButton.clone());
        inventory.setItem(SLOT_FEAST_LABEL, feastLabel.clone());

        int startIndex = holder.page * STANDARD_SLOTS.length;
        for (int i = 0; i < STANDARD_SLOTS.length; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= holder.standardRecipes.size()) {
                break;
            }
            CulinarySubsystem.CulinaryRecipe recipe = holder.standardRecipes.get(recipeIndex);
            ItemStack icon = createRecipeIcon(recipe);
            int slot = STANDARD_SLOTS[i];
            inventory.setItem(slot, icon);
            holder.standardMapping.put(slot, recipe);
        }

        for (int i = 0; i < FEAST_SLOTS.length; i++) {
            int slot = FEAST_SLOTS[i];
            if (i < holder.feastRecipes.size()) {
                CulinarySubsystem.CulinaryRecipe recipe = holder.feastRecipes.get(i);
                ItemStack icon = createRecipeIcon(recipe);
                inventory.setItem(slot, icon);
                holder.feastMapping.put(slot, recipe);
            } else {
                inventory.setItem(slot, filler.clone());
            }
        }
    }

    private ItemStack buildPageInfo(int page, int totalPages, CulinarySubsystem.CulinaryRecipe selected, boolean campfireContext) {
        ItemStack item = pageInfoTemplate.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(String.format(pageTitleFormat, (page + 1) + " / " + totalPages));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Select a recipe to bind it to your campfire.");
            if (campfireContext) {
                lore.add(ChatColor.GRAY + "This campfire is awaiting your choice.");
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Currently selected:");
            lore.add(selected != null ? ChatColor.GREEN + selected.getName() : ChatColor.RED + "None");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRecipeIcon(CulinarySubsystem.CulinaryRecipe recipe) {
        ItemStack icon = subsystem.createCataloguePreview(recipe);
        return icon == null ? new ItemStack(Material.PAPER) : icon.clone();
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

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof CatalogueHolder) {
            for (int slot : event.getRawSlots()) {
                if (slot < top.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof CatalogueHolder catalogueHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!catalogueHolder.isOwner(player.getUniqueId())) {
            player.sendMessage(PREFIX + ChatColor.RED + "This catalogue belongs to someone else.");
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot >= top.getSize()) {
            return;
        }

        if (rawSlot == SLOT_PREVIOUS && catalogueHolder.hasPreviousPage()) {
            catalogueHolder.page--;
            populateCatalogue(top, catalogueHolder, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            return;
        }
        if (rawSlot == SLOT_NEXT && catalogueHolder.hasNextPage()) {
            catalogueHolder.page++;
            populateCatalogue(top, catalogueHolder, player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            return;
        }

        CulinarySubsystem.CulinaryRecipe recipe = catalogueHolder.standardMapping.get(rawSlot);
        if (recipe == null) {
            recipe = catalogueHolder.feastMapping.get(rawSlot);
        }
        if (recipe == null) {
            return;
        }

        subsystem.setSelectedRecipe(player.getUniqueId(), recipe);
        player.closeInventory();

        if (catalogueHolder.hasCampfireContext()) {
            boolean started = subsystem.beginRecipeSessionFromCatalogue(
                    player,
                    catalogueHolder.getCampfireKey(),
                    catalogueHolder.getCampfireLocation(),
                    recipe
            );
            if (started) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
            }
        } else {
            String qualifier = recipe.isFeast() ? ChatColor.GOLD + "feast" : ChatColor.GREEN + "dish";
            player.sendMessage(PREFIX + ChatColor.GREEN + "Selected " + recipe.getName() + ChatColor.GRAY + " (" + qualifier + ChatColor.GRAY + "). Right-click a campfire to begin.");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.4f);
        }
    }

    private static final class CatalogueHolder implements InventoryHolder {
        private final UUID owner;
        private final List<CulinarySubsystem.CulinaryRecipe> standardRecipes;
        private final List<CulinarySubsystem.CulinaryRecipe> feastRecipes;
        private final Map<Integer, CulinarySubsystem.CulinaryRecipe> standardMapping = new HashMap<>();
        private final Map<Integer, CulinarySubsystem.CulinaryRecipe> feastMapping = new HashMap<>();
        private final String campfireKey;
        private final Location campfireLocation;
        private int page = 0;

        private CatalogueHolder(UUID owner,
                                List<CulinarySubsystem.CulinaryRecipe> standardRecipes,
                                List<CulinarySubsystem.CulinaryRecipe> feastRecipes,
                                String campfireKey,
                                Location campfireLocation) {
            this.owner = owner;
            this.standardRecipes = new ArrayList<>(standardRecipes);
            this.feastRecipes = feastRecipes == null ? new ArrayList<>() : new ArrayList<>(feastRecipes);
            this.campfireKey = campfireKey;
            this.campfireLocation = campfireLocation == null ? null : campfireLocation.clone();
        }

        private boolean isOwner(UUID uuid) {
            return owner.equals(uuid);
        }

        private int totalPages() {
            int size = standardRecipes.size();
            return Math.max(1, (int) Math.ceil(size / (double) STANDARD_SLOTS.length));
        }

        private boolean hasPreviousPage() {
            return page > 0;
        }

        private boolean hasNextPage() {
            return page + 1 < totalPages();
        }

        private boolean hasCampfireContext() {
            return campfireKey != null && campfireLocation != null;
        }

        private String getCampfireKey() {
            return campfireKey;
        }

        private Location getCampfireLocation() {
            return campfireLocation == null ? null : campfireLocation.clone();
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
