package net.lazygun.micromuse

import spock.lang.Specification

/**
 * Created by ewan on 26/03/2014.
 */
class Neo4JMuseMapTest extends Specification {
  def "maximum of two numbers"() {
    expect:
    Math.max(1, 3) == 3
  }
}
