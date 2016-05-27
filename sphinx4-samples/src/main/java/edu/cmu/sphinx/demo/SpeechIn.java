package edu.cmu.sphinx.demo;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;

import static edu.cmu.sphinx.demo.transcriber.TranscriberDemo.printResults;


/**
 * http://cmusphinx.sourceforge.net/wiki/tutorialsphinx4
 */
public class SpeechIn {

    public static void main(String[] args) throws Exception {

        Configuration configuration = new Configuration();
        configuration
                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration
                .setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
        configuration
                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");



        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);

        recognizer.startRecognition(true);
        System.out.println("recognition start");

        //Thread.sleep(5000);


        //SpeechResult result = recognizer.getResult();

        printResults(recognizer);
        System.out.println("recognition stop");

        recognizer.stopRecognition();




    }

}
