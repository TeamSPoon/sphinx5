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
package edu.cmu.sphinx.linguist.language.ngram;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.ConfigurationManagerUtils;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import static java.lang.Math.max;


/**
 * An ASCII ARPA language model loader. This loader makes no attempt to
 * optimize storage, so it can only load very small language models
 * <p>
 * Note that all probabilities in the grammar are stored in LogMath log base
 * format. Language Probabilities in the language model file are stored in log
 * 10 base.
 */

public class SimpleNGramModel implements LanguageModel {

    // ----------------------------
    // Configuration data
    // ----------------------------
    private String name;

    private URL urlLocation;
    private float unigramWeight;
    private Dictionary dictionary;
    private int desiredMaxDepth;
    private int maxNGram;


    /*  0: float logProbability;
        1: float logBackoff; */
    private Map<WordSequence, float[]> map;

    private Set<String> vocabulary;
    protected int lineNumber;
    protected BufferedReader reader;
    protected String fileName;
    private boolean allocated;
    private LinkedList<WordSequence> tokens;

    public SimpleNGramModel(String location, Dictionary dictionary,
            float unigramWeight, int desiredMaxDepth)
            throws MalformedURLException {
        this(ConfigurationManagerUtils.resourceToURL(location), dictionary,
             unigramWeight, desiredMaxDepth);
    }

    public SimpleNGramModel(URL urlLocation, Dictionary dictionary,
            float unigramWeight, int desiredMaxDepth)
    {
        this.urlLocation = urlLocation;
        this.unigramWeight = unigramWeight;
        this.desiredMaxDepth = desiredMaxDepth;
        this.dictionary = dictionary;
        this.map = new HashMap<>();
        this.vocabulary = new HashSet<>();
        this.tokens = new LinkedList<>();
    }

    public SimpleNGramModel() {

    }

