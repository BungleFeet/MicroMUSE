package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 06/04/2014 20:29
 *
 * @author Ewan
 */
public interface Transaction extends AutoCloseable {

    public void failure();

    public void success();

    @Override
    void close();
}
