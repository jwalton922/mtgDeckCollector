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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.UniqueFactory;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 *
 * @author jwalton
 */
public class MtgNeo4jInserter {

    private DBCollection deckCollection;
    private RestGraphDatabase graphDb;
    public static String NEO4J_DB_PATH = "testDB";
    private Map<String, Node> cardNodes = new HashMap<String, Node>();
    private String HEROKU_NEO4J = "http://2645f4a7d.hosted.neo4j.org:7343/db/data";
    private String herokuUser = "5c7d14ae5";
    private String herokuPassword = "579e8cded";
    private Map<String, Integer> relationshipToCountMap = new HashMap<String, Integer>();
    private ExecutorService execService;

    public MtgNeo4jInserter() {
        execService = Executors.newFixedThreadPool(10);
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
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void processDecksWithMultipleInsertsPerTransaction() {
        DBCursor cursor = deckCollection.find();
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        int countProcessed = 0;
        while (cursor.hasNext()) {
            countProcessed++;
            DBObject deckObject = cursor.next();
            List<String> cards = (List<String>) deckObject.get("cards");
            System.out.println("DECK " + countProcessed + ": Adding relationships for " + cards.size() + " cards");
            for (int i = 0; i < cards.size(); i++) {
                for (int j = i + 1; j < cards.size(); j++) {
                    String card1 = cards.get(i);
                    String card2 = cards.get(j);
                    List<String> cardRelationship = new ArrayList<String>();
                    cardRelationship.add(card1);
                    cardRelationship.add(card2);
                    Collections.sort(cardRelationship);
                    String jointCardString = cardRelationship.get(0) + "|||" + cardRelationship.get(1);
                    if (relationshipToCountMap.containsKey(jointCardString)) {
                        Integer count = relationshipToCountMap.get(jointCardString);
                        count++;
                        relationshipToCountMap.put(jointCardString, count);
//                        if(count >= 2){
//                            System.out.println("Found edge with count = "+count);
//                        }
                    } else {
                        relationshipToCountMap.put(jointCardString, 1);
                    }
                }
            }


            System.out.println("Processed " + countProcessed + " decks");

        }

        System.out.println("Found " + relationshipToCountMap.size() + " edges");
        Set<String> relationshipStrings = relationshipToCountMap.keySet();
        List<EdgeNodeHolder> relationshipList = new ArrayList<EdgeNodeHolder>();
        int minCount = 3;
        for (String relationshipString : relationshipStrings) {
            String[] stringSplit = relationshipString.split("\\|\\|\\|");
            EdgeNodeHolder relationship = new EdgeNodeHolder(stringSplit[0], stringSplit[1], relationshipToCountMap.get(relationshipString));
            if (relationship.count > minCount) {
                relationshipList.add(relationship);
            }
        }
        System.out.println("After eliminating counts <= " + minCount + " have " + relationshipList.size() + " edges");
        Collections.sort(relationshipList, new EdgeNodeHolderComparator());
        int transactionCount = 100;
        int outerLoopSize = (int) (Math.floor(relationshipList.size() / transactionCount) + 1);

        System.out.println("Will have " + outerLoopSize + " tranasctions of " + transactionCount + " relationship inserts");
        for (int i = 0; i < outerLoopSize; i++) {
            Transaction tx = graphDb.beginTx();
            try {
                for (int j = 0; j < transactionCount; j++) {
                    int index = i * transactionCount + j;
                    if (index >= relationshipList.size()) {
                        break;
                    }
                    EdgeNodeHolder r = relationshipList.get(index);

                    Node node1 = cardNodes.get(r.card1);
                    Node node2 = cardNodes.get(r.card2);
                    if (node1 == null) {
                        System.out.println("Could not find node for: " + r.card1);
                        continue;
                    }

                    if (node2 == null) {
                        System.out.println("Could not find node for: " + r.card2);
                        continue;
                    }


                    Iterable<Relationship> relationships = node1.getRelationships();
                    boolean foundRelationship = false;
                    for (Relationship relationship : relationships) {
                        Node relatedNode = relationship.getOtherNode(node1);
                        String relatedName = relatedNode.getProperty("name").toString();
                        if (relatedName.equalsIgnoreCase(node2.getProperty("name").toString())) {
                            Integer count = (Integer) relationship.getProperty("count");
                            count += r.getCount();
                            relationship.setProperty("count", count);
                            foundRelationship = true;
                            break;
                        }
                    }
                    if (!foundRelationship) {
                        System.out.println(": Creating new relationship between " + node1.getProperty("name") + " to " + node2.getProperty("name"));
                        Relationship relationship = node1.createRelationshipTo(node2, MtgRelationships.IN_DECK_WITH);
                        relationship.setProperty("count", r.getCount());
                    } else {
                        //System.out.println("updated relationship");
                    }

                    System.out.println("Processed relationship: " + index + " out of " + relationshipList.size());
                }
                tx.success();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                tx.finish();
            }
            System.out.println("Finished transaction set: " + i + " out of " + outerLoopSize);

        }


    }

    public void processDecksOld() {
        DBCursor cursor = deckCollection.find();
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        int countProcessed = 0;
        while (cursor.hasNext()) {
            countProcessed++;
            DBObject deckObject = cursor.next();
            List<String> cards = (List<String>) deckObject.get("cards");
            List<Node> nodes = new ArrayList<Node>();
            for (String card : cards) {

                //System.out.println("Found nodes? "+foundNodes);
                if (!cardNodes.containsKey(card)) {
                    System.out.println("Created new node for card: " + card);
                    Node newCardNode = graphDb.createNode();
                    newCardNode.setProperty("name", card);
                    nodes.add(newCardNode);
                    cardNodes.put(card, newCardNode);
                } else {
                    nodes.add(cardNodes.get(card));
                }

            }
            System.out.println("DECK " + countProcessed + ": Adding relationships for " + cards.size() + " cards");
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

    public void processDecks() {
        DBCursor cursor = deckCollection.find();
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        int countProcessed = 0;
        while (cursor.hasNext()) {
            countProcessed++;
            DBObject deckObject = cursor.next();
            List<String> cards = (List<String>) deckObject.get("cards");
            System.out.println("DECK " + countProcessed + ": Adding relationships for " + cards.size() + " cards");
            for (int i = 0; i < cards.size(); i++) {
                for (int j = i + 1; j < cards.size(); j++) {
                    String card1 = cards.get(i);
                    String card2 = cards.get(j);
                    List<String> cardRelationship = new ArrayList<String>();
                    cardRelationship.add(card1);
                    cardRelationship.add(card2);
                    Collections.sort(cardRelationship);
                    String jointCardString = cardRelationship.get(0) + "|||" + cardRelationship.get(1);
                    if (relationshipToCountMap.containsKey(jointCardString)) {
                        Integer count = relationshipToCountMap.get(jointCardString);
                        count++;
                        relationshipToCountMap.put(jointCardString, count);
                    } else {
                        relationshipToCountMap.put(jointCardString, 1);
                    }
                }
            }
            System.out.println("Processed " + countProcessed + " decks");

        }

        System.out.println("Found " + relationshipToCountMap.size() + " edges");
        Set<String> relationshipStrings = relationshipToCountMap.keySet();
        List<EdgeNodeHolder> relationshipList = new ArrayList<EdgeNodeHolder>();
        int minCount = 3;
        for (String relationshipString : relationshipStrings) {
            String[] stringSplit = relationshipString.split("\\|\\|\\|");
            EdgeNodeHolder relationship = new EdgeNodeHolder(stringSplit[0], stringSplit[1], relationshipToCountMap.get(relationshipString));
            if (relationship.count > minCount) {
                relationshipList.add(relationship);
            }
        }
        System.out.println("After eliminating counts <= " + minCount + " have " + relationshipList.size() + " edges");
        Collections.sort(relationshipList, new EdgeNodeHolderComparator());

        List<EdgeNodeHolder> processList = new ArrayList<EdgeNodeHolder>();
        int threadSize = 1000;
        for (int i = 0; i < relationshipList.size(); i++) {
            processList.add(relationshipList.get(i));
            if (processList.size() == threadSize) {
                List<EdgeNodeHolder> copy = new ArrayList<EdgeNodeHolder>(processList);
                processList = new ArrayList<EdgeNodeHolder>();
                RelationshipInserter inserter = new RelationshipInserter(i, copy);
                System.out.println("Submiting thread to insert relationships " + (i - 1000) + "-" + i);
                execService.submit(inserter);
            }
        }

        System.out.println("Submitted all threads");
        execService.shutdown();
        try {
            execService.awaitTermination(2, TimeUnit.DAYS);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        MtgNeo4jInserter inserter = new MtgNeo4jInserter();
        inserter.processDecks();
        System.exit(1);
    }

    public class EdgeNodeHolderComparator implements Comparator<EdgeNodeHolder> {

        public int compare(EdgeNodeHolder a, EdgeNodeHolder b) {
            return a.card1.compareTo(b.card1);
        }
    }

    private class EdgeNodeHolder {

        public String card1;
        public String card2;
        public int count;

        public EdgeNodeHolder(String card1, String card2, int count) {
            this.card1 = card1;
            this.card2 = card2;
            this.count = count;
        }

        public void incrementCount() {
            this.count++;
        }

        public int getCount() {
            return this.count;
        }
    }

    private class RelationshipInserter implements Runnable {

        private List<EdgeNodeHolder> relationshipList;
        private int id;

        public RelationshipInserter(int id, List<EdgeNodeHolder> relationshipList) {
            this.relationshipList = relationshipList;
            this.id = id;
        }

        public void run() {
            for (int i = 0; i < relationshipList.size(); i++) {
                Transaction tx = graphDb.beginTx();
                try {
                    EdgeNodeHolder r = relationshipList.get(i);

                    Node node1 = cardNodes.get(r.card1);
                    Node node2 = cardNodes.get(r.card2);
                    if (node1 == null) {
                        System.out.println("Could not find node for: " + r.card1);
                        continue;
                    }

                    if (node2 == null) {
                        System.out.println("Could not find node for: " + r.card2);
                        continue;
                    }

                    Iterable<Relationship> relationships = node1.getRelationships();
                    boolean foundRelationship = false;
                    for (Relationship relationship : relationships) {
                        Node relatedNode = relationship.getOtherNode(node1);
                        String relatedName = relatedNode.getProperty("name").toString();
                        if (relatedName.equalsIgnoreCase(node2.getProperty("name").toString())) {
                            Integer count = (Integer) relationship.getProperty("count");
                            count += r.getCount();
                            relationship.setProperty("count", count);
                            foundRelationship = true;
                            break;
                        }
                    }
                    if (!foundRelationship) {
                        System.out.println(": Creating new relationship between " + node1.getProperty("name") + " to " + node2.getProperty("name"));
                        Relationship relationship = node1.createRelationshipTo(node2, MtgRelationships.IN_DECK_WITH);
                        relationship.setProperty("count", r.getCount());
                    } else {
                        //System.out.println("updated relationship");
                    }

                    System.out.println("Id = " + id + ": Processed relationship: " + i + " out of " + relationshipList.size());

                    tx.success();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    tx.finish();
                }

            }
        }
    }
}
