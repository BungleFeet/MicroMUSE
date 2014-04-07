package net.lazygun.micromuse.neo4j

import net.lazygun.micromuse.RoomImpl
import net.lazygun.micromuse.TestUtils
import org.neo4j.graphdb.*
import org.neo4j.test.TestGraphDatabaseFactory
import spock.lang.Specification

import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * Created by ewan on 26/03/2014.
 */
class RoomNodeTest extends Specification {

  GraphDatabaseService db
  GraphTransaction tx

  def "replace unexplored room with regular room"() {
    given: 'a home room and a new room to be saved'
      def home = RoomNode.create("home", null, "", ["A"])
      def room = new RoomImpl("Room A", null, "Test room A", ['A', 'B', 'C'])

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
      incoming[0].startNode == home
      outgoing.every { it.endNode.name == RoomImpl.UNEXPLORED.name }
  }

  def "create a link back to home room"() {
    given: 'a new room, linked to the home room from its first exit'
      def home = RoomNode.create("home", null, "", ["A"])
      def room = home.link("A", new RoomImpl("Room A", null, "Test room A", ['H', 'B', 'C']))

    when: 'the new room is linked back to the home room via exit "1"'
      def linkedHome = room.link('H', new RoomImpl(home.name, home.description, home.description, home.exits))

    then: 'the new link connects back to home'
      linkedHome == home

    and: 'the new room has the correct exits linking to and from it'
      def incoming = room.getRelationships(INCOMING).toList()
      def outgoing = room.getRelationships(OUTGOING).toList()
      incoming.size() == 1
      outgoing.size() == 3
      incoming[0].name == 'A'
      outgoing.name.sort() == room.exits

    and: 'the new room is linked to the correct rooms'
      incoming[0].startNode == home
      def homeRel = outgoing.find { it.endNode == home }
      homeRel != null
      (outgoing - homeRel).every { it.endNode.name == RoomImpl.UNEXPLORED.name }
  }

  def "found room by id"() {
    given: 'a Room with an id'
      def roomNode = RoomNode.create("home", null, "", [])

    expect: 'the found Room is the same as the initial Room'
      RoomNode.findById(roomNode.id) == roomNode
  }

  def "find room by location"() {
    given: 'a saved Room with a location'
      def savedRoom = RoomNode.create("home", "#1", "", [])

    expect: 'the found room has the same id as the created room'
      RoomNode.findById(savedRoom.id) == savedRoom
  }

  def "find room by example"() {
    given: 'a saved Room without an id or location'
      def saved = RoomNode.create("home", null, "", ['a', 'b', 'c'])
      def example = new RoomImpl(saved.name, saved.location, saved.description, saved.exits)

    expect: 'the found room has the same id as the saved room'
      RoomNode.findByExample(example) == saved
  }

  def "room can be created"() {
    when: 'a room is created'
      def room = RoomNode.create('room', '#0', 'A Room', ["c", "a", "b"])

    then: 'the room has an id an the expected properties'
      room.id != null
      room.name == 'room'
      room.location == '#0'
      room.description == 'A Room'
      room.exits == ["a", "b", "c"]
  }

  def "RoomNode can be exited"() {
    given: 'two linked room'
      def roomA = RoomNode.create("roomA", "#A", "Room A", ["B"])
      def roomB = RoomNode.create("roomB", "#B", "Room B", ["A"])
      roomA.link("B", roomB).link("A", roomA)

    when: 'room A is exited'
      def nextRoom = roomA.exit("B")

    then: 'we get room B'
      nextRoom == roomB
  }

  def "can find nearest unexplored to room with unexplored exits"() {
    given: 'a room with two exits, one of which is unexplored, the other of which is linked to a room with unexplored exits'
      def roomA = RoomNode.create("roomA", "#A", "Room A", ["B", "C"])
      def roomB = RoomNode.create("roomB", "#B", "Room B", ["A", "D", "E"])
      roomA.link("B", roomB).link("A", roomA)

    when: 'we find the nearest unexplored room to room A'
      def route = roomA.findNearestUnexplored().toList()

    then: 'we get a route with one step, from room A to the unexplored room C'
      route.size() == 1
      def step = route[0]
      step.from == roomA
      step.exit == "C"
      step.to == roomA.exit("C")
  }

  def "can find nearest unexplored to room with no unexplored exits"() {
    given: 'a room with one exit, linked to a room with unexplored exits, linked to another room with unexplored exits'
      def roomA = RoomNode.create("roomA", "#A", "Room A", ["B"])
      def roomB = RoomNode.create("roomB", "#B", "Room B", ["A", "C", "D"])
      def roomC = RoomNode.create("roomC", "#C", "Room C", ["B", "E"])
      roomA.link("B", roomB).link("A", roomA)
      roomB.link("C", roomC).link("B", roomB)

    when: 'we find the nearest unexplored room to room A'
      def route = roomA.findNearestUnexplored().toList()

    then: 'we get a route with two steps, from A to B to the unexplored room D'
      route.size() == 2
      def step1 = route[0]
      step1.from == roomA
      step1.exit == "B"
      step1.to == roomA.exit("B")
      def step2 = route[1]
      step2.from == roomB
      step2.exit == "D"
      step2.to == roomB.exit("D")
  }

  def "full route to nearest unexplored room is given when no rooms are teleportable"() {
    given: 'a room with one exit, linked to a room with two exits, linked to another room with unexplored exits'
      def roomA = RoomNode.create("roomA", null, "Room A", ["B"])
      def roomB = RoomNode.create("roomB", null, "Room B", ["A", "C"])
      def roomC = RoomNode.create("roomC", null, "Room C", ["B", "D"])
      roomA.link("B", roomB).link("A", roomA)
      roomB.link("C", roomC).link("B", roomB)

    when: 'we find the nearest unexplored room to room A'
      def route = roomA.findNearestUnexplored().toList()

    then: 'we get a route with three steps, from A to B to C to unexplored room D'
      route.size() == 3
      def step1 = route[0]
      step1.from == roomA
      step1.exit == "B"
      step1.to == roomA.exit("B")
      def step2 = route[1]
      step2.from == roomB
      step2.exit == "C"
      step2.to == roomB.exit("C")
      def step3 = route[2]
      step3.from == roomC
      step3.exit == "D"
      step3.to == roomC.exit("D")
  }

  def setupSpec() {
    TestUtils.decoratePropertyContainer()
  }

  def setup() {
    db = new TestGraphDatabaseFactory().newImpermanentDatabase()
    RoomNode.initialise(db)
    tx = db.beginTx()
  }

  def cleanup() {
    TestUtils.printMap(db, "\nMap after test:")
    tx.close()
    db.shutdown()
  }
}
