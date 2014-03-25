package net.lazygun.micromuse;

import java.util.List;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 14:25
 *
 * @author Ewan
 */
public class TeleportableRoom extends Room implements Teleportable {

    private final String location;

    public TeleportableRoom(Long id, String location, String name, String description, List<String> exits) {
        super(id, name, description, exits);
        this.location = location;
    }

    @Override
    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TeleportableRoom that = (TeleportableRoom) o;

        if (!location.equals(that.location)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + location.hashCode();
        return result;
    }
}
