/*
 * Copyright 1999-2010 Carnegie Mellon University.
 * Portions Copyright 2010 PC-NG Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.jsgf;

import com.fasterxml.aalto.sax.SAXParserFactoryImpl;
import edu.cmu.sphinx.jsgf.rule.JSGFRule;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Grammar for GrXML W3C Standard
 */
public class GrXMLGrammar extends JSGFGrammar {

    final Map<String, JSGFRule> rules = new LinkedHashMap<>(16*1024);

    protected void loadXML() throws IOException {
        try {
            SAXParserFactoryImpl spf = new SAXParserFactoryImpl();
            spf.setValidating(false);

            SAXParser parser = spf.newSAXParser();


            XMLReader xr = parser.getXMLReader();


            GrXMLHandler handler = new GrXMLHandler(baseURL, rules, logger);
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            InputStream is = baseURL.openStream();
            xr.parse(new InputSource(is));
            is.close();
        } catch (SAXParseException e) {
            String msg = "Error while parsing line " + e.getLineNumber() + " of " + baseURL + ": " + e.getMessage();
            throw new IOException(msg);
        } catch (SAXException e) {
            throw new IOException("Problem with XML: " + e);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }

        return;
    }

    /**
     * Commit changes to all loaded grammars and all changes of grammar since
     * the last commitChange
     *
     * @throws JSGFGrammarParseException parse exception occured
     * @throws JSGFGrammarException other exception occured
     */
    @Override
    public void commitChanges() throws IOException, JSGFGrammarParseException,
            JSGFGrammarException {
        try {
            if (loadGrammar) {
                if (manager == null)
                    getGrammarManager();
                loadXML();
                loadGrammar = false;
            }

            ruleStack = new RuleStack();
            newGrammar();

            firstNode = createGrammarNode("<sil>");
            GrammarNode finalNode = createGrammarNode("<sil>");
            finalNode.setFinalNode(true);

            // go through each rule and create a network of GrammarNodes
            // for each of them

            for (Map.Entry<String, JSGFRule> entry : rules.entrySet()) {

                    GrammarGraph publicRuleGraph = newGG();
                    ruleStack.push(entry.getKey(), publicRuleGraph);
                    GrammarGraph graph = processRule(entry.getValue());
                    ruleStack.pop();

                    firstNode.add(publicRuleGraph.getStartNode(), 0.0f);
                    publicRuleGraph.getEndNode().add(finalNode, 0.0f);
                    publicRuleGraph.getStartNode().add(graph.getStartNode(),
                            0.0f);
                    graph.getEndNode().add(publicRuleGraph.getEndNode(), 0.0f);
            }
            postProcessGrammar();
        } catch (MalformedURLException mue) {
            throw new IOException("bad base grammar URL " + baseURL + ' ' + mue);
        }
    }

}
