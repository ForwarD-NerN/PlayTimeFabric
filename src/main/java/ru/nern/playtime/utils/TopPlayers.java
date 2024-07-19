package ru.nern.playtime.utils;

public class TopPlayers {
    public String name;
    public String uuid;
    public Integer time;

    public TopPlayers() {
        time = 0;
    }

    public TopPlayers(String name, String uuid, int time) {
        this.name = name;
        this.uuid = uuid;
        this.time = time;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopPlayers that = (TopPlayers) o;
        return name.equals(that.name);
    }
}
