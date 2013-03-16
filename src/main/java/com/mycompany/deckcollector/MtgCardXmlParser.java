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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author jwalton
 */
public class MtgCardXmlParser {

    private org.jdom2.Document jdomDoc;
    private String mongoHost = "dbh74.mongolab.com";
    private int mongoPort = 27747;
    private String collectionName = "cardInfo";
    private DBCollection cardCollection;
    
    public MtgCardXmlParser(){
        try {
            Mongo m = new Mongo(mongoHost, mongoPort);
            DB db = m.getDB("mtg");
            db.authenticate("mtg", "mtg".toCharArray());
            cardCollection = db.getCollection(collectionName);
        } catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    //Get JDOM document from DOM Parser
    private static org.jdom2.Document useDOMParser(String fileName)
            throws ParserConfigurationException, SAXException, IOException {
        //creating DOM Document
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new File(fileName));
        DOMBuilder domBuilder = new DOMBuilder();
        return domBuilder.build(doc);

    }

    public void parseFile(String file) {
        try {
            jdomDoc = useDOMParser(file);
            Element root = jdomDoc.getRootElement();

            Element cardListElement = root.getChild("cards");
            System.out.println("Found " + cardListElement.getChildren().size() + " card elements");
            List<Card> cardList = new ArrayList<Card>();
            int count = 0;
            for (Element cardElement : cardListElement.getChildren()) {

                Card card = new Card();
                card.setId(Integer.parseInt(cardElement.getChildText("id")));
                card.setName(cardElement.getChildText("name"));
                card.setCost(cardElement.getChildText("cost"));
                card.setColor(cardElement.getChildText("color"));
                card.setType(cardElement.getChildText("set"));
                card.setRarity(cardElement.getChildText("rarity"));
                if (cardElement.getChild("power") != null && cardElement.getChildText("power").length() > 0) {
                    try {
                        card.setPower(cardElement.getChildText("power"));
                    } catch (Exception e) {
                    }
                }
                if (cardElement.getChild("toughness") != null && cardElement.getChildText("toughness").length() > 0) {
                    try {
                        card.setToughness(cardElement.getChildText("toughness"));
                    } catch (Exception e) {
                    }
                }
                card.setRules(cardElement.getChildText("rules"));
                card.setPrintedname(cardElement.getChildText("printedname"));
                card.setPrintedtype(cardElement.getChildText("printedtype"));
                card.setFlavor(cardElement.getChildText("flavor"));
                card.setWatermark(cardElement.getChildText("watermark"));
                if (cardElement.getChild("cardnum") != null && cardElement.getChildText("cardnum").length() > 0) {
                    try {
                        card.setCardnum(Integer.parseInt(cardElement.getChildText("cardnum")));
                    } catch (Exception e) {
                    }
                }
                card.setArtist(cardElement.getChildText("artist"));
                card.setSets(cardElement.getChildText("sets"));
                card.setRulings(cardElement.getChildText("rulings"));
                System.out.println("Card: " + card.toString());
                Map<String,String> cardInfo = card.getInfo();
                DBObject query = new BasicDBObject();
                query.put("name", card.getName());
                DBObject cardObject = new BasicDBObject();
                cardObject.putAll(cardInfo);
                cardCollection.update(query, cardObject, true, false);
                count++;
                System.out.println("Processed card "+card.getName()+" "+count+" out of "+cardListElement.getChildren().size());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MtgCardXmlParser parser = new MtgCardXmlParser();
        parser.parseFile("/Users/jwalton/mtgCardXml/mtgCardInfo.xml");
    }
}
