package goat.projectLinearity.subsystems.enchanting;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Custom enchanting table logic requiring chiseled bookshelves.
 * <p>
 * This is a simplified implementation of the feature request and omits
 * visual book animations and the full vanilla enchantment algorithm.
 */
public class EnchantingManager implements Listener {
    private final JavaPlugin plugin;
    private final EnchantedManager enchantedManager;
    private final Map<Location, Session> sessions = new HashMap<>();
    private static final int LAPIS_REQUIRED = 8;
    private static final int BOOKS_REQUIRED = 6;
    private static final Random RANDOM = new Random();

    public EnchantingManager(JavaPlugin plugin, EnchantedManager enchantedManager) {
        this.plugin = plugin;
        this.enchantedManager = enchantedManager;
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private static class Session {
        Player owner;
        ItemStack item;
        ArmorStand display;
        int lapis;
        int booksConsumed;
        boolean enchanting;
        boolean ready;
        List<Block> shelves;
        BukkitTask facingTask;
        Location table;
        int activeFlights;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Block clicked = e.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.ENCHANTING_TABLE) return;
        if (e.getHand() != EquipmentSlot.HAND) return; // only main hand

        Player player = e.getPlayer();
        Location tableLoc = clicked.getLocation();
        Action action = e.getAction();
        Session session = sessions.get(tableLoc);

        if (action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            Material handType = hand.getType();

            if (session != null && session.owner != player) {
                player.sendMessage(ChatColor.RED + "Another player is using this table.");
                e.setCancelled(true);
                return;
            }

            if (session != null && session.owner == player && session.item != null && !session.enchanting
                    && !session.ready && session.lapis == 0 && handType != Material.LAPIS_LAZULI) {
                giveItemBack(session, player, tableLoc, ChatColor.YELLOW + "You take the item back.");
                e.setCancelled(true);
                return;
            }

            if (handType == Material.LAPIS_LAZULI && session != null && session.owner == player
                    && session.item != null && !session.enchanting && session.lapis < LAPIS_REQUIRED) {
                session.lapis++;
                hand.setAmount(hand.getAmount() - 1);
                if (hand.getAmount() <= 0) {
                    player.getInventory().setItemInMainHand(null);
                }
                player.sendMessage(ChatColor.AQUA + "Lapis placed: " + session.lapis + "/" + LAPIS_REQUIRED);
                e.setCancelled(true);
                return;
            }

            if (session == null && isEnchantable(hand)) {
                if (!hand.getEnchantments().isEmpty() || enchantedManager.getEnchantedLevel(hand) > 0) {
                    player.sendMessage(ChatColor.RED + "That item is already enchanted.");
                    e.setCancelled(true);
                    return;
                }
                List<Block> shelves = findNearbyShelves(tableLoc);
                if (shelves.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Requires chiseled bookshelves within 4 blocks.");
                    e.setCancelled(true);
                    return;
                }
                int totalBooks = countBooksOnShelves(shelves);
                if (totalBooks < BOOKS_REQUIRED) {
                    player.sendMessage(ChatColor.RED + "Need at least " + BOOKS_REQUIRED
                            + " books stored in nearby chiseled bookshelves.");
                    e.setCancelled(true);
                    return;
                }
                Session s = new Session();
                s.owner = player;
                s.item = hand.clone();
                s.shelves = shelves;
                s.table = tableLoc.clone();
                s.activeFlights = 0;
                player.getInventory().setItemInMainHand(null);
                ArmorStand stand = spawnDisplay(s, tableLoc, s.item);
                sessions.put(tableLoc, s);
                player.sendMessage(ChatColor.YELLOW + "Item placed. Add lapis to enchant.");
                e.setCancelled(true);
                return;
            }

            e.setCancelled(true);
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            if (session == null) return;
            if (session.owner != player) {
                player.sendMessage(ChatColor.RED + "Another player is using this table.");
                e.setCancelled(true);
                return;
            }
            if (session.ready) {
                deliverFinishedItem(session, player, tableLoc);
            } else if (!session.enchanting) {
                if (session.lapis < LAPIS_REQUIRED) {
                    player.sendMessage(ChatColor.RED + "Need " + LAPIS_REQUIRED + " lapis to enchant.");
                } else {
                    beginEnchant(session, tableLoc);
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Enchanting started...");
                }
            }
            e.setCancelled(true);
        }
    }

