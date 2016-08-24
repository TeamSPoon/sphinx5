package edu.cmu.sphinx.demo.live;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.frontend.util.VUMeterMonitor;
import edu.cmu.sphinx.result.WordResult;


/**
 * http://cmusphinx.sourceforge.net/wiki/tutorialsphinx4
 */
public class SpeechIn {


    public static void main(String[] args) throws Exception {

        Configuration configuration = new Configuration();
        configuration
                .setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
        configuration
                //.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
                .setDictionaryPath("resource:/deepstupid/en-us.simpler.dict");
        configuration
                .setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");

        configuration.setUseGrammar(false);



        LiveSpeechRecognizer recognizer = new LiveSpeechRecognizer(configuration);




        recognizer.startRecognition((d,result) -> {
            System.out.print(result.getHypothesis() + "\t");

            for (WordResult r : result.getWords()) {
                System.out.print(r + " ");
            }
            System.out.println();



            //System.out.println("\t" + result.getNbest(2));
            //System.out.println("\t" + result.getResult().getActiveTokens());
            //System.out.println("\t" + result.getLattice());
            //System.out.println("\t" + result);

            return true;
        });



    }

}
