package net.lazygun.micromuse.neo4j;

import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by ewan on 28/03/2014.
 */
public class HelperUtils {

    public static <T> List<T> resourceIterableToList(ResourceIterable<T> iterable) {
        try (ResourceIterator<T> iterator = iterable.iterator()) {
            List<T> list = new ArrayList<>();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
            return list;
        }
    }
}
