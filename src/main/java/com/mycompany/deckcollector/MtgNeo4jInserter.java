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
import static com.mycompany.deckcollector.TcgDeckCollector.DECK_COLLECTION_NAME;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;

/**
 *
 * @author jwalton
 */
public class MtgNeo4jInserter {

    private DBCollection deckCollection;
    private RestGraphDatabase graphDb;
    public static String NEO4J_DB_PATH = "testDB";
    private Map<String,Node> cardNodes = new HashMap<String,Node>();
    private String HEROKU_NEO4J = "http://2645f4a7d.hosted.neo4j.org:7343/db/data";
    private String herokuUser = "5c7d14ae5";
    private String herokuPassword = "579e8cded";

    public MtgNeo4jInserter() {
        init();
    }

    private void init() {
        try {
            String user = "mtguser";
            Mongo m = new Mongo(TcgDeckCollector.MONGO_HOST, TcgDeckCollector.MONGO_PORT);
            DB db = m.getDB(TcgDeckCollector.MONGO_DB);
            db.authenticate(user, user.toCharArray());
            deckCollection = db.getCollection(DECK_COLLECTION_NAME);

            //graphDb = new RestGraphDatabase("http://localhost:7474/db/data");
            graphDb = new RestGraphDatabase(HEROKU_NEO4J,herokuUser,herokuPassword);
            Iterable<Node> allNodes = graphDb.getAllNodes();
            Iterator<Node> it = allNodes.iterator();
            while(it.hasNext()){
                Node node = it.next();
                if(node.hasProperty("name")){
                    cardNodes.put(node.getProperty("name").toString(), node);
                }
            }
            System.out.println("There are "+cardNodes.size()+" card nodes");
            registerShutdownHook(graphDb);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void processDecks() {
        DBCursor cursor = deckCollection.find();
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        int countProcessed = 0;
        while (cursor.hasNext()) {
            countProcessed++;
            DBObject deckObject = cursor.next();
            List<String> cards = (List<String>) deckObject.get("cards");
            List<Node> nodes = new ArrayList<Node>();
            for (String card : cards) {
                
//                RestCypherQueryEngine engine = new RestCypherQueryEngine(graphDb.getRestAPI());
//                String queryString = "start n=node(*) where has(n.name) AND n.name = {card} return n, n.name";
//                Map<String,Object> paramMap = new HashMap<String,Object>();
//                paramMap.put("card", card);
//                QueryResult result = engine.query(queryString, paramMap);
//                Iterator<Map<String,Object>> it = result.iterator();
//                boolean foundNodes = false;
//                while(it.hasNext()){
//                    Map<String,Object> resObj = it.next();
//                    for(String key : resObj.keySet()){
//                        System.out.println("Key = "+key+" value = "+resObj.get(key).toString());
//                    }
//                    graphDb.g
//                    //System.out.println("Search result: "+it.next().toString());
//                    foundNodes = true;
//                }
                //System.out.println("Found nodes? "+foundNodes);
                if(!cardNodes.containsKey(card)){
                    System.out.println("Created new node for card: "+card);
                    Node newCardNode = graphDb.createNode();
                    newCardNode.setProperty("name", card);
                    nodes.add(newCardNode);
                    cardNodes.put(card, newCardNode);
                } else {
                    nodes.add(cardNodes.get(card));
                }
//                if(foundNodes != null && foundNodes.hasNext()){
//                    System.out.println("Found node for: "+card);
//                    nodes.add(foundNodes.next());
//                } else {
//                    System.out.println("Creating node for: "+card);
//                    Node newCardNode = graphDb.createNode();
//                    newCardNode.setProperty("name", card);
//                    nodes.add(newCardNode);
//                }
//                Node cardNode = getOrCreateUserWithUniqueFactory(card, graphDb);
//                System.out.println("Have node for: " + cardNode.getProperty("name"));
//                nodes.add(cardNode);
            }
            System.out.println("DECK "+countProcessed+": Adding relationships for " + cards.size() + " cards");
            for (int i = 0; i < nodes.size(); i++) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    Node node1 = nodes.get(i);
                    Node node2 = nodes.get(j);
                    Transaction tx = graphDb.beginTx();
                    try {
                        Iterable<Relationship> relationships = node1.getRelationships();
                        boolean foundRelationship = false;
                        for (Relationship relationship : relationships) {
                            Node relatedNode = relationship.getOtherNode(node1);
                            String relatedName = relatedNode.getProperty("name").toString();
                            if (relatedName.equalsIgnoreCase(node2.getProperty("name").toString())) {
                                Integer count = (Integer) relationship.getProperty("count");
                                count++;
                                relationship.setProperty("count", count);
                                foundRelationship = true;
                                break;
                            }
                        }
                        if (!foundRelationship) {
                            System.out.println(": Creating new relationship between " + node1.getProperty("name") + " to " + node2.getProperty("name"));
                            Relationship relationship = node1.createRelationshipTo(node2, MtgRelationships.IN_DECK_WITH);
                            relationship.setProperty("count", 1);
                        } else {
                            //System.out.println("updated relationship");
                        }
                        tx.success();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        tx.finish();
                    }
                }
            }

            
            System.out.println("Processed " + countProcessed + " decks");
        }
    }

    public Node getOrCreateUserWithUniqueFactory(String username, GraphDatabaseService graphDb) {
        UniqueFactory<Node> factory = new UniqueFactory.UniqueNodeFactory(graphDb, "cards") {
            @Override
            protected void initialize(Node created, Map<String, Object> properties) {
                System.out.println("unique factory init called: " + created.toString() + " properties: " + properties.toString());
                created.setProperty("name", properties.get("name"));
            }
        };

        return factory.getOrCreate("name", username);
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

    public static void main(String[] args) {
        MtgNeo4jInserter inserter = new MtgNeo4jInserter();
        inserter.processDecks();
        System.exit(1);
    }
}
