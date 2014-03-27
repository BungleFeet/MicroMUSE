package net.lazygun.micromuse

import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.PropertyContainer
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Transaction
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.graphdb.traversal.Traverser
import org.neo4j.graphdb.traversal.Uniqueness
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.tooling.GlobalGraphOperations
import spock.lang.Specification

import static net.lazygun.micromuse.Neo4JMuseMap.ROOM
import static net.lazygun.micromuse.Neo4JMuseMap.RelTypes.EXIT
import static net.lazygun.micromuse.Neo4JMuseMap.TELEPORTABLE
import static net.lazygun.micromuse.Neo4JMuseMap.UNEXPLORED
import static org.neo4j.graphdb.Direction.INCOMING
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * Created by ewan on 26/03/2014.
 */
class Neo4JMuseMapTest extends Specification {

  GraphDatabaseService graphDb
  GlobalGraphOperations globalGraphOperations
  Neo4JMuseMap map
  ExecutionEngine cypher
  Transaction tx
  Node home
  TraversalDescription traverser

  List<String> homeExits = ('A'..'C').toList()

  def "replace unexplored room with regular room"() {
    given: 'a new room to be saved'
    def room = new Room("Room A", "Test room A", ('1'..'3').toList())

    when: 'the new room is linked to from the home room via the first exit'
    def link = new Link(map.home, homeExits[0], room)
    link = map.createLink(link)

    then: 'the new room has been saved'
    link.to().id != null
    def roomNode = graphDb.getNodeById(link.to().id)
    roomNode.name == "Room A"

    and: 'the new room has the correct exits linking to and from it'
    def incoming =  roomNode.getRelationships(INCOMING).toList()
    def outgoing = roomNode.getRelationships(OUTGOING).toList()
    incoming.size() == 1
    outgoing.size() == 3
    incoming[0].name == homeExits[0]
    outgoing.name.sort() == room.exits

    and: 'the new room is linked to the correct rooms'
    incoming[0].startNode.id == home.id
    outgoing.every { it.endNode.name == UnexploredRoom.NAME }
  }

  def "create a link back to home room"() {
    given: 'a new room, linked to the home room from its first exit'
    def room = new Room("Room A", "Test room A", ('1'..'3').toList())
    def link = map.createLink(new Link(map.home, homeExits[0], room))
    room = link.to()

    when: 'the new room is linked back to the home room via exit "1"'
    def homeLink = new Link(room, '1', new Room(map.home.name, map.home.description, map.home.exits))
    homeLink = map.createLink(homeLink)

    then: 'the new link connects back to home'
    homeLink.to().id == home.id

    and: 'the new room has the correct exits linking to and from it'
    def roomNode = graphDb.getNodeById(link.to().id)
    def incoming =  roomNode.getRelationships(INCOMING).toList()
    def outgoing = roomNode.getRelationships(OUTGOING).toList()
    incoming.size() == 1
    outgoing.size() == 3
    incoming[0].name == homeExits[0]
    outgoing.name.sort() == room.exits

    and: 'the new room is linked to the correct rooms'
    incoming[0].startNode.id == home.id
    def homeRel = outgoing.find { it.endNode.id == home.id }
    homeRel != null
    (outgoing - homeRel).every { it.endNode.name == UnexploredRoom.NAME }
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
    map = new Neo4JMuseMap(graphDb)
    cypher = new ExecutionEngine(graphDb)
    tx = graphDb.beginTx()
    
    createHomeNode()
    createLinksTraversalDescription()

    printMap("Starting graph:")
  }

  def cleanup() {
    printMap("Finishing graph:")
    tx.close()
    graphDb.shutdown()
  }
  
  void createHomeNode() {
    home = graphDb.createNode(ROOM, TELEPORTABLE)
    home.name = "Home"
    home.location = "#0"
    home.description = ""
    homeExits.each { exit ->
      def unexploredNode = graphDb.createNode(ROOM, UNEXPLORED)
      unexploredNode.name = UnexploredRoom.NAME
      def rel = home.createRelationshipTo(unexploredNode, EXIT)
      rel.name = exit
      unexploredNode
    }
    assert map.home.id == 0
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
