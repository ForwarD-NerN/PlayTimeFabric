package ru.nern.playtime.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import ru.nern.playtime.PlayTime;

import java.io.FileReader;

public class Chat {
    public static String format(String commandLabel) {
        return commandLabel.replace("&", "§");
    }

    public static void message(ServerCommandSource sender, ServerPlayerEntity player, String commandLabel) {
        sender.sendMessage(Text.of(Placeholders.parseText(Text.literal(format(commandLabel)), PlaceholderContext.of(player)).getString()));
    }

    public static String console(String commandLabel, MinecraftServer server) {
        server.sendMessage(Text.literal(format(commandLabel)));
        return commandLabel;
    }

    public static int secondsPlayed(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME)) / 20;
    }

    public static int ticksPlayed(ServerPlayerEntity player) {
        return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_TIME));
    }

    public static int sessionsPlayed(ServerPlayerEntity player) {
        try {
            FileReader reader = new FileReader(PlayTime.getPlayerPath(player.getName().getString()));
            JsonObject playerJson = (JsonObject) JsonParser.parseReader(reader);
            reader.close();
            return Integer.parseInt(playerJson.get("joins").getAsString());
        } catch (Exception e) {
            return player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.LEAVE_GAME));
        }
    }
}
