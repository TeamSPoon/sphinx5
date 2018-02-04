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

package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.frontend.Data;

/** Represents an entity that can be scored against a data */
public interface Scoreable extends Data, Comparable<Scoreable> {

    /**
     * A {@code Scoreable} comparator that is used to order scoreables according to their score,
     * in descending order.
     *
     * <p>Note: since a higher score results in a lower natural order,
     * statements such as {@code Collections.min(list, Scoreable.COMPARATOR)}
     * actually return the Scoreable with the <b>highest</b> score,
     * in contrast to the natural meaning of the word "min".   
     */
    @Override
    default int compareTo(Scoreable o) {
        if (this == o) return 0;
        float s1 = score();
        float s2 = o.score();
        if (s1 > s2) return -1;
        else if (s1 < s2) return 1;
        else {
            //same score but ensure that different instances are not destructively merged
            return Integer.compare(
                    hashCode(), o.hashCode()
                    //System.identityHashCode(this), System.identityHashCode(o)
            );
        }
    }

    /**
     * Calculates a score against the given data. The score can be retrieved with get score
     *
     * @param data     the data to be scored
     * @return the score for the data
     */
    float calculateScore(Data data);


    /**
     * Retrieves a previously calculated (and possibly normalized) score
     *
     * @return the score
     */
    float score();


    /**
     * Normalizes a previously calculated score
     *
     * @param maxScore maximum score to use for norm
     * @return the normalized score
     */
    float normalizeScore(float maxScore);

}
