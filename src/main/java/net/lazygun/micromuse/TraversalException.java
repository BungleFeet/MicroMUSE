package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 07/04/2014 22:36
 *
 * @author Ewan
 */
public class TraversalException extends Exception {
    public TraversalException() {
    }

    public TraversalException(String message) {
        super(message);
    }

    public TraversalException(String message, Throwable cause) {
        super(message, cause);
    }

    public TraversalException(Throwable cause) {
        super(cause);
    }

    public TraversalException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
