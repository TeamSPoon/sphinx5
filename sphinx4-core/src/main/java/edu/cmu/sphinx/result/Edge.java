/*
* Copyright 1999-2002 Carnegie Mellon University.
* Portions Copyright 2002 Sun Microsystems, Inc.
* Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
* All Rights Reserved.  Use is subject to license terms.
*
* See the file "license.terms" for information on usage and
* redistribution of this file, and for a DISCLAIMER OF ALL
* WARRANTIES.
*
*/
package edu.cmu.sphinx.result;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

/** Edges are part of Lattices.  They connect Nodes, and contain the score associated with that sequence. */
public class Edge {

    protected double acousticScore;
    protected double lmScore;
    protected final Node fromNode;
    protected final Node toNode;


    /**
     * Create an Edge from fromNode to toNode with acoustic and Language Model scores.
     *
     * @param fromNode from node
     * @param toNode to node
     * @param acousticScore acoustic score
     * @param lmScore langauge model score
     */
    protected Edge(Node fromNode, Node toNode,
                   double acousticScore, double lmScore) {
        this.acousticScore = acousticScore;
        this.lmScore = lmScore;
        this.fromNode = fromNode;
        this.toNode = toNode;
    }


    @Override
    public String toString() {
        return "Edge(" + fromNode + "-->" + toNode + '[' + acousticScore
                + ',' + lmScore + "])";
    }


    /**
     * Internal routine used when creating a Lattice from a .LAT file
     *
     * @param lattice
     * @param tokens
     */
    static void load(Lattice lattice, StringTokenizer tokens) {

        String from = tokens.nextToken();
        String to = tokens.nextToken();
        double aScore = Double.parseDouble(tokens.nextToken());
        double lmScore = Double.parseDouble(tokens.nextToken());

        Node fromNode = lattice.getNode(from);
        if (fromNode == null) {
            throw new Error("Edge fromNode \"" + from + "\" does not exist");
        }

        Node toNode = lattice.getNode(to);
        if (toNode == null) {
            throw new Error("Edge toNode \"" + to + "\" does not exist");
        }

        lattice.addEdge(fromNode, toNode, aScore, lmScore);
    }


    /**
     * Internal routine used when dumping a Lattice as a .LAT file
     *
     * @param f
     * @throws IOException
     */
    void dump(PrintWriter f) {
        f.println("edge: " + fromNode.getId() + ' ' + toNode.getId() + ' '
                + acousticScore + ' ' + lmScore);
    }


    /**
     * Internal routine used when dumping a Lattice as an AiSee file
     *
     * @param f
     * @throws IOException
     */
    void dumpAISee(FileWriter f) throws IOException {
        f.write("edge: { sourcename: \"" + fromNode.getId()
                + "\" targetname: \"" + toNode.getId()
                + "\" label: \"" + acousticScore + ',' + lmScore + "\" }\n");
    }

    /**
     * Internal routine used when dumping a Lattice as an Graphviz file
     *
     * @param f file writer
     * @throws IOException if error occured
     */
    public void dumpDot(FileWriter f) throws IOException {
        String label = acousticScore + "," + lmScore;
        f.write("\tnode" + fromNode.getId() + " -> node" + toNode.getId() 
                + " [ label=\"" + label + "\" ]\n");
    }

    /**
     * Get the acoustic score associated with an Edge. This is the acoustic
     * score of the word that this edge is transitioning to, that is, the word
     * represented by the node returned by the getToNode() method.
     * 
     * @return the acoustic score of the word this edge is transitioning to
     */
    public double getAcousticScore() {
        return acousticScore;
    }


    /**
     * Get the language model score associated with an Edge
     *
     * @return the score
     */
    public double getLMScore() {
        return lmScore;
    }


    /**
     * Get the "from" Node associated with an Edge
     *
     * @return the Node
     */
    public Node getFromNode() {
        return fromNode;
    }


    /**
     * Get the "to" Node associated with an Edge
     *
     * @return the Node
     */
    public Node getToNode() {
        return toNode;
    }


    /**
     * Sets the acoustic score
     *
     * @param v the acoustic score.
     */
    public void setAcousticScore(double v) {
        acousticScore = v;
    }


    /**
     * Sets the language model score
     *
     * @param v the lm score.
     */
    public void setLMScore(double v) {
        lmScore = v;
    }


    /**
     * Returns true if the given edge is equivalent to this edge. Two edges are equivalent only if they have their
     * 'fromNode' and 'toNode' are equivalent, and that their acoustic and language scores are the same.
     *
     * @param other the Edge to compare this Edge against
     * @return true if the Edges are equivalent; false otherwise
     */
    public boolean isEquivalent(Edge other) {
        /*
         * TODO: Figure out why there would be minute differences
         * in the acousticScore. Therefore, the equality of the acoustic
         * score is judge based on whether the difference is bigger than 1.
         */
        double diff = Math.abs(acousticScore) * 0.00001;
        return ((Math.abs(acousticScore - other.acousticScore) <= diff &&
                lmScore == other.lmScore) &&
                (fromNode.isEquivalent(other.fromNode) &&
                        toNode.isEquivalent(other.toNode)));
    }
}
