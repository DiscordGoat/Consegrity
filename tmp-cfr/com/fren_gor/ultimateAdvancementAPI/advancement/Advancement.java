/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  net.md_5.bungee.api.ChatColor
 *  net.md_5.bungee.api.chat.BaseComponent
 *  net.md_5.bungee.api.chat.ComponentBuilder
 *  net.md_5.bungee.api.chat.ComponentBuilder$FormatRetention
 *  net.md_5.bungee.api.chat.HoverEvent
 *  net.md_5.bungee.api.chat.HoverEvent$Action
 *  org.bukkit.Bukkit
 *  org.bukkit.GameRule
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.jetbrains.annotations.ApiStatus$Internal
 *  org.jetbrains.annotations.Contract
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.advancement;

import com.fren_gor.ultimateAdvancementAPI.AdvancementTab;
import com.fren_gor.ultimateAdvancementAPI.advancement.BaseAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.RootAdvancement;
import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementDisplay;
import com.fren_gor.ultimateAdvancementAPI.database.DatabaseManager;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.events.advancement.AdvancementProgressionUpdateEvent;
import com.fren_gor.ultimateAdvancementAPI.exceptions.DisposedException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.IllegalOperationException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.InvalidAdvancementException;
import com.fren_gor.ultimateAdvancementAPI.nms.wrappers.advancement.AdvancementWrapper;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import com.fren_gor.ultimateAdvancementAPI.util.AfterHandle;
import com.fren_gor.ultimateAdvancementAPI.visibilities.IVisibility;
import com.google.common.base.Preconditions;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public abstract class Advancement {
    @NotNull
    protected final AdvancementKey key;
    @NotNull
    protected final AdvancementTab advancementTab;
    @NotNull
    protected final AdvancementDisplay display;
    protected final @Range(from=1L, to=0x7FFFFFFFL) int maxProgression;
    @Nullable
    private final MethodHandle iVisibilityMethod;

    private Advancement() {
        throw new UnsupportedOperationException("Private constructor.");
    }

    Advancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display) {
        this(advancementTab, key, display, 1);
    }

    Advancement(@NotNull AdvancementTab advancementTab, @NotNull String key, @NotNull AdvancementDisplay display, @Range(from=1L, to=0x7FFFFFFFL) int maxProgression) {
        if (!(this instanceof BaseAdvancement) && !(this instanceof RootAdvancement)) {
            throw new IllegalOperationException(this.getClass().getName() + " is neither an instance of RootAdvancement nor BaseAdvancement.");
        }
        Preconditions.checkArgument((maxProgression > 0 ? 1 : 0) != 0, (Object)"Maximum progression cannot be <= 0");
        this.advancementTab = Objects.requireNonNull(advancementTab, "AdvancementTab is null.");
        Preconditions.checkArgument((!advancementTab.isInitialised() ? 1 : 0) != 0, (Object)"AdvancementTab is already initialised.");
        Preconditions.checkArgument((!advancementTab.isDisposed() ? 1 : 0) != 0, (Object)"AdvancementTab is disposed.");
        this.key = new AdvancementKey(advancementTab.getNamespace(), key);
        this.display = Objects.requireNonNull(display, "Display is null.");
        this.maxProgression = maxProgression;
        this.iVisibilityMethod = this instanceof IVisibility ? this.getIVisibilityMethod(this.getClass()) : null;
    }

    @NotNull
    public final AdvancementKey getKey() {
        return this.key;
    }

    @NotNull
    public final AdvancementTab getAdvancementTab() {
        return this.advancementTab;
    }

    public final @Range(from=1L, to=0x7FFFFFFFL) int getMaxProgression() {
        return this.maxProgression;
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int getProgression(@NotNull Player player) {
        return this.getProgression(AdvancementUtils.uuidFromPlayer(player));
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int getProgression(@NotNull UUID uuid) {
        return this.getProgression(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int getProgression(@NotNull TeamProgression progression) {
        AdvancementUtils.validateTeamProgression(progression);
        return progression.getProgression(this);
    }

    public boolean isGranted(@NotNull Player player) {
        return this.isGranted(AdvancementUtils.uuidFromPlayer(player));
    }

    public boolean isGranted(@NotNull UUID uuid) {
        return this.isGranted(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public boolean isGranted(@NotNull TeamProgression progression) {
        AdvancementUtils.validateTeamProgression(progression);
        return this.getProgression(progression) >= this.maxProgression;
    }

    @Nullable
    public BaseComponent[] getAnnounceMessage(@NotNull Player player) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        ChatColor color = this.display.getFrame().getColor();
        return new ComponentBuilder(player.getName() + " " + this.display.getFrame().getChatText() + " ").color(ChatColor.WHITE).append(new ComponentBuilder("[").color(color).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, this.display.getChatDescription())).create(), ComponentBuilder.FormatRetention.NONE).append(this.display.getChatTitle(), ComponentBuilder.FormatRetention.EVENTS).append(new ComponentBuilder("]").color(color).create(), ComponentBuilder.FormatRetention.EVENTS).create();
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player) {
        return this.incrementProgression(player, true);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player, boolean giveReward) {
        return this.incrementProgression(player, 1, giveReward);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player, @Range(from=1L, to=0x7FFFFFFFL) int increment) {
        return this.incrementProgression(player, increment, true);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull Player player, @Range(from=1L, to=0x7FFFFFFFL) int increment, boolean giveReward) {
        return this.incrementProgression(AdvancementUtils.progressionFromPlayer(player, this), player, increment, giveReward);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid) {
        return this.incrementProgression(uuid, true);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid, boolean giveReward) {
        return this.incrementProgression(uuid, 1, giveReward);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid, @Range(from=1L, to=0x7FFFFFFFL) int increment) {
        return this.incrementProgression(uuid, increment, true);
    }

    public @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull UUID uuid, @Range(from=1L, to=0x7FFFFFFFL) int increment, boolean giveReward) {
        return this.incrementProgression(AdvancementUtils.progressionFromUUID(uuid, this), Bukkit.getPlayer((UUID)uuid), increment, giveReward);
    }

    protected @Range(from=0L, to=0x7FFFFFFFL) int incrementProgression(@NotNull TeamProgression pro, @Nullable Player player, @Range(from=1L, to=0x7FFFFFFFL) int increment, boolean giveRewards) {
        AdvancementUtils.validateTeamProgression(pro);
        AdvancementUtils.validateIncrement(increment);
        int progression = this.getProgression(pro);
        if (progression >= this.maxProgression) {
            return progression;
        }
        int newProgression = progression + increment;
        if (newProgression > this.maxProgression) {
            newProgression = this.maxProgression;
        }
        this.setProgression(pro, player, newProgression, giveRewards);
        return newProgression;
    }

    public void setProgression(@NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        this.setProgression(player, progression, true);
    }

    public void setProgression(@NotNull Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveReward) {
        this.setProgression(AdvancementUtils.progressionFromPlayer(player, this), player, progression, giveReward);
    }

    public void setProgression(@NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int progression) {
        this.setProgression(uuid, progression, true);
    }

    public void setProgression(@NotNull UUID uuid, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveReward) {
        this.setProgression(AdvancementUtils.progressionFromUUID(uuid, this), Bukkit.getPlayer((UUID)uuid), progression, giveReward);
    }

    protected void setProgression(@NotNull TeamProgression pro, @Nullable Player player, @Range(from=0L, to=0x7FFFFFFFL) int progression, boolean giveRewards) {
        AdvancementUtils.validateTeamProgression(pro);
        AdvancementUtils.validateProgressionValueStrict(progression, this.maxProgression);
        DatabaseManager ds = this.advancementTab.getDatabaseManager();
        int old = ds.updateProgression(this.key, pro, progression);
        try {
            Bukkit.getPluginManager().callEvent((Event)new AdvancementProgressionUpdateEvent(pro, old, progression, this));
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        this.handlePlayer(pro, player, progression, old, giveRewards, AfterHandle.UPDATE_ADVANCEMENTS_TO_TEAM);
    }

    protected void handlePlayer(@NotNull TeamProgression pro, @Nullable Player player, int newProgression, int oldProgression, boolean giveRewards, @Nullable AfterHandle afterHandle) {
        AdvancementUtils.validateTeamProgression(pro);
        if (newProgression >= this.maxProgression && oldProgression < this.maxProgression) {
            if (player != null) {
                this.onGrant(player, giveRewards);
            } else {
                DatabaseManager ds = this.advancementTab.getDatabaseManager();
                player = pro.getAnOnlineMember(ds);
                if (player != null) {
                    this.onGrant(player, giveRewards);
                } else {
                    ds.setUnredeemed(this.key, giveRewards, pro);
                    return;
                }
            }
        }
        if (afterHandle != null) {
            afterHandle.apply(pro, player, this);
        }
    }

    public void displayToastToPlayer(@NotNull Player player) {
        AdvancementUtils.displayToast(player, this.display.getIcon(), this.display.getTitle(), this.display.getFrame());
    }

    public boolean isVisible(@NotNull Player player) {
        return this.isVisible(AdvancementUtils.uuidFromPlayer(player));
    }

    public boolean isVisible(@NotNull UUID uuid) {
        return this.isVisible(AdvancementUtils.progressionFromUUID(uuid, this));
    }

    public boolean isVisible(@NotNull TeamProgression progression) {
        AdvancementUtils.validateTeamProgression(progression);
        if (this.iVisibilityMethod != null) {
            try {
                return (Boolean)this.iVisibilityMethod.invokeWithArguments(this, progression);
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public void onGrant(@NotNull Player player, boolean giveRewards) {
        BaseComponent[] msg;
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        Boolean gameRule = (Boolean)player.getWorld().getGameRuleValue(GameRule.ANNOUNCE_ADVANCEMENTS);
        if (this.display.doesAnnounceToChat() && (gameRule == null || gameRule.booleanValue()) && (msg = this.getAnnounceMessage(player)) != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.spigot().sendMessage(msg);
            }
        }
        if (this.display.doesShowToast()) {
            AdvancementUtils.runSync(this.advancementTab.getOwningPlugin(), 2L, () -> AdvancementUtils.displayToastDuringUpdate(player, this));
        }
        if (giveRewards) {
            this.giveReward(player);
        }
    }

    public void grant(@NotNull Player player) {
        this.grant(player, true);
    }

    public void grant(@NotNull Player player, boolean giveRewards) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        this.setProgression(player, this.maxProgression, giveRewards);
    }

    public void revoke(@NotNull Player player) {
        Preconditions.checkNotNull((Object)player, (Object)"Player is null.");
        this.setProgression(player, 0, false);
    }

    public void onUpdate(@NotNull TeamProgression teamProgression, @NotNull Map<AdvancementWrapper, Integer> addedAdvancements) {
        if (this.isVisible(teamProgression)) {
            addedAdvancements.put(this.getNMSWrapper(), this.getProgression(teamProgression));
        }
    }

    public void giveReward(@NotNull Player player) {
    }

    public void onRegister() {
    }

    public void validateRegister() throws InvalidAdvancementException {
    }

    public void onDispose() {
    }

    public boolean isValid() {
        return this.advancementTab.isActive() && this.advancementTab.hasAdvancement(this);
    }

    @NotNull
    public abstract AdvancementWrapper getNMSWrapper();

    protected final <E extends Event> void registerEvent(@NotNull Class<E> eventClass, @NotNull Consumer<E> consumer) {
        try {
            this.advancementTab.getEventManager().register(this, eventClass, consumer);
        }
        catch (IllegalStateException e) {
            throw new DisposedException(e);
        }
    }

    protected final <E extends Event> void registerEvent(@NotNull Class<E> eventClass, @NotNull EventPriority priority, @NotNull Consumer<E> consumer) {
        try {
            this.advancementTab.getEventManager().register(this, eventClass, priority, consumer);
        }
        catch (IllegalStateException e) {
            throw new DisposedException(e);
        }
    }

    public String toString() {
        return this.key.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Advancement that = (Advancement)o;
        return this.key.equals(that.key);
    }

    public int hashCode() {
        return this.key.hashCode();
    }

    private MethodHandle getIVisibilityMethod(Class<? extends Advancement> clazz) {
        for (Class<?> i : clazz.getInterfaces()) {
            if (i == IVisibility.class || !IVisibility.class.isAssignableFrom(i)) continue;
            try {
                Method m = i.getDeclaredMethod("isVisible", Advancement.class, TeamProgression.class);
                if (!m.isDefault()) continue;
                return MethodHandles.lookup().unreflectSpecial(m, i).bindTo(this);
            }
            catch (IllegalAccessException | NoSuchMethodException reflectiveOperationException) {
                // empty catch block
            }
        }
        Class<? extends Advancement> sClazz = clazz.getSuperclass();
        if (Advancement.class.isAssignableFrom(sClazz) && sClazz != Advancement.class) {
            return this.getIVisibilityMethod(sClazz.asSubclass(Advancement.class));
        }
        return null;
    }

    @NotNull
    public AdvancementDisplay getDisplay() {
        return this.display;
    }

    @Deprecated
    @ApiStatus.Internal
    @Contract(value="_, _ -> fail")
    public final boolean isVisible(Advancement advancement, TeamProgression progression) {
        throw new IllegalOperationException("This method cannot be called. Use Advancement#isVisible(TeamProgression).");
    }
}

