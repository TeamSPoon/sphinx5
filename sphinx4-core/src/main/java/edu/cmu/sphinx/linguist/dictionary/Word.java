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

package edu.cmu.sphinx.linguist.dictionary;

/** Represents a word, its spelling and its pronunciation. */
public class Word implements Comparable<Word> {

    /** The Word representing the unknown word. */
    public static final Word UNKNOWN;

    static {
        Pronunciation[] pros = {Pronunciation.UNKNOWN};
        UNKNOWN = new Word("<unk>", pros, false);
        Pronunciation.UNKNOWN.setWord(UNKNOWN);
    }

    public final String spelling; // the spelling of the word
    public final Pronunciation[] pronunciations; // pronunciations of this
                                                  // word
    public final boolean filler;

    /**
     * Creates a Word
     *
     * @param spelling the spelling of this word
     * @param pronunciations the pronunciations of this word
     * @param isFiller true if the word is a filler word
     */
    public Word(String spelling, Pronunciation[] pronunciations,
            boolean isFiller) {
        this.spelling = spelling;
        this.pronunciations = pronunciations;
        this.filler = isFiller;
    }

    /**
     * Returns true if this word is an end of sentence word
     *
     * @return true if the word matches Dictionary.SENTENCE_END_SPELLING
     */
    public boolean isSentenceEndWord() {
        return Dictionary.SENTENCE_END_SPELLING.equals(this.spelling);
    }

    /**
     * Returns true if this word is a start of sentence word
     *
     * @return true if the word matches Dictionary.SENTENCE_START_SPELLING
     */
    public boolean isSentenceStartWord() {
        return Dictionary.SENTENCE_START_SPELLING.equals(this.spelling);
    }

    /**
     * Get the highest probability pronunciation for a word
     *
     * @return the highest probability pronunciation
     */
    public Pronunciation getMostLikelyPronunciation() {
        float bestScore = Float.NEGATIVE_INFINITY;
        Pronunciation best = null;
        for (Pronunciation pronunciation : pronunciations) {
            if (pronunciation.probability > bestScore) {
                bestScore = pronunciation.probability;
                best = pronunciation;
            }
        }
        return best;
    }

    @Override
    public int hashCode() {
        return spelling.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj instanceof Word && spelling.equals(((Word) obj).spelling);
    }

    /**
     * Returns a string representation of this word, which is the spelling
     *
     * @return the spelling of this word
     */
    @Override
    public String toString() {
        return spelling;
    }

    public int compareTo(Word other) {
        return spelling.compareTo(other.spelling);
    }
}
