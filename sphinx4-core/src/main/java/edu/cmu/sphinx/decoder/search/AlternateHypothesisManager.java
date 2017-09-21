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

package edu.cmu.sphinx.decoder.search;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for pruned hypothesis
 * 
 * @author Joe Woelfel
 */
public class AlternateHypothesisManager {

    private final Map<Token, Collection<Token>> viterbiLoserMap = new ConcurrentHashMap<>();
    private final int maxEdges;


    /**
     * Creates an alternate hypotheses manager
     *
     * @param maxEdges the maximum edges allowed
     */
    public AlternateHypothesisManager(int maxEdges) {
        this.maxEdges = maxEdges;
    }


    /**
     * Collects adds alternate predecessors for a token that would have lost because of viterbi.
     *
     * @param token       - a token that has an alternate lower scoring predecessor that still might be of interest
     * @param predecessor - a predecessor that scores lower than token.getPredecessor().
     */

    public void addAlternatePredecessor(Token token, Token predecessor) {
        assert predecessor != token.predecessor();

        viterbiLoserMap.computeIfAbsent(token, t -> Collections.synchronizedSet(new LinkedHashSet<>()) ).add(predecessor);
    }


    /**
     * Returns a list of alternate predecessors for a token.
     *
     * @param token - a token that may have alternate lower scoring predecessor that still might be of interest
     * @return A list of predecessors that scores lower than token.getPredecessor().
     */
    public Collection<Token> getAlternatePredecessors(Token token) {
        return viterbiLoserMap.get(token);
    }


    /** Purge all but max number of alternate preceding token hypotheses. */
    public void purge() {

        int max = maxEdges - 1;

        for (Map.Entry<Token, Collection<Token>> entry : viterbiLoserMap.entrySet()) {
            Collection<Token> list = entry.getValue();
            ArrayList ll = new ArrayList(list);
            Collections.sort(ll);
            int s = list.size();
            for (int i = 0; i < (s - max); i++) {
                list.remove(ll.remove(--s));
            }

             //   List<Token> newList = list.subList(0, max);
             //   entry.setValue(newList);
            //}
        }
    }

}

