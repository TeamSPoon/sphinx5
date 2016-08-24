/*
 * Copyright 2013 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.api;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * High-level wrapper for {@link Result} instance.
 */
public final class SpeechResult {

    private final Result result;
    private final Lattice lattice;

    /**
     * Constructs recognition result based on {@link Result} object.
     *
     * @param result recognition result returned by {@link Recognizer}
     */
    public SpeechResult(Result result) {
        this.result = result;
        if (result.toCreateLattice()) {
            lattice = new Lattice(result);
            new LatticeOptimizer(lattice).optimize();
            lattice.computeNodePosteriors(1.0f);
        } else
            lattice = null;
    }

    /** returns min and max start times as a 2-element long[].  if the result is empty, it returns null */
    public long[] time() {
        List<WordResult> w = result.getTimedBestResult(false);
        if (!w.isEmpty()) {
            long start = w.get(0).getTimeFrame().getStart();
            long end = w.get(w.size() - 1).getTimeFrame().getEnd();
            return new long[] { start, end };
        }
        return null;
    }

    /**
     * Returns {@link List} of words of the recognition result.
     * Within the list words are ordered by time frame.
     *
     * @return words that form the result
     */
    public List<WordResult> getWords() {
        return lattice != null ? lattice.getWordResultPath() : result.getTimedBestResult(false);
    }

    /**
     * @return string representation of the result.
     */
    public String getHypothesis() {
	return result.getBestResultNoFiller();
    }

    /**
     * Return N best hypothesis.
     *
     * @param  n number of hypothesis to return
     * @return   {@link Collection} of several best hypothesis
     */
    public Collection<String> getNbest(int n) {
        if (lattice == null)
            return Collections.emptySet();
        return new Nbest(lattice).getNbest(n);
    }

    /**
     * Returns lattice for the recognition result.
     *
     * @return lattice object
     */
    public Lattice getLattice() {
        return lattice;
    }
    
    /**
     * Return Result object of current SpeechResult
     * 
     * @return Result object stored in this.result
     */
    public Result getResult() {
    	return result;
    }
}
