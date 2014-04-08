package net.lazygun.micromuse;

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
        Integer links = 0;
        while (true) {
            Room room = navigator.currentRoom();
            try (Transaction tx = roomService.beginTransaction()) {
                try {
                    Route route = room.findNearestUnexplored();
                    if (route == null) {
                        break;
                    }
                    System.err.println(Thread.currentThread().getName() + " exploring " + route.last().getTo());
                    Link lastStep = navigator.traverse(route);
                    Room from = lastStep.getFrom();
                    from.link(lastStep.getExit(), lastStep.getTo()).getTo();
                    links++;
                    tx.success();
                } catch (TraversalException ex) {
                    // Another crawler must be operating on this route,
                    // so we just try again.
                    System.err.println(ex.getLocalizedMessage());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        return links;
    }
}
