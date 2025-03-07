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

package edu.cmu.sphinx.linguist.lextree;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.HMM;
import edu.cmu.sphinx.linguist.acoustic.HMMPool;
import edu.cmu.sphinx.linguist.acoustic.HMMPosition;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Pronunciation;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.Utilities;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/** Represents a node in the HMM Tree */

// For large vocabularies we may create millions of these objects,
// therefore they are extremely space sensitive. So we want to make
// these objects as small as possible.  The requirements for these
// objects when building the tree of nodes are very different from once
// we have built it. When building, we need to easily add successor
// nodes and quickly identify duplicate children nodes. After the tree
// is built we just need to quickly identify successors.  We want the
// flexibility of a map to manage successors at startup, but we don't
// want the space penalty (at least 5 32 bit fields per map), instead
// we'd like an array.  To support this dual mode, we manage the
// successors in an Object which can either be a Map or a List
// depending upon whether the node has been frozen or not.

class Node {

    private final static AtomicInteger serial = new AtomicInteger(0);
    private final int hash;
    //private static int nodeCount;
    //private static int successorCount;
    
    /** 
     * This can be either Map during tree construction or Array after
     * tree freeze. Conversion to array helps to save memory.
     */
    private Object successors;
    private float logUnigramProbability;


    /**
     * Creates a node
     *
     * @param probability the unigram probability for the node
     */
    Node(float probability) {
        logUnigramProbability = probability;
        this.hash = serial.incrementAndGet();
        //nodeCount++;
//        if ((nodeCount % 10000) == 0) {
//             System.out.println("NC " + nodeCount);
//        }
    }


    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object obj) {
        throw new UnsupportedOperationException("shouldnt be tested for equality");
        //return this == obj;
    }

    /**
     * Returns the unigram probability
     *
     * @return the unigram probability
     */
    float getUnigramProbability() {
        return logUnigramProbability;
    }


    /**
     * Sets the unigram probability
     *
     * @param probability the unigram probability
     */
    void setUnigramProbability(float probability) {
        logUnigramProbability = probability;
    }


    /**
     * Given an object get the set of successors for this object
     *
     * @param key the object key
     * @return the node containing the successors
     */
    private Node getSuccessor(Object key) {
        Map<Object, Node> successors = getSuccessorMap(false);
        return successors!=null ? successors.get(key) : null;
    }


    /**
     * Add the child to the set of successors
     *
     * @param key   the object key
     * @param child the child to add
     */
    void putSuccessor(Object key, Node child) {
        getSuccessorMap(true).put(key, child);
    }


    /**
     * Gets the successor map for this node
     *
     * @return the successor map
     */
    @SuppressWarnings({"unchecked"})
    Map<Object, Node> getSuccessorMap(boolean create) {
        if (successors == null && create) {
            successors = new HashMap<Object, Node>(1);
        }

        return (Map<Object, Node>) successors;
    }


    /** Freeze the node. Convert the successor map into an array list */
    void freeze() {
        if (successors instanceof Map<?,?>) {
            Map<Object, Node> map = (Map)successors;
            Collection<Node> values = map.values();
            successors = values.toArray(new Node[map.size()]);
            values.forEach(Node::freeze);
            //successorCount += map.size();
        }
    }


