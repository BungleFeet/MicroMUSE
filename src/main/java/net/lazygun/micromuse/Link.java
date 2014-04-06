package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 12:56
 *
 * @author Ewan
 */
public class Link<T> {

    private final T from;
    private final String exit;
    private final T to;

    public Link(T from, String exit, T to) {
        this.from = from;
        this.exit = exit;
        this.to = to;
    }

    public T from() {
        return from;
    }

    public String exit() {
        return exit;
    }

    public T to() {
        return to;
    }
}
