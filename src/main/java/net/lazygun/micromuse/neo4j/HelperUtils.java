package net.lazygun.micromuse.neo4j;

import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * Created by ewan on 28/03/2014.
 */
public class HelperUtils {

    public static <T> List<T> iterableToList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        for (T e : iterable) {
            list.add(e);
        }
        return list;
    }

    public static List<Relationship> getRelationships(Node node) {
        return iterableToList(node.getRelationships());
    }

    public static List<Relationship> getRelationships(Node node, Direction direction, RelationshipType... types) {
        return iterableToList(node.getRelationships(direction, types));
    }

    public static List<String> getExits(Node node) {
        List<String> exits = new ArrayList<>();
        for (Relationship link : getRelationships(node, Direction.OUTGOING, RoomNode.Relation.EXIT)) {
            exits.add((String) link.getProperty("name"));
        }
        Collections.sort(exits);
        return exits;
    }
}
