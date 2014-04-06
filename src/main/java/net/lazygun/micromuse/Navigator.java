package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 14:22
 *
 * @author Ewan
 */
public class Navigator {

    private final Session session;

    public Navigator(Session session) {
        this.session = session;
    }

    Room currentRoom() {
        return session.getCurrentRoom();
    }

    Link<Room> traverse(Route<Room> route) {
        Link<Room> first = route.first();
        if (!first.from().equals(currentRoom())) {
            if (!first.from().isTeleportable()) {
                throw new IllegalArgumentException("Cannot traverse a Route unless it begins with a teleportable room");
            }
            session.teleport(first.from());
        }
        Link<Room> last = first;
        for (Link<Room> link : route) {
            last = follow(link);
        }
        return last;
    }

    private Link<Room> follow(Link<Room> link) {
        Room currentRoom = currentRoom();
        if (!currentRoom.equals(link.from())) {
            throw new UnexpectedRoomException("Room trying to exit is not the same as Link from Room", link.from(), currentRoom);
        }
        currentRoom = session.exit(link.exit());
        if (link.to().isUnexplored()) {
            return new Link<Room>(link.from(), link.exit(), currentRoom);
        } else if (currentRoom().equals(link.to())) {
            return link;
        } else {
            throw new UnexpectedRoomException("Room entered into is not the same as Link to Room", link.to(), currentRoom);
        }
    }

}
