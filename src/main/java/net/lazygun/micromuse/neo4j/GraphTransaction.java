package net.lazygun.micromuse.neo4j;

import net.lazygun.micromuse.Transaction;

/**
 *
 * @author Ewan
 */
public class GraphTransaction implements Transaction {

    private final org.neo4j.graphdb.Transaction transaction;

    public GraphTransaction(org.neo4j.graphdb.Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public void failure() {
        transaction.failure();
    }

    @Override
    public void success() {
        transaction.success();
    }

    @Override
    public void close() {
        transaction.close();
    }
}
