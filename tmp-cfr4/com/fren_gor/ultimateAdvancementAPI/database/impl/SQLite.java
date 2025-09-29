/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 *  org.sqlite.SQLiteConfig
 *  org.sqlite.SQLiteConfig$Encoding
 *  org.sqlite.SQLiteConfig$SynchronousMode
 */
package com.fren_gor.ultimateAdvancementAPI.database.impl;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.database.IDatabase;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.IllegalKeyException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotRegisteredException;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.sqlite.SQLiteConfig;

public class SQLite
implements IDatabase {
    private final Logger logger;
    private final Connection connection;

    public SQLite(@NotNull AdvancementMain main, @NotNull File dbFile) throws Exception {
        this(dbFile, Objects.requireNonNull(main, "AdvancementMain is null.").getLogger());
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    public SQLite(@NotNull File dbFile, @NotNull Logger logger) throws Exception {
        Preconditions.checkNotNull((Object)dbFile, (Object)"Database file is null.");
        Preconditions.checkNotNull((Object)logger, (Object)"Logger is null.");
        if (!dbFile.exists() && !dbFile.createNewFile()) {
            throw new IOException("Cannot create the database file.");
        }
        Class.forName("org.sqlite.JDBC");
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setEncoding(SQLiteConfig.Encoding.UTF8);
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + String.valueOf(dbFile), config.toProperties());
        this.logger = logger;
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    protected SQLite(@NotNull Logger logger) throws Exception {
        Preconditions.checkNotNull((Object)logger, (Object)"Logger is null.");
        Class.forName("org.sqlite.JDBC");
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        config.setEncoding(SQLiteConfig.Encoding.UTF8);
        config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
        this.connection = DriverManager.getConnection("jdbc:sqlite::memory:", config.toProperties());
        this.logger = logger;
    }

    @Override
    public void setUp() throws SQLException {
        try (Statement statement = this.openConnection().createStatement();){
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Teams` (`ID` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Players` (`UUID` TEXT NOT NULL PRIMARY KEY, `Name` TEXT NOT NULL, `TeamID` INTEGER NOT NULL, FOREIGN KEY(`TeamID`) REFERENCES `Teams`(`ID`) ON DELETE CASCADE ON UPDATE CASCADE);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Advancements` (`Namespace` TEXT NOT NULL, `Key` TEXT NOT NULL, `TeamID` INTEGER NOT NULL, `Progression` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`Namespace`,`Key`,`TeamID`), FOREIGN KEY(`TeamID`) REFERENCES `Teams`(`ID`) ON DELETE CASCADE ON UPDATE CASCADE);");
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Unredeemed` (`Namespace` TEXT NOT NULL, `Key` TEXT NOT NULL, `TeamID` INTEGER NOT NULL, `GiveRewards` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`Namespace`,`Key`,`TeamID`), FOREIGN KEY(`Namespace`, `Key`,`TeamID`) REFERENCES `Advancements`(`Namespace`, `Key`,`TeamID`) ON DELETE CASCADE ON UPDATE CASCADE);");
            statement.executeBatch();
        }
    }

    @Override
    public Connection openConnection() throws SQLException {
        return this.connection;
    }

    @Override
    public void close() throws SQLException {
        this.connection.close();
    }

    @Override
    public int getTeamId(@NotNull UUID uuid) throws SQLException, UserNotRegisteredException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT `TeamID` FROM `Players` WHERE `UUID`=?;");){
            ps.setString(1, uuid.toString());
            ResultSet r = ps.executeQuery();
            if (r.next()) {
                int n = r.getInt(1);
                return n;
            }
            throw new UserNotRegisteredException("No user " + String.valueOf(uuid) + " has been found.");
        }
    }

    @Override
    public List<UUID> getTeamMembers(int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT `UUID` FROM `Players` WHERE `TeamID`=?;");){
            ps.setInt(1, teamId);
            ResultSet r = ps.executeQuery();
            LinkedList<UUID> list = new LinkedList<UUID>();
            while (r.next()) {
                list.add(UUID.fromString(r.getString(1)));
            }
            LinkedList<UUID> linkedList = list;
            return linkedList;
        }
    }

    @Override
    public Map<AdvancementKey, Integer> getTeamAdvancements(int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT `Namespace`,`Key`,`Progression` FROM `Advancements` WHERE `TeamID`=?;");){
            ps.setInt(1, teamId);
            ResultSet r = ps.executeQuery();
            HashMap<AdvancementKey, Integer> map = new HashMap<AdvancementKey, Integer>();
            while (r.next()) {
                String namespace = r.getString(1);
                String key = r.getString(2);
                int progression = r.getInt(3);
                try {
                    map.put(new AdvancementKey(namespace, key), progression);
                }
                catch (IllegalKeyException e) {
                    this.logger.warning("Invalid AdvancementKey (" + namespace + ":" + key + ") encountered while reading Advancements table: " + e.getMessage());
                }
            }
            HashMap<AdvancementKey, Integer> hashMap = map;
            return hashMap;
        }
    }

    @Override
    public Map.Entry<TeamProgression, Boolean> loadOrRegisterPlayer(@NotNull UUID uuid, @NotNull String name) throws SQLException {
        int teamId;
        try (PreparedStatement psTeamId = this.openConnection().prepareStatement("SELECT `TeamID` FROM `Players` WHERE `UUID`=?;");){
            psTeamId.setString(1, uuid.toString());
            ResultSet r = psTeamId.executeQuery();
            if (!r.next()) {
                int teamId2;
                try (PreparedStatement psInsert = this.openConnection().prepareStatement("INSERT INTO `Teams` DEFAULT VALUES RETURNING `ID`;");){
                    r = psInsert.executeQuery();
                    if (!r.next()) {
                        throw new SQLException("Cannot insert default values into Teams table.");
                    }
                    teamId2 = r.getInt(1);
                }
                try (PreparedStatement psInsertPl = this.openConnection().prepareStatement("INSERT INTO `Players` (`UUID`, `Name`, `TeamID`) VALUES (?, ?, ?);");){
                    psInsertPl.setString(1, uuid.toString());
                    psInsertPl.setString(2, name);
                    psInsertPl.setInt(3, teamId2);
                    psInsertPl.execute();
                    AbstractMap.SimpleEntry<TeamProgression, Boolean> simpleEntry = new AbstractMap.SimpleEntry<TeamProgression, Boolean>(new TeamProgression(teamId2, uuid), true);
                    return simpleEntry;
                }
            }
            teamId = r.getInt(1);
        }
        List<UUID> list = this.getTeamMembers(teamId);
        Map<AdvancementKey, Integer> map = this.getTeamAdvancements(teamId);
        return new AbstractMap.SimpleEntry<TeamProgression, Boolean>(new TeamProgression(map, teamId, list), false);
    }

    @Override
    public TeamProgression loadUUID(@NotNull UUID uuid) throws SQLException, UserNotRegisteredException {
        int teamID = Integer.MIN_VALUE;
        LinkedList<UUID> list = new LinkedList<UUID>();
        try (PreparedStatement psTeamId = this.openConnection().prepareStatement("SELECT `UUID`, `TeamID` FROM `Players` WHERE `TeamID`=(SELECT `TeamID` FROM `Players` WHERE `UUID`=? LIMIT 1);");){
            psTeamId.setString(1, uuid.toString());
            ResultSet r = psTeamId.executeQuery();
            while (r.next()) {
                list.add(UUID.fromString(r.getString(1)));
                if (teamID != Integer.MIN_VALUE) continue;
                teamID = r.getInt(2);
            }
        }
        if (teamID == Integer.MIN_VALUE) {
            throw new UserNotRegisteredException("No user " + String.valueOf(uuid) + " has been found.");
        }
        try (PreparedStatement psAdv = this.openConnection().prepareStatement("SELECT `Namespace`, `Key`, `Progression` FROM `Advancements` WHERE `TeamID`=?;");){
            HashMap<AdvancementKey, Integer> map = new HashMap<AdvancementKey, Integer>();
            psAdv.setInt(1, teamID);
            ResultSet r = psAdv.executeQuery();
            while (r.next()) {
                String namespace = r.getString(1);
                String key = r.getString(2);
                int progression = r.getInt(3);
                try {
                    map.put(new AdvancementKey(namespace, key), progression);
                }
                catch (IllegalKeyException e) {
                    this.logger.warning("Invalid AdvancementKey (" + namespace + ":" + key + ") encountered while reading Advancements table: " + e.getMessage());
                }
            }
            TeamProgression teamProgression = new TeamProgression(map, teamID, list);
            return teamProgression;
        }
    }

    @Override
    public void updateAdvancement(@NotNull AdvancementKey key, int teamId, @Range(from=0L, to=0x7FFFFFFFL) int progression) throws SQLException {
        if (progression <= 0) {
            try (PreparedStatement ps = this.openConnection().prepareStatement("DELETE FROM `Advancements` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
                ps.setString(1, key.getNamespace());
                ps.setString(2, key.getKey());
                ps.setInt(3, teamId);
                ps.execute();
            }
        }
        try (PreparedStatement ps = this.openConnection().prepareStatement("INSERT OR REPLACE INTO `Advancements` (`Namespace`, `Key`, `TeamID`, `Progression`) VALUES (?, ?, ?, ?);");){
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            ps.setInt(3, teamId);
            ps.setInt(4, progression);
            ps.execute();
        }
    }

    @Override
    public List<Map.Entry<AdvancementKey, Boolean>> getUnredeemed(int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT `Namespace`, `Key`, `GiveRewards` FROM `Unredeemed` WHERE `TeamID`=?;");){
            ps.setInt(1, teamId);
            ResultSet r = ps.executeQuery();
            LinkedList<Map.Entry<AdvancementKey, Boolean>> list = new LinkedList<Map.Entry<AdvancementKey, Boolean>>();
            while (r.next()) {
                String namespace = r.getString(1);
                String key = r.getString(2);
                boolean giveRewards = r.getInt(3) != 0;
                try {
                    list.add(new AbstractMap.SimpleEntry<AdvancementKey, Boolean>(new AdvancementKey(namespace, key), giveRewards));
                }
                catch (IllegalKeyException e) {
                    this.logger.warning("Invalid AdvancementKey (" + namespace + ":" + key + ") encountered while reading Unredeemed table: " + e.getMessage());
                }
            }
            LinkedList<Map.Entry<AdvancementKey, Boolean>> linkedList = list;
            return linkedList;
        }
    }

    @Override
    public void setUnredeemed(@NotNull AdvancementKey key, boolean giveRewards, int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("INSERT OR IGNORE INTO `Unredeemed` (`Namespace`, `Key`, `TeamID`, `GiveRewards`) VALUES (?, ?, ?, ?);");){
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            ps.setInt(3, teamId);
            ps.setInt(4, giveRewards ? 1 : 0);
            ps.execute();
        }
    }

    @Override
    public boolean isUnredeemed(@NotNull AdvancementKey key, int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT Count(*) FROM `Unredeemed` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            ps.setInt(3, teamId);
            ResultSet r = ps.executeQuery();
            boolean bl = r.next() && r.getInt(1) > 0;
            return bl;
        }
    }

    @Override
    public void unsetUnredeemed(@NotNull AdvancementKey key, int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("DELETE FROM `Unredeemed` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            ps.setInt(3, teamId);
            ps.execute();
        }
    }

    @Override
    public void unsetUnredeemed(@NotNull List<Map.Entry<AdvancementKey, Boolean>> keyList, int teamId) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("DELETE FROM `Unredeemed` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
            for (Map.Entry<AdvancementKey, Boolean> key : keyList) {
                ps.setString(1, key.getKey().getNamespace());
                ps.setString(2, key.getKey().getKey());
                ps.setInt(3, teamId);
                ps.execute();
            }
        }
    }

    @Override
    public void unregisterPlayer(@NotNull UUID uuid) throws SQLException {
        try (PreparedStatement stDelete = this.openConnection().prepareStatement("DELETE FROM `Players` WHERE `UUID`=?;");){
            stDelete.setString(1, uuid.toString());
            stDelete.execute();
        }
    }

    @Override
    public void movePlayer(@NotNull UUID uuid, int newTeamId) throws SQLException {
        try (PreparedStatement stUpdate = this.openConnection().prepareStatement("UPDATE `Players` SET `TeamID`=? WHERE `UUID`=?;");){
            stUpdate.setInt(1, newTeamId);
            stUpdate.setString(2, uuid.toString());
            stUpdate.execute();
        }
    }

    @Override
    public TeamProgression movePlayerInNewTeam(@NotNull UUID uuid) throws SQLException {
        int teamId;
        try (PreparedStatement psInsert = this.openConnection().prepareStatement("INSERT INTO `Teams` DEFAULT VALUES RETURNING `ID`;");){
            ResultSet r = psInsert.executeQuery();
            if (!r.next()) {
                throw new SQLException("Cannot insert default values into Teams table.");
            }
            teamId = r.getInt(1);
        }
        this.movePlayer(uuid, teamId);
        return new TeamProgression(teamId, uuid);
    }

    @Override
    public List<UUID> getPlayersByName(@NotNull String name) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT `UUID` FROM `Players` WHERE `Name`=?;");){
            ps.setString(1, name);
            ResultSet r = ps.executeQuery();
            LinkedList<UUID> list = new LinkedList<UUID>();
            while (r.next()) {
                list.add(UUID.fromString(r.getString(1)));
            }
            LinkedList<UUID> linkedList = list;
            return linkedList;
        }
    }

    @Override
    public String getPlayerName(@NotNull UUID uuid) throws SQLException, UserNotRegisteredException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("SELECT `Name` FROM `Players` WHERE `UUID`=? LIMIT 1;");){
            ps.setString(1, uuid.toString());
            ResultSet r = ps.executeQuery();
            if (!r.next()) {
                throw new UserNotRegisteredException("No user " + String.valueOf(uuid) + " has been found.");
            }
            String string = r.getString(1);
            return string;
        }
    }

    @Override
    public void updatePlayerName(@NotNull UUID uuid, @NotNull String name) throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("UPDATE `Players` SET `Name`=? WHERE `UUID`=?;");){
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.execute();
        }
    }

    @Override
    public void clearUpTeams() throws SQLException {
        try (PreparedStatement ps = this.openConnection().prepareStatement("DELETE FROM `Teams` WHERE `ID` NOT IN (SELECT `TeamID` FROM `Players` GROUP BY `TeamID`);");){
            ps.execute();
        }
    }
}

