package net.lazygun.micromuse;

import java.util.List;

/**
 * TODO: Write Javadocs for this class.
 * Created: 06/04/2014 13:24
 *
 * @author Ewan
 */
public interface Room {

    public long getId();

    public String getName();

    public String getLocation();

    public String getDescription();

    public List<String> getExits();

    public boolean isTeleportable();

    public boolean isUnexplored();

    public Link link(String exit, Room to) throws TraversalException;

    public Route findNearestUnexplored() throws TraversalException;

    public Room exit(String exit) throws TraversalException;
}
