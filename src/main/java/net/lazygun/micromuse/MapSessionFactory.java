package net.lazygun.micromuse;

import java.util.List;

/**
 * TODO: Write Javadocs for this class.
 * Created: 06/04/2014 21:28
 *
 * @author Ewan
 */
public class MapSessionFactory implements SessionFactory {

    private final List<Link> links;
    private final RoomService roomService;

    public MapSessionFactory(List<Link> links, RoomService roomService) {
        this.links = links;
        this.roomService = roomService;
    }

    @Override
    public Session createSession() {
        return new MapSession(roomService, links);
    }
}
