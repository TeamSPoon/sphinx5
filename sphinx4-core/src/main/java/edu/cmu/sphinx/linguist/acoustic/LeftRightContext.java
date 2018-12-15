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

import java.util.Arrays;

/** Represents  the context for a unit */
@SuppressWarnings("serial")
public class LeftRightContext extends Context {

    //String stringRepresentation;
    public final Unit[] left;
    public final Unit[] right;
    private final String id;

    /**
     * Creates a LeftRightContext
     *
     * @param left  the left context or null if no left context
     * @param right the right context or null if no right context
     */
    private LeftRightContext(Unit[] left, Unit[] right) {
        this.left = left;
        this.right = right;
        if (Arrays.equals(left,right)) {
            this.id = getContextName(left) + ':'; //the ':' indicates a double repeat
        } else {
            this.id = getContextName(left) + ',' + getContextName(right);
        }
    }

    /** Provides a string representation of a context */
    @Override
    public String toString() {
        return id;
    }

    /**
     * Determines if an object is equal to this context
     *
     * @param o the object to check
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof LeftRightContext) {
            LeftRightContext otherContext = (LeftRightContext) o;
            return id.equals(otherContext.id);
        } else {
            return false;
        }
    }


    /**
     * calculates a hashCode for this context. Since we defined an equals for context, we must define a hashCode as
     * well
     *
     * @return the hashcode for this object
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Factory method for creating a left/right context
     *
     * @param leftContext  the left context or null if no left context
     * @param rightContext the right context or null if no right context
     * @return a left right context
     */
    public static LeftRightContext get(Unit[] leftContext, Unit[] rightContext) { 
        return new LeftRightContext(leftContext, rightContext);
    }

    /**
     * Gets the context name for a particular array of units
     *
     * @param context the context
     * @return the context name
     */
    public static String getContextName(Unit[] context) {
        if (context == null)
            return "*";
        if (context.length == 0)
            return "_";
            //return "(empty)";
        StringBuilder sb = new StringBuilder();
        for (Unit unit : context) {
            sb.append(unit == null ? null : unit.name).append('.');
        }
        return sb.substring(0, sb.length()-1); // remove last period
    }

    /**
     * Checks to see if there is a partial match with the given context. If both contexts are LeftRightContexts then  a
     * left or right context that is null is considered a wild card and matches anything, othewise the contexts must
     * match exactly. Anything matches the Context.EMPTY_CONTEXT
     *
     * @param context the context to check
     * @return true if there is a partial match
     */
    @Override
    public boolean isPartialMatch(Context context) {
        if (context instanceof LeftRightContext) {
            LeftRightContext lrContext = (LeftRightContext)context;
            Unit[] lc = lrContext.left;
            Unit[] rc = lrContext.right;

            return (lc == null || left == null || Unit.isContextMatch(lc, left))
                && (rc == null || right == null || Unit.isContextMatch(rc, right));
        }
        return context == Context.EMPTY_CONTEXT && left == null && right == null;
    }

}
