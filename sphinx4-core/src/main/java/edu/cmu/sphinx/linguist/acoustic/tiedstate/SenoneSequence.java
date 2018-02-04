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

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/** Contains an ordered list of senones. */
@SuppressWarnings("serial")
public class SenoneSequence implements Serializable {

    /**
     * Returns the ordered set of senones for this sequence
     *
     * @return the ordered set of senones for this sequence
     */
    public final Senone[] senones;
    private final int hash;


    /**
     * a factory method that creates a SeononeSequence from a list of senones.
     *
     * @param senoneList the list of senones
     * @return a composite senone
     */
    public static SenoneSequence create(List<CompositeSenone> senoneList) {
        return new SenoneSequence(senoneList.toArray(new Senone[senoneList.size()]));
    }


    /**
     * Constructs a senone sequence
     *
     * @param sequence the ordered set of senones for this sequence
     */
    public SenoneSequence(Senone[] sequence) {
        this.senones = sequence;
        int hashCode = 31;
        for (Senone senone : sequence) {
            hashCode = hashCode * 91 + senone.hashCode();
        }
        this.hash = hashCode;
    }


    /**
     * Returns the hashCode for this object
     *
     * @return the object hashcode
     */
    @Override
    public int hashCode() {
        return hash;
    }


    /**
     * Returns true if the objects are equal
     *
     * @return true  if the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            if (o instanceof SenoneSequence) {
                SenoneSequence ss = (SenoneSequence) o;
                return hash == ss.hash && Arrays.equals(senones, ss.senones);
            }
            return false;
        }
    }


    /**
     * Dumps this senone sequence
     *
     * @param msg a string annotation
     */
    public void dump(String msg) {
        System.out.println(" SenoneSequence " + msg + ':');
        for (Senone senone : senones) {
            senone.dump("  seq:");
        }
    }
}
