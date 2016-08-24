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

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import org.eclipse.collections.impl.collector.Collectors2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author plamere
 */
public class SortingActiveListFactory extends ActiveListFactory {
    /**
     * @param absoluteBeamWidth absolute pruning beam
     * @param relativeBeamWidth relative pruning beam
     */
    public SortingActiveListFactory(int absoluteBeamWidth,
                                    double relativeBeamWidth) {
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

        private final int absoluteBeamWidth;
        private final float logRelativeBeamWidth;
        //private Token bestToken;
        // when the list is changed these things should be
        // changed/updated as well
        //private List<Token> tokenList;

        private final ConcurrentSkipListSet<Token> tokens = new ConcurrentSkipListSet<>();
        //private final ArrayList buffer = new ArrayList(); //copy where the set is buffered for traversal
        private final AtomicInteger size = new AtomicInteger(0); //concurrent skiplist set is slow tracking its own size so it can be done here


        /**
         * Creates an empty active list
         *
         * @param absoluteBeamWidth    beam for absolute pruning
         * @param logRelativeBeamWidth beam for relative pruning
         */
        public SortingActiveList(int absoluteBeamWidth, float logRelativeBeamWidth) {
            this.absoluteBeamWidth = absoluteBeamWidth;
            this.logRelativeBeamWidth = logRelativeBeamWidth;

            //int initListSize = absoluteBeamWidth > 0 ? absoluteBeamWidth : DEFAULT_SIZE;
            //this.tokens = new ArrayList<>(initListSize);
        }


        /**
         * Adds the given token to the list
         *
         * @param token the token to add
         */
        public void add(Token token) {


            int nt = size.get();
            int capacity = absoluteBeamWidth;

//            if (atCapacity) {
//                if (token.score() <= tokens.last().score())
//                    return; //reject immediately
//            }

            int delta = 0;
            if (tokens.add(token)) {
                delta++;
                int toRemove = (nt + 1) - (capacity);
                for (int i = 0; i < toRemove; i++) {
                    if (tokens.pollLast() != null) {
                        delta--;
                    }
                }
            }
            size.addAndGet(delta);

        }

        /**
         * Purges excess members. Reduce the size of the token list to the absoluteBeamWidth
         *
         * @return a (possible new) active list
         */
        public final ActiveList commit() {
            //this self-sorts, no further maintenance required
            return this;
        }


        /**
         * gets the beam threshold best upon the best scoring token
         *
         * @return the beam threshold
         */
        public final float getBeamThreshold() {
            return bestScore() + logRelativeBeamWidth;
        }


        /**
         * gets the best score in the list
         *
         * @return the best score
         */
        public final float bestScore() {
//            float bestScore = -Float.MAX_VALUE;
//            if (bestToken != null) {
//                bestScore = bestToken.score();
//            }
//            return bestScore;
            if (isEmpty())
                return -Float.MAX_VALUE;
            else
                return tokens.first().score();
        }

        private boolean isEmpty() {
            return size.get() <= 0;
        }


        /**
         * Gets the best scoring token for this active list
         *
         * @return the best scoring token
         */
        public Token best() {
            //return bestToken;
            return !isEmpty() ? tokens.first() : null;
        }


        /**
         * Retrieves the iterator for this tree.
         *
         * @return the iterator for this token list
         */
        public Iterator<Token> iterator() {
            return tokens.iterator();
        }

        @Override
        public void forEach(Consumer<? super Token> action) {
            //buffer.clear();
            //buffer.addAll(tokens);

            int threads = 4;
            int granularity = 4;
            int chunkSize = Math.max(1, size.get()/(granularity * threads));
            //this buffers the set into sublists and then parallel processes them in the fork join common pool
            tokens.stream().sequential().collect(Collectors2.chunk(chunkSize)).parallelStream().forEach(x -> {
                //System.out.println(Thread.currentThread() + " thread processing " + x.size());
                x.forEach(action);
            });
        }


        /**
         * Returns the number of tokens on this active list
         *
         * @return the size of the active list
         */
        public final int size() {
            return size.get();
        }


        /* (non-Javadoc)
        * @see edu.cmu.sphinx.decoder.search.ActiveList#newInstance()
        */
        public ActiveList newInstance() {
            return SortingActiveListFactory.this.newInstance();
        }
    }


//    class SortingActiveListWorking implements ActiveList {
//
//        private final static int DEFAULT_SIZE = 1000;
//        private final int absoluteBeamWidth;
//        private final float logRelativeBeamWidth;
//        private Token bestToken;
//        // when the list is changed these things should be
//        // changed/updated as well
//        private List<Token> tokenList;
//
//
//        /**
//         * Creates an empty active list
//         *
//         * @param absoluteBeamWidth beam for absolute pruning
//         * @param logRelativeBeamWidth beam for relative pruning
//         */
//        public SortingActiveListWorking(int absoluteBeamWidth, float logRelativeBeamWidth) {
//            this.absoluteBeamWidth = absoluteBeamWidth;
//            this.logRelativeBeamWidth = logRelativeBeamWidth;
//
//            int initListSize = absoluteBeamWidth > 0 ? absoluteBeamWidth : DEFAULT_SIZE;
//            this.tokenList = new ArrayList<>(initListSize);
//        }
//
//
//        /**
//         * Adds the given token to the list
//         *
//         * @param token the token to add
//         */
//        public void add(Token token) {
//            tokenList.add(token);
//            if (bestToken == null || token.score() > bestToken.score()) {
//                bestToken = token;
//            }
//        }
//
//        /**
//         * Purges excess members. Reduce the size of the token list to the absoluteBeamWidth
//         *
//         * @return a (possible new) active list
//         */
//        public ActiveList commit() {
//            // if the absolute beam is zero, this means there
//            // should be no constraint on the abs beam size at all
//            // so we will only be relative beam pruning, which means
//            // that we don't have to sort the list
//            if (absoluteBeamWidth > 0 && tokenList.size() > absoluteBeamWidth) {
//                Collections.sort(tokenList, Scoreable.COMPARATOR);
//                tokenList = tokenList.subList(0, absoluteBeamWidth);
//            }
//            return this;
//        }
//
//
//        /**
//         * gets the beam threshold best upon the best scoring token
//         *
//         * @return the beam threshold
//         */
//        public float getBeamThreshold() {
//            return bestScore() + logRelativeBeamWidth;
//        }
//
//
//        /**
//         * gets the best score in the list
//         *
//         * @return the best score
//         */
//        public float bestScore() {
//            float bestScore = -Float.MAX_VALUE;
//            if (bestToken != null) {
//                bestScore = bestToken.score();
//            }
//            return bestScore;
//        }
//
//
//        /**
//         * Sets the best scoring token for this active list
//         *
//         * @param token the best scoring token
//         */
//        public void setBestToken(Token token) {
//            bestToken = token;
//        }
//
//
//        /**
//         * Gets the best scoring token for this active list
//         *
//         * @return the best scoring token
//         */
//        public Token best() {
//            return bestToken;
//        }
//
//
//        /**
//         * Retrieves the iterator for this tree.
//         *
//         * @return the iterator for this token list
//         */
//        public Iterator<Token> iterator() {
//            return tokenList.iterator();
//        }
//
//
//        /**
//         * Gets the list of all tokens
//         *
//         * @return the list of tokens
//         */
//        public Iterable<Token> getTokens() {
//            return tokenList;
//        }
//
//        /**
//         * Returns the number of tokens on this active list
//         *
//         * @return the size of the active list
//         */
//        public final int size() {
//            return tokenList.size();
//        }
//
//
//        /* (non-Javadoc)
//        * @see edu.cmu.sphinx.decoder.search.ActiveList#newInstance()
//        */
//        public ActiveList newInstance() {
//            return SortingActiveListFactory.this.newInstance();
//        }
//    }

}
