package net.lazygun.micromuse;

import net.lazygun.micromuse.neo4j.GraphRoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 *
 * @author Ewan
 */
public class Crawler implements Callable<Integer> {

    public static final String DB_PATH = "./db";
    public static final int CONCURRENT_CRAWLERS = 1;

    public static void main(String[] args) {
        RoomService roomService = new GraphRoomService(DB_PATH);
        //Navigator nav = new Navigator(new Session(roomService));
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Integer>> jobs = new ArrayList<>(CONCURRENT_CRAWLERS);
        for (int i = 0; i < CONCURRENT_CRAWLERS; i++) {
            //jobs.set(i, executorService.submit(new Crawler(nav)));
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ignored) {
            }
        }
        for (int i = 0; i < CONCURRENT_CRAWLERS; i++) {
            try {
                Integer links = jobs.get(i).get();
                System.out.println("Crawler thread " + i + " of " + CONCURRENT_CRAWLERS + " completed. Created " + links + "links.");
            } catch (Exception e) {
                System.err.println("Exception running crawler thread " + i + " of " + CONCURRENT_CRAWLERS + ": " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private final Navigator navigator;

    public Crawler(Navigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public Integer call() {
        Room room = navigator.currentRoom();
        Route route;
        Integer links = 0;
        while ((route = room.findNearestUnexplored()) != null) {
            try {
                Link lastStep = navigator.traverse(route);
                room = room.link(lastStep.exit(), lastStep.to());
                links++;
            } catch (LinkAlreadyExistsException ex) {
                // Another crawler must have followed this exit first.
                // Go back to previous room and look for another unexplored room.
                navigator.traverse(route.head());
            }
        }
        return links;
    }
}
