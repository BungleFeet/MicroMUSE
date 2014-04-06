package net.lazygun.micromuse;

import java.util.*;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 12:45
 *
 * @author Ewan
 */
public class Route<T> implements Iterable<Link<T>> {

    private final List<Link<T>> path;

    public Route(List<Link<T>> path) {
        this.path = new ArrayList<>(path);
    }

    public List<Link<T>> getPath() {
        return Collections.unmodifiableList(path);
    }

    public Route<T> head() {
        return new Route<>(path.subList(0, path.size() - 1));
    }

    public Route<T> tail() {
        return new Route<>(path.subList(1, path.size()));
    }

    public Link<T> first() {
        return path.get(0);
    }

    public Link<T> last() {
        return path.get(path.size() - 1);
    }

    public int size() {
        return path.size();
    }

    public ListIterator<Link<T>> iterator() {
        final ListIterator<Link<T>> iterator = path.listIterator();
        return new ListIterator<Link<T>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Link<T> next() {
                return iterator.next();
            }

            @Override
            public boolean hasPrevious() {
                return iterator.hasPrevious();
            }

            @Override
            public Link<T> previous() {
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
            public void set(Link<T> link) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void add(Link<T> link) {
                throw new UnsupportedOperationException();
            }
        };
    }

}
