package net.lazygun.micromuse.neo4j

import net.lazygun.micromuse.Room
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.traversal.Uniqueness
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.tooling.GlobalGraphOperations
import spock.lang.Specification

import static net.lazygun.micromuse.neo4j.RoomNode.ROOM
import static net.lazygun.micromuse.neo4j.RoomNode.Relation.EXIT
import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * Created by ewan on 26/03/2014.
 */
class RoomNodeTest extends Specification {

  GraphDatabaseService graphDb
  GlobalGraphOperations globalGraphOperations
  ExecutionEngine cypher
  Transaction tx
  Room home
  TraversalDescription traverser

  def "replace unexplored room with regular room"() {
    given: 'a home room and a new room to be saved'
      def home = createHomeNode(["A"])
      def room = new Room("Room A", null, "Test room A", ['A', 'B', 'C'])

    when: 'the new room is linked to from the home room via the first exit'
      room = home.link("A", room)

    then: 'the new room has been saved'
      room.id != null

    and: 'the new room has the correct exits linking to and from it'
      def incoming = room.getRelationships(INCOMING).toList()
      def outgoing = room.getRelationships(OUTGOING).toList()
      incoming.size() == 1
      outgoing.size() == 3
      incoming[0].name == 'A'
      outgoing.name.sort() == room.exits

    and: 'the new room is linked to the correct rooms'
      incoming[0].startNode.id == home.id
      outgoing.every { it.endNode.name == Room.UNEXPLORED.name }
  }

  def "create a link back to home room"() {
    given: 'a new room, linked to the home room from its first exit'
      def home = createHomeNode(['A'])
      def room = home.link("A", new Room("Room A", null, "Test room A", ['H', 'B', 'C']))

    when: 'the new room is linked back to the home room via exit "1"'
      def linkedHome = room.link('H', new Room(home.name, home.description, home.description, home.exits))

    then: 'the new link connects back to home'
      linkedHome.id == home.id

    and: 'the new room has the correct exits linking to and from it'
      def incoming = room.getRelationships(INCOMING).toList()
      def outgoing = room.getRelationships(OUTGOING).toList()
      incoming.size() == 1
      outgoing.size() == 3
      incoming[0].name == 'A'
      outgoing.name.sort() == room.exits

    and: 'the new room is linked to the correct rooms'
      incoming[0].startNode.id == home.id
      def homeRel = outgoing.find { it.endNode.id == home.id }
      homeRel != null
      (outgoing - homeRel).every { it.endNode.name == Room.UNEXPLORED.name }
  }

  def "load room by id"() {
    given: 'a Room with an id'
      def roomNode = createHomeNode()

    when: 'that room is loaded'
      def sameRoomNode = RoomNode.load(roomNode)

    then: 'the loaded Room is the same as the initial Room'
      roomNode.isSameAs(sameRoomNode)
  }

  def "load room by location"() {
    given: 'a saved Room with a location'
      def savedRoom = createHomeNode([], '#1')

    when: 'a Room with the same location is created and passed to RoomNode#load(Room)'
      def room = RoomNode.load(new Room(savedRoom.name, savedRoom.location, savedRoom.description))

    then: 'the loaded Room is equal to the initially saved room'
      room.isSameAs(savedRoom)
  }

  def "load room by name"() {
    given: 'a saved Room without an id or location'
      def saved = createHomeNode(['a', 'b', 'c'], '')
      saved = new Room(saved.name, saved.location, saved.description, saved.exits)

    when: 'a copy of the saved room is loaded'
      def loaded = RoomNode.load(new Room(saved.name, saved.location, saved.description, saved.exits))

    then: 'the loaded room is equal to the saved room'
      saved.isSameAs(loaded)
  }

  def "error when loading room by name and more than one match exists"() {
    given: 'two saved rooms without ids or locations, and the same name and exits'
      def room = createHomeNode(["a", "b", "c"], null)
      createHomeNode(["a", "b", "c"], null)

    when: 'try to load a room with the same name and exists and no location'
      RoomNode.load(new Room(room.name, room.location, room.description, room.exits))

    then: 'an IllegalStateException is thrown'
      thrown IllegalStateException
  }


  def setupSpec() {
    PropertyContainer.metaClass  {
      propertyMissing { String name -> delegate.getProperty(name)}
      propertyMissing { String name, val -> delegate.setProperty(name, val)}
    }
  }

  def setup() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
    RoomNode.initialise(graphDb)
    globalGraphOperations = GlobalGraphOperations.at(graphDb)
    cypher = new ExecutionEngine(graphDb)
    tx = graphDb.beginTx()

    createLinksTraversalDescription()
  }

  def cleanup() {
    printMap("\nMap after test:")
    tx.close()
    graphDb.shutdown()
  }

  RoomNode createHomeNode(Collection<String> exits = [], String location = "#0") {
    home = RoomNode.persist(new Room("Home", location, "", exits))
  }

  void createLinksTraversalDescription() {
    traverser = graphDb.traversalDescription()
      .breadthFirst()
      .relationships(EXIT, OUTGOING)
      .uniqueness(Uniqueness.NODE_PATH)
      .evaluator(Evaluators.includingDepths(1, 1))
  }

  void printMap(title, includeLabels = false) {
    println(title)
    traverser
      .traverse(GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(ROOM).iterator().toList().toArray() as Node[])
      .iterator().toList().each { p ->
        p.each { PropertyContainer e ->
          switch (e) {
            case Node:
              includeLabels ?
                print("($e.name {${e.getLabels().join(',')}})") :
                print("($e.name)")
              break
            case Relationship:
              print "-[$e.name]->"
          }
        }
        println ""
      }
    println ""
  }
}
