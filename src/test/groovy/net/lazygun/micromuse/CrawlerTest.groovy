package net.lazygun.micromuse

import net.lazygun.micromuse.neo4j.GraphRoomService
import net.lazygun.micromuse.neo4j.RoomNode
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.test.TestGraphDatabaseFactory
import org.neo4j.tooling.GlobalGraphOperations
import spock.lang.Specification

/**
 * @author Ewan
 */
class CrawlerTest extends Specification {

  GraphDatabaseService db
  GlobalGraphOperations ops
  List<Link> links = []
  Map<String, Room> rooms

  void createMap(int breadth, int depth) {
    def loc = { def i = 0; { -> Math.random() < 0.1 ? "#${++i}" : null } }()
    def createRoomWithLocation = { String from, String name, String location, boolean hasExits ->
      def exits = []
      if (hasExits) (1..breadth).each { exits << "${name}$it".toString()}
      if (from) exits << from
      new RoomImpl(name, location, '', exits)
    }
    def createRoom = { String from, String name, boolean exits ->  createRoomWithLocation(from, name, loc(), exits) }
    rooms = [('0'): createRoomWithLocation(null, '0', '#0', true)]
    (1..depth).each { currentDepth ->
      rooms.each { name, room ->
        room.exits.each { exit ->
          if (!rooms.containsKey(exit)) {
            def newRoom = createRoom(name, exit, currentDepth != depth)
            rooms += [(exit): newRoom]
            links << new Link(room, exit, newRoom)
            links << new Link(newRoom, name, room)
          }
        }
      }
    }
  }

  def "map can be crawled"(int crawlers) {
    given:
      def roomService = new GraphRoomService(db)
      def sessionFactory = new MapSessionFactory(links, roomService)

    when:
      def linksCreated = Crawler.crawl(crawlers, 1000, roomService, sessionFactory)
      def relationships = 0
      def nodes = 0
      org.neo4j.graphdb.Transaction tx = db.beginTx()
      try {
        relationships = ops.allRelationships.toList().size()
        nodes = ops.getAllNodesWithLabel(RoomNode.ROOM).toList().size()
      } finally {
        tx.close()
      }

    then:
      relationships == links.size()
      nodes == rooms.size()
      linksCreated == links.size()

    where:
      crawlers = 5
  }

  def setupSpec() {
    TestUtils.decoratePropertyContainer()
  }

  def setup() {
    db = new TestGraphDatabaseFactory().newImpermanentDatabase()
    ops = GlobalGraphOperations.at(db)
    createMap(10, 4)
    //links.each { println it }
  }

  def cleanup() {
    org.neo4j.graphdb.Transaction tx = db.beginTx()
    try {
      //TestUtils.printMap(db, "\nMap after test:")
    } finally {
      tx.close()
    }
    db.shutdown()
  }
}