//    static void dumpNodeInfo() {
//        System.out.println("Nodes: " + nodeCount + " successors " +
//                successorCount + " avg " + (successorCount / nodeCount));
//    }


    /**
     * Adds a child node holding an hmm to the successor.  If a node similar to the child has already been added, we use
     * the previously added node, otherwise we add this. Also, we record the base unit of the child in the set of right
     * context
     *
     * @param hmm the hmm to add
     * @return the node that holds the hmm (new or old)
     */
    Node addSuccessor(HMM hmm, float probability) {
        Node child = null;
        Node matchingChild = getSuccessor(hmm);
        if (matchingChild == null) {
            child = new HMMNode(hmm, probability);
            putSuccessor(hmm, child);
        } else {
            if (matchingChild.logUnigramProbability < probability) {
                matchingChild.setUnigramProbability(probability);
            }
            child = matchingChild;
        }
        return child;
    }


    /**
     * Adds a child node holding a pronunciation to the successor. If a node similar to the child has already been
     * added, we use the previously added node, otherwise we add this. Also, we record the base unit of the child in the
     * set of right context
     *
     * @param pronunciation the pronunciation to add
     * @param wordNodeMap 
     * @return the node that holds the pronunciation (new or old)
     */
    WordNode addSuccessor(Pronunciation pronunciation, float probability, Map<Pronunciation, WordNode> wordNodeMap) {
        WordNode child = null;
        WordNode matchingChild = (WordNode) getSuccessor(pronunciation);
        if (matchingChild == null) {
            child = wordNodeMap.computeIfAbsent(pronunciation, c -> new WordNode(pronunciation, probability));
            putSuccessor(pronunciation, child);
        } else {
            if (matchingChild.getUnigramProbability() < probability) {
                matchingChild.setUnigramProbability(probability);
            }
            child = matchingChild;
        }
        return child;
    }


    void addSuccessor(WordNode wordNode) {
        putSuccessor(wordNode, wordNode);
    }


    /**
     * Adds an EndNode to the set of successors for this node If a node similar to the child has already been added, we
     * use the previously added node, otherwise we add this.
     *
     * @param child       the endNode to add
     * @param probability probability for this transition
     * @return the node that holds the endNode (new or old)
     */
    EndNode addSuccessor(EndNode child, float probability) {
        Unit baseUnit = child.baseUnit();
        EndNode matchingChild = (EndNode) getSuccessor(baseUnit);
        if (matchingChild == null) {
            putSuccessor(baseUnit, child);
        } else {
            if (matchingChild.getUnigramProbability() < probability) {
                matchingChild.setUnigramProbability(probability);
            }
            child = matchingChild;
        }
        return child;
    }


    /**
     * Adds a child node to the successor.  If a node similar to the child has already been added, we use the previously
     * added node, otherwise we add this. Also, we record the base unit of the child in the set of right context
     *
     * @param child the child to add
     * @return the node (may be different than child if there was already a node attached holding the hmm held by
     *         child)
     */
    UnitNode addSuccessor(UnitNode child) {
        UnitNode matchingChild = (UnitNode) getSuccessor(child.key());
        if (matchingChild == null) {
            putSuccessor(child.key(), child);
        } else {
            child = matchingChild;
        }

        return child;
    }


    /**
     * Returns the successors for this node
     *
     * @return the set of successor nodes
     */
    Node[] getSuccessors() {
        if (successors instanceof Map<?, ?>) {
            freeze();
        }
        return (Node[])successors;
    }


    /**
     * Returns the string representation for this object
     *
     * @return the string representation of the object
     */
    @Override
    public String toString() {
        return "Node ";
    }
}


/** A node representing a word in the HMM tree */
class WordNode extends Node {

    public final Pronunciation pronunciation;
    public final boolean isFinal;

    /**
     * Creates a word node
     *
     * @param pronunciation the pronunciation to wrap in this node
     * @param probability   the word unigram probability
     */
    WordNode(Pronunciation pronunciation, float probability) {
        super(probability);
        this.pronunciation = pronunciation;
        this.isFinal = pronunciation.getWord().isSentenceEndWord();
    }


    /**
     * Gets the word associated with this node
     *
     * @return the word
     */
    Word getWord() {
        return pronunciation.getWord();
    }


    /**
     * Gets the last unit for this word
     *
     * @return the last unit
     */
    Unit getLastUnit() {
        Unit[] units = pronunciation.units;
        return units[units.length - 1];
    }


    /**
     * Returns the successors for this node
     *
     * @return the set of successor nodes
     */
    @Override
    Node[] getSuccessors() {
        throw new Error("Not supported");
    }


    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "WordNode " + pronunciation + " p " +
                getUnigramProbability();
    }


}


/**
 * A class that represents the initial word in the search space. It is treated specially because we need to keep track
 * of the context as well. The context is embodied in the parent node
 */
class InitialWordNode extends WordNode {

    private final HMMNode parent;


    /**
     * Creates an InitialWordNode
     *
     * @param pronunciation the pronunciation
     * @param parent        the parent node
     */
    InitialWordNode(Pronunciation pronunciation, HMMNode parent) {
        super(pronunciation, LogMath.LOG_ONE);
        this.parent = parent;
    }


    /**
     * Gets the parent for this word node
     *
     * @return the parent
     */
    HMMNode getParent() {
        return parent;
    }

}


abstract class UnitNode extends Node {

