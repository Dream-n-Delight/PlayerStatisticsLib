package com.dehys.norbecore.data;

import com.dehys.norbecore.exceptions.StatisticAlreadyLoadedException;
import com.dehys.norbecore.main.Main;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class StatisticsManager {

    private HashMap<UUID, PlayerStatistic> playerStatistics;

    public StatisticsManager() {
        playerStatistics = new HashMap<>();
    }

    public void loadStatistic(Player player) throws StatisticAlreadyLoadedException {
        loadStatistic(player.getUniqueId());
    }

    public void loadStatistic(OfflinePlayer offlinePlayer) throws StatisticAlreadyLoadedException {
        loadStatistic(offlinePlayer.getUniqueId());
    }

    public void loadStatistic(UUID uuid) throws StatisticAlreadyLoadedException {
        if (playerStatistics.containsKey(uuid)) {
            throw new StatisticAlreadyLoadedException("The statistics for that player are already loaded");
        }
        fetchStatistic(uuid);
    }

    public Optional<PlayerStatistic> getStatistic(Player player) {
        return getStatistic(player.getUniqueId());
    }

    public Optional<PlayerStatistic> getStatistic(OfflinePlayer player) {
        return getStatistic(player.getUniqueId());
    }

    public Optional<PlayerStatistic> getStatistic(UUID uuid) {
        return Optional.ofNullable(playerStatistics.get(uuid));
    }


    private Optional<PlayerStatistic> fetchStatistic(UUID uuid) {
        Optional<String> userid = Main.getInstance().getUserData().getPlayerID(uuid);
        if (!userid.isPresent()) throw new RuntimeException("No UserID found for given UUID.");
        try {
            PreparedStatement preparedStatement = SQL.prepareStatement("SELECT * FROM blockbreak WHERE userid = ?");
            preparedStatement.setString(1, userid.get());
            ResultSet resultSet = preparedStatement.executeQuery();

            HashMap<Material, Integer> blocksBroken = new HashMap<>();
            while (resultSet.next()) {
                Material blockType = Material.getMaterial(resultSet.getString("blocktype"));
                if (blockType == null) continue;
                int amount = resultSet.getInt("amount");
                blocksBroken.put(blockType, amount);
            }

            preparedStatement = SQL.prepareStatement("SELECT * FROM plain WHERE userid = ?");
            preparedStatement.setString(1, userid.get());
            resultSet = preparedStatement.executeQuery();

            int deaths = 0;
            while (resultSet.next()) {
                deaths = resultSet.getInt("deaths");
            }
            return Optional.of(new PlayerStatistic(uuid, userid.get(), blocksBroken, deaths));
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Optional.empty();
    }

    private Optional<PlayerStatistic> fetchStatistics(UUID... uuids) {
        Thread thread = new Thread(() -> {
            for (UUID uuid : uuids) {
                fetchStatistic(uuid);
            }
        });
        thread.start();
        return Optional.empty();
    }

    public HashMap<UUID, PlayerStatistic> getPlayerStatistics() {
        return playerStatistics;
    }

    public PlayerStatistic createStatistic(Player player) {
        Main.getInstance().getUserData().registerPlayer(player);
        return createStatistic(player.getUniqueId());
    }

    private PlayerStatistic createStatistic(UUID uuid) {
        PlayerStatistic statistic = new PlayerStatistic(uuid, Main.getInstance().getUserData().getPlayerID(uuid).orElseThrow(() -> new RuntimeException("Player was not registered due to an unknown error")));
        playerStatistics.put(uuid, statistic);
        return statistic;
    }

    public PlayerStatistic fetchOrCreate(Player player) {
        return fetchOrCreate(player.getUniqueId());
    }

    public PlayerStatistic fetchOrCreate(UUID uuid) {
        Optional<PlayerStatistic> statistic = fetchStatistic(uuid);
        return statistic.orElseGet(() -> createStatistic(uuid));
    }
}