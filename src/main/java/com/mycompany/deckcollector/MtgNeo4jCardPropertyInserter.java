/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.deckcollector;

import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 *
 * @author jwalton
 */
public class MtgNeo4jCardPropertyInserter {

    private String mongoHost = "dbh74.mongolab.com";
    private int mongoPort = 27747;
    private String collectionName = "cardInfo";
    private DBCollection cardCollection;
    private String HEROKU_NEO4J = "http://2645f4a7d.hosted.neo4j.org:7343/db/data/";
    private String herokuUser = "5c7d14ae5";
    private String herokuPassword = "579e8cded";
    private Map<String, Node> cardNodes = new HashMap<String, Node>();
    private RestGraphDatabase graphDb;

    public MtgNeo4jCardPropertyInserter() {
        init();
    }

    private void init() {
        try {
            Mongo m = new Mongo(mongoHost, mongoPort);
            DB db = m.getDB("mtg");
            db.authenticate("mtg", "mtg".toCharArray());
            cardCollection = db.getCollection(collectionName);

            graphDb = new RestGraphDatabase(HEROKU_NEO4J, herokuUser, herokuPassword);
            Iterable<Node> allNodes = graphDb.getAllNodes();
            Iterator<Node> it = allNodes.iterator();
            while (it.hasNext()) {
                Node node = it.next();
                if (node.hasProperty("name")) {
                    cardNodes.put(node.getProperty("name").toString(), node);
                }
            }
            System.out.println("There are " + cardNodes.size() + " card nodes");
            registerShutdownHook(graphDb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addCardInfoToNeo4j() {
        DBCursor cursor = cardCollection.find();
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        while (cursor.hasNext()) {
            DBObject cardObject = cursor.next();
            String cardName = cardObject.get("name").toString().toUpperCase();
            if (cardNodes.get(cardName) == null) {
                Node newCardNode = graphDb.createNode();
                newCardNode.setProperty("name", cardName);
                for(String property : cardObject.keySet()){
                    if(!property.equalsIgnoreCase("name")){
                        newCardNode.setProperty(property, cardObject.get(property).toString());
                    }
                }
                cardNodes.put(cardName, newCardNode);
            } else {
                Node node = cardNodes.get(cardName);
                for(String property : cardObject.keySet()){
                    if(!property.equalsIgnoreCase("name")){
                        node.setProperty(property, cardObject.get(property).toString());
                    }
                }
            }
        }
    }

    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running example before it's completed)
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }
    
    public static void main(String[] args){
        MtgNeo4jCardPropertyInserter dataInserter = new MtgNeo4jCardPropertyInserter();
        dataInserter.addCardInfoToNeo4j();
    }
}