    private void beginEnchant(Session session, Location tableLoc) {
        if (session.table == null) {
            session.table = tableLoc.clone();
        }
        session.enchanting = true;
        session.booksConsumed = 0;
        session.activeFlights = 0;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!session.enchanting || session.display == null || !session.display.isValid()) {
                    cancel();
                    return;
                }
                ticks++;
                if (ticks % 40 == 0) {
                    if (session.booksConsumed >= BOOKS_REQUIRED) {
                        return;
                    }
                    if (!consumeRandomBook(session)) {
                        Player owner = session.owner;
                        if (owner != null) {
                            giveItemBack(session, owner, tableLoc,
                                    ChatColor.RED + "Enchanting halted: nearby shelves ran out of books.");
                        } else {
                            cancelFacingTask(session);
                            sessions.remove(tableLoc);
                            session.enchanting = false;
                            session.ready = false;
                            if (session.display != null && session.display.isValid()) {
                                session.display.remove();
                            }
                            session.display = null;
                            session.table = null;
                        }
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void finishEnchant(Session session, Location tableLoc) {
        if (session.ready) {
            return;
        }
        session.enchanting = false;
        session.ready = true;
        session.lapis = 0; // lapis consumed
        int baseTier = enchantedManager.isGoldItem(session.item) ? 2 : 1;
        enchantedManager.applyBaseEnchant(session.item, baseTier);
        if (session.display != null && session.display.getEquipment() != null) {
            session.display.getEquipment().setHelmet(session.item.clone());
        }
        Player p = session.owner;
        p.playSound(tableLoc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        updateDisplayFacing(session.display, session.owner);
    }

    private void giveItemBack(Session session, Player player, Location tableLoc, String message) {
        cancelFacingTask(session);
        if (session.display != null && session.display.isValid()) {
            session.display.remove();
        }
        session.display = null;
        sessions.remove(tableLoc);
        session.enchanting = false;
        session.ready = false;
        session.lapis = 0;
        session.table = null;
        ItemStack returned = session.item;
        session.item = null;
        if (returned != null) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(returned);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(stack -> player.getWorld().dropItem(player.getLocation(), stack));
            }
        }
        player.playSound(tableLoc, Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.7f, 1.1f);
        player.sendMessage(message);
    }

    private void deliverFinishedItem(Session session, Player player, Location tableLoc) {
        cancelFacingTask(session);
        if (session.display != null && session.display.isValid()) {
            session.display.remove();
        }
        session.display = null;
        sessions.remove(tableLoc);
        ItemStack rewarded = session.item;
        session.item = null;
        session.table = null;
        if (rewarded != null) {
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(rewarded);
            if (!overflow.isEmpty()) {
                overflow.values().forEach(stack -> player.getWorld().dropItem(player.getLocation(), stack));
            }
        }
        player.playSound(tableLoc, Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.9f, 1.2f);
        player.sendMessage(ChatColor.GREEN + "Enchanted item acquired.");
    }

    private boolean consumeRandomBook(Session session) {
        if (session.shelves == null || session.shelves.isEmpty()) {
            return false;
        }
        Collections.shuffle(session.shelves);
        for (Block shelfBlock : session.shelves) {
            if (!(shelfBlock.getState() instanceof ChiseledBookshelf shelf)) continue;

            Inventory inv = shelf.getInventory();
            List<Integer> occupied = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0) {
                    occupied.add(i);
                }
            }
            if (occupied.isEmpty()) continue;

            int slot = occupied.get(RANDOM.nextInt(occupied.size()));

            ItemStack slotItem = inv.getItem(slot);
            if (slotItem != null) {
                slotItem.setAmount(0);
            }

            org.bukkit.block.data.type.ChiseledBookshelf data =
                    (org.bukkit.block.data.type.ChiseledBookshelf) shelfBlock.getBlockData();

            data.setSlotOccupied(slot, false);
            shelfBlock.setBlockData(data, true);
            shelf.update();

            session.booksConsumed++;

            // Spawn book flying animation
            animateBookFlight(session, shelfBlock, session.display);
            inv.clear(slot);
            return true;
        }
        return false;
    }

    private int countBooksOnShelves(List<Block> shelves) {
        int total = 0;
        for (Block shelfBlock : shelves) {
            if (!(shelfBlock.getState() instanceof ChiseledBookshelf shelf)) continue;
            Inventory inv = shelf.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack != null && stack.getType() != Material.AIR && stack.getAmount() > 0) {
                    total += stack.getAmount();
                }
            }
        }
        return total;
    }
    private void animateBookFlight(Session session, Block from, ArmorStand display) {
        Location start = from.getLocation().add(0.5, 1, 0.5);
        ItemStack bookItem = new ItemStack(Material.BOOK);
        ItemMeta meta = bookItem.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            bookItem.setItemMeta(meta);
        }
        Item itemEntity = from.getWorld().dropItem(start, bookItem);
        itemEntity.setPickupDelay(Integer.MAX_VALUE);
        itemEntity.setGravity(false);
        session.activeFlights++;
        Location tableLoc = session.table != null ? session.table.clone() : null;

        new BukkitRunnable() {
            int travelTicks = 0;
            int orbitTicks = 0;
            double angle = 0;
            boolean orbiting = false;

            private void stop() {
                if (itemEntity.isValid()) {
                    itemEntity.remove();
                }
                handleFlightComplete(session, tableLoc);
                cancel();
            }

            @Override
            public void run() {
                if (!itemEntity.isValid()) {
                    stop();
                    return;
                }
                if (display == null || !display.isValid() || !session.enchanting) {
                    stop();
                    return;
                }
                Location center = display.getLocation().clone().add(0, 1.0, 0);
                if (!orbiting) {
                    Location current = itemEntity.getLocation();
                    Vector dir = center.toVector().subtract(current.toVector());
                    double distanceSq = dir.lengthSquared();
                    if (distanceSq < 0.05) {
                        orbiting = true;
                        angle = Math.random() * (Math.PI * 2);
                        itemEntity.setVelocity(new Vector(0, 0, 0));
                        return;
                    }
                    dir.normalize().multiply(0.2);
                    itemEntity.setVelocity(dir);
                    travelTicks++;
                    if (travelTicks > 60) {
                        stop();
                    }
                } else {
                    angle += Math.PI / 64;
                    double radius = 0.6;
                    double x = center.getX() + Math.cos(angle) * radius;
                    double z = center.getZ() + Math.sin(angle) * radius;
                    double y = center.getY() + 0.15 * Math.sin(angle * 2) + 0.05 * Math.sin(orbitTicks / 4.0);
                    Location orbit = new Location(center.getWorld(), x, y, z, (float) (Math.toDegrees(angle) + 90f), 0f);
                    itemEntity.teleport(orbit);
                    itemEntity.setVelocity(new Vector(0, 0, 0));
                    orbitTicks++;
                    if (orbitTicks >= 60) {
                        center.getWorld().spawnParticle(Particle.ENCHANT, center, 40, 0.5, 0.5, 0.5, 0.1);
                        center.getWorld().spawnParticle(Particle.WITCH, center, 20, 0.3, 0.3, 0.3, 0.02);
                        center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.6f, 1.3f);
                        stop();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    private ArmorStand spawnDisplay(Session session, Location table, ItemStack item) {
        Player owner = session.owner;
        Location loc = table.clone().add(0.5, -0.4, 0.5);
        if (owner != null) {
            Vector direction = owner.getEyeLocation().toVector().subtract(loc.toVector());
            if (direction.lengthSquared() > 0.0001) {
                loc.setDirection(direction);
            }
        }
        ArmorStand stand = (ArmorStand) table.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setGravity(false);
        stand.setMarker(true);
        if (stand.getEquipment() != null) {
            stand.getEquipment().setHelmet(item.clone());
        }
        session.display = stand;
        updateDisplayFacing(stand, owner);
        startFacingTask(session);
        return stand;
    }

    private List<Block> findNearbyShelves(Location table) {
        List<Block> shelves = new ArrayList<>();
        World world = table.getWorld();
        int bx = table.getBlockX();
        int by = table.getBlockY();
        int bz = table.getBlockZ();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (b.getType() == Material.CHISELED_BOOKSHELF) {
                        shelves.add(b);
                    }
                }
            }
        }
        return shelves;
    }

    private void startFacingTask(Session session) {
        cancelFacingTask(session);
        if (session.display == null) {
            return;
        }
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                ArmorStand stand = session.display;
                if (stand == null || !stand.isValid()) {
                    cancel();
                    session.facingTask = null;
                    return;
                }
                Player owner = session.owner;
                if (owner == null || !owner.isOnline()) {
                    return;
                }
                updateDisplayFacing(stand, owner);
            }
        };
        session.facingTask = runnable.runTaskTimer(plugin, 0L, 2L);
    }

    private void cancelFacingTask(Session session) {
        if (session.facingTask != null) {
            session.facingTask.cancel();
            session.facingTask = null;
        }
    }

    private void handleFlightComplete(Session session, Location tableLoc) {
        session.activeFlights = Math.max(0, session.activeFlights - 1);
        if (!session.enchanting || session.ready) {
            return;
        }
        if (session.booksConsumed >= BOOKS_REQUIRED && session.activeFlights == 0) {
            Location target = session.table != null ? session.table.clone() : (tableLoc != null ? tableLoc.clone() : null);
            if (target == null && session.display != null && session.display.isValid()) {
                target = session.display.getLocation().clone();
            }
            if (target != null) {
                finishEnchant(session, target);
            }
        }
    }

    private void updateDisplayFacing(ArmorStand stand, Player player) {
        if (stand == null || !stand.isValid() || player == null) {
            return;
        }
        Location standLoc = stand.getLocation();
        Location target = player.getEyeLocation();
        double dx = target.getX() - standLoc.getX();
        double dz = target.getZ() - standLoc.getZ();
        if (Math.abs(dx) < 1.0E-5 && Math.abs(dz) < 1.0E-5) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        stand.setRotation(yaw, 0f);
    }

    private boolean isEnchantable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        for (Enchantment e : Enchantment.values()) {
            if (e.canEnchantItem(item)) return true;
        }
        return false;
    }
}
