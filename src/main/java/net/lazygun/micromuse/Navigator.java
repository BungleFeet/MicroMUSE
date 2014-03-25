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

    Link traverse(Route route) {
        Link first = route.first();
        if (!first.from().equals(currentRoom())) {
            if (!(first.from() instanceof TeleportableRoom)) {
                throw new IllegalArgumentException("Cannot traverse a Route unless it begins with a TeleportableRoom");
            }
            TeleportableRoom begin = (TeleportableRoom) first.from();
            session.teleport(begin);
        }
        Link last = first;
        for (Link link : route) {
            last = follow(link);
        }
        return last;
    }

    private Link follow(Link link) {
        Room currentRoom = currentRoom();
        if (!currentRoom.equals(link.from())) {
            throw new UnexpectedRoomException("Room trying to exit is not the same as Link from Room", link.from(), currentRoom);
        }
        currentRoom = session.exit(link.exit());
        if (link.to() instanceof UnexploredRoom) {
            return new Link(link.from(), link.exit(), currentRoom);
        } else if (currentRoom().equals(link.to())) {
            return link;
        } else {
            throw new UnexpectedRoomException("Room entered into is not the same as Link to Room", link.to(), currentRoom);
        }
    }

}
