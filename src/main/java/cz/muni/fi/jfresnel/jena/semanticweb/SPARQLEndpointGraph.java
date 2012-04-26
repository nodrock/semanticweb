/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.jfresnel.jena.semanticweb;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.WrappedIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author nodrock
 */
public class SPARQLEndpointGraph extends GraphBase implements Graph {

    private String service;
    private Map<String,Collection<Triple>> cache;
    
    public SPARQLEndpointGraph(String service) {
         this.service = service;
         this.cache = new HashMap<String, Collection<Triple>>();
    }

    @Override
    protected ExtendedIterator graphBaseFind(TripleMatch tm) {
        String subject;
        if(tm.getMatchSubject() == null){
            subject = "?subject";
        }else{
            subject = "<" + tm.getMatchSubject().getURI() + ">";
        }
        String predicate;
        if(tm.getMatchPredicate() == null){
            predicate = "?predicate";
        }else{
            predicate = "<" + tm.getMatchPredicate().getURI() + ">";
        }
        String object;
        if(tm.getMatchObject() == null){
            object = "?object";
        }else{
            object = tm.getMatchObject().isURI() ? "<" + tm.getMatchObject().getURI() + ">" : "\"" + tm.getMatchObject().getLiteralLexicalForm() + "\"";
        }
      
        String queryType;
        if(tm.getMatchSubject() != null && tm.getMatchPredicate() != null && tm.getMatchObject() != null){
            queryType = "ASK";
        }else{
            queryType = "SELECT";
        }
        
        String where = "WHERE {" + subject + " " + predicate + " " + object + "}";
        
        
        String query;
        if(queryType.equals("ASK")){
            query = "ASK " + where;
        }else{
            query = "SELECT " + (tm.getMatchSubject() == null ? "?subject " : "") +  
                (tm.getMatchPredicate() == null ? "?predicate " : "") +  
                (tm.getMatchObject() == null ? "?object " : "") + where; 
        }
        
        
        if(cache.containsKey(query)){
            System.out.println("CACHE HIT: " + query);
            return WrappedIterator.create(cache.get(query).iterator());
        }else{
            System.out.println(query);
        
            QueryExecution sparqlService = QueryExecutionFactory.sparqlService(service, query);

            List<Triple> triples = new ArrayList<Triple>();
            if(queryType.equals("SELECT")){                               
                ResultSet result = sparqlService.execSelect();

                while(result.hasNext()){
                    QuerySolution solution = result.nextSolution();

                    Node s = tm.getMatchSubject() == null ? solution.getResource("subject").asNode() : tm.getMatchSubject();
                    Node p = tm.getMatchPredicate() == null ? solution.getResource("predicate").asNode() : tm.getMatchPredicate();
                    Node o = tm.getMatchObject() == null ? solution.get("object").asNode() : tm.getMatchObject();

                    Triple t = new Triple(s, p, o); 

                    triples.add(t);
                }
            }else{
                boolean exist = sparqlService.execAsk();
                if(exist){
                    triples.add(tm.asTriple());
                }
            }
            cache.put(query, triples);
            return WrappedIterator.create(triples.iterator());
        }
        
    }
    
}
