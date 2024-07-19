package ru.nern.playtime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nern.playtime.commands.CommandsManager;
import ru.nern.playtime.placeholderapi.Expansion;
import ru.nern.playtime.utils.Chat;
import ru.nern.playtime.utils.ConfigWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class PlayTime implements ModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("playtime");
	public HashMap<String, Long> sessions = new HashMap<>();
	public static String storagePath = FabricLoader.getInstance().getGameDir() + "/PlayTime/";
	public static ConfigWrapper config;

	@Override
	public void onInitialize() {

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			checkStorage();
			config = ConfigManager.loadConfig();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.player;
			JsonObject target = new JsonObject();
			sessions.put(player.getUuid().toString(), System.currentTimeMillis());

			int ticksPlayed = Chat.secondsPlayed(player);
			int sessionsPlayed = Chat.sessionsPlayed(player);

			File playerPath = new File(getPlayerPath(player.getName().getString()));

			if (ticksPlayed < 1 || !playerPath.exists()) {
				target.addProperty("uuid", player.getUuid().toString());
				target.addProperty("lastName", player.getName().getString());

				target.addProperty("time", ticksPlayed + 1);
				target.addProperty("joins", sessionsPlayed + 1);
				target.addProperty("session", ticksPlayed);
				//Vanilla doesn't have scheduling? So we just do the thing on the main thread. Shouldn't affect performance too much, I think.
				writePlayer(target);
			} else {
				try {
					final FileReader reader = new FileReader(playerPath);
					final JsonObject playerJSON = (JsonObject) JsonParser.parseReader(reader);
					reader.close();

					boolean changed = false;
					if (ticksPlayed + 1 > Integer.parseInt(playerJSON.get("time").toString())) {
						target.addProperty("time", ticksPlayed + 1);
						changed = true;
					} else {
						target.addProperty("time", Integer.parseInt(playerJSON.get("time").toString()));
					}

					if (sessionsPlayed > Integer.parseInt(playerJSON.get("joins").toString())) {
						target.addProperty("joins", sessionsPlayed);
						changed = true;
					} else {
						target.addProperty("joins", Integer.parseInt(playerJSON.get("joins").toString()));
					}
					if (changed) {
						target.addProperty("uuid", player.getUuid().toString());
						target.addProperty("lastName", player.getName().getString());
						target.addProperty("session", ticksPlayed);
						//Same as before
						//Bukkit.getScheduler().runTaskAsynchronously(this, () -> writePlayer(target));
						writePlayer(target);
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> savePlayer(handler.player));
		CommandsManager.register();
		Expansion.register();
	}

	public static int getPlayerSession(final String name, MinecraftServer server) {
		try {
			final FileReader reader = new FileReader(getPlayerPath(name));
			final JsonObject player = (JsonObject) JsonParser.parseReader(reader);
			reader.close();

			if (player.get("lastName").getAsString().equals(name)) {
				final ServerPlayerEntity p = server.getPlayerManager().getPlayer(name);
				final int session = Integer.parseInt(player.get("session").getAsString());
				final int current = Chat.ticksPlayed(p);
				return current - session;
			}

		} catch (Exception ignored) {}
		return 0;
	}

	public void savePlayer(ServerPlayerEntity player) {
		JsonObject target = new JsonObject();

		String uuid = player.getUuid().toString();
		int sessionOnTime = (int) (System.currentTimeMillis() - sessions.get(uuid)) / 50;
		sessions.remove(uuid);

		try {
			FileReader reader = new FileReader(getPlayerPath(player.getName().getString()));

			JsonObject oldData = (JsonObject) JsonParser.parseReader(reader);
			reader.close();

			target.addProperty("uuid", uuid);
			target.addProperty("lastName", player.getName().getString());
			target.addProperty("time", Integer.parseInt(oldData.get("time").getAsString()) + sessionOnTime);
			target.addProperty("joins", Integer.parseInt(oldData.get("joins").getAsString()) + 1);
			target.addProperty("session", sessionOnTime);
		} catch (Exception e) {
			e.printStackTrace();
			//Removed legacy method here, because why
		}

		writePlayer(target);
	}

	public static void writePlayer(JsonObject target) {
		String playerPath = getPlayerPath(target.get("lastName").getAsString());

		try {
			File userdataFile = new File(playerPath);
			if (!userdataFile.exists()) {
				try {
					FileWriter writer = new FileWriter(userdataFile.getAbsoluteFile());
					writer.write("{}");
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			FileReader reader = new FileReader(playerPath);
			JsonObject oldData = (JsonObject) JsonParser.parseReader(reader);
			reader.close();

			if (oldData.get("time") == null || Integer.parseInt(target.get("time").getAsString()) > Integer
					.parseInt(oldData.get("time").getAsString())) {
				FileWriter writer = new FileWriter(playerPath);
				writer.write(target.toString());
				writer.flush();
				writer.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkStorage() {
		File dataFolder = new File(storagePath);

		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}
		//Removed legacy conversion here because I don't need it
	}

	public static String getPlayerPath(String name) {
		return storagePath + name + ".json";
	}

}
