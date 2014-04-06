package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Link;
import net.lazygun.micromuse.Room;
import net.lazygun.micromuse.Route;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Uniqueness;

import java.util.*;

import static net.lazygun.micromuse.neo4j.RoomNode.Relation.EXIT;
import static org.neo4j.graphdb.Direction.OUTGOING;

/**
 *
 * Created by ewan on 27/03/2014.
 */
@SuppressWarnings("deprecation")
public class RoomNode implements Room, Node {

    static final String NAME = "name";
    static final String DESCRIPTION = "description";
    static final String LOCATION = "location";
    static final String EXITS = "exits";

    static final Label ROOM = new Label() {
        @Override
        public String name() {
            return "ROOM";
        }
    };
    static final Label UNEXPLORED = new Label() {
        @Override
        public String name() {
            return "UNEXPLORED";
        }
    };
    static final Label TELEPORTABLE = new Label() {
        @Override
        public String name() {
            return "TELEPORTABLE";
        }
    };

    private final Node node;

    RoomNode(Node node) {
        this.node = node;
    }

    public static RoomNode findById(long id, GraphDatabaseService db) {
        return new RoomNode(db.getNodeById(id));
    }

    public static RoomNode findByLocation(String location, GraphDatabaseService db) {
        List<Node> matches = HelperUtils.iterableToList(db.findNodesByLabelAndProperty(TELEPORTABLE, "location", location));
        switch (matches.size()) {
            case 0:
                return null;
            case 1:
                return new RoomNode(matches.get(0));
            default:
                throw new IllegalStateException("Found more than one Node with location " + location);
        }
    }

    public static List<RoomNode> findAllByNameAndExits(String name, List<String> exits, GraphDatabaseService db) {
        List<RoomNode> matches = new ArrayList<>();
        List<Node> potentialMatches = HelperUtils.iterableToList(
                db.findNodesByLabelAndProperty(ROOM, "name", name)
        );
        for (Node potentialMatch : potentialMatches) {
            List<String> nodeExits = HelperUtils.getExits(potentialMatch);
            if (nodeExits.equals(exits)) {
                matches.add(new RoomNode(potentialMatch));
            }
        }
        return matches;
    }

    public static RoomNode findByExample(Room room, GraphDatabaseService db) {
        if (room instanceof RoomNode) {
            return (RoomNode) room;
        }
        if (room.isTeleportable()) {
            return findByLocation(room.getLocation(), db);
        }
        List<RoomNode> matches = findAllByNameAndExits(room.getName(), room.getExits(), db);
        return matches.size() > 0 ? matches.get(0) : null;
    }

    public static RoomNode create(String name, String location, String description, List<String> exits, GraphDatabaseService graphDb) {
        Node node = graphDb.createNode(getLabels(name, location));
        node.setProperty(NAME, name);
        if (location != null) {
            node.setProperty(LOCATION, location);
        }
        node.setProperty(DESCRIPTION, description);
        if (exits != null) {
            for (String exit : exits) {
                RoomNode unexplored = create(UNEXPLORED.name(), null, "", null, graphDb);
                createExitRelationship(node, unexplored, exit);
            }
        }
        return new RoomNode(node);
    }

    static Relationship createExitRelationship(Node from, Node to, String name) {
        Relationship exit = from.createRelationshipTo(to, EXIT);
        exit.setProperty(NAME, name);
        return exit;
    }

    private static Label[] getLabels(String name, String location) {
        List<Label> labels = new ArrayList<>();
        labels.add(ROOM);
        if (name.equals(UNEXPLORED.name())) {
            labels.add(UNEXPLORED);
        } else if (location != null) {
            labels.add(TELEPORTABLE);
        }
        return labels.toArray(new Label[labels.size()]);
    }

    private List<String> getExitRelationNames(Node node) {
        List<String> exits = new ArrayList<>();
        for (Relationship rel : node.getRelationships(Relation.EXIT, Direction.OUTGOING)) {
            exits.add((String) rel.getProperty(NAME));
        }
        Collections.sort(exits);
        return exits;
    }

