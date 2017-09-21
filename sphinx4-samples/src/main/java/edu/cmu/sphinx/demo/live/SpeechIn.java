package edu.cmu.sphinx.demo.live;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;

import edu.cmu.sphinx.result.WordResult;
import edu.stanford.nlp.simple.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

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


        ExecutorService exe = ForkJoinPool.commonPool(); //Executors.newSingleThreadExecutor();

        recognizer.startRecognition((d, result) -> {

            System.out.print(result.getHypothesis() + "\n\t");

            for (WordResult r : result.getWords()) {
                System.out.print(r + " ");
            }
            System.out.println();

            exe.submit(() -> {


                //System.out.println("\t" + result.getNbest(2));
                //System.out.println("\t" + result.getResult().getActiveTokens());
                //System.out.println("\t" + result.getLattice());
                //System.out.println("\t" + result);


                Document doc = new Document(result.getHypothesis());
                for (Sentence sent : doc.sentences()) {  // Will iterate over two sentences
                    System.out.println("\t" + sent);
                    System.out.println("\t" + sent.parse());
                    System.out.println("\t" + sent.dependencyGraph());
                }
            });

            return true;
        });


    }

}
