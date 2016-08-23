/*
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.decoder.search;

import edu.cmu.sphinx.decoder.scorer.Scoreable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author plamere
 */
public class SortingActiveListFactory extends ActiveListFactory {
    /**
     * @param absoluteBeamWidth absolute pruning beam
     * @param relativeBeamWidth relative pruning beam
     */
    public SortingActiveListFactory(int absoluteBeamWidth,
            double relativeBeamWidth)
    {
        super(absoluteBeamWidth, relativeBeamWidth);
    }

    public SortingActiveListFactory() {

    }
    
    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
    */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
    }


    /*
    * (non-Javadoc)
    *
    * @see edu.cmu.sphinx.decoder.search.ActiveListFactory#newInstance()
    */
    @Override
    public ActiveList newInstance() {
        return new SortingActiveList(absoluteBeamWidth, logRelativeBeamWidth);
    }


    /**
     * An active list that tries to be simple and correct. This type of active list will be slow, but should exhibit
     * correct behavior. Faster versions of the ActiveList exist (HeapActiveList, TreeActiveList).
     * <p>
     * This class is not thread safe and should only be used by a single thread.
     * <p>
     * Note that all scores are maintained in the LogMath log base.
     */

    class SortingActiveList implements ActiveList {

        private final static int DEFAULT_SIZE = 1000;
        private final int absoluteBeamWidth;
        private final float logRelativeBeamWidth;
        private Token bestToken;
        // when the list is changed these things should be
        // changed/updated as well
        //private List<Token> tokenList;
        private final ConcurrentSkipListSet<Token> tokens = new ConcurrentSkipListSet<>();



        /** 
         * Creates an empty active list
         * 
         * @param absoluteBeamWidth beam for absolute pruning
         * @param logRelativeBeamWidth beam for relative pruning
         */
        public SortingActiveList(int absoluteBeamWidth, float logRelativeBeamWidth) {
            this.absoluteBeamWidth = absoluteBeamWidth;
            this.logRelativeBeamWidth = logRelativeBeamWidth;

            int initListSize = absoluteBeamWidth > 0 ? absoluteBeamWidth : DEFAULT_SIZE;
            //this.tokens = new ArrayList<>(initListSize);
        }


        /**
         * Adds the given token to the list
         *
         * @param token the token to add
         */
        public void add(Token token) {
            tokens.add(token);
            if (bestToken == null || token.score() > bestToken.score()) {
                bestToken = token;
            }
        }

        /**
         * Purges excess members. Reduce the size of the token list to the absoluteBeamWidth
         *
         * @return a (possible new) active list
         */
        public ActiveList commit() {
            // if the absolute beam is zero, this means there
            // should be no constraint on the abs beam size at all
            // so we will only be relative beam pruning, which means
            // that we don't have to sort the list
            int s = tokens.size();
            int capacity = this.absoluteBeamWidth;

            //System.out.println(tokens.first() + " " + tokens.first().score() + "   -- - - - - - - " + tokens.last() + " " + tokens.last().score());

            if (capacity > 0 && s > capacity) {
//                Collections.sort(tokens, Scoreable.COMPARATOR);
//                tokens = tokens.subList(0, absoluteBeamWidth);

                int toRemove = s - (capacity-1);
                for (int i = 0; i < toRemove; i++)
                    tokens.pollLast();

                //System.out.println("\t" + tokens.first() + " " + tokens.first().score() + " " + tokens.last() + " " + tokens.last().score());

            }
            return this;
        }


        /**
         * gets the beam threshold best upon the best scoring token
         *
         * @return the beam threshold
         */
        public float getBeamThreshold() {
            return bestScore() + logRelativeBeamWidth;
        }


        /**
         * gets the best score in the list
         *
         * @return the best score
         */
        public float bestScore() {
            float bestScore = -Float.MAX_VALUE;
            if (bestToken != null) {
                bestScore = bestToken.score();
            }
            return bestScore;
        }


        /**
         * Sets the best scoring token for this active list
         *
         * @param token the best scoring token
         */
        public void setBestToken(Token token) {
            bestToken = token;
        }


        /**
         * Gets the best scoring token for this active list
         *
         * @return the best scoring token
         */
        public Token best() {
            return bestToken;
        }


        /**
         * Retrieves the iterator for this tree.
         *
         * @return the iterator for this token list
         */
        public Iterator<Token> iterator() {
            return tokens.iterator();
        }


        /**
         * Gets the list of all tokens
         *
         * @return the list of tokens
         */
        public Iterable<Token> getTokens() {
            return tokens;
        }

        /**
         * Returns the number of tokens on this active list
         *
         * @return the size of the active list
         */
        public final int size() {
            return tokens.size();
        }


        /* (non-Javadoc)
        * @see edu.cmu.sphinx.decoder.search.ActiveList#newInstance()
        */
        public ActiveList newInstance() {
            return SortingActiveListFactory.this.newInstance();
        }
    }


    class SortingActiveListWorking implements ActiveList {

        private final static int DEFAULT_SIZE = 1000;
        private final int absoluteBeamWidth;
        private final float logRelativeBeamWidth;
        private Token bestToken;
        // when the list is changed these things should be
        // changed/updated as well
        private List<Token> tokenList;


        /**
         * Creates an empty active list
         *
         * @param absoluteBeamWidth beam for absolute pruning
         * @param logRelativeBeamWidth beam for relative pruning
         */
        public SortingActiveListWorking(int absoluteBeamWidth, float logRelativeBeamWidth) {
            this.absoluteBeamWidth = absoluteBeamWidth;
            this.logRelativeBeamWidth = logRelativeBeamWidth;

            int initListSize = absoluteBeamWidth > 0 ? absoluteBeamWidth : DEFAULT_SIZE;
            this.tokenList = new ArrayList<>(initListSize);
        }


        /**
         * Adds the given token to the list
         *
         * @param token the token to add
         */
        public void add(Token token) {
            tokenList.add(token);
            if (bestToken == null || token.score() > bestToken.score()) {
                bestToken = token;
            }
        }

        /**
         * Purges excess members. Reduce the size of the token list to the absoluteBeamWidth
         *
         * @return a (possible new) active list
         */
        public ActiveList commit() {
            // if the absolute beam is zero, this means there
            // should be no constraint on the abs beam size at all
            // so we will only be relative beam pruning, which means
            // that we don't have to sort the list
            if (absoluteBeamWidth > 0 && tokenList.size() > absoluteBeamWidth) {
                Collections.sort(tokenList, Scoreable.COMPARATOR);
                tokenList = tokenList.subList(0, absoluteBeamWidth);
            }
            return this;
        }


        /**
         * gets the beam threshold best upon the best scoring token
         *
         * @return the beam threshold
         */
        public float getBeamThreshold() {
            return bestScore() + logRelativeBeamWidth;
        }


        /**
         * gets the best score in the list
         *
         * @return the best score
         */
        public float bestScore() {
            float bestScore = -Float.MAX_VALUE;
            if (bestToken != null) {
                bestScore = bestToken.score();
            }
            return bestScore;
        }


        /**
         * Sets the best scoring token for this active list
         *
         * @param token the best scoring token
         */
        public void setBestToken(Token token) {
            bestToken = token;
        }


        /**
         * Gets the best scoring token for this active list
         *
         * @return the best scoring token
         */
        public Token best() {
            return bestToken;
        }


        /**
         * Retrieves the iterator for this tree.
         *
         * @return the iterator for this token list
         */
        public Iterator<Token> iterator() {
            return tokenList.iterator();
        }


        /**
         * Gets the list of all tokens
         *
         * @return the list of tokens
         */
        public Iterable<Token> getTokens() {
            return tokenList;
        }

        /**
         * Returns the number of tokens on this active list
         *
         * @return the size of the active list
         */
        public final int size() {
            return tokenList.size();
        }


        /* (non-Javadoc)
        * @see edu.cmu.sphinx.decoder.search.ActiveList#newInstance()
        */
        public ActiveList newInstance() {
            return SortingActiveListFactory.this.newInstance();
        }
    }

}
