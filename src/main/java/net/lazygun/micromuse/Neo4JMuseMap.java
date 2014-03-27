package net.lazygun.micromuse;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: Write Javadocs for this class.
 * Created: 23/03/14 13:34
 *
 * @author Ewan
 */
public class Neo4JMuseMap implements MuseMap {

    public static final Label ROOM = new Label() {
        @Override
        public String name() {
            return "ROOM";
        }
    };
    public static final Label UNEXPLORED = new Label() {
        @Override
        public String name() {
            return "UNEXPLORED";
        }
    };
    public static final Label TELEPORTABLE = new Label() {
        @Override
        public String name() {
            return "TELEPORTABLE";
        }
    };
    public static enum RelTypes implements RelationshipType { EXIT }

    private final GraphDatabaseService graphDb;
    private final TraversalDescription unexploredRoomNodeFinder;

    public Neo4JMuseMap(GraphDatabaseService graphDb) {
        this.graphDb = graphDb;
        unexploredRoomNodeFinder = graphDb.traversalDescription()
                .breadthFirst()
                .relationships(RelTypes.EXIT, Direction.OUTGOING)
                .uniqueness(Uniqueness.NODE_PATH)
                .evaluator(new Evaluator() {
                    @Override
                    public Evaluation evaluate(Path path) {
                        return path.endNode().hasLabel(UNEXPLORED)
                                ? Evaluation.INCLUDE_AND_PRUNE
                                : Evaluation.EXCLUDE_AND_CONTINUE;
                    }
                });
    }

    @Override
    public Room getHome() {
        try (Transaction tx = graphDb.beginTx()) {
            Room room = nodeToRoom(graphDb.getNodeById(0));
            tx.success();
            return room;
        }
    }

    @Override
    public Link createLink(Link link) {
        System.out.println("Creating link (" + link.from().getName() +")-[" + link.exit() + "]->(" + link.to().getName() + ")");
        try (Transaction tx = graphDb.beginTx()) {
            Node from = findNode(link.from());
            Relationship exit = null;
            for (Relationship rel : from.getRelationships(RelTypes.EXIT, Direction.OUTGOING)) {
                if (rel.getProperty("name").equals(link.exit())) {
                    exit = rel;
                }
            }
            if (exit == null) {
                throw new IllegalStateException("Expected exit relation '" + link.exit() + "' doesn't exist on room node " + from);
            }
            Node to = exit.getEndNode();
            if (to.hasLabel(UNEXPLORED)) {
                exit.delete();
                to.delete();
                to = findNode(link.to());
                if (to == null) {
                    to = saveRoom(link.to());
                }
                createExitRelationship(from, to, link.exit());
            }
            else {
                Room toRoom = nodeToRoom(to);
                if (toRoom != link.to()) {
                    throw new IllegalStateException("Link already exists, but destination doesn't match that given.");
                }
            }
            tx.success();
            return new Link(link.from(), link.exit(), nodeToRoom(to));
        }
    }

    @Override
    public Route findUnexploredRoom(Room nearestTo) {
        try (Transaction ignored = graphDb.beginTx();
             ResourceIterator<Path> unexploredNodePaths = unexploredRoomNodeFinder.traverse(findNode(nearestTo)).iterator()) {
            Path shortestPath = null;
            while (unexploredNodePaths.hasNext()) {
                Path path = unexploredNodePaths.next();
                if (path.length() == 1) {
                    return pathToRoute(path);
                } else if (shortestPath == null || path.length() < shortestPath.length()) {
                    shortestPath = path;
                }
            }
            return pathToRoute(shortestPath);
        }
    }

    private Route pathToRoute(Path path) {
        if (path == null) {
            return null;
        }
        List<Link> links = new ArrayList<>();
        for (Relationship rel : path.reverseRelationships()) {
            Link link = relationshipToLink(rel);
            links.add(link);
            if (link.from() instanceof Teleportable) {
                Collections.reverse(links);
                return new Route(links);
            }
        }
        throw new IllegalArgumentException("Cannot create a Route from a Path that doesn't contain a Teleportable Room");
    }

    private Link relationshipToLink(Relationship relationship) {
        Room from = nodeToRoom(relationship.getStartNode());
        String exit = (String) relationship.getProperty("name");
        Room to = nodeToRoom(relationship.getEndNode());
        return new Link(from, exit, to);
    }

