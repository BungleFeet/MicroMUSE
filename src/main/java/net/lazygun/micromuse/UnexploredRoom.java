package net.lazygun.micromuse;

import java.util.ArrayList;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 14:27
 *
 * @author Ewan
 */
public class UnexploredRoom extends Room {

    public static final String NAME = "UNEXPLORED ROOM";

    private static final UnexploredRoom INSTANCE = new UnexploredRoom(null);

    public static UnexploredRoom getInstance() {
        return INSTANCE;
    }

    public UnexploredRoom(Long id) {
        super(id, NAME, "", new ArrayList<String>());
    }
}
