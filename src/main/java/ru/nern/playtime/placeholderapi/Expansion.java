package ru.nern.playtime.placeholderapi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import ru.nern.playtime.PlayTime;
import ru.nern.playtime.commands.CommandsManager;
import ru.nern.playtime.utils.Chat;
import ru.nern.playtime.utils.ConfigWrapper;
import ru.nern.playtime.utils.TimeFormat;
import ru.nern.playtime.utils.TopPlayers;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Expansion {
    static Pattern topPlaceholder = Pattern.compile("([0-9]+)_(name|time)");
    private static final String MOD_ID = "playtime";

    public static void register() {
        Placeholders.register(new Identifier(MOD_ID, "serveruptime"), (ctx, arg) -> PlaceholderResult.value(TimeFormat.uptime()));
        Placeholders.register(new Identifier(MOD_ID, "position"), (ctx, arg) -> {
            try {
                if (!ctx.hasPlayer())
                    return PlaceholderResult.invalid("No player!");

                File dir = new File(PlayTime.storagePath);

                File[] fileList = dir.listFiles();

                int i = 0;
                if (fileList != null) {
                    ArrayList<TopPlayers> allPlayers = new ArrayList<>();
                    TopPlayers target = new TopPlayers();

                    for (File jsonFile : fileList) {
                        FileReader reader = new FileReader(jsonFile);
                        JsonObject playerJSON = (JsonObject) JsonParser.parseReader(reader);
                        reader.close();

                        TopPlayers element = new TopPlayers(playerJSON.get("lastName").toString(),
                                playerJSON.get("uuid").toString(), Integer.parseInt(playerJSON.get("time").toString()));

                        if (Objects.equals(element.name, ctx.player().getName().getString()))
                            target = element;
                        allPlayers.add(element);
                    }

                    allPlayers.sort(Comparator.comparing(e -> e.time));
                    Collections.reverse(allPlayers);

                    i = allPlayers.indexOf(target);

                }
                return PlaceholderResult.value(i >= 0 ? i + "" : "0");
            } catch (Exception e) {
                e.printStackTrace();
                return PlaceholderResult.value("0");
            }
        });

        Placeholders.register(new Identifier(MOD_ID, "top_"), (ctx, arg) -> {
            if (arg == null) return PlaceholderResult.invalid("No arg provided!");
            Matcher m = topPlaceholder.matcher(arg);
            if (m.find()) {
                int pos = Integer.parseInt(m.group(1));
                String type = m.group(2);
                return PlaceholderResult.value(get(ctx.server(), pos, type));
            }
            return PlaceholderResult.invalid("Invalid argument!");
        });

        Placeholders.register(new Identifier(MOD_ID, "player"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            return PlaceholderResult.value(ctx.player().getName().getString());
        });

        Placeholders.register(new Identifier(MOD_ID, "time"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            return PlaceholderResult.value(TimeFormat.getTime(Duration.of(Chat.secondsPlayed(ctx.player()), ChronoUnit.SECONDS)));
        });

        Placeholders.register(new Identifier(MOD_ID, "time_seconds"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            return PlaceholderResult.value(String.valueOf(Duration.of(Chat.secondsPlayed(ctx.player()), ChronoUnit.SECONDS).getSeconds()));
        });

        Placeholders.register(new Identifier(MOD_ID, "time_hours"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            long sec = Duration.of(Chat.secondsPlayed(ctx.player()), ChronoUnit.SECONDS).getSeconds();
            long min = sec / 60, hour = min / 60;

            return PlaceholderResult.value(String.valueOf(Long.valueOf(hour).intValue()));
        });

        Placeholders.register(new Identifier(MOD_ID, "time_days"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            long sec = Duration.of(Chat.secondsPlayed(ctx.player()), ChronoUnit.SECONDS).getSeconds();
            long min = sec / 60, hour = min / 60, day = hour / 24;

            return PlaceholderResult.value(String.valueOf(Long.valueOf(hour).intValue()));
        });

        Placeholders.register(new Identifier(MOD_ID, "time_weeks"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            long sec = Duration.of(Chat.secondsPlayed(ctx.player()), ChronoUnit.SECONDS).getSeconds();
            long min = sec / 60, hour = min / 60, day = hour / 24, week = day / 7;

            return PlaceholderResult.value(String.valueOf(Long.valueOf(week).intValue()));
        });

        Placeholders.register(new Identifier(MOD_ID, "session"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            return PlaceholderResult.value(String.valueOf(Duration.of(PlayTime.getPlayerSession(ctx.player().getName().getString(), ctx.server()), ChronoUnit.SECONDS)));
        });

        Placeholders.register(new Identifier(MOD_ID, "timesjoined"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player!");
            return PlaceholderResult.value(String.valueOf(Chat.sessionsPlayed(ctx.player())));
        });
    }

    public static String get(MinecraftServer server, int pos, String type) {
        ConfigWrapper config = PlayTime.config;
        TopPlayers[] top10 = CommandsManager.getTopTen(server);
        if (top10.length <= pos - 1)
            return type.equals("name") ? Chat.format(config.placeholder.top.name)
                    : Chat.format(config.placeholder.top.time);
        TopPlayers top = top10[pos - 1];
        return type.equals("name") ? top.name : TimeFormat.getTime(Duration.of(top.time, ChronoUnit.SECONDS));
    }
}
