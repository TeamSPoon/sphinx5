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

package edu.cmu.sphinx.linguist.acoustic;

import java.io.Serializable;

/** Represents  the context for a unit */
@SuppressWarnings("serial")
abstract public class Context implements Serializable {

    /** Represents an empty context */
    public final static Context EMPTY_CONTEXT = new Context() {

        @Override
        boolean isPartialMatch(Context context) {
            return true;
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public boolean equals(Object o) {
            return this == o; //super.equals(o);
        }

        @Override
        public int hashCode() {
            return 0;
        }
    };

    /**
     * Checks to see if there is a partial match with the given context. For a simple context such as this we always
     * match.
     *
     * @param context the context to check
     * @return true if there is a partial match
     */
    abstract boolean isPartialMatch(Context context);

}