    @Override
    public RoomNode link(String exit, Room to) {
        if (!getExits().contains(exit)) {
            throw new IllegalArgumentException("Room has no exit name '" + exit + "'");
        }

        System.out.println("Creating link (" + getName() + ")-[" + exit + "]->(" + to.getName() + ")");

        // Now get the saved RoomNode on the other side of the exit
        RoomNode persistedTo = exit(exit);

        // If the to Node represents an unexplored room, we replace it with the room on the TO side of the given link
        if (persistedTo.hasLabel(UNEXPLORED)) {
            persistedTo.delete();
            persistedTo = findByExample(to, getGraphDatabase());
            if (persistedTo == null) {
                persistedTo = create(to.getName(), to.getLocation(), to.getDescription(), to.getExits(), getGraphDatabase());
            }
            createExitRelationship(this, persistedTo, exit);
        }

        // Otherwise, the link already exists, so we simply check that the given TO Room matches that in the database
        else {
            RoomNode existing = findByExample(to, getGraphDatabase());
            if (existing.getId() != persistedTo.getId()) {
                throw new IllegalStateException(
                        "A link connecting via this exit to a different room already exists: " + existing);
            }
        }

        return persistedTo;
    }

    @Override
    public String getName() {
        return (String) getProperty(NAME);
    }

    @Override
    public String getLocation() {
        return (String) getProperty(LOCATION, null);
    }

    @Override
    public String getDescription() {
        return (String) getProperty(DESCRIPTION);
    }

    @Override
    public List<String> getExits() {
        return getExitRelationNames(this);
    }

    @Override
    public boolean isTeleportable() {
        return getLocation() != null;
    }

    @Override
    public boolean isUnexplored() {
        return getName().equals(UNEXPLORED.name());
    }

    @Override
    public Route findNearestUnexplored() {
        TraversalDescription finder = createUnexploredRoomNodeFinder();
        ResourceIterable<Path> unexploredNodePaths = finder.traverse(this);
        Path shortestPath = null;
        for (Path path : unexploredNodePaths) {
            if (path.length() == 1) {
                    return pathToRoute(path);
                } else if (shortestPath == null || path.length() < shortestPath.length()) {
                    shortestPath = path;
                }
            }
            return pathToRoute(shortestPath);
    }

    @Override
    public RoomNode exit(String exit) {
        if (!getExits().contains(exit)) {
            throw new IllegalArgumentException("Room has no exit name '" + exit + "'");
        }
        return new RoomNode(exitRelationship(exit).getEndNode());
    }

    @Override
    public String toString() {
        return "RoomNode{name=" + getName() + ",location=" + getLocation() + ",exits=[" +
               Arrays.toString(getExits().toArray()) + "],node=" + node + "}";
    }

    private Relationship exitRelationship(String exit) {
        Relationship exitRel = null;
        for (Relationship rel : getRelationships(EXIT, OUTGOING)) {
            if (rel.getProperty(NAME).equals(exit)) {
                exitRel = rel;
            }
        }
        if (exitRel == null) {
            throw new IllegalStateException("Expected exit relation '" + exit + "' doesn't exist on this node ");
        }
        return exitRel;
    }

    private Route<RoomNode> pathToRoute(Path path) {
        if (path == null) {
            return null;
        }
        List<Link<RoomNode>> links = new ArrayList<>();
        for (Relationship rel : path.relationships()) {
            Link<RoomNode> link = relationshipToLink(rel);
            links.add(link);
        }
        return new Route<>(links);
    }

    private Link<RoomNode> relationshipToLink(Relationship relationship) {
        RoomNode from = new RoomNode(relationship.getStartNode());
        String exit = (String) relationship.getProperty("name");
        RoomNode to = new RoomNode(relationship.getEndNode());
        return new Link<>(from, exit, to);
    }

