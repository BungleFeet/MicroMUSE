package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Room;
import net.lazygun.micromuse.RoomBuilder;
import net.lazygun.micromuse.RoomService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 *
 * @author Ewan
 */
public class GraphRoomService implements RoomService {

    public GraphRoomService(String dbPath) {
        final GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.shutdown();
            }
        });
        RoomNode.initialise(db);
    }

    @Override
    public RoomBuilder builder() {
        return new GraphRoomBuilder();
    }

    @Override
    public Room findByLocation(String location) {
        return RoomNode.findByLocation(location);
    }
}
