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
                .evaluator(new Evaluator() {
                    @Override
                    public Evaluation evaluate(Path path) {
                        if (path.length() == 0) {
                            return path.endNode().getRelationships(Direction.OUTGOING).iterator().hasNext()
                                    ? Evaluation.EXCLUDE_AND_CONTINUE
                                    : Evaluation.INCLUDE_AND_PRUNE;
                        }
                        for (Relationship rel : path.endNode().getRelationships(Direction.OUTGOING)) {
                            if (rel.getEndNode().getId() != path.lastRelationship().getStartNode().getId()) {
                                return Evaluation.EXCLUDE_AND_CONTINUE;
                            }
                        }
                        return Evaluation.INCLUDE_AND_PRUNE;
                    }
                });
    }

    public TraversalDescription getUnexploredRoomNodeFinder() {
        return unexploredRoomNodeFinder;
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
        try (Transaction tx = graphDb.beginTx()) {
            Node from = getNode(link.from());
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
                to = saveRoom(link.to());
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
             ResourceIterator<Path> unexploredNodePaths = unexploredRoomNodeFinder.traverse(getNode(nearestTo)).iterator()) {
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

    public Node getNode(Room room) {
        if (room.getId() != null) {
            return graphDb.getNodeById(room.getId());
        } else  {
            throw new IllegalArgumentException("Cannot find Room without Id");
        }
    }

    private Node saveRoom(Room room) {
        if (room.getId() == null) {
            Node roomNode = graphDb.createNode(roomLabels(room));
            roomNode.setProperty("name", room.getName());
            roomNode.setProperty("description", room.getDescription());
            for (String exit : room.getExits()) {
                Node unexplored = saveRoom(new UnexploredRoom());
                createExitRelationship(roomNode, unexplored, exit);
            }
            return roomNode;
        } else {
            throw new IllegalArgumentException("Cannot create Node from Room with existing Id");
        }
    }
}
