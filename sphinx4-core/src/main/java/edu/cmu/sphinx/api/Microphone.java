/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.api;

import javax.sound.sampled.*;
import java.io.InputStream;

/**
 * InputStream adapter
 */
public class Microphone {

    private final TargetDataLine line;
    private final InputStream inputStream;

    public Microphone(
            float sampleRate,
            int sampleSize,
            boolean signed,
            boolean bigEndian) {

        AudioFormat format =
                new AudioFormat(sampleRate, sampleSize, 1, signed, bigEndian);
        try {
            line = AudioSystem.getTargetDataLine(format);
            line.open();
            inputStream = new AudioInputStream(line);
        } catch (LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }

    void startRecording() {
        line.start();
    }

    void stopRecording() {
        line.stop();
    }

    public InputStream getStream() {
        return inputStream;
    }
}
