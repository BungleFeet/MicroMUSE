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

    }

    public static long crawl(int threads, long delay, RoomService roomService, SessionFactory sessionFactory) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Integer>> jobs = new ArrayList<>(threads);
        jobs.add(executorService.submit(new Crawler(sessionFactory, roomService)));
        for (int i = 1; i < threads; i++) {
            try {
                Thread.sleep(delay);
                jobs.add(executorService.submit(new Crawler(sessionFactory, roomService)));
            } catch (InterruptedException ignored) {
            }
        }
        long totalLinks = 0;
        for (int i = 1; i <= threads; i++) {
            try {
                Integer links = jobs.get(i-1).get();
                System.out.println("Crawler thread " + i + " of " + threads + " completed. Created " + links + " links.");
                totalLinks += links;
            } catch (Exception e) {
                System.err.println("Exception running crawler thread " + i + " of " + threads + ": " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
        return totalLinks;
    }

    private final Navigator navigator;
    private final RoomService roomService;

    public Crawler(SessionFactory sessionFactory, RoomService roomService) {
        this.navigator = new Navigator(sessionFactory.createSession());
        this.roomService = roomService;
    }

    @Override
    public Integer call() {
        Room room = navigator.currentRoom();
        Integer links = 0;
        while (true) {
            try (Transaction tx = roomService.beginTransaction()) {
                System.err.println("At " + room);
                tx.acquireLock(room, false);
                Route route = room.findNearestUnexplored();
                tx.acquireLock(route, false);
                if (route == null) {
                    break;
                }
                try {
                    Link lastStep = navigator.traverse(route);
                    tx.acquireLock(lastStep, true);
                    Room from = lastStep.getFrom();
                    room = from.link(lastStep.getExit(), lastStep.getTo()).getTo();
                    links++;
                    tx.success();
                } catch (LinkAlreadyExistsException ex) {
                    // Another crawler must have followed this exit first.
                    // Go back to previous room and look for another unexplored room.
                    navigator.traverse(route.head());
                }
            }
        }
        return links;
    }
}
