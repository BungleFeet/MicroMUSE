package net.lazygun.micromuse;

/**
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

    Link traverse(Route route) throws TraversalException {
        if (route.last().getTo().isTeleportable()) {
            session.teleport(route.last().getTo().getLocation());
            return route.last();
        }
        route = optimiseRoute(route);
        Link first = route.first();
        if (!first.getFrom().equals(currentRoom())) {
            if (!first.getFrom().isTeleportable()) {
                throw new IllegalArgumentException("Cannot traverse a Route that doesn't contain a teleportable room");
            }
            session.teleport(first.getFrom().getLocation());
        }
        Link last = first;
        for (Link link : route) {
            last = follow(link);
        }
        return last;
    }

    private Route optimiseRoute(Route route) {
        if (countTeleportableRooms(route) > 0) {
            while (!route.first().getFrom().isTeleportable() || countTeleportableRooms(route) > 1) {
                route = route.tail();
            }
        }
        return route;
    }

    private int countTeleportableRooms(Route route) {
        int count = 0;
        for (Link link : route) {
            if (link.getFrom().isTeleportable()) {
                count++;
            }
        }
        return count;
    }

    private Link follow(Link link) throws TraversalException {
        Room currentRoom = currentRoom();
        if (!currentRoom.equals(link.getFrom())) {
            throw new UnexpectedRoomException("Room trying to exit is not the same as Link from Room", link.getFrom(), currentRoom);
        }
        currentRoom = session.exit(link.getExit());
        if (link.getTo().isUnexplored()) {
            return new Link(link.getFrom(), link.getExit(), currentRoom);
        } else if (currentRoom().equals(link.getTo())) {
            return link;
        } else {
            throw new UnexpectedRoomException("Room entered into is not the same as Link to Room", link.getTo(), currentRoom);
        }
    }

}
