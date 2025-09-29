/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Preconditions
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Range
 */
package com.fren_gor.ultimateAdvancementAPI.database.impl;

import com.fren_gor.ultimateAdvancementAPI.AdvancementMain;
import com.fren_gor.ultimateAdvancementAPI.database.IDatabase;
import com.fren_gor.ultimateAdvancementAPI.database.TeamProgression;
import com.fren_gor.ultimateAdvancementAPI.exceptions.IllegalKeyException;
import com.fren_gor.ultimateAdvancementAPI.exceptions.UserNotRegisteredException;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.Library;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.LibraryManager;
import com.fren_gor.ultimateAdvancementAPI.libs.net.byteflux.libby.classloader.IsolatedClassLoader;
import com.fren_gor.ultimateAdvancementAPI.util.AdvancementKey;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
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
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class MySQL
implements IDatabase {
    private final Logger logger;
    private final IsolatedClassLoader classLoader;
    private final DataSource dataSource;
    private final Method close;

    public MySQL(@NotNull AdvancementMain main, @NotNull String username, @NotNull String password, @NotNull String databaseName, @NotNull String host, @Range(from=1L, to=0x7FFFFFFFL) int port, @Range(from=1L, to=0x7FFFFFFFL) int poolSize, @Range(from=250L, to=0x7FFFFFFFFFFFFFFFL) long connectionTimeout) throws Exception {
        this(username, password, databaseName, host, port, poolSize, connectionTimeout, Objects.requireNonNull(main, "AdvancementMain is null.").getLogger(), main.getLibbyManager());
    }

    @Deprecated(forRemoval=true, since="2.5.0")
    public MySQL(@NotNull String username, @NotNull String password, @NotNull String databaseName, @NotNull String host, @Range(from=1L, to=0x7FFFFFFFL) int port, @Range(from=1L, to=0x7FFFFFFFL) int poolSize, @Range(from=250L, to=0x7FFFFFFFFFFFFFFFL) long connectionTimeout, @NotNull Logger logger, @NotNull LibraryManager manager) throws Exception {
        Preconditions.checkNotNull((Object)username, (Object)"Username is null.");
        Preconditions.checkNotNull((Object)password, (Object)"Password is null.");
        Preconditions.checkNotNull((Object)databaseName, (Object)"Database name is null.");
        Preconditions.checkNotNull((Object)host, (Object)"Host is null.");
        Preconditions.checkArgument((port > 0 ? 1 : 0) != 0, (Object)"Port must be greater than zero.");
        Preconditions.checkArgument((poolSize > 0 ? 1 : 0) != 0, (Object)"Pool size must be greater than zero.");
        Preconditions.checkArgument((connectionTimeout >= 250L ? 1 : 0) != 0, (Object)"Connection timeout must be greater or equals to 250.");
        Preconditions.checkNotNull((Object)logger, (Object)"Logger is null.");
        Preconditions.checkNotNull((Object)manager, (Object)"LibraryManager is null.");
        this.classLoader = new IsolatedClassLoader(new URL[0]);
        this.classLoader.addPath(manager.downloadLibrary(Library.builder().groupId("org.slf4j").artifactId("slf4j-api").version("1.7.36").checksum("0+9XXj5JeWeNwBvx3M5RAhSTtNEft/G+itmCh3wWocA=").build()));
        this.classLoader.addPath(manager.downloadLibrary(Library.builder().groupId("org.slf4j").artifactId("slf4j-nop").version("1.7.36").checksum("whSViweBbLRBKzDHvb1DCP/ca6KoN2e486kinL2SdNY=").build()));
        this.classLoader.addPath(manager.downloadLibrary(Library.builder().groupId("com.zaxxer").artifactId("HikariCP").version("5.1.0").checksum("pHpu5iN5aU7lLDADbwkxty+a7iqAHVkDQe2CvYOeITQ=").build()));
        Class<?> hikariConfig = this.classLoader.loadClass("com.zaxxer.hikari.HikariConfig");
        Class<?> hikariDataSource = this.classLoader.loadClass("com.zaxxer.hikari.HikariDataSource");
        this.close = hikariDataSource.getDeclaredMethod("close", new Class[0]);
        Properties props = new Properties();
        props.put("jdbcUrl", "jdbc:mysql://" + host + ":" + port + "/" + databaseName);
        props.put("driverClassName", "com.mysql.jdbc.Driver");
        props.put("username", username);
        props.put("password", password);
        props.put("minimumIdle", (Object)poolSize);
        props.put("maximumPoolSize", (Object)poolSize);
        props.put("connectionTimeout", (Object)connectionTimeout);
        props.put("poolName", "UltimateAdvancementAPI");
        props.put("dataSource.useSSL", (Object)false);
        props.put("dataSource.cachePrepStmts", (Object)true);
        props.put("dataSource.prepStmtCacheSize", (Object)250);
        props.put("dataSource.prepStmtCacheSqlLimit", (Object)2048);
        props.put("dataSource.useServerPrepStmts", (Object)true);
        props.put("dataSource.useLocalSessionState", (Object)true);
        props.put("dataSource.rewriteBatchedStatements", (Object)true);
        props.put("dataSource.cacheResultSetMetadata", (Object)true);
        props.put("dataSource.cacheServerConfiguration", (Object)true);
        props.put("dataSource.maintainTimeStats", (Object)false);
        Object config = hikariConfig.getConstructor(Properties.class).newInstance(props);
        this.dataSource = (DataSource)hikariDataSource.getConstructor(hikariConfig).newInstance(config);
        try {
            Connection ignored = this.openConnection();
            if (ignored != null) {
                ignored.close();
            }
        }
        catch (SQLException e) {
            throw new SQLException("An exception occurred while testing the established connection.", e);
        }
        this.logger = logger;
    }

    @Override
    public void setUp() throws SQLException {
        try (Connection conn = this.openConnection();
             Statement statement = conn.createStatement();){
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Teams` (`ID` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT) DEFAULT CHARSET = utf8mb4;");
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Players` (`UUID` VARCHAR(36) NOT NULL, `Name` VARCHAR(16) NOT NULL, `TeamID` INTEGER NOT NULL, PRIMARY KEY(`UUID`), FOREIGN KEY(`TeamID`) REFERENCES `Teams`(`ID`) ON DELETE CASCADE ON UPDATE CASCADE) DEFAULT CHARSET = utf8mb4;");
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Advancements` (`Namespace` VARCHAR(127) NOT NULL, `Key` VARCHAR(127) NOT NULL, `TeamID` INTEGER NOT NULL, `Progression` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`Namespace`,`Key`,`TeamID`), FOREIGN KEY(`TeamID`) REFERENCES `Teams`(`ID`) ON DELETE CASCADE ON UPDATE CASCADE) DEFAULT CHARSET = utf8mb4;");
            statement.addBatch("CREATE TABLE IF NOT EXISTS `Unredeemed` (`Namespace` VARCHAR(127) NOT NULL, `Key` VARCHAR(127) NOT NULL, `TeamID` INTEGER NOT NULL, `GiveRewards` INTEGER NOT NULL, PRIMARY KEY(`Namespace`,`Key`,`TeamID`), FOREIGN KEY(`Namespace`, `Key`, `TeamID`) REFERENCES `Advancements`(`Namespace`, `Key`, `TeamID`) ON DELETE CASCADE ON UPDATE CASCADE) DEFAULT CHARSET = utf8mb4;");
            statement.executeBatch();
        }
    }

    @Override
    public Connection openConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    @Override
    public void close() throws SQLException {
        try {
            this.close.invoke((Object)this.dataSource, new Object[0]);
        }
        catch (ReflectiveOperationException e) {
            throw new SQLException("Cannot close HikariDataSource.", e);
        }
        finally {
            try {
                this.classLoader.close();
            }
            catch (IOException iOException) {}
        }
    }

    @Override
    public int getTeamId(@NotNull UUID uuid) throws SQLException, UserNotRegisteredException {
        try (Connection conn = this.openConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT `TeamID` FROM `Players` WHERE `UUID`=?;");){
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
        try (Connection conn = this.openConnection();){
            List<UUID> list = this.getTeamMembers(conn, teamId);
            return list;
        }
    }

    private List<UUID> getTeamMembers(Connection connection, int teamId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT `UUID` FROM `Players` WHERE `TeamID`=?;");){
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
        try (Connection conn = this.openConnection();){
            Map<AdvancementKey, Integer> map = this.getTeamAdvancements(conn, teamId);
            return map;
        }
    }

    private Map<AdvancementKey, Integer> getTeamAdvancements(Connection connection, int teamId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT `Namespace`,`Key`,`Progression` FROM `Advancements` WHERE `TeamID`=?;");){
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
        try (Connection conn = this.openConnection();){
            int teamId;
            block29: {
                PreparedStatement psTeamId = conn.prepareStatement("SELECT `TeamID` FROM `Players` WHERE `UUID`=?;");
                psTeamId.setString(1, uuid.toString());
                ResultSet r = psTeamId.executeQuery();
                if (!r.next()) {
                    try (PreparedStatement psInsert = conn.prepareStatement("INSERT INTO `Teams` () VALUES ();", 1);){
                        AbstractMap.SimpleEntry<TeamProgression, Boolean> simpleEntry;
                        block28: {
                            PreparedStatement psInsertPl = conn.prepareStatement("INSERT INTO `Players` (`UUID`, `Name`, `TeamID`) VALUES (?, ?, ?);");
                            try {
                                psInsert.executeUpdate();
                                r = psInsert.getGeneratedKeys();
                                if (!r.next()) {
                                    throw new SQLException("Cannot insert default values into Teams table.");
                                }
                                int teamId2 = r.getInt(1);
                                psInsertPl.setString(1, uuid.toString());
                                psInsertPl.setString(2, name);
                                psInsertPl.setInt(3, teamId2);
                                psInsertPl.execute();
                                simpleEntry = new AbstractMap.SimpleEntry<TeamProgression, Boolean>(new TeamProgression(teamId2, uuid), true);
                                if (psInsertPl == null) break block28;
                            }
                            catch (Throwable throwable) {
                                if (psInsertPl != null) {
                                    try {
                                        psInsertPl.close();
                                    }
                                    catch (Throwable throwable2) {
                                        throwable.addSuppressed(throwable2);
                                    }
                                }
                                throw throwable;
                            }
                            psInsertPl.close();
                        }
                        return simpleEntry;
                    }
                }
                teamId = r.getInt(1);
                break block29;
                finally {
                    if (psTeamId != null) {
                        psTeamId.close();
                    }
                }
            }
            List<UUID> list = this.getTeamMembers(conn, teamId);
            Map<AdvancementKey, Integer> map = this.getTeamAdvancements(conn, teamId);
            AbstractMap.SimpleEntry<TeamProgression, Boolean> simpleEntry = new AbstractMap.SimpleEntry<TeamProgression, Boolean>(new TeamProgression(map, teamId, list), false);
            return simpleEntry;
        }
    }

    @Override
    public TeamProgression loadUUID(@NotNull UUID uuid) throws SQLException, UserNotRegisteredException {
        int teamID = Integer.MIN_VALUE;
        LinkedList<UUID> list = new LinkedList<UUID>();
        try (Connection conn = this.openConnection();){
            TeamProgression teamProgression;
            block24: {
                try (PreparedStatement psTeamId = conn.prepareStatement("SELECT `UUID`, `TeamID` FROM `Players` WHERE `TeamID`=(SELECT `TeamID` FROM `Players` WHERE `UUID`=? LIMIT 1);");){
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
                PreparedStatement psAdv = conn.prepareStatement("SELECT `Namespace`, `Key`, `Progression` FROM `Advancements` WHERE `TeamID`=?;");
                try {
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
                    teamProgression = new TeamProgression(map, teamID, list);
                    if (psAdv == null) break block24;
                }
                catch (Throwable throwable) {
                    if (psAdv != null) {
                        try {
                            psAdv.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                psAdv.close();
            }
            return teamProgression;
        }
    }

    @Override
    public void updateAdvancement(@NotNull AdvancementKey key, int teamId, @Range(from=0L, to=0x7FFFFFFFL) int progression) throws SQLException {
        block19: {
            try (Connection conn = this.openConnection();){
                if (progression <= 0) {
                    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM `Advancements` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
                        ps.setString(1, key.getNamespace());
                        ps.setString(2, key.getKey());
                        ps.setInt(3, teamId);
                        ps.execute();
                        break block19;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO `Advancements` (`Namespace`, `Key`, `TeamID`, `Progression`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `Progression`=VALUES(`Progression`);");){
                    ps.setString(1, key.getNamespace());
                    ps.setString(2, key.getKey());
                    ps.setInt(3, teamId);
                    ps.setInt(4, progression);
                    ps.execute();
                }
            }
        }
    }

    @Override
    public List<Map.Entry<AdvancementKey, Boolean>> getUnredeemed(int teamId) throws SQLException {
        try (Connection conn = this.openConnection();){
            LinkedList<Map.Entry<AdvancementKey, Boolean>> linkedList;
            block15: {
                PreparedStatement ps = conn.prepareStatement("SELECT `Namespace`, `Key`, `GiveRewards` FROM `Unredeemed` WHERE `TeamID`=?;");
                try {
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
                    linkedList = list;
                    if (ps == null) break block15;
                }
                catch (Throwable throwable) {
                    if (ps != null) {
                        try {
                            ps.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                ps.close();
            }
            return linkedList;
        }
    }

    @Override
    public void setUnredeemed(@NotNull AdvancementKey key, boolean giveRewards, int teamId) throws SQLException {
        try (Connection conn = this.openConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO `Unredeemed` (`Namespace`, `Key`, `TeamID`, `GiveRewards`) VALUES (?, ?, ?, ?);");){
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            ps.setInt(3, teamId);
            ps.setInt(4, giveRewards ? 1 : 0);
            ps.execute();
        }
    }

    @Override
    public boolean isUnredeemed(@NotNull AdvancementKey key, int teamId) throws SQLException {
        try (Connection conn = this.openConnection();){
            boolean bl;
            block12: {
                PreparedStatement ps = conn.prepareStatement("SELECT Count(*) FROM `Unredeemed` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");
                try {
                    ps.setString(1, key.getNamespace());
                    ps.setString(2, key.getKey());
                    ps.setInt(3, teamId);
                    ResultSet r = ps.executeQuery();
                    boolean bl2 = bl = r.next() && r.getInt(1) > 0;
                    if (ps == null) break block12;
                }
                catch (Throwable throwable) {
                    if (ps != null) {
                        try {
                            ps.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                ps.close();
            }
            return bl;
        }
    }

    @Override
    public void unsetUnredeemed(@NotNull AdvancementKey key, int teamId) throws SQLException {
        try (Connection conn = this.openConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM `Unredeemed` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
            ps.setString(1, key.getNamespace());
            ps.setString(2, key.getKey());
            ps.setInt(3, teamId);
            ps.execute();
        }
    }

    @Override
    public void unsetUnredeemed(@NotNull List<Map.Entry<AdvancementKey, Boolean>> keyList, int teamId) throws SQLException {
        try (Connection conn = this.openConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM `Unredeemed` WHERE `Namespace`=? AND `Key`=? AND `TeamID`=?;");){
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
        try (Connection conn = this.openConnection();
             PreparedStatement stDelete = conn.prepareStatement("DELETE FROM `Players` WHERE `UUID`=?;");){
            stDelete.setString(1, uuid.toString());
            stDelete.execute();
        }
    }

    @Override
    public void movePlayer(@NotNull UUID uuid, int newTeamId) throws SQLException {
        try (Connection conn = this.openConnection();){
            this.movePlayer(conn, uuid, newTeamId);
        }
    }

    private void movePlayer(Connection connection, @NotNull UUID uuid, int newTeamId) throws SQLException {
        try (PreparedStatement stUpdate = connection.prepareStatement("UPDATE `Players` SET `TeamID`=? WHERE `UUID`=?;");){
            stUpdate.setInt(1, newTeamId);
            stUpdate.setString(2, uuid.toString());
            stUpdate.execute();
        }
    }

    @Override
    public TeamProgression movePlayerInNewTeam(@NotNull UUID uuid) throws SQLException {
        try (Connection conn = this.openConnection();){
            TeamProgression teamProgression;
            block13: {
                PreparedStatement psInsert = conn.prepareStatement("INSERT INTO `Teams` () VALUES ();", 1);
                try {
                    psInsert.executeUpdate();
                    ResultSet r = psInsert.getGeneratedKeys();
                    if (!r.next()) {
                        throw new SQLException("Cannot insert default values into Teams table.");
                    }
                    int teamId = r.getInt(1);
                    this.movePlayer(conn, uuid, teamId);
                    teamProgression = new TeamProgression(teamId, uuid);
                    if (psInsert == null) break block13;
                }
                catch (Throwable throwable) {
                    if (psInsert != null) {
                        try {
                            psInsert.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                psInsert.close();
            }
            return teamProgression;
        }
    }

    @Override
    public List<UUID> getPlayersByName(@NotNull String name) throws SQLException {
        try (Connection conn = this.openConnection();){
            LinkedList<UUID> linkedList;
            block13: {
                PreparedStatement ps = conn.prepareStatement("SELECT `UUID` FROM `Players` WHERE `Name`=?;");
                try {
                    ps.setString(1, name);
                    ResultSet r = ps.executeQuery();
                    LinkedList<UUID> list = new LinkedList<UUID>();
                    while (r.next()) {
                        list.add(UUID.fromString(r.getString(1)));
                    }
                    linkedList = list;
                    if (ps == null) break block13;
                }
                catch (Throwable throwable) {
                    if (ps != null) {
                        try {
                            ps.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                ps.close();
            }
            return linkedList;
        }
    }

    @Override
    public String getPlayerName(@NotNull UUID uuid) throws SQLException, UserNotRegisteredException {
        try (Connection conn = this.openConnection();){
            String string;
            block13: {
                PreparedStatement ps = conn.prepareStatement("SELECT `Name` FROM `Players` WHERE `UUID`=? LIMIT 1;");
                try {
                    ps.setString(1, uuid.toString());
                    ResultSet r = ps.executeQuery();
                    if (!r.next()) {
                        throw new UserNotRegisteredException("No user " + String.valueOf(uuid) + " has been found.");
                    }
                    string = r.getString(1);
                    if (ps == null) break block13;
                }
                catch (Throwable throwable) {
                    if (ps != null) {
                        try {
                            ps.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                ps.close();
            }
            return string;
        }
    }

    @Override
    public void updatePlayerName(@NotNull UUID uuid, @NotNull String name) throws SQLException {
        try (Connection conn = this.openConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE `Players` SET `Name`=? WHERE `UUID`=?;");){
            ps.setString(1, name);
            ps.setString(2, uuid.toString());
            ps.execute();
        }
    }

    @Override
    public void clearUpTeams() throws SQLException {
        try (Connection conn = this.openConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM `Teams` WHERE `ID` NOT IN (SELECT `TeamID` FROM `Players` GROUP BY `TeamID`);");){
            ps.execute();
        }
    }
}

