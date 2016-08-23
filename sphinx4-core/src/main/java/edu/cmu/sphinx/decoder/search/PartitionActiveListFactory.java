///*
// *
// * Copyright 1999-2004 Carnegie Mellon University.
// * Portions Copyright 2004 Sun Microsystems, Inc.
// * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
// * All Rights Reserved.  Use is subject to license terms.
// *
// * See the file "license.terms" for information on usage and
// * redistribution of this file, and for a DISCLAIMER OF ALL
// * WARRANTIES.
// *
// */
//package edu.cmu.sphinx.decoder.search;
//
//import edu.cmu.sphinx.util.props.PropertyException;
//import edu.cmu.sphinx.util.props.PropertySheet;
//
//import java.util.*;
//import java.util.concurrent.ConcurrentSkipListSet;
//
///** A factory for PartitionActiveLists */
//public class PartitionActiveListFactory extends ActiveListFactory {
//
//    /**
//     *
//     * @param absoluteBeamWidth beam for absolute pruning
//     * @param relativeBeamWidth beam for relative pruning
//     */
//    public PartitionActiveListFactory(int absoluteBeamWidth, double relativeBeamWidth) {
//        super(absoluteBeamWidth, relativeBeamWidth);
//    }
//
//    public PartitionActiveListFactory() {
//
//    }
//
//    /*
//    * (non-Javadoc)
//    *
//    * @see edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util.props.PropertySheet)
//    */
//    @Override
//    public void newProperties(PropertySheet ps) throws PropertyException {
//        super.newProperties(ps);
//    }
//
//
//    /*
//    * (non-Javadoc)
//    *
//    * @see edu.cmu.sphinx.decoder.search.ActiveListFactory#newInstance()
//    */
//    @Override
//    public ActiveList newInstance() {
//        return new PartitionActiveList(absoluteBeamWidth, logRelativeBeamWidth);
//    }
//
//
//    /**
//     * An active list that does absolute beam with pruning by partitioning the
//     * token list based on absolute beam width, instead of sorting the token
//     * list, and then chopping the list up with the absolute beam width. The
//     * expected run time of this partitioning algorithm is O(n), instead of O(n log n)
//     * for merge sort.
//     * <p>
//     * This class is not thread safe and should only be used by a single thread.
//     * <p>
//     * Note that all scores are maintained in the LogMath log base.
//     */
//    class PartitionActiveList implements ActiveList {
//
//
//        private final int absoluteBeamWidth;
//        private final float logRelativeBeamWidth;
//
//
//        // when the list is changed these things should be
//        // changed/updated as well
//        //private Token[] tokens;
//        private final ConcurrentSkipListSet<Token> tokens = new ConcurrentSkipListSet<>();
//        private Token bestToken;
//
//
//        /** Creates an empty active list
//         * @param absoluteBeamWidth beam for absolute pruning
//         * @param logRelativeBeamWidth beam for relative pruning
//         */
//        public PartitionActiveList(int absoluteBeamWidth,
//                                   float logRelativeBeamWidth) {
//            this.absoluteBeamWidth = absoluteBeamWidth;
//            this.logRelativeBeamWidth = logRelativeBeamWidth;
//        }
//
//
//        /**
//         * Adds the given token to the list
//         *
//         * @param token the token to add
//         */
//        public void add(Token token) {
//
//
//            int capacity = absoluteBeamWidth;
//
//            int nt = tokens.size();
//            if (nt >= capacity) {
//                if (token.score() < tokens.last().score())
//                    return ; //reject immediately
//
//                int toRemove = nt - (capacity-1);
//                for (int i = 0; i < toRemove; i++)
//                    tokens.pollLast();
//
//            }
//
//            tokens.add(token);
//
//        }
//
//        /**
//         * Purges excess members. Remove all nodes that fall below the relativeBeamWidth
//         *
//         * @return a (possible new) active list
//         */
//        public ActiveList commit() {
////            // if the absolute beam is zero, this means there
////            // should be no constraint on the abs beam size at all
////            // so we will only be relative beam pruning, which means
////            // that we don't have to sort the list
////            if (tokenList!=null && tokenList.length > 0 && absoluteBeamWidth > 0) {
////                // if we have an absolute beam, then we will
////                // need to sort the tokens to apply the beam
////                if (size > absoluteBeamWidth) {
////                    size = Partitioner.partition(tokenList, size,
////                            absoluteBeamWidth) + 1;
////                }
////            }
////            if (size > 0 && tokens !=null && absoluteBeamWidth > 0) {
////                Arrays.sort(tokens, 0, size, (a, b) -> Float.compare(score(b), score(a)));
////
////                //System.out.println(size + " " + absoluteBeamWidth);
////
////                size = Math.min(size, absoluteBeamWidth);
////            }
//
//            return this;
//        }
//
////        private float score(Token a) {
////            if (a == null)
////                return Float.NEGATIVE_INFINITY;
////            return a.score();
////        }
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
//            if (bestToken == null)
//            //if (tokens.isEmpty())
//                return -Float.MAX_VALUE;
//            else {
//                //return best().score();
//                return bestToken.score();
//            }
////            return bestToken!=null ? bestToken.score() : -Float.MAX_VALUE;
//////            float bestScore = -Float.MAX_VALUE;
//////            if (bestToken != null) {
//////                bestScore = score(bestToken);
//////            }
//////            // A sanity check
//////            // for (Token t : this) {
//////            //    if (t.getScore() > bestScore) {
//////            //         System.out.println("GBS: found better score "
//////            //             + t + " vs. " + bestScore);
//////            //    }
//////            // }
//////            return bestScore;
//        }
//
//
//        /**
//         * Sets the best scoring token for this active list
//         *
//         * @param best the best scoring token
//         */
//        @Deprecated public void setBestToken(Token best) {
//            /*if (!best().equals(best))
//                System.err.println("why " + best + " is it " + best());*/
//            /*if (best!=null)
//                add(best);*/
//            bestToken = best;
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
//            //return tokens.first();
//        }
//
//
//        /**
//         * Retrieves the iterator for this tree.
//         *
//         * @return the iterator for this token list
//         */
//        public final Iterator<Token> iterator() {
//            return tokens.iterator(); //new TokenArrayIterator(tokens, size);
//        }
//
////        /*public final void forEach(Consumer<? super Token> action) {
////            Arrays.stream(tokens, 0, size)./*parallel().*/forEach(action);
////        }*/
//
//
////        public final void forEach2(Consumer<? super Token> action) {
////            if (size == 0)
////                return;
////
////            boolean parallel = true;
////
////            int threads = 4;
////            int minBatchSize = 16;
////
////            int batchSize = size / threads;
////            if (batchSize < minBatchSize)
////                forEachSerial(action, 0, size);
////            else {
////                System.out.println(size);
////
////                List<Callable<Object>> cc = new ArrayList(threads);
////                int from = -batchSize;
////                for (int i = 0; i < threads; i++) {
////                    from += batchSize;
////                    int to = Math.min(size, from + batchSize);
////                    int finalFrom = from;
////                    cc.add(() -> {
////                        System.out.println("\t" + finalFrom + " .. " + to );
////                        forEachSerial(action, finalFrom, to);
////                        System.out.println("\t done " + finalFrom + " .. " + to );
////                        return null;
////                    });
////                }
////
////
////                try {
////                    ForkJoinPool.commonPool().invokeAll(cc, 10000, TimeUnit.MILLISECONDS);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
////            }
////
////
////
////        }
////
////        private void forEachSerial(Consumer<? super Token> action, int start, int stop) {
////            Arrays.stream(tokens, start, stop).forEach(action);
////        }
////
////        /**
////         * Gets the list of all tokens
////         *
////         * @return the list of tokens
////         */
//        @Override
//        public Iterable<Token> getTokens() {
//            return tokens; //Arrays.asList(tokens).subList(0, size);
//        }
//
//        /**
//         * Returns the number of tokens on this active list
//         *
//         * @return the size of the active list
//         */
//        public final int size() {
//            return tokens.size();
//        }
//
//
//        /* (non-Javadoc)
//        * @see edu.cmu.sphinx.decoder.search.ActiveList#createNew()
//        */
//        public ActiveList newInstance() {
//            return PartitionActiveListFactory.this.newInstance();
//        }
//    }
//}
//
//class TokenArrayIterator implements Iterator<Token> {
//
//    private final Token[] tokenArray;
//    private final int size;
//    private int pos;
//
//
//    TokenArrayIterator(Token[] tokenArray, int size) {
//        this.tokenArray = tokenArray;
//        this.pos = 0;
//        this.size = size;
//    }
//
//
//    /** Returns true if the iteration has more tokens. */
//    public boolean hasNext() {
//        return pos < size;
//    }
//
//
//    /** Returns the next token in the iteration. */
//    public Token next() throws NoSuchElementException {
//        if (pos >= tokenArray.length) {
//            throw new NoSuchElementException();
//        }
//        return tokenArray[pos++];
//    }
//
//
//    /** Unimplemented, throws an Error if called. */
//    public void remove() {
//        throw new Error("TokenArrayIterator.remove() unimplemented");
//    }
//}