    private Relationship createExitRelationship(Node from, Node to, String name) {
        Relationship exit = from.createRelationshipTo(to, RelTypes.EXIT);
        exit.setProperty("name", name);
        return exit;
    }

    public Room nodeToRoom(Node roomNode) {
        Long id = roomNode.getId();
        String name = (String) roomNode.getProperty("name");
        String description = (String) roomNode.getProperty("description");
        List<String> exits = new ArrayList<>();
        for (Relationship rel : roomNode.getRelationships(RelTypes.EXIT, Direction.OUTGOING)) {
            exits.add((String) rel.getProperty("name"));
        }
        if (roomNode.hasLabel(ROOM)) {
            if (roomNode.hasLabel(UNEXPLORED)) {
                return new UnexploredRoom(id);
            } else if (roomNode.hasLabel(TELEPORTABLE)) {
                String location = (String) roomNode.getProperty("location");
                return new TeleportableRoom(id, location, name, description, exits);
            } else {
                return new Room(id, name, description, exits);
            }
        } else {
            throw new IllegalArgumentException("Node does not have label " + ROOM.name());
        }
    }

    private Label[] roomLabels(Room room) {
        List<Label> labels = new ArrayList<>();
        labels.add(ROOM);
        if (room instanceof UnexploredRoom) {
            labels.add(UNEXPLORED);
        } else if (room instanceof Teleportable) {
            labels.add(TELEPORTABLE);
        }
        return labels.toArray(new Label[labels.size()]);
    }

    public Node findNode(final Room room) {
        // If room has an Id, use that to load the node
        if (room.getId() != null) {
            return graphDb.getNodeById(room.getId());
        }

        // Otherwise, if room is teleportable, use the location (which should be unique) to find the node
        else if (room instanceof Teleportable)  {
            String location = ((Teleportable) room).getLocation();
            try (ResourceIterator<Node> matches = graphDb.findNodesByLabelAndProperty(TELEPORTABLE, "location", location).iterator()) {
                if (matches.hasNext()) {
                    Node match = matches.next();
                    if (matches.hasNext()) {
                        throw new IllegalStateException("Found more than one Node with location " + location);
                    }
                    return match;
                }
                return null;
            }
        }

        // Otherwise, try to find a ROOM node with the same name, and same exit relationships.
        else {
            String name = room.getName();
            TraversalDescription roomMatcher = graphDb.traversalDescription().breadthFirst().evaluator(new Evaluator() {
                @Override
                public Evaluation evaluate(Path path) {
                    List<String> exitsToMatch = new ArrayList<>(room.getExits());
                    Iterable<Relationship> relationships =
                            path.startNode().getRelationships(RelTypes.EXIT, Direction.OUTGOING);
                    for (Relationship rel : relationships) {
                        if (!exitsToMatch.remove(rel.getProperty("name").toString())) {
                            return Evaluation.EXCLUDE_AND_PRUNE;
                        }
                    }
                    return exitsToMatch.size() == 0 ? Evaluation.INCLUDE_AND_PRUNE
                                                    : Evaluation.EXCLUDE_AND_PRUNE;
                }
            });
            List<Node> matches = new ArrayList<>();
            try (ResourceIterator<Node> matcher = graphDb.findNodesByLabelAndProperty(ROOM, "name", name).iterator()) {
                while (matcher.hasNext()) {
                    matches.addAll(resourceIterableToList(roomMatcher.traverse(matcher.next()).nodes()));
                }
            }
            switch (matches.size()) {
                case 0: return null;
                case 1: return matches.get(0);
                default: throw new IllegalStateException("Found " + matches.size() + " Node matching Room " + room);
            }
        }
    }

    private Node saveRoom(Room room) {
        if (room.getId() == null) {
            Node roomNode = graphDb.createNode(roomLabels(room));
            roomNode.setProperty("name", room.getName());
            roomNode.setProperty("description", room.getDescription());
            for (String exit : room.getExits()) {
                Node unexplored = saveRoom(UnexploredRoom.getInstance());
                createExitRelationship(roomNode, unexplored, exit);
            }
            return roomNode;
        } else {
            throw new IllegalArgumentException("Cannot create Node from Room with existing Id");
        }
    }

    private <T> List<T> resourceIterableToList(ResourceIterable<T> iterable) {
        try (ResourceIterator<T> iterator = iterable.iterator()) {
            List<T> list = new ArrayList<>();
            while (iterator.hasNext()) {
                list.add(iterator.next());
            }
            return list;
        }
    }
}
