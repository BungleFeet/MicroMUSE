package net.lazygun.micromuse.neo4j

import net.lazygun.micromuse.Room
import org.neo4j.graphdb.*
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.traversal.Uniqueness
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.tooling.GlobalGraphOperations
import spock.lang.Ignore
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
  Transaction tx
  TraversalDescription traverser

  def "replace unexplored room with regular room"() {
    given: 'a home room and a new room to be saved'
      def home = RoomNode.persist(new Room("home", null, "", ["A"]), graphDb)
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
      def home = RoomNode.persist(new Room("home", null, "", ["A"]), graphDb)
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
      def roomNode = RoomNode.persist(new Room("home", null, "", []), graphDb)

    when: 'that room is loaded'
      def sameRoomNode = RoomNode.load(roomNode, graphDb)

    then: 'the loaded Room is the same as the initial Room'
      roomNode.isSameAs(sameRoomNode)
  }

  def "load room by location"() {
    given: 'a saved Room with a location'
      def savedRoom = RoomNode.persist(new Room("home", "#1", "", []), graphDb)

    when: 'a Room with the same location is created and passed to RoomNode#load(Room)'
      def room = RoomNode.load(new Room(savedRoom.name, savedRoom.location, savedRoom.description), graphDb)

    then: 'the loaded Room is equal to the initially saved room'
      room.isSameAs(savedRoom)
  }

  def "load room by name"() {
    given: 'a saved Room without an id or location'
      def saved = RoomNode.persist(new Room("home", null, "", ['a', 'b', 'c']), graphDb)
      saved = new Room(saved.name, saved.location, saved.description, saved.exits)

    when: 'a copy of the saved room is loaded'
      def loaded = RoomNode.load(new Room(saved.name, saved.location, saved.description, saved.exits), graphDb)

    then: 'the loaded room is equal to the saved room'
      saved.isSameAs(loaded)
  }

  def "error when loading room by name and more than one match exists"() {
    given: 'two saved rooms without ids or locations, and the same name and exits'
      def room = RoomNode.persist(new Room("home", null, "", ['a', 'b', 'c']), graphDb)
      RoomNode.persist(new Room(room.name, room.location, room.description, room.exits), graphDb)

    when: 'try to load a room with the same name and exists and no location'
      RoomNode.load(new Room(room.name, room.location, room.description, room.exits), graphDb)

    then: 'an IllegalStateException is thrown'
      thrown IllegalStateException
  }

  def "room can be persisted"() {
    given: 'a Room instance'
      def room = new Room("room1", "#1", "This is room 1", ["A", "B"])

    when: 'the room is persisted'
      def persistedRoom = RoomNode.persist(room, graphDb)

    then: 'the persisted room is the same as the initial room'
      persistedRoom.isSameAs(room)

    and: 'the node backing the persisted room is correct'
      def node = persistedRoom.getNode()
      node.name == room.name
      node.location == room.location
      node.description == room.description
      HelperUtils.getExits(node) == room.exits
  }

  def "RoomNode can be exited"() {
    given: 'two linked room'
      def roomA = RoomNode.persist(new Room("roomA", "#A", "Room A", ["B"]), graphDb)
      def roomB = RoomNode.persist(new Room("roomB", "#B", "Room B", ["A"]), graphDb)
      roomA.link("B", roomB).link("A", roomA)

    when: 'room A is exited'
      def nextRoom = roomA.exit("B")

    then: 'we get room B'
      nextRoom.isSameAs(roomB)
  }

  def "can find nearest unexplored to room with unexplored exits"() {
    given: 'a room with two exits, one of which is unexplored, the other of which is linked to a room with unexplored exits'
      def roomA = RoomNode.persist(new Room("roomA", "#A", "Room A", ["B", "C"]), graphDb)
      def roomB = RoomNode.persist(new Room("roomB", "#B", "Room B", ["A", "D", "E"]), graphDb)
      roomA.link("B", roomB).link("A", roomA)

    when: 'we find the nearest unexplored room to room A'
      def route = roomA.findNearestUnexplored().toList()

    then: 'we get a route with one step, from room A to the unexplored room C'
      route.size() == 1
      def step = route[0]
      step.from().isSameAs(roomA)
      step.exit() == "C"
      step.to().isSameAs(roomA.exit("C"))
  }

  def "can find nearest unexplored to room with no unexplored exits"() {
    given: 'a room with one exit, linked to a room with unexplored exits, linked to another room with unexplored exits'
      def roomA = RoomNode.persist(new Room("roomA", "#A", "Room A", ["B"]), graphDb)
      def roomB = RoomNode.persist(new Room("roomB", "#B", "Room B", ["A", "C", "D"]), graphDb)
      def roomC = RoomNode.persist(new Room("roomC", "#C", "Room C", ["B", "E"]), graphDb)
      roomA.link("B", roomB).link("A", roomA)
      roomB.link("C", roomC).link("B", roomB)

    when: 'we find the nearest unexplored room to room A'
      def route = roomA.findNearestUnexplored().toList()

    then: 'we get a route with two steps, from A to B to the unexplored room D'
      route.size() == 2
      def step1 = route[0]
      step1.from().isSameAs(roomA)
      step1.exit() == "B"
      step1.to().isSameAs(roomA.exit("B"))
      def step2 = route[1]
      step2.from().isSameAs(roomB)
      step2.exit() == "D"
      step2.to().isSameAs(roomB.exit("D"))
  }

  def "full route to nearest unexplored room is given when no rooms are teleportable"() {
    given: 'a room with one exit, linked to a room with two exits, linked to another room with unexplored exits'
      def roomA = RoomNode.persist(new Room("roomA", null, "Room A", ["B"]), graphDb)
      def roomB = RoomNode.persist(new Room("roomB", null, "Room B", ["A", "C"]), graphDb)
      def roomC = RoomNode.persist(new Room("roomC", null, "Room C", ["B", "D"]), graphDb)
      roomA.link("B", roomB).link("A", roomA)
      roomB.link("C", roomC).link("B", roomB)

    when: 'we find the nearest unexplored room to room A'
      def unexplored = roomA.findNearestUnexplored().toList()

    then: 'we get a route with three steps, from A to B to C to unexplored room D'
      unexplored.size() == 3
      def step1 = unexplored[0]
      step1.from().isSameAs(roomA)
      step1.exit() == "B"
      step1.to().isSameAs(roomA.exit("B"))
      def step2 = unexplored[1]
      step2.from().isSameAs(roomB)
      step2.exit() == "C"
      step2.to().isSameAs(roomB.exit("C"))
      def step3 = unexplored[2]
      step3.from().isSameAs(roomC)
      step3.exit() == "D"
      step3.to().isSameAs(roomC.exit("D"))
  }

  def setupSpec() {
    PropertyContainer.metaClass  {
      propertyMissing { String name -> delegate.getProperty(name)}
      propertyMissing { String name, val -> delegate.setProperty(name, val)}
    }
  }

  def setup() {
    graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
    globalGraphOperations = GlobalGraphOperations.at(graphDb)
    tx = graphDb.beginTx()
    createLinksTraversalDescription()
  }

  def cleanup() {
    printMap("\nMap after test:")
    tx.close()
    graphDb.shutdown()
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