    final static int SIMPLE_UNIT = 1;
    final static int WORD_BEGINNING_UNIT = 2;
    final static int SILENCE_UNIT = 3;
    final static int FILLER_UNIT = 4;




    /**
     * Creates the UnitNode
     *
     * @param probablilty the probability for the node
     */
    UnitNode(float probablilty) {
        super(probablilty);
    }


    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    abstract Unit baseUnit();


    abstract Object key();

    abstract HMMPosition position();


    /**
     * Gets the unit type (one of SIMPLE_UNIT, WORD_BEGINNING_UNIT, SIMPLE_UNIT or FILLER_UNIT
     *
     * @return the unit type
     */
    abstract int type();

//
//    /**
//     * Sets the unit type
//     *
//     * @param type the unit type
//     */
//    void setType(int type) {
//        this.type = type;
//    }

}

/** A node that represents an HMM in the hmm tree */

class HMMNode extends UnitNode {

    public final HMM hmm;

    // There can potentially be a large number of nodes (millions),
    // therefore it is important to conserve space as much as
    // possible.  While building the HMMNodes, we keep right contexts
    // in a set to allow easy pruning of duplicates.  Once the tree is
    // entirely built, we no longer need to manage the right contexts
    // as a set, a simple array will do. The freeze method converts
    // the set to the array of units.  This rcSet object holds the set
    // during construction and the array after the freeze.

    private Object rcSet;

    private final int type;

    /**
     * Creates the node, wrapping the given hmm
     *
     * @param hmm the hmm to hold
     */
    HMMNode(HMM hmm, float probablilty) {
        super(probablilty);
        this.hmm = hmm;

        Unit base = baseUnit();

        int type = SIMPLE_UNIT;
        if (base.silence) {
            type = SILENCE_UNIT;
        } else if (base.filler) {
            type = FILLER_UNIT;
        } else if (hmm.getPosition().isWordBeginning()) {
            type = WORD_BEGINNING_UNIT;
        }
        this.type = type;
    }

    @Override
    public final int type() {
        return type;
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    @Override
    Unit baseUnit() {
        // return hmm.getUnit().getBaseUnit();
        return hmm.getBaseUnit();
    }


    /**
     * Returns the hmm for this node
     *
     * @return the hmm
     */
    HMM getHMM() {
        return hmm;
    }


    @Override
    HMMPosition position() {
        return hmm.getPosition();
    }


    @Override
    HMM key() {
        return hmm;
    }


    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "HMMNode " + hmm + " p " + getUnigramProbability();
    }


    /**
     * Adds a right context to the set of possible right contexts for this node. This is typically only needed for hmms
     * at the ends of words.
     *
     * @param rc the right context.
     */
    void addRC(Unit rc) {
        getRCSet().add(rc);
    }


    /** Freeze this node. Convert the set into an array to reduce memory overhead */
    @Override
    @SuppressWarnings({"unchecked"})
    void freeze() {
        super.freeze();
        if (rcSet instanceof Set) {
            Set<Unit> set = (Set<Unit>) rcSet;
            rcSet = set.toArray(new Unit[set.size()]);
        }
    }


    /**
     * Gets the rc as a set. If we've already been frozen it is an error
     *
     * @return the set of right contexts
     */
    @SuppressWarnings({"unchecked"})
    private Set<Unit> getRCSet() {
        if (rcSet == null) {
            rcSet = new HashSet<Unit>();
        }

        assert rcSet instanceof HashSet;
        return (Set<Unit>) rcSet;
    }


    /**
     * returns the set of right contexts for this node
     *
     * @return the set of right contexts
     */
    Unit[] getRC() {
        if (rcSet instanceof HashSet<?>) {
            freeze();
        }
        return (Unit[]) rcSet;
    }
}


class EndNode extends UnitNode {

    private final Unit baseUnit;
    private final Unit leftContext;
    private final Integer key;


    /**
     * Creates the node, wrapping the given hmm
     *
     * @param baseUnit    the base unit for this node
     * @param lc          the left context
     * @param probablilty the probability for the transition to this node
     */
    EndNode(Unit baseUnit, Unit lc, float probablilty) {
        super(probablilty);
        this.baseUnit = baseUnit;
        this.leftContext = lc;
        key = baseUnit.baseID * 121 + leftContext.baseID;
    }


    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    @Override
    Unit baseUnit() {
        return baseUnit;
    }


    @Override
    final int type() {
        return 0;
    }

