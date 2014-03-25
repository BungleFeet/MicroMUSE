package net.lazygun.micromuse;

import java.io.Closeable;

/**
 * TODO: Write Javadocs for this class.
 * Created: 23/03/14 12:14
 *
 * @author Ewan
 */
public interface Session extends Closeable {

    public static final String HOST = "micromuse.musenet.org";
    public static final Integer PORT = 4201;

    Room getCurrentRoom();
    Room teleport(TeleportableRoom destination);
    Room exit(String exit);
}
