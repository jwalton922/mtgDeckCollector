/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.deckcollector;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author jwalton
 */
public class TcgDeckCollector {

    public static final String TCG_PLAYER_ROOT = "http://magic.tcgplayer.com/";
    public static final String MONGO_HOST = "dbh74.mongolab.com";
    public static final int MONGO_PORT = 27747;
    public static final String MONGO_DB = "mtg";
    private DBCollection deckCollection;
    public static final String DECK_COLLECTION_NAME = "successfulDeckLists";
    private long startDeckCount;
    
    public TcgDeckCollector(){
        init();
    }
    
    private void init(){
        try {
            String user = "mtguser";
            Mongo m = new Mongo(MONGO_HOST, MONGO_PORT);
            DB db = m.getDB(MONGO_DB);
            db.authenticate(user, user.toCharArray());
            deckCollection = db.getCollection(DECK_COLLECTION_NAME);
            
            startDeckCount = deckCollection.getCount();
        } catch(Exception e){
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Do a deck search on tcgplayer, then save page and pass it to this method
     *
     * @param file
     */
    public void gatherDecksFromHtmlFile(String file) {
        File input = new File(file);
        try {
            Document doc = Jsoup.parse(input, "UTF-8", TCG_PLAYER_ROOT);
            Elements links = doc.select("td a");
            for (Element link : links) {
                String href = link.attr("href");
                if (href.indexOf("deck_id") >= 0) {
                    processDeckLink(href);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processDeckLink(String link) {
        String deckUrl = TCG_PLAYER_ROOT + link;
        List<String> deckCards = new ArrayList<String>();
        try {
            Document doc = Jsoup.connect(deckUrl).get();
            Elements linkElements = doc.select("td a");

            for (Element linkElement : linkElements) {
                String href = linkElement.attr("href");
                if (href.indexOf("magic_single_card") >= 0) {
                    String cardName = linkElement.text().trim();
                    System.out.println("Card: " + cardName);
                    if(cardName.length() > 1){
                        cardName = cardName.toUpperCase();
                        deckCards.add(cardName);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if(deckCards.size() >0){
            saveCards(deckCards);
        }
    }
    
    private void saveCards(List<String> deck){
        DBObject deckObject = new BasicDBObject();
        deckObject.put("cards", deck);
        deckCollection.insert(deckObject);
        startDeckCount++;
    }

    public static void main(String[] args) {
        TcgDeckCollector deckCollector = new TcgDeckCollector();
        deckCollector.gatherDecksFromHtmlFile("/Users/jwalton/Downloads/deckList.html");
    }
}
