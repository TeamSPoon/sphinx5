/*
 * Copyright 1999-2012 Carnegie Mellon University. Portions Copyright 2002 Sun
 * Microsystems, Inc. Portions Copyright 2002 Mitsubishi Electric Research
 * Laboratories. Portions Copyright 2012 Nexiwave All Rights Reserved. Use is
 * subject to license terms. See the file "license.terms" for information on
 * usage and redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.linguist.language.ngram.large;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.acoustic.UnitManager;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.TextDictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;


public class LargeNgramTest {

    @Test
    public void testNgram() throws IOException {
        URL dictUrl = getClass().getResource("100.dict");
        URL noisedictUrl = getClass()
                .getResource("/edu/cmu/sphinx/models/en-us/en-us/noisedict");

        Dictionary dictionary = new TextDictionary(dictUrl,
                                                   noisedictUrl,
                                                   null,
                                                   null,
                                                   new UnitManager());

        URL lm = getClass().getResource("100.arpa.dmp");
        LargeTrigramModel model = new LargeTrigramModel("",
                                                        lm,
                                                        null,
                                                        100,
                                                        100,
                                                        false,
                                                        3,
                                                        dictionary,
                                                        false,
                                                        1.0f,
                                                        1.0f,
                                                        1.0f,
                                                        false);
        dictionary.allocate();
        model.allocate();
        assertThat(model.getMaxDepth(), equalTo(3));

        Word[] words = {
            new Word("huggins", null, false),
            new Word("daines", null, false)};
        assertThat((double) model.getProbability(new WordSequence(words)),
                   closeTo(-830.862, .001));

        Word[] words1 = {
            new Word("huggins", null, false),
            new Word("daines", null, false),
            new Word("david", null, false)};
        assertThat((double) model.getProbability(new WordSequence(words1)),
                   closeTo(-67625.77, .01));
    }
}
