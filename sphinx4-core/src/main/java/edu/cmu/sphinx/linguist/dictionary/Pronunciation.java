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

import edu.cmu.sphinx.linguist.acoustic.Unit;

import java.util.List;


/** Provides pronunciation information for a word. */
public class Pronunciation {

    public static final Pronunciation UNKNOWN = new Pronunciation(Unit.EMPTY_ARRAY, null, 1.0f);

    /**
     * Retrieves the word that this Pronunciation object represents.
     *
     * @return the word
     */
    private Word word;

    /**
     * Retrieves the units for this pronunciation
     *
     * @return the units for this pronunciation
     */
    public final Unit[] units;

    /**
     * Retrieves the tag associated with the pronunciation or null if there is no tag associated with this
     * pronunciation. Pronunciations can optionally be tagged to allow applications to distinguish between different
     * pronunciations.
     *
     * @return the tag or null if no tag is available.
     */
    public final String tag;

    /**
     * Retrieves the probability for the pronunciation. A word may have multiple pronunciations that are not all equally
     * probable. All probabilities for particular word sum to 1.0.
     *
     * @return the probability of this pronunciation as a value between 0 and 1.0.
     *         <p>
     *         TODO: FIX Note that probabilities are currently maintained in the linear domain (unlike just about
     *         everything else)
     */
    public final float probability;


    /**
     * Creates a pronunciation
     *
     * @param units              represents the pronunciation
     * @param tag                a grammar specific tag
     * @param probability        the probability of this pronunciation occurring
     */
    public Pronunciation(Unit[] units,
                  String tag,
                  float probability) {
        this.units = units;
        this.tag = tag;
        this.probability = probability;
    }

    /**
     * Creates a pronunciation
     *
     * @param units              represents the pronunciation
     * @param tag                a grammar specific tag
     * @param probability        the probability of this pronunciation occurring
     */
    protected Pronunciation(List<Unit> units,
                  String tag,
                  float probability) {
        Unit[] unitsArray = units.toArray(new Unit[units.size()]);
        this.units = unitsArray;
        this.tag = tag;
        this.probability = probability;
    }

    /**
     * Creates a pronunciation with defaults
     *
     * @param units              represents the pronunciation
     */
    protected Pronunciation(List<Unit> units) {
        this(units, null, 1.0f);
    }


    /**
     * Sets the word this pronunciation represents.
     *
     * @param word the Word this Pronunciation represents
     */
    public void setWord(Word word) {
        if (this.word == null) {
            this.word = word;
        } else {
            throw new Error("Word of Pronunciation cannot be set twice.");
        }
    }



    public Word getWord() {
        return word;
    }


    /** Dumps a pronunciation */
    public void dump() {
        System.out.println(toString());
    }


    /**
     * Returns a string representation of this Pronunication.
     *
     * @return a string of this Pronunciation
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder().append(word).append('(');
        for (Unit unit : units) {
            result.append(unit).append(' ');
        }
        result.append(')');
        return result.toString();
    }


    /**
     * Returns a detailed string representation of this Pronunciation.
     *
     * @return a string of this Pronunciation
     */
    public String toDetailedString() {
        StringBuilder result = new StringBuilder().append(word).append(' ');
        for (Unit unit : units) {
            result.append(unit).append(' ');
        }
        result.append("\n   class: ").append(" tag: ").append(tag).append(" prob: ").append(probability);

        return result.toString();
    }
}

