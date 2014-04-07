package net.lazygun.micromuse;

/**
 *
 * @author Ewan
 */
public interface RoomBuilder {
    public RoomBuilder name(String name);

    public RoomBuilder location(String location);

    public RoomBuilder description(String description);

    public RoomBuilder exits(String... exits);

    public Room build();

    public RoomBuilder copy(Room room);
}
