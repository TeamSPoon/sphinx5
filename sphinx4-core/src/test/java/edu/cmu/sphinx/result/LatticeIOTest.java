/*
 * Copyright 2015 Carnegie Mellon University. 
 * All Rights Reserved. Use is subject to license terms. See the
 * file "license.terms" for information on usage and redistribution of this
 * file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package edu.cmu.sphinx.result;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Compares the lattices after recognition and loaded from file for LAT and HTK
 * format
 */
public class LatticeIOTest {

    final static Path tmp;

    static {
        try {
            tmp = Files.createTempDirectory(LatticeIOTest.class.getSimpleName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final File latFile = tmp.resolve("tmp.lat").toFile();
    private final File slfFile = tmp.resolve("tmp.slf").toFile();

    /**
     * Method for cleaning tmp files if any was created
     */
    @AfterTest
    public void removeTmpFiles() {
//        if (latFile.exists())
//            latFile.delete();
//        if (slfFile.exists())
//            slfFile.delete();
        if (tmp.toFile().exists())
            tmp.toFile().delete();
    }

    /**
     * Main method for running the LatticeIOTest demo.
     * 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    @Test
    public void testLatticeIO() throws IOException {
        Configuration configuration = new Configuration();

        // Load model from the jar
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/result/hellongram.trigram.lm");

        // Simple recognition with generic model
        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);

        InputStream stream = getClass().getResourceAsStream("green.wav");
        stream.skip(44);
        recognizer.startRecognition(stream);

        SpeechResult result = recognizer.getResult();

        Lattice lattice = result.getLattice();
        lattice.dump(latFile.getAbsolutePath());
        lattice.dumpSlf(new FileWriter(slfFile));
        Iterator<WordResult> latIt = lattice.getWordResultPath().iterator();

        Lattice latLattice = new Lattice(latFile.getAbsolutePath());
        latLattice.computeNodePosteriors(1.0f);
        Iterator<WordResult> latLatIt = latLattice.getWordResultPath().iterator();

        Lattice slfLattice = Lattice.readSlf(slfFile.getAbsolutePath());
        slfLattice.computeNodePosteriors(1.0f);
        Iterator<WordResult> slfLatIt = slfLattice.getWordResultPath().iterator();


        while (latIt.hasNext()) {
            WordResult latWord = latIt.next();
            WordResult latLatWord = latLatIt.next();
            WordResult slfLatWord = slfLatIt.next();
            Assert.assertEquals(latWord.word.toString(), latLatWord.word.toString());
            Assert.assertEquals(latWord.word.toString(), slfLatWord.word.toString());
            Assert.assertEquals(latWord.timeFrame.start, latLatWord.timeFrame.start);
        }
        Assert.assertEquals(lattice.getTerminalNode().getViterbiScore(), latLattice.getTerminalNode().getViterbiScore(), 0.001);
        Assert.assertEquals(lattice.getTerminalNode().getViterbiScore(), slfLattice.getTerminalNode().getViterbiScore(), 0.001);
    }
}
