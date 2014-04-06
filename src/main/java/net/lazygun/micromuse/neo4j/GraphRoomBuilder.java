package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Room;
import net.lazygun.micromuse.RoomBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Ewan
 */
public class GraphRoomBuilder implements RoomBuilder {

    private String name;
    private String location;
    private String description;
    private List<String> exits;

    @Override
    public RoomBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public RoomBuilder location(String location) {
        this.location = location;
        return this;
    }

    @Override
    public RoomBuilder description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public RoomBuilder exits(String... exits) {
        this.exits = new ArrayList<>(Arrays.asList(exits));
        return this;
    }

    @Override
    public Room build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Room must have a non-empty name property");
        }
        if (location != null && location.isEmpty()) {
            location = null;
        }
        if (description == null) {
            description = "";
        }
        if (exits == null) {
            exits = Collections.emptyList();
        }
        return RoomNode.create(name, location, description, exits);
    }
}
