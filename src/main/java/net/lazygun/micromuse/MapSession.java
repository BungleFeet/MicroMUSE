package net.lazygun.micromuse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ewan
 */
public class MapSession implements Session {

    private final RoomService roomService;
    private final List<Link> links;

    private Room currentRoom;

    public MapSession(RoomService roomService, List<Link> links) {
        this.roomService = roomService;
        this.links = new ArrayList<>(links);
        Room firstRoom = links.get(0).getFrom();
        try (Transaction tx = roomService.beginTransaction()) {
            currentRoom = roomService.findOrCreate(firstRoom);
            tx.success();
        }
    }

    @Override
    public Room getCurrentRoom() {
        return currentRoom;
    }

    @Override
    public Room teleport(String location) {
        for (Link link : links) {
            if (link.getFrom().isTeleportable() && link.getFrom().getLocation().equals(location)) {
                currentRoom = roomService.findOrCreate(link.getFrom());
                return currentRoom;
            }
            if (link.getTo().isTeleportable() && link.getTo().getLocation().equals(location)) {
                currentRoom = roomService.findOrCreate(link.getTo());
                return currentRoom;
            }
        }
        throw new IllegalArgumentException("No room with location '" + location + "' exists.");
    }

    @Override
    public Room exit(String exit) throws TraversalException {
        if (currentRoom.getExits().contains(exit)) {
            for (Link link : links) {
                if (link.getFrom().getName().equals(currentRoom.getName()) && link.getExit().equals(exit)) {
                    try {
                        currentRoom = roomService.findOrCreate(link.getTo());
                    } catch (Exception e) {
                        throw new TraversalException(e);
                    }
                    return currentRoom;
                }
            }
            throw new IllegalStateException("This exit isn't in the map: " + exit);
        }
        throw new IllegalArgumentException("The current room has no exit name '" + exit + "'");
    }

    @Override
    public void close() throws IOException {}
}
