package net.lazygun.micromuse;

/**
 *
 * @author Ewan
 */
public interface RoomService {
    public RoomBuilder builder();

    public Room findOrCreate(Room room);

    public Transaction beginTransaction();
}
