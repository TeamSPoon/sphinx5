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

import edu.cmu.sphinx.decoder.Decoder;
import edu.cmu.sphinx.decoder.search.WordPruningBreadthFirstSearchManager;
import edu.cmu.sphinx.frontend.util.StreamDataSource;

import java.io.IOException;
import java.util.function.BiPredicate;


/**
 * High-level class for live speech recognition.
 */
public class LiveSpeechRecognizer extends AbstractSpeechRecognizer {

    public final Microphone microphone;

    public LiveSpeechRecognizer(Configuration configuration, int sampleRate) throws IOException {
        super(configuration);
        microphone = SpeechSourceProvider.getMicrophone(sampleRate);
        context.getInstance(StreamDataSource.class)
                .setInputStream(microphone.getStream());
    }

    /**
     * Constructs new live recognition object.
     *
     * @param configuration common configuration
     * @throws IOException if model IO went wrong
     */
    public LiveSpeechRecognizer(Configuration configuration) throws IOException {
        this(configuration, 16000);
    }

    /**
     * Starts recognition process.
     *
     * @see LiveSpeechRecognizer#stopRecognition()
     */
    public void startRecognition() {
        recognizer.allocate();
        microphone.startRecording();
    }

    /**
     * starts an async recognizer process, providing access to the recognizer and each result that it streams
     */
    public void startRecognition(BiPredicate<Decoder<WordPruningBreadthFirstSearchManager>, SpeechResult> eachResult) {
        startRecognition();

        recognizer.recognize(eachResult);

        stopRecognition();
    }

    /**
     * Stops recognition process.
     * <p>
     * Recognition process is paused until the next call to startRecognition.
     *
     * @see #startRecognition()
     */
    public void stopRecognition() {
        microphone.stopRecording();
        recognizer.deallocate();
    }
}
