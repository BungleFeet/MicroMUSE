package net.lazygun.micromuse;

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

    private final Long id;
    private final String name;
    private final String description;
    private final List<String> exits = new ArrayList<String>();

    public Room(Long id, String name, String description, List<String> exits) {
        this.id = id;
        this.name = name;
        this.description = description;
        exits.addAll(exits);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getExits() {
        return Collections.unmodifiableList(exits);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Room room = (Room) o;

        if (!exits.equals(room.exits)) return false;
        if (!name.equals(room.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + exits.hashCode();
        return result;
    }
}
