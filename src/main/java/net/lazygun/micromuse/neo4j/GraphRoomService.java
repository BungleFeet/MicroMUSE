package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Room;
import net.lazygun.micromuse.RoomBuilder;
import net.lazygun.micromuse.RoomService;
import net.lazygun.micromuse.Transaction;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 *
 * @author Ewan
 */
public class GraphRoomService implements RoomService {

    private final GraphDatabaseService db;

    public GraphRoomService(GraphDatabaseService db) {
        this.db = db;
        RoomNode.initialise(db);
    }

    public GraphRoomService(String dbPath) {
        db = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
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
    public Room findOrCreate(Room room) {
        Room persisted = RoomNode.findByExample(room);
        if (persisted == null) {
            persisted = RoomNode.create(room.getName(), room.getLocation(), room.getDescription(), room.getExits());
        }
        return persisted;
    }

    @Override
    public Transaction beginTransaction() {
        return new GraphTransaction(db.beginTx());
    }
}
