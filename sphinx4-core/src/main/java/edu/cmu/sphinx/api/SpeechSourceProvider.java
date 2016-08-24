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


public class SpeechSourceProvider {

    static Microphone getMicrophone() {
        return getMicrophone(16000);
    }

    static Microphone getMicrophone(int sampleRate) {
        return new Microphone(sampleRate, 16, true, false);
    }
}
