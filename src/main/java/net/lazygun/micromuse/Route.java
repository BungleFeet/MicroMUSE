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
        this.path = new ArrayList<>(path);
    }

    public Route head() {
        return size() > 1 ? new Route(path.subList(0, path.size() - 1)) : new Route(Collections.<Link>emptyList());
    }

    public Route tail() {
        return size() > 1 ? new Route(path.subList(1, path.size())) : new Route(Collections.<Link>emptyList());
    }

    public Link first() {
        return size() > 0 ? path.get(0) : null;
    }

    public Link last() {
        return size() > 0 ? path.get(size() - 1) : null;
    }

    public int size() {
        return path.size();
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
