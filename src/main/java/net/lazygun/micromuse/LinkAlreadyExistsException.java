package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 13:38
 *
 * @author Ewan
 */
public class LinkAlreadyExistsException extends TraversalException {
    private final Link link;

    public LinkAlreadyExistsException(Link link) {
        super("Link already exists: " + link);
        this.link = link;
    }

    public Link getLink() {
        return link;
    }
}
