package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 14:55
 *
 * @author Ewan
 */
public class UnexpectedRoomException extends TraversalException {

    private final Room expected;
    private final Room actual;

    public UnexpectedRoomException(String message, Room expected, Room actual) {
        super(message);
        this.expected = expected;
        this.actual = actual;
    }

    public Room getExpected() {
        return expected;
    }

    public Room getActual() {
        return actual;
    }
}
