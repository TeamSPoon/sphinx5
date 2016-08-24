/*
 * 
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

import java.util.*;

/** A factory for simple active lists */
public class SimpleActiveListFactory extends ActiveListFactory {

    /**
     * Creates factory for simple active lists
     * @param absoluteBeamWidth absolute pruning beam
     * @param relativeBeamWidth relative pruning beam
     */
    public SimpleActiveListFactory(int absoluteBeamWidth,
            double relativeBeamWidth)
    {
        super(absoluteBeamWidth, relativeBeamWidth);
    }

    public SimpleActiveListFactory() {
        
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
        return new SimpleActiveList(absoluteBeamWidth, logRelativeBeamWidth);
    }


    /**
     * An active list that tries to be simple and correct. This type of active list will be slow, but should exhibit
     * correct behavior. Faster versions of the ActiveList exist (HeapActiveList, TreeActiveList).
     * <p>
     * This class is not thread safe and should only be used by a single thread.
     * <p>
     * Note that all scores are maintained in the LogMath log domain
     */
    public static class SimpleActiveList implements ActiveList {

        private int absoluteBeamWidth;
        private final float logRelativeBeamWidth;
        private Token bestToken;
        private final List<Token> tokenList = new ArrayList<>();


        /**
         * Creates an empty active list
         *
         * @param absoluteBeamWidth    the absolute beam width
         * @param logRelativeBeamWidth the relative beam width (in the log domain)
         */
        public SimpleActiveList(int absoluteBeamWidth,
                                float logRelativeBeamWidth) {
            this.absoluteBeamWidth = absoluteBeamWidth;
            this.logRelativeBeamWidth = logRelativeBeamWidth;
        }

        public void clear() {
            tokenList.clear();
            bestToken = null;
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


//        /**
//         * Replaces an old token with a new token
//         *
//         * @param oldToken the token to replace (or null in which case, replace works like add).
//         * @param newToken the new token to be placed in the list.
//         */
//        public void replace(Token oldToken, Token newToken) {
//            add(newToken);
//            if (oldToken != null) {
//                if (!tokenList.remove(oldToken)) {
//                    // Some optional debugging code here to dump out the paths
//                    // when this "should never happen" error happens
//                    // System.out.println("SimpleActiveList: remove "
//                    //         + oldToken + " missing, but replaced by "
//                    //         + newToken);
//                    // oldToken.dumpTokenPath(true);
//                    // newToken.dumpTokenPath(true);
//                }
//            }
//        }


        /**
         * Purges excess members. Remove all nodes that fall below the relativeBeamWidth
         *
         * @return a (possible new) active list
         */
        public ActiveList commit() {
            int s = size();
            if (absoluteBeamWidth > 0 && s > absoluteBeamWidth) {
                Collections.sort(tokenList, Scoreable.COMPARATOR);
                int overflow = s - absoluteBeamWidth;
                int last = s - 1;
                for (int i = 0; i < overflow; i++) {
                    tokenList.remove(last--);
                }
            }
            return this;
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
         * Retrieves the spliterator for this tree.
         *
         * @return the iterator for this token list
         */
        @Override
        public Spliterator<Token> spliterator() {
            return tokenList.spliterator();
        }

        /**
         * Gets the set of all tokens
         *
         * @return the set of tokens
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
        public final float bestScore() {
            if (bestToken != null) {
                return bestToken.score();
            } else {
                return -Float.MAX_VALUE;
            }
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


        /* (non-Javadoc)
        * @see edu.cmu.sphinx.decoder.search.ActiveList#createNew()
        */
        public ActiveList newInstance() {
            //return SimpleActiveListFactory.this.newInstance();
            clear();
            return this;
        }
    }
}
