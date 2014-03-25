package net.lazygun.micromuse;

import java.util.*;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 12:45
 *
 * @author Ewan
 */
public class Route implements Iterable<Link> {

    private final List<Link> path;

    public Route(List<Link> path) {
        this.path = new ArrayList<Link>(path);
    }

    public List<Link> getPath() {
        return Collections.unmodifiableList(path);
    }

    public Route head() {
        return new Route(path.subList(0, path.size() - 1));
    }

    public Route tail() {
        return new Route(path.subList(1, path.size()));
    }

    public Link first() {
        return path.get(0);
    }

    public Link last() {
        return path.get(path.size() - 1);
    }

    public ListIterator<Link> iterator() {
        final ListIterator<Link> iterator = path.listIterator();
        return new ListIterator<Link>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Link next() {
                return iterator.next();
            }

            @Override
            public boolean hasPrevious() {
                return iterator.hasPrevious();
            }

            @Override
            public Link previous() {
                return iterator.previous();
            }

            @Override
            public int nextIndex() {
                return iterator.nextIndex();
            }

            @Override
            public int previousIndex() {
                return iterator.previousIndex();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();

            }

            @Override
            public void set(Link link) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(Link link) {
                throw new UnsupportedOperationException();
            }
        };
    }

}
