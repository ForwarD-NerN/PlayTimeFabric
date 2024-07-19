package ru.nern.playtime;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import ru.nern.playtime.PlayTime;
import ru.nern.playtime.utils.ConfigWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class ConfigManager {
    public static final String CONFIG_DIR = FabricLoader.getInstance().getGameDir() + "/config/";
    public static void saveConfig() {
        File file = new File(CONFIG_DIR + "playtime.yaml");
        if(file.exists()) return;

        try {
            FileWriter writer = new FileWriter(file.getAbsoluteFile());
            writer.write("#  Playtime By F64_Rx(Ported to Fabric by ForwarD NerN) - Need Help? PM me on Spigot or post in the discussion.\r\n" + "\r\n"
                    + "#  =================\r\n"
                    + "#  | CONFIGURATION |\r\n"
                    + "#  =================\r\n" + "\r\n"
                    + "#  available placeholders\r\n"
                    + "#  %playtime_player% - returns the player name\r\n"
                    + "#  %offlineplayer% - returns the offline player name\r\n"
                    + "#  %offlinetime% - shows offline time of a player\r\n"
                    + "#  %offlinetimesjoined% - shows the amount of joins a player has had\r\n"
                    + "#  %playtime_time% - shows time played\r\n"
                    + "#  %playtime_timesjoined% - shows the amount of times the player has joined the server\r\n"
                    + "#  %playtime_serveruptime% - shows the uptime of the server\r\n"
                    + "#  %playtime_position% - shows the players current position\r\n"
                    + "#  %playtime_top #_name% - shows the name of the top 10\r\n"
                    + "#  %playtime_top #_time% - shows the time of the top 10\r\n"
                    + "#  You can also use any other placeholder that PlaceholderAPI supports :) \r\n");

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

            Representer representer = new Representer(options);
            representer.addClassTag(ConfigWrapper.class, Tag.MAP);

            Yaml yaml = new Yaml(representer, options);
            yaml.dump(PlayTime.config == null ? new ConfigWrapper() : PlayTime.config, writer);
            writer.close();

        }catch (Exception e) {
            PlayTime.LOGGER.error("Exception occured while saving config: " +e.getMessage());
        }
    }

    public static ConfigWrapper loadConfig() {
        File file = new File(CONFIG_DIR + "playtime.yaml");
        if(file.exists()) {
            try {
                FileReader reader = new FileReader(file);
                ConfigWrapper config = new Yaml().loadAs(reader, ConfigWrapper.class);
                reader.close();
                return config;
            }catch (Exception e) {
                PlayTime.LOGGER.error("Exception occurred while saving config: " +e.getMessage());
            }
        }else{
            PlayTime.LOGGER.info("Creating PlayTime's config");
            new File(CONFIG_DIR).mkdir();
            saveConfig();
        }
        return new ConfigWrapper();
    }

}
