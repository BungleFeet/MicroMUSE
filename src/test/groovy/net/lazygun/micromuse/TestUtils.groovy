package net.lazygun.micromuse

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.PropertyContainer
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.traversal.Evaluators
import org.neo4j.graphdb.traversal.Uniqueness
import org.neo4j.tooling.GlobalGraphOperations

import static net.lazygun.micromuse.neo4j.RoomNode.ROOM
import static net.lazygun.micromuse.neo4j.RoomNode.Relation.EXIT
import static org.neo4j.graphdb.Direction.OUTGOING

/**
 * TODO: Write Javadocs for this class.
 * Created: 06/04/2014 18:11
 * @author Ewan
 */
class TestUtils {

  static void decoratePropertyContainer() {
    PropertyContainer.metaClass  {
      propertyMissing { String name -> delegate.getProperty(name)}
      propertyMissing { String name, val -> delegate.setProperty(name, val)}
    }
  }

  static void printMap(GraphDatabaseService db, title, includeLabels = false) {
    println(title)
    db.traversalDescription()
      .breadthFirst()
      .relationships(EXIT, OUTGOING)
      .uniqueness(Uniqueness.NODE_PATH)
      .evaluator(Evaluators.includingDepths(1, 1))
      .traverse(GlobalGraphOperations.at(db).getAllNodesWithLabel(ROOM).iterator().toList().toArray() as org.neo4j.graphdb.Node[])
      .iterator().toList().each { p ->
      p.each { PropertyContainer e ->
        switch (e) {
          case org.neo4j.graphdb.Node:
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
