package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Room;
import org.neo4j.graphdb.Node;

/**
 *
 * Created by ewan on 28/03/2014.
 */
public class NodeMismatchException extends RuntimeException {

    private final Node node;
    private final Room room;

    public NodeMismatchException(String property, Node node, Room room) {
        super(node + " missing '" + property + "' property, or property value doesn't match" + room);
        this.node = node;
        this.room = room;
    }

    public Node getNode() {
        return node;
    }

    public Room getRoom() {
        return room;
    }
}
