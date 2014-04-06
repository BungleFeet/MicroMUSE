package net.lazygun.micromuse;

import net.lazygun.micromuse.neo4j.RoomNode;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * TODO: Write Javadocs for this class.
 * Created: 23/03/14 12:39
 *
 * @author Ewan
 */
public class Crawler {

    public static final String DB_PATH = "./db";

    private static GraphDatabaseService db;

    public static void main(String[] args) {
        db = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.shutdown();
            }
        });
    }

    private final Navigator navigator;

    public Crawler(Navigator navigator) {
        this.navigator = navigator;
    }

    public void run() {
        Room room = RoomNode.load(navigator.currentRoom(), db);
        Route route;
        while ((route = room.findNearestUnexplored()) != null) {
            try {
                Link lastStep = navigator.traverse(route);
                room = room.link(lastStep.exit(), lastStep.to());
            } catch (LinkAlreadyExistsException ex) {
                // Another crawler must have followed this exit first.
                // Go back to previous room and look for another unexplored room.
                navigator.traverse(route.head());
            }
        }
    }
}
