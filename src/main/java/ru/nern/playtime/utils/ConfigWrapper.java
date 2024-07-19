package ru.nern.playtime.utils;

public class ConfigWrapper {

    public static class Time {
        public static class FormattedTimeUnit {
            public boolean enabled = true;
            public String prefix;

            public FormattedTimeUnit() {}

            public FormattedTimeUnit(String prefix) {
                this.prefix = prefix;
            }
        }
        public FormattedTimeUnit second = new FormattedTimeUnit("s");
        public FormattedTimeUnit minute = new FormattedTimeUnit("m");
        public FormattedTimeUnit hour = new FormattedTimeUnit("h");
        public FormattedTimeUnit day = new FormattedTimeUnit("d");
        public FormattedTimeUnit week = new FormattedTimeUnit("w");
    }

    public static class Messages {
        public static class TopMessageType {
            public String[] header = new String[]{"&bTop &e10 &bplayers playtime:", ""};
            public String[] message = new String[]{"&a%position%. &b%player%: &e%playtime%"};
            public String[] footer = new String[]{""};
        }
        //Removed because we are using brigadier
//        public String[] no_permission = new String[]{"&8[&bPlayTime&8] &cYou don't have permission."};
        public String[] doesnt_exist = new String[]{"&8[&bPlayTime&8] &cPlayer %offlineplayer% has not joined before!"};
        public String[] player = new String[]{"&b%playtime:player%'s Stats are:", "&bPlayTime: &7%playtime:time%", "&bTimes Joined: &7%playtime:timesjoined%"};
        public String[] offline_players = new String[]{"&b%offlineplayer%'s Stats are:", "&bPlayTime: &7%offlinetime%", "&bTimes Joined: &7%offlinetimesjoined%"};
        public String[] other_players = new String[]{"&b%playtime:player%'s Stats are:", "&bPlayTime: &7%playtime:time%", "&bTimes Joined: &7%playtime:timesjoined%"};
        public TopMessageType playtimetop = new TopMessageType();
        public String[] server_uptime = new String[]{"&8[&bPlayTime&8] &bServer's total uptime is %playtime:serveruptime%"};
        public String[] reload_config = new String[]{"&8[&bPlayTime&8] &bYou have successfully reloaded the config."};
        public String[] resync_stats = new String[]{"&8[&bPlayTime&8] &bStarted async player data synchronization.", "&8[&bPlayTime&8] &bAsync player data synchronization finished!"};
    }

    public static class PlaceHolder {
        public static class Top {
            public String name = "none";
            public String time = "-";
        }
        public Top top = new Top();
    }

    public Time time = new Time();
    public Messages messages = new Messages();
    public PlaceHolder placeholder = new PlaceHolder();
}