    /**
     * Returns the base unit for this hmm node
     *
     * @return the base unit
     */
    Unit getLeftContext() {
        return leftContext;
    }


    @Override
    Integer key() {
        return key;
    }


    @Override
    HMMPosition position() {
        return HMMPosition.END;
    }


    /**
     * Returns a string representation for this object
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "EndNode base:" + baseUnit + " lc " + leftContext + ' ' + key;
    }


    /** Freeze this node. Convert the set into an array to reduce memory overhead */
    @Override
    void freeze() {
        super.freeze();
    }
}



/**
 * Represents the vocabulary as a lex tree with nodes in the tree representing either words (WordNode) or units
 * (HMMNode). HMMNodes may be shared.
 */
class HMMTree {

    private final HMMPool hmmPool;
    protected InitialWordNode initialNode;
    protected Dictionary dictionary;

    private LanguageModel lm;
    private final boolean addFillerWords;
    private final Set<Unit> entryPoints = new LinkedHashSet<>();
    private Set<Unit> exitPoints = new LinkedHashSet<>();
    private Set<Word> allWords;
    private EntryPointTable entryPointTable;
    private boolean debug;
    private final float languageWeight;
    
    private final Map<Object, HMMNode[]> endNodeMap;
    private final Map<Pronunciation, WordNode> wordNodeMap;
    
    private WordNode sentenceEndWordNode;
    private final Logger logger;


    /**
     * Creates the HMMTree
     *
     * @param pool           the pool of HMMs and units
     * @param dictionary     the dictionary containing the pronunciations
     * @param lm             the source of the set of words to add to the lex tree
     * @param addFillerWords if <code>false</code> add filler words
     * @param languageWeight the languageWeight
     */
    HMMTree(HMMPool pool, Dictionary dictionary, LanguageModel lm,
            boolean addFillerWords, float languageWeight) {
        this.hmmPool = pool;
        this.dictionary = dictionary;
        this.lm = lm;
        this.endNodeMap = new HashMap<>();
        this.wordNodeMap = new HashMap<>();
        this.addFillerWords = addFillerWords;
        this.languageWeight = languageWeight;
        
        logger = Logger.getLogger(HMMTree.class.getSimpleName());
        compile();
    }


    /**
     * Given a base unit and a left context, return the set of entry points into the lex tree
     *
     * @param lc   the left context
     * @param base the center unit
     * @return the set of entry points
     */
    Node[] getEntryPoint(Unit lc, Unit base) {
        EntryPoint ep = entryPointTable.getEntryPoint(base);
        return ep.getEntryPointsFromLeftContext(lc).getSuccessors();
    }


    /**
     * Gets the  set of hmm nodes associated with the given end node
     *
     * @param endNode the end node
     * @return an array of associated hmm nodes
     */
    HMMNode[] getHMMNodes(EndNode endNode) {
        HMMNode[] results = endNodeMap.get(endNode.key());
        if (results == null) {
            // System.out.println("Filling cache for " + endNode.getKey()
            //        + " size " + endNodeMap.size());
            Map<HMM, HMMNode> resultMap = new HashMap<>(entryPoints.size());
            Unit baseUnit = endNode.baseUnit();
            Unit lc = endNode.getLeftContext();
            for (Unit rc : entryPoints) {
                HMM hmm = hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.END);
                HMMNode hmmNode = resultMap.get(hmm);
                if (hmmNode == null) {
                    hmmNode = new HMMNode(hmm, LogMath.LOG_ONE);
                    resultMap.put(hmm, hmmNode);
                }
                hmmNode.addRC(rc);
                for (Node node : endNode.getSuccessors()) {
                    WordNode wordNode = (WordNode)node;
                    hmmNode.addSuccessor(wordNode);
                }
            }

            // cache it
            results = resultMap.values().toArray(new HMMNode[resultMap.size()]);
            endNodeMap.put(endNode.key(), results);
        }

        // System.out.println("GHN: " + endNode + " " + results.length);
        return results;
    }


    /**
     * Returns the word node associated with the sentence end word
     *
     * @return the sentence end word node
     */
    WordNode getSentenceEndWordNode() {
        assert sentenceEndWordNode != null;
        return sentenceEndWordNode;
    }


