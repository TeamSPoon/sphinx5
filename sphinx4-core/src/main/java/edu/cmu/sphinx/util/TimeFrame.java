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

package edu.cmu.sphinx.util;


public class TimeFrame {

    public static final TimeFrame ZERO = new TimeFrame(0, 0);
    public static final TimeFrame INFINITE = new TimeFrame(0, Long.MAX_VALUE);

    public final long start;
    public final long end;

    private TimeFrame(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public static TimeFrame time(long start, long end) {
        if (start == 0 && end == 0)
            return ZERO;
        else
            return new TimeFrame(start, end);
    }

    public long length() {
        return end - start;
    }

    @Override
    public String toString() {
        return String.format("%d:%d", start, end);
    }
}
