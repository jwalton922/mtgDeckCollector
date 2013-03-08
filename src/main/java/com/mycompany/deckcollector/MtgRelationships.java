/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.deckcollector;

import org.neo4j.graphdb.RelationshipType;

/**
 *
 * @author jwalton
 */
public enum MtgRelationships implements RelationshipType{
    IN_DECK_WITH;
}
