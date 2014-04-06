package net.lazygun.micromuse;

/**
 *
 * @author Ewan
 */
public interface RoomService {
    public RoomBuilder builder();

    public Room findByLocation(String location);
}
