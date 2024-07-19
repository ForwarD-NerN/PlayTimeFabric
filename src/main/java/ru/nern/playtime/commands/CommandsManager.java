package ru.nern.playtime.commands;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import org.apache.commons.lang3.StringUtils;
import ru.nern.playtime.ConfigManager;
import ru.nern.playtime.PlayTime;
import ru.nern.playtime.utils.Chat;
import ru.nern.playtime.utils.TimeFormat;
import ru.nern.playtime.utils.TopPlayers;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandsManager {
    private static final SuggestionProvider<ServerCommandSource> PLAYERS_SUGGESTION_PROVIDER = ((context, builder) -> CommandSource.suggestMatching(context.getSource().getServer().getPlayerNames(), builder));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register(literal("playtime").requires(Permissions.require("playtime.check", 0))
                    .executes(context -> getSenderStats(context.getSource()))
                    .then(literal("reload").requires(Permissions.require("playtime.reload", 2))
                            .executes(ctx -> reloadConfig(ctx.getSource())))
                    .then(literal("uptime").requires(Permissions.require("playtime.uptime", 0))
                            .executes(context -> uptime(context.getSource())))
                    .then(literal("top").requires(Permissions.require("playtime.checktop", 0))
                            .executes(context -> top(context.getSource())))
                    .then(argument("player", StringArgumentType.string()).suggests(PLAYERS_SUGGESTION_PROVIDER).requires(Permissions.require("playtime.checkothers", 0))
                            .executes(context -> checkOther(context.getSource(), StringArgumentType.getString(context, "player"))))
                    .then(literal("resync").requires(Permissions.require("playtime.resync", 3)).executes(context -> syncStats(context.getSource()))));
            dispatcher.register(literal("pt").requires(Permissions.require("playtime.check", 0))
                    .executes((context -> getSenderStats(context.getSource())))
                    .redirect(literalCommandNode));
        });
    }

    private static int getSenderStats(ServerCommandSource source) throws CommandSyntaxException {
        for(String player : PlayTime.config.messages.player) {
            Chat.message(source, source.getPlayerOrThrow(), player);
        }
        return 1;
    }

    private static int reloadConfig(ServerCommandSource source) {
        for(String player : PlayTime.config.messages.reload_config) {
            Chat.message(source, source.getPlayer(), player);
        }
        PlayTime.config = ConfigManager.loadConfig();
        return 1;
    }

    private static int uptime(ServerCommandSource source) {
        for(String player : PlayTime.config.messages.server_uptime) {
            Chat.message(source, source.getPlayer(), player);
        }
        return 1;
    }

    private static int top(ServerCommandSource source) {
        TopPlayers[] top10 = getTopTen(source.getServer());
        for (String header : PlayTime.config.messages.playtimetop.header)
            Chat.message(source, source.getPlayer(), header);
        for (int i = 0; i < top10.length; i++) {
            if (top10[i].time == 0) {
                break;
            }
            for (String message : PlayTime.config.messages.playtimetop.message) {
                Chat.message(source, source.getPlayer(), message.replace("%position%", Integer.toString(i + 1))
                        .replace("%player%", top10[i].name).replace("%playtime%",
                                TimeFormat.getTime(Duration.of(top10[i].time / 20, ChronoUnit.SECONDS))));
            }
        }
        for (String footer : PlayTime.config.messages.playtimetop.footer)
            Chat.message(source, source.getPlayer(), footer);
        return 1;
    }


    private static int checkOther(ServerCommandSource source, String name) {
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(name);
        if (target == null) {
            String storedTime = getPlayerTime(name);
            String storedJoins = getPlayerJoins(name);
            if (storedTime == null || storedJoins == null) {
                for (String notOnline : PlayTime.config.messages.doesnt_exist)
                    Chat.message(source, source.getPlayer(), notOnline.replace("%offlineplayer%", name));
            } else {
                for (String offlinePlayers : PlayTime.config.messages.offline_players)
                    Chat.message(source, source.getPlayer(),
                            offlinePlayers.replace("%offlineplayer%", name)
                                    .replace("%offlinetime%",
                                            TimeFormat.getTime(Duration.of(Integer.parseInt(storedTime) / 20,
                                                    ChronoUnit.SECONDS)))
                                    .replace("%offlinetimesjoined%", storedJoins));
            }
        } else {
            for (String otherPlayer : PlayTime.config.messages.other_players) {
                Chat.message(source, target, otherPlayer);
            }

        }
        return 1;
    }

    public static String getPlayerTime(String name) {
        try {
            FileReader reader = new FileReader(PlayTime.getPlayerPath(name));
            JsonObject player = (JsonObject) JsonParser.parseReader(reader);
            reader.close();
            return player.get("time").getAsString();
        } catch (Exception ignored) {}
        return null;
    }

    public static String getPlayerJoins(String name) {
        try {
            FileReader reader = new FileReader(PlayTime.getPlayerPath(name));
            JsonObject player = (JsonObject) JsonParser.parseReader(reader);
            reader.close();
            return player.get("joins").getAsString();
        } catch (Exception ignored) {}
        return null;
    }


    public static TopPlayers[] getTopTen(MinecraftServer server) {
        TopPlayers[] topTen = {};
        try {

            File dir = new File(PlayTime.storagePath);

            File[] fileList = dir.listFiles();

            if (fileList != null) {
                HashSet<TopPlayers> allPlayers = Sets.newHashSet();

                for (File jsonFile : fileList) {
                    if(jsonFile.isDirectory()) continue;

                    FileReader reader = new FileReader(jsonFile);
                    JsonObject player = (JsonObject) JsonParser.parseReader(reader);
                    reader.close();

                    allPlayers.add(new TopPlayers(player.get("lastName").getAsString(), player.get("uuid").getAsString(),
                            Integer.parseInt(player.get("time").getAsString())));
                }

                //Removed checkOnlinePlayers and instead added this.
                for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    TopPlayers topPlayers = new TopPlayers(player.getName().getString(), player.getUuid().toString(), Chat.ticksPlayed(player));
                    //It doesn't make sense when you look at it, but it does make sense when you use it.
                    allPlayers.remove(topPlayers);
                    allPlayers.add(topPlayers);
                }

                List<TopPlayers> topPlayers = new ArrayList<>(allPlayers);
                topPlayers.sort(Comparator.comparing(e -> e.time));
                Collections.reverse(topPlayers);

                int len = Math.min(topPlayers.size(), 10);
                topTen = new TopPlayers[len];
                topTen[0] = topPlayers.get(0);

                for (int i = 0; i < len; ++i) {
                    topTen[i] = topPlayers.get(i);
                }
            }
            return topTen;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return topTen;
    }

    private static int syncStats(ServerCommandSource source) {
        new Thread(() -> {
            try {
                File playerData = source.getServer().getSavePath(WorldSavePath.STATS).toFile();
                File[] statFiles = playerData.listFiles();
                if (statFiles == null) return;

                for (File stat : statFiles) {
                    String fileName = stat.getName();
                    if(stat.isDirectory()) continue;
                    FileReader fileReader = new FileReader(stat);
                    JsonObject jsonObject = (JsonObject) JsonParser.parseReader(fileReader);

                    fileName = fileName.replace(".json", "");
                    Optional<GameProfile> profile = source.getServer().getUserCache().getByUuid(UUID.fromString(fileName));

                    if(profile.isPresent()) {
                        JsonObject custom = jsonObject.getAsJsonObject("stats").getAsJsonObject("minecraft:custom");
                        if(custom == null) continue;
                        int joins = custom.get("minecraft:leave_game") == null ? 1 : custom.get("minecraft:leave_game").getAsInt();
                        int playTime = custom.get("minecraft:play_time") == null ? 0 : custom.get("minecraft:play_time").getAsInt();

                        JsonObject playTimeData = new JsonObject();
                        playTimeData.addProperty("uuid", fileName);
                        playTimeData.addProperty("lastName", profile.get().getName());
                        playTimeData.addProperty("time", playTime);
                        playTimeData.addProperty("joins", joins);
                        playTimeData.addProperty("session", 0);
                        PlayTime.writePlayer(playTimeData);
                    }

                    fileReader.close();
                }

                if(PlayTime.config.messages.resync_stats.length > 1) {
                    Chat.message(source, source.getPlayer(), PlayTime.config.messages.resync_stats[1]);
                }
            }catch (Exception e) {
                e.printStackTrace();
                //PlayTime.LOGGER.error("Error occurred while syncing player data: " + e);
            }
        }, "PlayTime Sync Thread").start();


        if(PlayTime.config.messages.resync_stats != null) {
            Chat.message(source, source.getPlayer(), PlayTime.config.messages.resync_stats[0]);
        }
        return 1;
    }
}
