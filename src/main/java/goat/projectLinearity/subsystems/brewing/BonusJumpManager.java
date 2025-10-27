package goat.projectLinearity.subsystems.brewing;

import goat.projectLinearity.ProjectLinearity;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Grants bonus mid-air jumps to players under the custom leaping potion effect.
 */
public final class BonusJumpManager implements Listener {

    private final ProjectLinearity plugin;
    private final Map<UUID, JumpState> states = new HashMap<>();

    public BonusJumpManager(ProjectLinearity plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void updatePlayer(Player player, int potency) {
        if (player == null || potency <= 0) {
            return;
        }
        JumpState state = states.computeIfAbsent(player.getUniqueId(), uuid -> new JumpState());
        state.potency = potency;
        state.maxCharges = potency;
        state.charges = potency;
        state.wasOnGround = player.isOnGround();
        ensureFlightState(player, state);
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        JumpState state = states.remove(player.getUniqueId());
        if (state == null) return;
        if (state.addedFlight && canModifyFlight(player)) {
            player.setAllowFlight(false);
        }
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        for (Map.Entry<UUID, JumpState> entry : states.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            JumpState state = entry.getValue();
            if (player != null && state.addedFlight && canModifyFlight(player)) {
                player.setAllowFlight(false);
            }
        }
        states.clear();
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (!canModifyFlight(player)) {
            return;
        }
        JumpState state = states.get(player.getUniqueId());
        if (state == null || state.charges <= 0) {
            return;
        }

        event.setCancelled(true);
        player.setFlying(false);

        Vector look = player.getLocation().getDirection();
        
        // Get horizontal direction (flattened look vector)
        Vector horizontal = look.clone().setY(0);
        if (horizontal.lengthSquared() > 1.0e-4) {
            horizontal.normalize();
        } else {
            horizontal.zero();
        }
        
        double baseHorizontalScale = 0.25 * Math.max(1, state.potency);
        double baseVertical = 0.45 + 0.15 * Math.min(3, state.potency);

        double pitchComponent = look.getY();
        double absPitch = Math.abs(pitchComponent);
        double pitchThreshold = Math.sin(Math.toRadians(45));

        double verticalBonus = 0.0;
        double horizontalFactor = 1.0;
        if (absPitch > pitchThreshold) {
            double excess = (absPitch - pitchThreshold) / (1.0 - pitchThreshold);
            excess = Math.min(1.0, Math.max(0.0, excess));
            verticalBonus = baseVertical * excess * 0.9;
            horizontalFactor = 1.0 - 0.65 * excess;
            if (pitchComponent < 0) {
                verticalBonus = -verticalBonus;
            }
        }

        Vector boost = horizontal.clone().multiply(baseHorizontalScale * horizontalFactor);
        boost.setY(baseVertical + verticalBonus);

        player.setVelocity(player.getVelocity().add(boost));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.6f, 1.4f);
        player.setFallDistance(0f);

        state.charges = Math.max(0, state.charges - 1);
        state.wasOnGround = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                ensureFlightState(player, state);
            }
        }.runTask(plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        JumpState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        boolean onGround = player.isOnGround();
        if (onGround && !state.wasOnGround) {
            state.charges = state.maxCharges;
            ensureFlightState(player, state);
        } else if (!onGround) {
            state.wasOnGround = false;
        }
        if (onGround) {
            state.wasOnGround = true;
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        JumpState state = states.get(player.getUniqueId());
        if (state == null) {
            return;
        }
        ensureFlightState(player, state);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer());
    }

    private void ensureFlightState(Player player, JumpState state) {
        if (!canModifyFlight(player)) {
            return;
        }
        if (state.charges > 0) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
                state.addedFlight = true;
            }
        } else if (state.addedFlight) {
            player.setAllowFlight(false);
            state.addedFlight = false;
        }
    }

    private boolean canModifyFlight(Player player) {
        GameMode mode = player.getGameMode();
        return mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR;
    }

    private static final class JumpState {
        int potency = 0;
        int maxCharges = 0;
        int charges = 0;
        boolean addedFlight = false;
        boolean wasOnGround = true;
    }
}