//    private Object getKey(EndNode endNode) {
//        Unit base = endNode.getBaseUnit();
//        Unit lc = endNode.getLeftContext();
//        return null;
//    }


    /** Compiles the vocabulary into an HMM Tree */
    private void compile() {
        collectEntryAndExitUnits();
        entryPointTable = new EntryPointTable(entryPoints);
        addWords();
        entryPointTable.createEntryPointMaps();
        freeze();
    }


    /** Dumps the tree */
    void dumpTree() {
        System.out.println("Dumping Tree ...");
        Map<Node, Node> dupNode = new HashMap<>();
        dumpTree(0, initialNode, dupNode);
        System.out.println("... done Dumping Tree");
    }


    /**
     * Dumps the tree
     *
     * @param level   the level of the dump
     * @param node    the root of the tree to dump
     * @param dupNode map of visited nodes
     */
    private static void dumpTree(int level, Node node, Map<Node, Node> dupNode) {
        if (dupNode.get(node) == null) {
            dupNode.put(node, node);
            System.out.println(Utilities.pad(level) + node);
            if (!(node instanceof WordNode)) {
                for (Node nextNode : node.getSuccessors()) {
                    dumpTree(level + 1, nextNode, dupNode);
                }
            }
        }
    }


    /** Collects all of the entry and exit points for the vocabulary. */
    private void collectEntryAndExitUnits() {
        Collection<Word> words = getAllWords();
        for (Word word : words) {
            for (int j = 0; j < word.pronunciations.length; j++) {
                Pronunciation p = word.pronunciations[j];
                Unit first = p.units[0];
                Unit last = p.units[p.units.length - 1];
                entryPoints.add(first);
                exitPoints.add(last);
            }
        }

        if (debug) {
            System.out.println("Entry Points: " + entryPoints.size());
            System.out.println("Exit Points: " + exitPoints.size());
        }
    }


    /**
     * Called after the lex tree is built. Frees all temporary structures. After this is called, no more words can be
     * added to the lex tree.
     */
    private void freeze() {
        entryPointTable.freeze();
        dictionary = null;
        lm = null;
        exitPoints = null;
        allWords = null;
        wordNodeMap.clear();
        endNodeMap.clear();
    }


    /** Adds the given collection of words to the lex tree */
    private void addWords() {
        Set<Word> words = getAllWords();
        words.forEach(this::addWord);
    }


    /**
     * Adds a single word to the lex tree
     *
     * @param word the word to add
     */
    private void addWord(Word word) {
        float prob = getWordUnigramProbability(word);
        Pronunciation[] pronunciations = word.pronunciations;
        for (Pronunciation pronunciation : pronunciations) {
            addPronunciation(pronunciation, prob);
        }
    }


    /**
     * Adds the given pronunciation to the lex tree
     *
     * @param pronunciation the pronunciation
     * @param probability   the unigram probability
     */
    private void addPronunciation(Pronunciation pronunciation,
                                  float probability) {
        Unit baseUnit;
        Unit lc;
        Unit rc;
        Node curNode;
        WordNode wordNode;

        Unit[] units = pronunciation.units;
        baseUnit = units[0];
        EntryPoint ep = entryPointTable.getEntryPoint(baseUnit);

        ep.addProbability(probability);

        if (units.length > 1) {
            curNode = ep.baseNode;
            lc = baseUnit;
            for (int i = 1; i < units.length - 1; i++) {
                baseUnit = units[i];
                rc = units[i + 1];
                HMM hmm = hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.INTERNAL);
                if (hmm == null) {
                    logger.severe("Missing HMM for unit " + baseUnit.name + " with lc=" + lc.name + " rc=" + rc.name);
                } else {
                    curNode = curNode.addSuccessor(hmm, probability);
                }
                lc = baseUnit;          // next lc is this baseUnit
            }

            // now add the last unit as an end unit
            baseUnit = units[units.length - 1];
            EndNode endNode = new EndNode(baseUnit, lc, probability);
            curNode = curNode.addSuccessor(endNode, probability);
            wordNode = curNode.addSuccessor(pronunciation, probability, wordNodeMap);
            if (wordNode.getWord().isSentenceEndWord()) {
                sentenceEndWordNode = wordNode;
            }
        } else {
            ep.addSingleUnitWord(pronunciation);
        }
    }

    
    /**
     * Gets the unigram probability for the given word
     *
     * @param word the word
     * @return the unigram probability for the word.
     */
    private float getWordUnigramProbability(Word word) {
        float prob = LogMath.LOG_ONE;
        if (!word.filler) {
            Word[] wordArray = new Word[1];
            wordArray[0] = word;
            prob = lm.getProbability((new WordSequence(wordArray)));
            // System.out.println("gwup: " + word + " " + prob);
            prob *= languageWeight;
        }
        return prob;
    }


    /**
     * Returns the entire set of words, including filler words
     *
     * @return the set of all words (as Word objects)
     */
    private Set<Word> getAllWords() {
        if (allWords == null) {
            allWords = new HashSet<>();
            for (String spelling : lm.getVocabulary()) {
                Word word = dictionary.word(spelling);
                if (word != null) {
                    allWords.add(word);
                }
            }

            boolean addSilenceWord = true;
            if (addFillerWords) {
                allWords.addAll(Arrays.asList(dictionary.getFillerWords()));
            } else if (addSilenceWord) {
                allWords.add(dictionary.getSilenceWord());
            }
        }
        return allWords;
    }


    /**
     * Returns the initial node for this lex tree
     *
     * @return the initial lex node
     */
    InitialWordNode getInitialNode() {
        return initialNode;
    }


    /** The EntryPoint table is used to manage the set of entry points into the lex tree. */
    class EntryPointTable {

        private final Map<Unit, EntryPoint> entryPoints;


        /**
         * Create the entry point table give the set of all possible entry point units
         *
         * @param entryPointCollection the set of possible entry points
         */
        EntryPointTable(Collection<Unit> entryPointCollection) {
            entryPoints = new HashMap<>(entryPointCollection.size());
            for (Unit unit : entryPointCollection) {
                entryPoints.put(unit, new EntryPoint(unit));
            }
        }


        /**
         * Given a CI unit, return the EntryPoint object that manages the entry point for the unit
         *
         * @param baseUnit the unit of interest (A ci unit)
         * @return the object that manages the entry point for the unit
         */
        EntryPoint getEntryPoint(Unit baseUnit) {
            return entryPoints.get(baseUnit);
        }


        /** Creates the entry point maps for all entry points. */
        void createEntryPointMaps() {
            entryPoints.values().forEach((entryPoint) -> entryPoint.createEntryPointMap(HMMTree.this));
        }


        /** Freezes the entry point table */
        void freeze() {
            entryPoints.values().forEach(EntryPoint::freeze);
        }


        /** Dumps the entry point table */
        void dump() {
            entryPoints.values().forEach(EntryPoint::dump);
        }
    }


    /** Manages a single entry point. */
    static final class EntryPoint {


        final Unit baseUnit;
        public final Node baseNode;      // second units and beyond start here
        final Map<Unit, Node> unitToEntryPointMap;
        List<Pronunciation> singleUnitWords;
        int nodeCount;
        Set<Unit> rcSet;
        float totalProbability;


        /**
         * Creates an entry point for the given unit
         *
         * @param baseUnit the EntryPoint is created for this unit
         */
        EntryPoint(Unit baseUnit) {
            this.baseUnit = baseUnit;
            this.baseNode = new Node(LogMath.LOG_ZERO);
            this.unitToEntryPointMap = new HashMap<>();
            this.singleUnitWords = new FastList<>();
            this.totalProbability = LogMath.LOG_ZERO;
        }


        /**
         * Given a left context get a node that represents a single set of entry points into this unit
         *
         * @param leftContext the left context of interest
         * @return the node representing the entry point
         */
        Node getEntryPointsFromLeftContext(Unit leftContext) {
            return unitToEntryPointMap.get(leftContext);
        }


        /**
         * Accumulates the probability for this entry point
         *
         * @param probability a new  probability
         */
        void addProbability(float probability) {
            if (probability > totalProbability) {
                totalProbability = probability;
            }
        }


        /**
         * Returns the probability for all words reachable from this node
         *
         * @return the log probability
         */
        float getProbability() {
            return totalProbability;
        }


        /** Once we have built the full entry point we can eliminate some fields */
        void freeze() {
            unitToEntryPointMap.values().forEach(Node::freeze);
            singleUnitWords = null;
            rcSet = null;
        }


        /**
         * Adds a one-unit word to this entry point. Such single unit words need to be dealt with specially.
         *
         * @param p the pronunciation of the single unit word
         */
        void addSingleUnitWord(Pronunciation p) {
            singleUnitWords.add(p);
        }


        /**
         * Gets the set of possible right contexts that we can transition to from this entry point
         *
         * @return the set of possible transition points.
         */
        private Collection<Unit> getEntryPointRC() {
            if (rcSet == null) {
                Map<Object, Node> m = baseNode.getSuccessorMap(false);
                if (m!=null) {
                    rcSet = m.values().stream().map(
                            node -> ((UnitNode) node).baseUnit()
                    ).collect(Collectors.toSet());
                } else {
                    rcSet = Set.of();
                }
            }
            return rcSet;
        }


        /**
         * A version of createEntryPointMap that compresses common hmms across all entry points.
         * @param hmmTree
         */
        void createEntryPointMap(HMMTree hmmTree) {
            HashMap<HMM, Node> map = new HashMap<>();
            HashMap<HMM, HMMNode> singleUnitMap = new HashMap<>();

            for (Unit lc : hmmTree.exitPoints) {
                Node epNode = new Node(LogMath.LOG_ZERO);
                for (Unit rc : getEntryPointRC()) {
                    HMM hmm = hmmTree.hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.BEGIN);
                    connectEntryPointNode(map.compute(hmm, (h, e)->{
                        if (e == null)
                            return epNode.addSuccessor(hmm, totalProbability);
                        else {
                            epNode.putSuccessor(hmm, e);
                            return e;
                        }
                    }), rc);
                    nodeCount++;

                }
                connectSingleUnitWords(hmmTree, lc, epNode, singleUnitMap);
                unitToEntryPointMap.put(lc, epNode);
            }
        }


        /**
         * Connects the single unit words associated with this entry point.   The singleUnitWords list contains all
         * single unit pronunciations that have as their sole unit, the unit associated with this entry point. Entry
         * points for these words are added to the epNode for all possible left (exit) and right (entry) contexts.
         *
         * @param hmmTree
         * @param lc     the left context
         * @param epNode the entry point node
         */
        private void connectSingleUnitWords(HMMTree hmmTree, Unit lc, Node epNode, HashMap<HMM, HMMNode> map) {
            if (!singleUnitWords.isEmpty()) {

                for (Unit rc : hmmTree.entryPoints) {
                    HMM hmm = hmmTree.hmmPool.getHMM(baseUnit, lc, rc, HMMPosition.SINGLE);

                    HMMNode tailNode;
                    if (( tailNode = map.get(hmm)) == null) {
                        tailNode = (HMMNode)
                                epNode.addSuccessor(hmm, totalProbability);
                        map.put(hmm, tailNode);
                    } else {
                        epNode.putSuccessor(hmm, tailNode);
                    }
                    WordNode wordNode;
                    tailNode.addRC(rc);
                    nodeCount++;

                    for (Pronunciation p : singleUnitWords) {
                        if (p.getWord() == hmmTree.dictionary.getSentenceStartWord()) {
                            hmmTree.initialNode = new InitialWordNode(p, tailNode);
                        } else {
                            float prob = hmmTree.getWordUnigramProbability(p.getWord());
                            wordNode = tailNode.addSuccessor(p, prob, hmmTree.wordNodeMap);
                            if (p.getWord() ==
                                hmmTree.dictionary.getSentenceEndWord()) {
                                hmmTree.sentenceEndWordNode = wordNode;
                            }
                        }
                        nodeCount++;
                    }
                }
            }
        }


        /**
         * Connect the entry points that match the given rc to the given epNode
         *
         * @param epNode add matching successors here
         * @param rc     the next unit
         */
        private void connectEntryPointNode(Node epNode, Unit rc) {
            for (Node node : baseNode.getSuccessors()) {
                UnitNode successor = (UnitNode) node;
                if (successor.baseUnit() == rc) {
                    epNode.addSuccessor(successor);
                }
            }
        }


        /** Dumps the entry point */
        void dump() {
            System.out.println("EntryPoint " + baseUnit + " RC Followers: "
                    + getEntryPointRC().size());
            int count = 0;
            Collection<Unit> rcs = getEntryPointRC();
            System.out.print("    ");
            for (Unit rc : rcs) {
                System.out.print(Utilities.pad(rc.name, 4));
                if (count++ >= 12) {
                    count = 0;
                    System.out.println();
                    System.out.print("    ");
                }
            }
            System.out.println();
        }
    }
}

