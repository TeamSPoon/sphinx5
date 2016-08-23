package edu.cmu.sphinx.api;

import edu.cmu.sphinx.result.WordResult;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

public class LiveRecognizerTest {
    @Test
    public void testLm() throws IOException {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LiveRecognizerTest.class
                .getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();

        assertEquals("one zero zero zero one", result.getHypothesis());

        WordResult word = result.getWords().get(0);

        //assertEquals("{what, 0.768, [820:1080]}", );
        assertEquals("what", word.getWord().toString());
        assertEquals(0.775f, word.confLinear(), 0.25f);
        assertEquals(820, word.getTimeFrame().getStart());
        assertEquals(1080, word.getTimeFrame().getEnd());
    }


    @Test
    public void testGram() throws IOException {
        Configuration configuration = new Configuration();

        configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration.setGrammarPath("resource:/edu/cmu/sphinx/jsgf/test/");
        configuration.setGrammarName("digits.grxml");
        configuration.setUseGrammar(true);

        StreamSpeechRecognizer recognizer = new StreamSpeechRecognizer(configuration);
        InputStream stream = LiveRecognizerTest.class
                .getResourceAsStream("/edu/cmu/sphinx/tools/bandwidth/10001-90210-01803.wav");
        stream.skip(44);

        // Simple recognition with generic model
        recognizer.startRecognition(stream);
        SpeechResult result = recognizer.getResult();

        assertNotNull(result);

        assertEquals("one zero zero zero one", result.getHypothesis());

        WordResult word = result.getWords().get(0);
        assertEquals("{one, 1.000, [840:1060]}", word.toString());
    }
}
