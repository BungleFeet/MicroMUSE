package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 23/03/14 12:33
 *
 * @author Ewan
 */
public interface MuseMap {
    public Room getHome();
    public Link createLink(Link link);
    public Route findUnexploredRoom(Room nearestTo);
}
