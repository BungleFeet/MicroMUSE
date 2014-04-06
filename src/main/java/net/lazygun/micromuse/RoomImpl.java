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
public class RoomImpl implements Room {

    public static final RoomImpl UNEXPLORED = new RoomImpl("UNEXPLORED", null, "");

    private final String name;
    private final String location;
    private final String description;
    private final List<String> exits = new ArrayList<>();

    public RoomImpl(String name, String location, String description, Collection<String> exits) {
        this(name, location, description);
        this.exits.addAll(exits);
        Collections.sort(this.exits);
    }

    public RoomImpl(String name, String location, String description) {
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

    @Override
    public Room link(String exit, Room to) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isSameAs(RoomImpl room) {
        return (getClass().equals(room.getClass()) && this.equals(room)) || (
                this.name.equals(room.getName()) &&
                ((this.location == null && room.getLocation() == null) ||
                 (this.location != null && this.location.equals(room.getLocation()))) &&
                this.description.equals(room.getDescription()) &&
                this.exits.equals(room.getExits())
        );
    }

    public Route findNearestUnexplored() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public RoomImpl exit(String exit) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoomImpl)) return false;

        final RoomImpl room = (RoomImpl) o;

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
