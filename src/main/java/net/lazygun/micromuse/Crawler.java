package net.lazygun.micromuse;

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

    public static void main(String[] args) {
        final GraphDatabaseService db = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                db.shutdown();
            }
        });
    }

    private final Navigator navigator;
    private final MuseMap museMap;

    public Crawler(Navigator navigator, MuseMap museMap) {
        this.navigator = navigator;
        this.museMap = museMap;
    }

    public void run() {
        for (Route route = museMap.findUnexploredRoom(navigator.currentRoom());
             route != null;
             route = museMap.findUnexploredRoom(navigator.currentRoom())) {
            try {
                Link lastStep = navigator.traverse(route);
                museMap.createLink(lastStep);
            } catch (LinkAlreadyExistsException ex) {
                // Another crawler must have followed this exit first.
                // Go back to previous room and look for another unexplored room.
                navigator.traverse(route.head());
            }
        }
    }
}
