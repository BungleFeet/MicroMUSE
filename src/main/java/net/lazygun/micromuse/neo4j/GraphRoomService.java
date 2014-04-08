package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Room;
import net.lazygun.micromuse.RoomBuilder;
import net.lazygun.micromuse.RoomService;
import net.lazygun.micromuse.Transaction;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import static net.lazygun.micromuse.neo4j.RoomNode.FINGERPRINT;
import static net.lazygun.micromuse.neo4j.RoomNode.ROOM;

/**
 *
 * @author Ewan
 */
public class GraphRoomService implements RoomService {

    private final GraphDatabaseService db;

    public GraphRoomService(GraphDatabaseService db) {
        this.db = db;
        RoomNode.initialise(db);
        try (org.neo4j.graphdb.Transaction tx = db.beginTx()) {
            db.schema().constraintFor(ROOM).assertPropertyIsUnique(FINGERPRINT).create();
            tx.success();
        }
    }

    public GraphRoomService(String dbPath) {
        this(new GraphDatabaseFactory().newEmbeddedDatabase(dbPath));
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.shutdown();
            }
        });
    }

    @Override
    public RoomBuilder builder() {
        return new GraphRoomBuilder();
    }

    @Override
    public Room findOrCreate(Room room) {
        while (true) {
            try {
                Room persisted = RoomNode.findByExample(room);
                if (persisted == null) {
                    persisted = RoomNode.create(room.getName(), room.getLocation(), room.getDescription(), room.getExits());
                }
                return persisted;
            } catch (NotFoundException ignored) {
            }
        }
    }

    @Override
    public Transaction beginTransaction() {
        return new GraphTransaction(db.beginTx());
    }
}
