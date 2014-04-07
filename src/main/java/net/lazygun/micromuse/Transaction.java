package net.lazygun.micromuse;

import java.io.Closeable;

/**
 * TODO: Write Javadocs for this class.
 * Created: 06/04/2014 20:29
 *
 * @author Ewan
 */
public interface Transaction extends AutoCloseable {

    public void failure();

    public void success();

    public void acquireLock(Room room, boolean write);

    public void acquireLock(Link room, boolean write);

    public void acquireLock(Route room, boolean write);

    @Override
    void close();
}
