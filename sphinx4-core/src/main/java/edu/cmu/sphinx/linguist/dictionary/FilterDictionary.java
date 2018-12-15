package edu.cmu.sphinx.linguist.dictionary;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Tool for generating filtered dictionaries
 */
public class FilterDictionary {


    int keep, total;
    Set<String> include = new HashSet(128*1024);

    public FilterDictionary(String filter, String input, String output) throws IOException {

        Files.lines(Paths.get(filter)).forEach(l -> {
            l = l.trim();
            if (!l.isEmpty())
                if (l.charAt(0)!='#')
                    include.add(l.trim());
        });

        System.out.println("Filter include: " + include.size() + " words");

        PrintStream out = new PrintStream(new BufferedOutputStream( new FileOutputStream(output)) );

        Files.lines(Paths.get(input)).forEach(l -> {


            int wordLen = l.indexOf(' ');
            String word = l.substring(0, wordLen);

            if (include.contains(word)) {
                System.out.println(word);
                out.println(l);
                keep++;
            }

            total++;
        });

        out.flush();
        out.close();

        System.out.println(keep + "/" + total + " (" + ((float)keep/total)*100f + "% included");
    }

    public static void main(String[] args) throws Exception {
        new FilterDictionary(
                ("/home/me/sphinx4/sphinx4-data/src/main/resources/deepstupid/en-us.simpler.filter"),
                ("/home/me/sphinx4/sphinx4-data/src/main/resources/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict"),
                ("/home/me/sphinx4/sphinx4-data/src/main/resources/deepstupid/en-us.simpler.dict")
        );
    }

}
