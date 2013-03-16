/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.deckcollector;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jwalton
 */
public class Card {
    /**
     * <card>
			<id>366252</id>
			<lang>English</lang>
			<name>Zhur-Taa Swine</name>
			<altart></altart>
			<cost>3RG</cost>
			<color></color>
			<type>Creature - Boar</type>
			<set>Gatecrash</set>
			<rarity>Common</rarity>
			<power>5</power>
			<toughness>4</toughness>
			<rules>Bloodrush - {1}{R}{G}, Discard Zhur-Taa Swine: Target attacking creature gets +5/+4 until end of turn.</rules>
			<printedname>Zhur-Taa Swine</printedname>
			<printedtype>Creature - Boar</printedtype>
			<printedrules>Bloodrush - {1}{R}{G}, Discard Zhur-Taa Swine: Target attacking creature gets +5/+4 until end of turn.</printedrules>
			<flavor>Gurley was the first to domesticate one, though his widow didn&apos;t take much solace in that accomplishment.</flavor>
			<watermark>Gruul</watermark>
			<cardnum>210</cardnum>
			<artist>Yeong-Hao Han</artist>
			<sets>Gatecrash Common</sets>
			<rulings></rulings>
		</card>
     */
    private int id;
    private String lang;
    private String name;
    private String altart;
    private String cost;
    private String color;
    private String type;
    private String set;
    private String rarity;
    private String power;
    private String toughness;
    private String rules;
    private String printedname;
    private String printedtype;
    private String printedrules;
    private String flavor;
    private String watermark;
    private int cardnum;
    private String artist;
    private String sets;
    private String rulings;
    
    public Card(){
        
    }
    
    public Map<String,String> getInfo(){
        Map<String,String> info = new HashMap<String,String>();
        if(id >= 0){
        info.put("id",""+id);
        }
        
        if(lang != null){
            info.put("lang",lang);
        }
        
        if(name != null){
            info.put("name",name);
        }
        
        if(altart != null){
            info.put("altart",altart);
        }
        
        if(cost != null){
            info.put("cost",cost);
        }
        
        if(color != null){
            info.put("color",color);
        }
        
        if(type != null){
            info.put("type",type);
        }
        
        if(set != null){
            info.put("set",set);
        }
        
        if(rarity != null){
            info.put("rarity",rarity);
        }
        
        if(power != null){
            info.put("power",power);
        }
        
        if(toughness != null){
            info.put("toughness",toughness);
        }
        
        if(printedname != null){
            info.put("printedname",printedname);
        }
        
        if(printedtype != null){
            info.put("printedtype",printedtype);
        }
        
        if(printedrules != null){
            info.put("printedrules",printedrules);
        }
        
        if(flavor != null){
            info.put("flavor",flavor);
        }
        
        if(cardnum >= 0){
            info.put("cardnum", ""+cardnum);
        }
        
        if(artist != null){
            info.put("artist",artist);
        }
        
        if(sets != null){
            info.put("sets",sets);
        }
        
        if(rulings != null){
            info.put("rulings",rulings);
        }
        
        if(rules != null){
            info.put("rules", rules);
        }
        
        return info;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAltart() {
        return altart;
    }

    public void setAltart(String altart) {
        this.altart = altart;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSet() {
        return set;
    }

    public void setSet(String set) {
        this.set = set;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getPower() {
        return power;
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getToughness() {
        return toughness;
    }

    public void setToughness(String toughness) {
        this.toughness = toughness;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public String getPrintedname() {
        return printedname;
    }

    public void setPrintedname(String printedname) {
        this.printedname = printedname;
    }

    public String getPrintedtype() {
        return printedtype;
    }

    public void setPrintedtype(String printedtype) {
        this.printedtype = printedtype;
    }

    public String getPrintedrules() {
        return printedrules;
    }

    public void setPrintedrules(String printedrules) {
        this.printedrules = printedrules;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }

    public int getCardnum() {
        return cardnum;
    }

    public void setCardnum(int cardnum) {
        this.cardnum = cardnum;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getSets() {
        return sets;
    }

    public void setSets(String sets) {
        this.sets = sets;
    }

    public String getRulings() {
        return rulings;
    }

    public void setRulings(String rulings) {
        this.rulings = rulings;
    }
    
    @Override
    public String toString(){
        return "Card: "+this.name;
    }
    
    
    
}