    private TraversalDescription createUnexploredRoomNodeFinder() {
        return getGraphDatabase()
                .traversalDescription()
                .breadthFirst()
                .relationships(EXIT, OUTGOING)
                .uniqueness(Uniqueness.NODE_PATH)
                .evaluator(new Evaluator() {
                    @Override
                    public Evaluation evaluate(Path path) {
                        return path.endNode().hasLabel(UNEXPLORED) ? Evaluation.INCLUDE_AND_PRUNE
                                : Evaluation.EXCLUDE_AND_CONTINUE;
                    }
                });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Room)) return false;
        if (!super.equals(o)) return false;

        final RoomNode roomNode = (RoomNode) o;

        return node.getId() == roomNode.node.getId();

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + new Long(node.getId()).hashCode();
        return result;
    }

    /**
     * Returns the unique id of this node. Ids are garbage collected over time
     * so they are only guaranteed to be unique during a specific time span: if
     * the node is deleted, it's likely that a new node at some point will get
     * the old id. <b>Note</b>: this makes node ids brittle as public APIs.
     *
     * @return the id of this node
     */
    @Override
    public long getId() {return node.getId();}

    @Override
    public void delete() {
        for (Relationship rel : node.getRelationships()) {
            rel.delete();
        }
        node.delete();
    }

    /**
     * Returns all the relationships attached to this node. If no relationships
     * are attached to this node, an empty iterable will be returned.
     *
     * @return all relationships attached to this node
     */
    @Override
    public Iterable<Relationship> getRelationships() {return node.getRelationships();}

    /**
     * Returns <code>true</code> if there are any relationships attached to this
     * node, <code>false</code> otherwise.
     *
     * @return <code>true</code> if there are any relationships attached to this
     *         node, <code>false</code> otherwise
     */
    @Override
    public boolean hasRelationship() {return node.hasRelationship();}

    /**
     * Returns all the relationships of any of the types in <code>types</code>
     * that are attached to this node, regardless of direction. If no
     * relationships of the given types are attached to this node, an empty
     * iterable will be returned.
     *
     * @param types the given relationship type(s)
     * @return all relationships of the given type(s) that are attached to this
     *         node
     */
    @Override
    public Iterable<Relationship> getRelationships(RelationshipType... types) {return node.getRelationships(types);}

    /**
     * Returns all the relationships of any of the types in <code>types</code>
     * that are attached to this node and have the given <code>direction</code>.
     * If no relationships of the given types are attached to this node, an empty
     * iterable will be returned.
     *
     * @param direction the direction of the relationships to return.
     * @param types the given relationship type(s)
     * @return all relationships of the given type(s) that are attached to this
     *         node
     */
    @Override
    public Iterable<Relationship> getRelationships(Direction direction, RelationshipType... types) {return node.getRelationships(direction, types);}

    /**
     * Returns <code>true</code> if there are any relationships of any of the
     * types in <code>types</code> attached to this node (regardless of
     * direction), <code>false</code> otherwise.
     *
     * @param types the given relationship type(s)
     * @return <code>true</code> if there are any relationships of any of the
     *         types in <code>types</code> attached to this node,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean hasRelationship(RelationshipType... types) {return node.hasRelationship(types);}

    /**
     * Returns <code>true</code> if there are any relationships of any of the
     * types in <code>types</code> attached to this node (for the given
     * <code>direction</code>), <code>false</code> otherwise.
     *
     * @param direction the direction to check relationships for
     * @param types the given relationship type(s)
     * @return <code>true</code> if there are any relationships of any of the
     *         types in <code>types</code> attached to this node,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean hasRelationship(Direction direction, RelationshipType... types) {return node.hasRelationship(direction, types);}

    /**
     * Returns all {@link org.neo4j.graphdb.Direction#OUTGOING OUTGOING} or
     * {@link org.neo4j.graphdb.Direction#INCOMING INCOMING} relationships from this node. If
     * there are no relationships with the given direction attached to this
     * node, an empty iterable will be returned. If {@link org.neo4j.graphdb.Direction#BOTH BOTH}
     * is passed in as a direction, relationships of both directions are
     * returned (effectively turning this into <code>getRelationships()</code>).
     *
     * @param dir the given direction, where <code>Direction.OUTGOING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getStartNode() start node} and <code>
     * Direction.INCOMING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getEndNode() end node}
     * @return all relationships with the given direction that are attached to
     *         this node
     */
    @Override
    public Iterable<Relationship> getRelationships(Direction dir) {return node.getRelationships(dir);}

    /**
     * Returns <code>true</code> if there are any relationships in the given
     * direction attached to this node, <code>false</code> otherwise. If
     * {@link org.neo4j.graphdb.Direction#BOTH BOTH} is passed in as a direction, relationships of
     * both directions are matched (effectively turning this into
     * <code>hasRelationships()</code>).
     *
     * @param dir the given direction, where <code>Direction.OUTGOING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getStartNode() start node} and <code>
     * Direction.INCOMING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getEndNode() end node}
     * @return <code>true</code> if there are any relationships in the given
     *         direction attached to this node, <code>false</code> otherwise
     */
    @Override
    public boolean hasRelationship(Direction dir) {return node.hasRelationship(dir);}

    /**
     * Returns all relationships with the given type and direction that are
     * attached to this node. If there are no matching relationships, an empty
     * iterable will be returned.
     *
     * @param type the given type
     * @param dir the given direction, where <code>Direction.OUTGOING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getStartNode() start node} and <code>
     * Direction.INCOMING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getEndNode() end node}
     * @return all relationships attached to this node that match the given type
     *         and direction
     */
    @Override
    public Iterable<Relationship> getRelationships(RelationshipType type, Direction dir) {return node.getRelationships(type, dir);}

    /**
     * Returns <code>true</code> if there are any relationships of the given
     * relationship type and direction attached to this node, <code>false</code>
     * otherwise.
     *
     * @param type the given type
     * @param dir the given direction, where <code>Direction.OUTGOING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getStartNode() start node} and <code>
     * Direction.INCOMING</code>
     *            means all relationships that have this node as
     *            {@link org.neo4j.graphdb.Relationship#getEndNode() end node}
     * @return <code>true</code> if there are any relationships of the given
     *         relationship type and direction attached to this node,
     *         <code>false</code> otherwise
     */
    @Override
    public boolean hasRelationship(RelationshipType type, Direction dir) {return node.hasRelationship(type, dir);}

    /**
     * Returns the only relationship of a given type and direction that is
     * attached to this node, or <code>null</code>. This is a convenience method
     * that is used in the commonly occurring situation where a node has exactly
     * zero or one relationships of a given type and direction to another node.
     * Typically this invariant is maintained by the rest of the code: if at any
     * time more than one such relationships exist, it is a fatal error that
     * should generate an unchecked exception. This method reflects that
     * semantics and returns either:
     * <p>
     * <ol>
     * <li><code>null</code> if there are zero relationships of the given type
     * and direction,</li>
     * <li>the relationship if there's exactly one, or
     * <li>throws an unchecked exception in all other cases.</li>
     * </ol>
     * <p>
     * This method should be used only in situations with an invariant as
     * described above. In those situations, a "state-checking" method (e.g.
     * <code>hasSingleRelationship(...)</code>) is not required, because this
     * method behaves correctly "out of the box."
     *
     * @param type the type of the wanted relationship
     * @param dir the direction of the wanted relationship (where
     *            <code>Direction.OUTGOING</code> means a relationship that has
     *            this node as {@link org.neo4j.graphdb.Relationship#getStartNode() start node}
     *            and <code>
     * Direction.INCOMING</code> means a relationship that has
     *            this node as {@link org.neo4j.graphdb.Relationship#getEndNode() end node}) or
     *            {@link org.neo4j.graphdb.Direction#BOTH} if direction is irrelevant
     * @return the single relationship matching the given type and direction if
     *         exactly one such relationship exists, or <code>null</code> if
     *         exactly zero such relationships exists
     * @throws RuntimeException if more than one relationship matches the given
     *             type and direction
     */
    @Override
    public Relationship getSingleRelationship(RelationshipType type, Direction dir) {return node.getSingleRelationship(type, dir);}

    /**
     * Creates a relationship between this node and another node. The
     * relationship is of type <code>type</code>. It starts at this node and
     * ends at <code>otherNode</code>.
     * <p>
     * A relationship is equally well traversed in both directions so there's no
     * need to create another relationship in the opposite direction (in regards
     * to traversal or performance).
     *
     * @param otherNode the end node of the new relationship
     * @param type the type of the new relationship
     * @return the newly created relationship
     */
    @Override
    public Relationship createRelationshipTo(Node otherNode, RelationshipType type) {return node.createRelationshipTo(otherNode, type);}

    /**
     * Instantiates a traverser that will start at this node and traverse
     * according to the given order and evaluators along the specified
     * relationship type and direction. If the traverser should traverse more
     * than one <code>RelationshipType</code>/<code>Direction</code> pair, use
     * one of the overloaded variants of this method. The created traverser will
     * iterate over each node that can be reached from this node by the spanning
     * tree formed by the given relationship types (with direction) exactly
     * once. For more information about traversal, see the {@link Traverser}
     * documentation.
     *
     *
     * @param traversalOrder the traversal order
     * @param stopEvaluator an evaluator instructing the new traverser about
     *            when to stop traversing, either a predefined evaluator such as
     *            {@link StopEvaluator#END_OF_GRAPH} or a custom-written
     *            evaluator
     * @param returnableEvaluator an evaluator instructing the new traverser
     *            about whether a specific node should be returned from the
     *            traversal, either a predefined evaluator such as
     *            {@link ReturnableEvaluator#ALL} or a customer-written
     *            evaluator
     * @param relationshipType the relationship type that the traverser will
     *            traverse along
     * @param direction the direction in which the relationships of type
     *            <code>relationshipType</code> will be traversed
     * @return a new traverser, configured as above
     * @deprecated because of an unnatural and too tight coupling with
     *             {@link org.neo4j.graphdb.Node}. Also because of the introduction of a new
     *             traversal framework. The new way of doing traversals is by
     *             creating a new {@link org.neo4j.graphdb.traversal.TraversalDescription} from
     *             {@link org.neo4j.kernel.Traversal#traversal()}, add rules and behaviors to it
     *             and then calling
     *             {@link org.neo4j.graphdb.traversal.TraversalDescription#traverse(org.neo4j.graphdb.Node...)}
     */
    @Override
    @Deprecated
    public Traverser traverse(Traverser.Order traversalOrder, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, RelationshipType relationshipType, Direction direction) {return node.traverse(traversalOrder, stopEvaluator, returnableEvaluator, relationshipType, direction);}

    /**
     * Instantiates a traverser that will start at this node and traverse
     * according to the given order and evaluators along the two specified
     * relationship type and direction pairs. If the traverser should traverse
     * more than two <code>RelationshipType</code>/<code>Direction</code> pairs,
     * use the overloaded
     * {@link #traverse(org.neo4j.graphdb.Traverser.Order, org.neo4j.graphdb.StopEvaluator, org.neo4j.graphdb.ReturnableEvaluator, Object...)
     * varargs variant} of this method. The created traverser will iterate over
     * each node that can be reached from this node by the spanning tree formed
     * by the given relationship types (with direction) exactly once. For more
     * information about traversal, see the {@link org.neo4j.graphdb.Traverser} documentation.
     *
     * @param traversalOrder the traversal order
     * @param stopEvaluator an evaluator instructing the new traverser about
     *            when to stop traversing, either a predefined evaluator such as
     *            {@link org.neo4j.graphdb.StopEvaluator#END_OF_GRAPH} or a custom-written
     *            evaluator
     * @param returnableEvaluator an evaluator instructing the new traverser
     *            about whether a specific node should be returned from the
     *            traversal, either a predefined evaluator such as
     *            {@link org.neo4j.graphdb.ReturnableEvaluator#ALL} or a customer-written
     *            evaluator
     * @param firstRelationshipType the first of the two relationship types that
     *            the traverser will traverse along
     * @param firstDirection the direction in which the first relationship type
     *            will be traversed
     * @param secondRelationshipType the second of the two relationship types
     *            that the traverser will traverse along
     * @param secondDirection the direction that the second relationship type
     *            will be traversed
     * @return a new traverser, configured as above
     * @deprecated because of an unnatural and too tight coupling with
     * {@link org.neo4j.graphdb.Node}. Also because of the introduction of a new traversal
     * framework. The new way of doing traversals is by creating a
     * new {@link org.neo4j.graphdb.traversal.TraversalDescription} from
     * {@link org.neo4j.kernel.Traversal#traversal()}, add rules and
     * behaviors to it and then calling
     * {@link org.neo4j.graphdb.traversal.TraversalDescription#traverse(org.neo4j.graphdb.Node...)}
     */
    @Override
    @Deprecated
    public Traverser traverse(Traverser.Order traversalOrder, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, RelationshipType firstRelationshipType, Direction firstDirection, RelationshipType secondRelationshipType, Direction secondDirection) {return node.traverse(traversalOrder, stopEvaluator, returnableEvaluator, firstRelationshipType, firstDirection, secondRelationshipType, secondDirection);}

    /**
     * Instantiates a traverser that will start at this node and traverse
     * according to the given order and evaluators along the specified
     * relationship type and direction pairs. Unlike the overloaded variants of
     * this method, the relationship types and directions are passed in as a
     * "varargs" variable-length argument which means that an arbitrary set of
     * relationship type and direction pairs can be specified. The
     * variable-length argument list should be every other relationship type and
     * direction, starting with relationship type, e.g:
     * <p>
     * <code>node.traverse( BREADTH_FIRST, stopEval, returnableEval,
     * MyRels.REL1, Direction.OUTGOING, MyRels.REL2, Direction.OUTGOING,
     * MyRels.REL3, Direction.BOTH, MyRels.REL4, Direction.INCOMING );</code>
     * <p>
     * Unfortunately, the compiler cannot enforce this so an unchecked exception
     * is raised if the variable-length argument has a different constitution.
     * <p>
     * The created traverser will iterate over each node that can be reached
     * from this node by the spanning tree formed by the given relationship
     * types (with direction) exactly once. For more information about
     * traversal, see the {@link org.neo4j.graphdb.Traverser} documentation.
     *
     * @param traversalOrder the traversal order
     * @param stopEvaluator an evaluator instructing the new traverser about
     *            when to stop traversing, either a predefined evaluator such as
     *            {@link org.neo4j.graphdb.StopEvaluator#END_OF_GRAPH} or a custom-written
     *            evaluator
     * @param returnableEvaluator an evaluator instructing the new traverser
     *            about whether a specific node should be returned from the
     *            traversal, either a predefined evaluator such as
     *            {@link org.neo4j.graphdb.ReturnableEvaluator#ALL} or a customer-written
     *            evaluator
     * @param relationshipTypesAndDirections a variable-length list of
     *            relationship types and their directions, where the first
     *            argument is a relationship type, the second argument the first
     *            type's direction, the third a relationship type, the fourth
     *            its direction, etc
     * @return a new traverser, configured as above
     * @throws RuntimeException if the variable-length relationship type /
     *             direction list is not as described above
     * @deprecated because of an unnatural and too tight coupling with
     * {@link org.neo4j.graphdb.Node}. Also because of the introduction of a new traversal
     * framework. The new way of doing traversals is by creating a
     * new {@link org.neo4j.graphdb.traversal.TraversalDescription} from
     * {@link org.neo4j.kernel.Traversal#traversal()}, add rules and
     * behaviors to it and then calling
     * {@link org.neo4j.graphdb.traversal.TraversalDescription#traverse(org.neo4j.graphdb.Node...)}
     */
    @Override
    @Deprecated
    public Traverser traverse(Traverser.Order traversalOrder, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... relationshipTypesAndDirections) {return node.traverse(traversalOrder, stopEvaluator, returnableEvaluator, relationshipTypesAndDirections);}

    /**
     * Adds a {@link org.neo4j.graphdb.Label} to this node. If this node doesn't already have
     * this label it will be added. If it already has the label, nothing will happen.
     *
     * @param label the label to add to this node.
     */
    @Override
    public void addLabel(Label label) {node.addLabel(label);}

    /**
     * Removes a {@link org.neo4j.graphdb.Label} from this node. If this node doesn't have this label,
     * nothing will happen.
     *
     * @param label the label to remove from this node.
     */
    @Override
    public void removeLabel(Label label) {node.removeLabel(label);}

    /**
     * Checks whether or not this node has the given label.
     *
     * @param label the label to check for.
     * @return {@code true} if this node has the given label, otherwise {@code false}.
     */
    @Override
    public boolean hasLabel(Label label) {return node.hasLabel(label);}

    /**
     * Lists all labels attached to this node. If this node has no
     * labels an empty {@link Iterable} will be returned.
     *
     * @return all labels attached to this node.
     */
    @Override
    public Iterable<Label> getLabels() {return node.getLabels();}

    /**
     * Get the {@link org.neo4j.graphdb.GraphDatabaseService} that this {@link org.neo4j.graphdb.Node} or
     * {@link org.neo4j.graphdb.Relationship} belongs to.
     *
     * @return The GraphDatabase this Node or Relationship belongs to.
     */
    @Override
    public GraphDatabaseService getGraphDatabase() {return node.getGraphDatabase();}

    /**
     * Returns <code>true</code> if this property container has a property
     * accessible through the given key, <code>false</code> otherwise. If key is
     * <code>null</code>, this method returns <code>false</code>.
     *
     * @param key the property key
     * @return <code>true</code> if this property container has a property
     *         accessible through the given key, <code>false</code> otherwise
     */
    @Override
    public boolean hasProperty(String key) {return node.hasProperty(key);}

    /**
     * Returns the property value associated with the given key. The value is of
     * one of the valid property types, i.e. a Java primitive, a {@link String
     * String} or an array of any of the valid types.
     * <p>
     * If there's no property associated with <code>key</code> an unchecked
     * exception is raised. The idiomatic way to avoid an exception for an
     * unknown key and instead get <code>null</code> back is to use a default
     * value: {@link #getProperty(String, Object) Object valueOrNull =
     * nodeOrRel.getProperty( key, null )}
     *
     * @param key the property key
     * @return the property value associated with the given key
     * @throws org.neo4j.graphdb.NotFoundException if there's no property associated with
     *             <code>key</code>
     */
    @Override
    public Object getProperty(String key) {return node.getProperty(key);}

    /**
     * Returns the property value associated with the given key, or a default
     * value. The value is of one of the valid property types, i.e. a Java
     * primitive, a {@link String String} or an array of any of the valid types.
     *
     * @param key the property key
     * @param defaultValue the default value that will be returned if no
     *            property value was associated with the given key
     * @return the property value associated with the given key
     */
    @Override
    public Object getProperty(String key, Object defaultValue) {return node.getProperty(key, defaultValue);}

    /**
     * Sets the property value for the given key to <code>value</code>. The
     * property value must be one of the valid property types, i.e:
     * <ul>
     * <li><code>boolean</code> or <code>boolean[]</code></li>
     * <li><code>byte</code> or <code>byte[]</code></li>
     * <li><code>short</code> or <code>short[]</code></li>
     * <li><code>int</code> or <code>int[]</code></li>
     * <li><code>long</code> or <code>long[]</code></li>
     * <li><code>float</code> or <code>float[]</code></li>
     * <li><code>double</code> or <code>double[]</code></li>
     * <li><code>char</code> or <code>char[]</code></li>
     * <li><code>java.lang.String</code> or <code>String[]</code></li>
     * </ul>
     * <p>
     * This means that <code>null</code> is not an accepted property value.
     *
     * @param key the key with which the new property value will be associated
     * @param value the new property value, of one of the valid property types
     * @throws IllegalArgumentException if <code>value</code> is of an
     *             unsupported type (including <code>null</code>)
     */
    @Override
    public void setProperty(String key, Object value) {node.setProperty(key, value);}

    /**
     * Removes the property associated with the given key and returns the old
     * value. If there's no property associated with the key, <code>null</code>
     * will be returned.
     *
     * @param key the property key
     * @return the property value that used to be associated with the given key
     */
    @Override
    public Object removeProperty(String key) {return node.removeProperty(key);}

    /**
     * Returns all existing property keys, or an empty iterable if this property
     * container has no properties.
     *
     * @return all property keys on this property container
     */
    @Override
    public Iterable<String> getPropertyKeys() {return node.getPropertyKeys();}

    public static enum Relation implements RelationshipType { EXIT }
}
