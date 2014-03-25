package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 13:38
 *
 * @author Ewan
 */
public class LinkAlreadyExistsException extends RuntimeException {
    private final Link link;

    public LinkAlreadyExistsException(Link link) {
        this.link = link;
    }

    public LinkAlreadyExistsException(String message, Link link) {
        super(message);
        this.link = link;
    }

    public Link getLink() {
        return link;
    }
}
