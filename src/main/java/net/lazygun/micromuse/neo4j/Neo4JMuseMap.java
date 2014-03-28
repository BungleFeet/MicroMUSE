package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.*;
import org.neo4j.graphdb.*;

/**
 * Created: 23/03/14 13:34
 *
 * @author Ewan
 */
public class Neo4JMuseMap implements MuseMap {

    private final GraphDatabaseService graphDb;

    public Neo4JMuseMap(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
    }

    @Override
    public Room getHome() {
        try (Transaction tx = graphDb.beginTx()) {
            Room room = new RoomNode(graphDb.getNodeById(0));
            tx.success();
            return room;
        }
    }

}
