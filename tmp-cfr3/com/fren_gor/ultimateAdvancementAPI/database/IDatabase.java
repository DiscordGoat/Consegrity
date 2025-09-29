/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.database;

import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotRegisteredException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public interface IDatabase {
    public void setUp() throws SQLException;

    public Connection openConnection() throws SQLException;

    public void close() throws SQLException;

    default public int getTeamId(@NotNull Player player) throws SQLException, UserNotRegisteredException {
        return this.getTeamId(AdvancementUtils.uuidFromPlayer(player));
    }

    public int getTeamId(@NotNull UUID var1) throws SQLException, UserNotRegisteredException;

    public List<UUID> getTeamMembers(int var1) throws SQLException;

    public Map<AdvancementKey, Integer> getTeamAdvancements(int var1) throws SQLException;

    public Map.Entry<TeamProgression, Boolean> loadOrRegisterPlayer(@NotNull UUID var1, @NotNull String var2) throws SQLException;

    public TeamProgression loadUUID(@NotNull UUID var1) throws SQLException, UserNotRegisteredException;

    public void updateAdvancement(@NotNull AdvancementKey var1, int var2, @Range(from=0L, to=0x7FFFFFFFL) int var3) throws SQLException;

    public List<Map.Entry<AdvancementKey, Boolean>> getUnredeemed(int var1) throws SQLException;

    public void setUnredeemed(@NotNull AdvancementKey var1, boolean var2, int var3) throws SQLException;

    public boolean isUnredeemed(@NotNull AdvancementKey var1, int var2) throws SQLException;

    public void unsetUnredeemed(@NotNull AdvancementKey var1, int var2) throws SQLException;

    public void unsetUnredeemed(@NotNull List<Map.Entry<AdvancementKey, Boolean>> var1, int var2) throws SQLException;

    default public void unregisterPlayer(@NotNull Player player) throws SQLException {
        this.unregisterPlayer(AdvancementUtils.uuidFromPlayer(player));
    }

    public void unregisterPlayer(@NotNull UUID var1) throws SQLException;

    default public void movePlayer(@NotNull Player player, int newTeamId) throws SQLException {
        this.movePlayer(AdvancementUtils.uuidFromPlayer(player), newTeamId);
    }

    public void movePlayer(@NotNull UUID var1, int var2) throws SQLException;

    default public TeamProgression movePlayerInNewTeam(@NotNull Player player) throws SQLException {
        return this.movePlayerInNewTeam(AdvancementUtils.uuidFromPlayer(player));
    }

    public TeamProgression movePlayerInNewTeam(@NotNull UUID var1) throws SQLException;

    default public UUID getPlayerByName(@NotNull String name) throws SQLException, UserNotRegisteredException {
        List<UUID> l = this.getPlayersByName(name);
        if (l.size() == 0) {
            throw new UserNotRegisteredException("Couldn't find any player with name '" + name + "'");
        }
        return l.get(0);
    }

    public List<UUID> getPlayersByName(@NotNull String var1) throws SQLException;

    public String getPlayerName(@NotNull UUID var1) throws SQLException, UserNotRegisteredException;

    public void updatePlayerName(@NotNull UUID var1, @NotNull String var2) throws SQLException;

    public void clearUpTeams() throws SQLException;
}

