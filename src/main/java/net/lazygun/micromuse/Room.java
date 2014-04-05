package net.lazygun.micromuse;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * TODO: Write Javadocs for this class.
 * Created: 23/03/14 12:19
 *
 * @author Ewan
 */
public class Room {

    public static final Room UNEXPLORED = new Room("UNEXPLORED", null, "");

    private final String name;
    private final String location;
    private final String description;
    private final List<String> exits = new ArrayList<>();

    public Room(String name, String location, String description, Collection<String> exits) {
        this(name, location, description);
        this.exits.addAll(exits);
        Collections.sort(this.exits);
    }

    public Room(String name, String location, String description) {
        this.name = name;
        this.location = location;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getExits() {
        return Collections.unmodifiableList(exits);
    }

    public boolean isTeleportable() {
        return location != null && !location.isEmpty();
    }

    public boolean isUnexplored() {
        return name.equals(UNEXPLORED.getName());
    }

    public boolean isSameAs(Room room) {
        return this.equals(room) || (
                this.name.equals(room.getName()) &&
                ((this.location == null && room.getLocation() == null) ||
                 (this.location != null && this.location.equals(room.getLocation()))) &&
                this.description.equals(room.getDescription()) &&
                this.exits.equals(room.getExits())
        );
    }

    public Room link(String exit, Room to) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Route findNearestUnexplored() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Room exit(String exit) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;

        final Room room = (Room) o;

        return exits.equals(room.exits) && !(location != null ? !location.equals(room.location)
                                                              : room.location != null) && name.equals(room.name);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + exits.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Room{" + "name='" + name + '\'' + ", exits=" + exits + ", location='" + location + '\'' + '}';
    }
}
