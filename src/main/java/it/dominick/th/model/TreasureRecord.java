package it.dominick.th.model;

import lombok.Getter;

@Getter
public class TreasureRecord {
    private final String id;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String command;

    public TreasureRecord(String id, String world, int x, int y, int z, String command) {
        this.id = id;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.command = command;
    }

    @Override
    public String toString() {
        return "TreasureRecord{" +
                "id='" + id + '\'' +
                ", world='" + world + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", command='" + command + '\'' +
                '}';
    }
}

