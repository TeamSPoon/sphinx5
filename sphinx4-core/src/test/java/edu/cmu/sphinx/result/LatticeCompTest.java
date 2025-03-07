/*
 * Copyright 1999-2004 Carnegie Mellon University. Portions Copyright 2004 Sun
 * Microsystems, Inc. Portions Copyright 2004 Mitsubishi Electric Research
 * Laboratories. All Rights Reserved. Use is subject to license terms. See the
 * file "license.terms" for information on usage and redistribution of this
 * file, and for a DISCLAIMER OF ALL WARRANTIES.
 */

package edu.cmu.sphinx.result;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.api.StreamSpeechRecognizer;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.testng.annotations.Test;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Compares the lattices generated when the LexTreeLinguist flag 'keepAllTokens'
 * is turned on/off.
 */
public class LatticeCompTest {

    /**
     * Main method for running the LatticeCompTest demo.
     * 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     */
    @Test
    public void testLatticeComp() throws UnsupportedAudioFileException, IOException {

        Configuration configuration = new Configuration();

        // Load model from the jar
        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/result/hellongram.trigram.lm");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LatticeCompTest.class.getResourceAsStream("green.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();
        Lattice lattice = result.getLattice();

        Lattice otherLattice = Lattice.readSlf(getClass().getResourceAsStream("correct.slf"));

        Collection<Node> latNodes = lattice.getNodes();
        Collection<Node> otherLatNodes = otherLattice.getNodes();

        MutableSet diff = Sets.symmetricDifference(
                lattice.getNodes().stream().map(x -> x.getWord().toString()).collect(Collectors.toSet()),
                otherLattice.getNodes().stream().map(x -> x.getWord().toString()).collect(Collectors.toSet())
        );
        System.out.println("diff=" + diff.size() + "/(" + lattice.getNodes().size() + "|" + otherLattice.getNodes().size() + ")\n" + diff);

        int max = Math.max(latNodes.size(), otherLatNodes.size());
        assertTrue( max > 40 ); //min size
        assertTrue( ((float)diff.size()) / max < 0.2f ); //% difference

//        Iterator<Node> it = latNodes.iterator();
//
//        int edgeCountTolerance = 10; //to account for variability due to multithreading. if this test is using the single threaded scorer, this should be zero
//
//        boolean latticesAreEquivalent = true;
//        while (it.hasNext()) {
//            Node node = it.next();
//            Iterator<Node> otherIt = otherLatNodes.iterator();
//            boolean hasEquivalentNode = false;
//            while (otherIt.hasNext()) {
//                Node otherNode = otherIt.next();
//                boolean nodesAreEquivalent = node.getWord().getSpelling().equals(otherNode.getWord().getSpelling())
//                        && Math.abs(node.getEnteringEdges().size() - otherNode.getEnteringEdges().size()) < edgeCountTolerance
//                        && Math.abs(node.getLeavingEdges().size() - otherNode.getLeavingEdges().size()) < edgeCountTolerance;
//                if (nodesAreEquivalent) {
//                    hasEquivalentNode = true;
//                    break;
//                }
//            }
//
//            if (!hasEquivalentNode) {
//                latticesAreEquivalent = false;
//                break;
//            }
//        }
//        assertTrue(latticesAreEquivalent);

    }
}