    /*
     * (non-Javadoc)
     * @see
     * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.
     * util.props.PropertySheet)
     */
    public void newProperties(PropertySheet ps) throws PropertyException {

        if (allocated) {
            throw new RuntimeException("Can't change properties after allocation");
        }

        urlLocation = ConfigurationManagerUtils.getResource(PROP_LOCATION, ps);
        unigramWeight = ps.getFloat(PROP_UNIGRAM_WEIGHT);
        desiredMaxDepth = ps.getInt(PROP_MAX_DEPTH);
        dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY);
        map = new HashMap<>();
        vocabulary = new HashSet<>();
        tokens = new LinkedList<>();
    }

    /*
     * (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.language.ngram.LanguageModel#allocate()
     */
    public void allocate() throws IOException {
        allocated = true;
        load(urlLocation, unigramWeight, dictionary);
        if (desiredMaxDepth > 0) {
            if (desiredMaxDepth < maxNGram) {
                maxNGram = desiredMaxDepth;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.language.ngram.LanguageModel#deallocate()
     */
    public void deallocate() {
        allocated = false;
    }

    /*
     * (non-Javadoc)
     * @see edu.cmu.sphinx.util.props.Configurable#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the ngram probability of the word sequence represented by the word
     * list
     *
     * @param wordSequence the word sequence
     * @return the probability of the word sequence. Probability is in logMath
     *         log base
     */
    public float getProbability(WordSequence wordSequence) {
        float logProbability = 0.0f;
        float[] prob = getProb(wordSequence);
        if (prob == null) {
            if (wordSequence.size() > 1) {
                logProbability = getBackoff(wordSequence.getOldest())
                                 + getProbability(wordSequence.getNewest());
            } else { // if the single word is not in the model at all
                // then its zero likelihood that we'll use it
                logProbability = LogMath.LOG_ZERO;
            }
        } else {
            logProbability = prob[0];
        }
        // System.out.println("Search: " + wordSequence + " : "
        // + logProbability + " "
        // + LogMath.logToLinear(logProbability));
        return logProbability;
    }

    /**
     * Gets the smear term for the given wordSequence
     *
     * @param wordSequence the word sequence
     * @return the smear term associated with this word sequence
     */
    public float getSmear(WordSequence wordSequence) {
        return 0.0f; // TODO not implemented
    }

    /**
     * Returns the backoff probability for the give sequence of words
     *
     * @param wordSequence the sequence of words
     * @return the backoff probability in LogMath log base
     */
    public float getBackoff(WordSequence wordSequence) {
        float logBackoff = 0.0f; // log of 1.0
        float[] prob = getProb(wordSequence);
        if (prob != null) {
            logBackoff = prob[1];
        }
        return logBackoff;
    }

    /**
     * Returns the maximum depth of the language model
     *
     * @return the maximum depth of the language model
     */
    public int getMaxDepth() {
        return maxNGram;
    }

    /**
     * Returns the set of words in the language model. The set is unmodifiable.
     *
     * @return the unmodifiable set of words
     */
    public Set<String> getVocabulary() {
        return Collections.unmodifiableSet(vocabulary);
    }

    @Override
    public void onUtteranceEnd() {
        //TODO not implemented
    }

    /**
     * Gets the probability entry for the given word sequence or null if there
     * is no entry
     *
     * @param wordSequence a word sequence
     * @return the probability entry for the wordlist or null
     */
    private float[] getProb(WordSequence wordSequence) {
        return map.get(wordSequence);
    }

    /**
     * Converts a wordList to a string
     *
     * @param wordList the wordList
     * @return the string
     */
    @SuppressWarnings("unused")
    private static String listToString(List<Word> wordList) {
        StringBuilder sb = new StringBuilder();
        for (Word word : wordList)
            sb.append(word).append(' ');
        return sb.toString();
    }

    /** Dumps the language model */
    public void dump() {
        for (Map.Entry<WordSequence, float[]> entry : map.entrySet())
            System.out.println(entry.getKey() + " " + entry.getValue());
    }

    /**
     * Retrieves a string representation of the wordlist, suitable for map
     * access
     *
     * @param wordList the list of words
     * @return a string representation of the word list
     */
    @SuppressWarnings("unused")
    private static String getRepresentation(List<String> wordList) {
        if (wordList.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (String word : wordList)
            sb.append(word).append('+');
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Loads the language model from the given location.
     *
     * @param location the URL location of the model
     * @param unigramWeight the unigram weight
     * @throws IOException if an error occurs while loading
     */
    private void load(URL location, float unigramWeight,
                      Dictionary dictionary) throws IOException {
        String line;
        float logUnigramWeight = LogMath.linearToLog(unigramWeight);
        float inverseLogUnigramWeight = LogMath
                .linearToLog(1.0 - unigramWeight);

        open(location);
        // look for beginning of data
        readUntil("\\data\\");
        // look for ngram statements


        List<Integer> ngramList = new ArrayList<>(64*1024);

        while ((line = readLine()) != null) {
            if (line.startsWith("ngram")) {
                StringTokenizer st = new StringTokenizer(line, " \t\n\r\f=");
                if (st.countTokens() != 3) {
                    corrupt("corrupt ngram field " + line + ' '
                            + st.countTokens());
                }
                st.nextToken();
                int index = Integer.parseInt(st.nextToken());
                int count = Integer.parseInt(st.nextToken());
                ngramList.add(index - 1, count);
                maxNGram = max(index, maxNGram);
            } else if (line.equals("\\1-grams:")) {
                break;
            }
        }
        int numUnigrams = ngramList.get(0) - 1;
        // -log(x) = log(1/x)
        float logUniformProbability = -LogMath.linearToLog(numUnigrams);
        for (int index = 0; index < ngramList.size(); index++) {
            int ngram = index + 1;
            int ngramCount = ngramList.get(index);
            for (int i = 0; i < ngramCount; i++) {
                StringTokenizer tok = new StringTokenizer(readLine());
                int tokenCount = tok.countTokens();
                if (tokenCount != ngram + 1 && tokenCount != ngram + 2) {
                    corrupt("Bad format");
                }
                float log10Prob = Float.parseFloat(tok.nextToken());
                float log10Backoff = 0.0f;
                // construct the WordSequence for this N-Gram
                List<Word> wordList = new ArrayList<>(maxNGram);
                for (int j = 0; j < ngram; j++) {
                    String word = tok.nextToken();
                    vocabulary.add(word);
                    Word wordObject = dictionary.word(word);
                    if (wordObject == null) {
                        wordObject = Word.UNKNOWN;
                    }
                    wordList.add(wordObject);
                }
                WordSequence wordSequence = new WordSequence(wordList);
                if (tok.hasMoreTokens()) {
                    log10Backoff = Float.parseFloat(tok.nextToken());
                }
                float logProb = LogMath.log10ToLog(log10Prob);
                float logBackoff = LogMath.log10ToLog(log10Backoff);
                // Apply unigram weights if this is a unigram probability
                if (ngram == 1) {
                    float p1 = logProb + logUnigramWeight;
                    float p2 = logUniformProbability + inverseLogUnigramWeight;
                    logProb = LogMath.addAsLinear(p1, p2);
                    // System.out
                    // .println("p1 " + p1 + " p2 " + p2 + " luw "
                    // + logUnigramWeight + " iluw "
                    // + inverseLogUnigramWeight + " lup "
                    // + logUniformProbability + " logprog "
                    // + logProb);
                }
                put(wordSequence, logProb, logBackoff);
            }
            if (index < ngramList.size() - 1) {
                String next = "\\" + (ngram + 1) + "-grams:";
                readUntil(next);
            }
        }
        readUntil("\\end\\");
        close();
    }

    /**
     * Puts the probability into the map
     *
     * @param wordSequence the tag for the prob.
     * @param logProb the probability in log math base
     * @param logBackoff the backoff probability in log math base
     */
    private void put(WordSequence wordSequence, float...logProbAndBackoff) {
        // System.out.println("Putting " + wordSequence + " p " + logProb
        // + " b " + logBackoff);
        map.put(wordSequence, logProbAndBackoff);
        tokens.add(wordSequence);
    }

    /**
     * Returns a list of all the word sequences in the language model This
     * method is used to create Finite State Transducers of the language model.
     *
     * @return List containing all the word sequences
     */
    public LinkedList<WordSequence> getNGrams() {
        return tokens;
    }

    /**
     * Reads the next line from the LM file. Keeps track of line number.
     *
     * @throws IOException if an error occurs while reading the input or an EOF
     *         is encountered.
     */
    private String readLine() throws IOException {
        String line;
        lineNumber++;
        line = reader.readLine();
        if (line == null) {
            corrupt("Premature EOF");
        }
        return line.trim();
    }

    /**
     * Opens the language model at the given location
     *
     * @param location the path to the language model
     * @throws IOException if an error occurs while opening the file
     */
    private void open(URL location) throws
            IOException {
        lineNumber = 0;
        fileName = location.toString();
        reader = new BufferedReader
            (new InputStreamReader(location.openStream()));
    }

    /**
     * Reads from the input stream until the input matches the given string
     *
     * @param match the string to match on
     * @throws IOException if an error occurs while reading the input or an EOF
     *         is encountered before finding the match
     */
    private void readUntil(String match) throws IOException {
        try {
            while (!readLine().equals(match)) {
            }
        } catch (IOException ioe) {
            corrupt("Premature EOF while waiting for " + match);
        }
    }

    /**
     * Closes the language model file
     *
     * @throws IOException if an error occurs
     */
    private void close() throws IOException {
        reader.close();
        reader = null;
    }

    /**
     * Generates a 'corrupt' IO exception
     *
     * @throws IOException with the given string
     */
    private void corrupt(String why) throws IOException {
        throw new IOException("Corrupt Language Model " + fileName
                              + " at line " + lineNumber + ':' + why);
    }
}


///** Represents a probability and a backoff probability */
//
//class Probability {
//
//    final float logProbability;
//    final float logBackoff;
//
//    /**
//     * Constructs a probability
//     *
//     * @param logProbability the probability
//     * @param logBackoff the backoff probability
//     */
//    Probability(float logProbability, float logBackoff) {
//        this.logProbability = logProbability;
//        this.logBackoff = logBackoff;
//    }
//
//    /**
//     * Returns a string representation of this object
//     *
//     * @return the string form of this object
//     */
//    @Override
//    public String toString() {
//        return "Prob: " + logProbability + ' ' + logBackoff;
//    }
//}
