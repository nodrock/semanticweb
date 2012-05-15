/*   AUTHOR : The-Nhan LUONG (The-Nhan.LUONG@inria.fr)
 *
 *  (c) COPYRIGHT INRIA (Institut National de Recherche en Informatique et en Automatique), 2005-2006.
 *  Licensed under the GNU LGPL. For full terms see the file COPYING.
 *
 * $Id: SPARQLJenaEvaluator.java 59 2007-11-22 16:41:17Z luong $
 */
package cz.muni.fi.jfresnel.jena.semanticweb;

import java.util.*;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Property;

import com.hp.hpl.jena.rdf.model.RDFErrorHandler;
import com.hp.hpl.jena.query.*;

import de.fuberlin.wiwiss.ng4j.semwebclient.SemanticWebClient;

import fr.inria.jfresnel.sparql.*;
import fr.inria.jfresnel.sparql.jena.SPARQLJenaEvaluator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SPARQLJenaSemWebClientEvaluator extends SPARQLJenaEvaluator implements RDFErrorHandler {
    private static final Logger logger = Logger.getLogger(SPARQLJenaSemWebClientEvaluator.class.getName());
    
    private static String RDFXMLAB = "RDF/XML-ABBREV";
    private Property RDF_TYPE;
    private SemanticWebClient swc;

    public SPARQLJenaSemWebClientEvaluator() {
    }

    public SPARQLJenaSemWebClientEvaluator(SPARQLNSResolver nsr) {
        this.nsr = nsr;
    }

    public void setSemanticWebClient(SemanticWebClient swc) {
        this.swc = swc;
        setModel(swc.asJenaModel("default"));
    } 

    /*Evaluate the SPARQL Query*/
    /*Now, we support only one variable in the query*/
    @Override
    public List<RDFNode> evaluateQuery(SPARQLQuery sparqlQuery) {
        List<RDFNode> queryResult = new ArrayList<RDFNode>();
        String queryString = sparqlQuery.toString();
        Map<String,String> prefixTable = nsr.getPrefixTable();
        Iterator<String> iter = prefixTable.keySet().iterator();
        while (iter.hasNext()) {
            String prefix = (String) iter.next();
            String prolog = "PREFIX " + prefix + ": <" + (String) prefixTable.get(prefix) + ">";
            queryString = prolog + NL + queryString;
        }
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, getModel());
        try {
            ResultSet results = qexec.execSelect();
            List varList = results.getResultVars();
            String var = (String) (varList.toArray())[0];
            for (; results.hasNext();) {
                QuerySolution soln = results.nextSolution();
                RDFNode x = soln.get(var);
                // Check the type of the result value
                if (x.isLiteral()) {
                    queryResult.add((Literal) x);
                } else // x is Resource
                {
                    queryResult.add((Resource) x);
                }
            }
        } finally {
            qexec.close();
            if (swc != null) {
                for(String s : swc.successfullyDereferencedURIs()){
                    logger.log(Level.INFO, "Dereferenced URI: {0}", s);
                }
                swc.clear();
            }
        }
        return queryResult;
    }
}
