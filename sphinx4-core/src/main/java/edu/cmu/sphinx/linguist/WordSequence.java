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

package edu.cmu.sphinx.linguist;

import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

/**
 * This class can be used to keep track of a word sequence. This class is an
 * immutable class. It can never be modified once it is created (except,
 * perhaps for transient, cached things such as a precalculated hashcode).
 */

public final class WordSequence implements Comparable<WordSequence> {

    /**
     * Comparator that compares two sequences by their oldest part.
     */
    public final static Comparator<WordSequence> OLDEST_COMPARATOR =
            (o1, o2) -> o1.getOldest().compareTo(o2.getOldest());

    /** an empty word sequence, that is, it has no words. */
    public final static WordSequence EMPTY = new WordSequence(0);

    public static WordSequence asWordSequence(final Dictionary dictionary,
            String... words) {
        Word[] dictWords = new Word[words.length];
        for (int i = 0; i < words.length; i++) {
            dictWords[i] = dictionary.getWord(words[i]);
        }
        return new WordSequence(dictWords);
    }

    private final Word[] words;
    private transient int hashCode;

    /**
     * Constructs a word sequence with the given depth.
     *
     * @param size the maximum depth of the word history
     */
    private WordSequence(int size) {

        words = new Word[size];
    }

    public static int hash(Word[] words) {
        int code = 123;
        int i = 0;
        for (Word word : words) {
            code += word.hashCode() * (2 * i + 1);
            i++;
        }
        return code;
    }

    /**
     * Constructs a word sequence with the given word IDs
     *
     * @param words the word IDs of the word sequence
     */
    public WordSequence(Word... words) {
        this(Arrays.asList(words));
    }

    /**
     * Constructs a word sequence from the list of words
     *
     * @param list the list of words
     */
    public WordSequence(List<Word> list) {
        this.words = list.toArray(new Word[list.size()]);
        check();
    }

    private void check() {
        this.hashCode = hash(words);
//        for (Word word : words)
//            if (word == null)
//                throw new Error("WordSequence should not have null Words.");
    }

    /**
     * Returns a new word sequence with the given word added to the sequence
     *
     * @param word the word to add to the sequence
     * @param maxSize the maximum size of the generated sequence
     * @return a new word sequence with the word added (but trimmed to
     *         maxSize).
     */
    public WordSequence addWord(Word word, int maxSize) {
        if (maxSize <= 0) {
            return EMPTY;
        }
        int nextSize = ((size() + 1) > maxSize) ? maxSize : (size() + 1);
        WordSequence next = new WordSequence(nextSize);
        int nextIndex = nextSize - 1;
        int thisIndex = size() - 1;
        next.words[nextIndex--] = word;

        while (nextIndex >= 0 && thisIndex >= 0) {
            next.words[nextIndex--] = this.words[thisIndex--];
        }
        next.check();

        return next;
    }

    /**
     * Returns the oldest words in the sequence (the newest word is omitted)
     *
     * @return the oldest words in the sequence, with the newest word omitted
     */
    public WordSequence getOldest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(words.length - 1);
            System.arraycopy(this.words, 0, next.words, 0, next.words.length);
            next.check();
        }
        return next;
    }

    /**
     * Returns the newest words in the sequence (the old word is omitted)
     *
     * @return the newest words in the sequence with the oldest word omitted
     */
    public WordSequence getNewest() {
        WordSequence next = EMPTY;

        if (size() >= 1) {
            next = new WordSequence(words.length - 1);
            System.arraycopy(this.words, 1, next.words, 0, next.words.length);
            next.check();
        }
        return next;
    }

    /**
     * Returns a word sequence that is no longer than the given size, that is
     * filled in with the newest words from this sequence
     *
     * @param maxSize the maximum size of the sequence
     * @return a new word sequence, trimmed to maxSize.
     */
    public WordSequence trim(int maxSize) {
        if (maxSize <= 0 || size() == 0) {
            return EMPTY;
        } else if (maxSize == size()) {
            return this;
        } else {
            if (maxSize > size()) {
                maxSize = size();
            }
            WordSequence next = new WordSequence(maxSize);
            int thisIndex = words.length - 1;
            int nextIndex = next.words.length - 1;

            for (int i = 0; i < maxSize; i++) {
                next.words[nextIndex--] = this.words[thisIndex--];
            }
            next.check();
            return next;
        }
    }

    /**
     * Returns the n-th word in this sequence
     *
     * @param n which word to return
     * @return the n-th word in this sequence
     */
    public Word getWord(int n) {
        assert n < words.length;
        return words[n];
    }

    /**
     * Returns the number of words in this sequence
     *
     * @return the number of words
     */
    public final int size() {
        return words.length;
    }

    /**
     * Returns a string representation of this word sequence. The format is:
     * [ID_0][ID_1][ID_2].
     *
     * @return the string
     */
    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        for (Word word : words)
            sb.append('[').append(word).append(']');
        return sb.toString();
    }

    /**
     * Calculates the hashcode for this object
     *
     * @return a hashcode for this object
     */
    @Override
    public final int hashCode() {
        return hashCode;
    }

    /**
     * compares the given object to see if it is identical to this WordSequence
     *
     * @param object the object to compare this to
     * @return true if the given object is equal to this object
     */
    @Override
    public final boolean equals(Object object) {
        if (this == object)
            return true;
        if (!(object instanceof WordSequence))
            return false;

        return Arrays.equals(words, ((WordSequence) object).words);
    }

    /**
     * @param startIndex start index
     * @param stopIndex stop index
     * @return a subsequence with both <code>startIndex</code> and
     *         <code>stopIndex</code> exclusive.
     */
    public WordSequence getSubSequence(int startIndex, int stopIndex) {
        List<Word> subseqWords = new ArrayList<>();

        for (int i = startIndex; i < stopIndex; i++) {
            subseqWords.add(getWord(i));
        }

        return new WordSequence(subseqWords);
    }

    /**
     * @return the words of the <code>WordSequence</code>.
     */
    public Word[] getWords() {
        return getSubSequence(0, size()).words; // create a copy to keep the
                                                // class immutable
    }

    public int compareTo(WordSequence other) {
        Word[] aa = this.words;
        Word[] bb = other.words;
        int n = min(aa.length, bb.length);
        for (int i = 0; i < n; ++i) {
            Word a = aa[i];
            Word b = bb[i];
            if (!a.equals(b)) {
                return a.compareTo(b);
            }
        }

        return aa.length - bb.length;
    }
}
