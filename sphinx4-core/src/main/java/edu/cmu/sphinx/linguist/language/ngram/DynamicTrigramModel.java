package edu.cmu.sphinx.linguist.language.ngram;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 3-gram language model that can change its content at runtime.
 * 
 * @author Alexander Solovets
 * 
 */
public class DynamicTrigramModel implements LanguageModel {

    private static final Pattern COMPILE = Pattern.compile("\\s+");
    private Dictionary dictionary;
    private final Set<String> vocabulary;
    private int maxDepth;
    private float unigramWeight;

    private List<String> sentences;
    private final ObjectFloatHashMap<WordSequence> logProbs;
    private final ObjectFloatHashMap<WordSequence> logBackoffs;

    public DynamicTrigramModel() {
        vocabulary = new HashSet<>();
        logProbs = new ObjectFloatHashMap<>();
        logBackoffs = new ObjectFloatHashMap<>();
    }

    public DynamicTrigramModel(Dictionary dictionary) {
        this();
        this.dictionary = dictionary;
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY);
        maxDepth = ps.getInt(PROP_MAX_DEPTH);
        unigramWeight = ps.getFloat(PROP_UNIGRAM_WEIGHT);
    }

    public void allocate() {
        vocabulary.clear();
        logProbs.clear();
        logBackoffs.clear();
        HashMap<WordSequence, Integer> unigrams = new HashMap<>();
        HashMap<WordSequence, Integer> bigrams = new HashMap<>();
        HashMap<WordSequence, Integer> trigrams = new HashMap<>();
        int wordCount = 0;

        for (String sentence : sentences) {
            String[] textWords = COMPILE.split(sentence);
            List<Word> words = new ArrayList<>();
            words.add(dictionary.getSentenceStartWord());
            for (String wordString : textWords) {
        	if (wordString.length() == 0) {
        	    continue;
        	}
                vocabulary.add(wordString);
                Word word = dictionary.word(wordString);
                if (word == null) {
                    words.add(Word.UNKNOWN);
                } else {
                    words.add(word);
                }
            }
            words.add(dictionary.getSentenceEndWord());

            if (words.size() > 0) {
                addSequence(unigrams, new WordSequence(words.get(0)));
                wordCount++;
            }

            if (words.size() > 1) {
                wordCount++;
                addSequence(unigrams, new WordSequence(words.get(1)));
                addSequence(bigrams, new WordSequence(words.get(0), words.get(1)));
            }

            for (int i = 2; i < words.size(); ++i) {
                wordCount++;
                addSequence(unigrams, new WordSequence(words.get(i)));
                addSequence(bigrams, new WordSequence(words.get(i - 1), words.get(i)));
                addSequence(trigrams, new WordSequence(words.get(i - 2), words.get(i - 1), words.get(i)));
            }
        }

        float discount = .5f;
        float deflate = 1 - discount;
        Map<WordSequence, Float> uniprobs = new HashMap<>();
        for (Map.Entry<WordSequence, Integer> e : unigrams.entrySet()) {
            uniprobs.put(e.getKey(), e.getValue() * deflate / wordCount);
        }

        float logUnigramWeight = LogMath.linearToLog(unigramWeight);
        float invLogUnigramWeight = LogMath.linearToLog(1 - unigramWeight);
        float logUniformProb = -LogMath.linearToLog(uniprobs.size());

        Set<WordSequence> sorted1grams = new TreeSet<>(unigrams.keySet());
        Iterator<WordSequence> iter = new TreeSet<>(bigrams.keySet()).iterator();
        WordSequence ws = iter.hasNext() ? iter.next() : null;
        for (WordSequence unigram : sorted1grams) {
            float p = LogMath.linearToLog(uniprobs.get(unigram));
            p += logUnigramWeight;
            p = LogMath.addAsLinear(p, logUniformProb + invLogUnigramWeight);
            logProbs.put(unigram, p);

            float sum = 0.f;
            while (ws != null) {
                int cmp = ws.getOldest().compareTo(unigram);
                if (cmp > 0) {
                    break;
                }
                if (cmp == 0) {
                    sum += uniprobs.get(ws.getNewest());
                }
                ws = iter.hasNext() ? iter.next() : null;
            }

            logBackoffs.put(unigram, LogMath.linearToLog(discount / (1 - sum)));
        }

        Map<WordSequence, Float> biprobs = new HashMap<>();
        for (Map.Entry<WordSequence, Integer> entry : bigrams.entrySet()) {
            int unigramCount = unigrams.get(entry.getKey().getOldest());
            biprobs.put(entry.getKey(), entry.getValue() * deflate / unigramCount);
        }

        Set<WordSequence> sorted2grams = new TreeSet<>(bigrams.keySet());
        iter = new TreeSet<>(trigrams.keySet()).iterator();
        ws = iter.hasNext() ? iter.next() : null;
        for (WordSequence biword : sorted2grams) {
            logProbs.put(biword, LogMath.linearToLog(biprobs.get(biword)));

            float sum = 0.f;
            while (ws != null) {
                int cmp = ws.getOldest().compareTo(biword);
                if (cmp > 0) {
                    break;
                }
                if (cmp == 0) {
                    sum += biprobs.get(ws.getNewest());
                }
                ws = iter.hasNext() ? iter.next() : null;
            }
            logBackoffs.put(biword, LogMath.linearToLog(discount / (1 - sum)));
        }

        for (Map.Entry<WordSequence, Integer> e : trigrams.entrySet()) {
            float p = e.getValue() * deflate;
            WordSequence key = e.getKey();
            p /= bigrams.get(key.getOldest());
            logProbs.put(key, LogMath.linearToLog(p));
        }

        logBackoffs.compact();
        logProbs.compact();
    }

    private static void addSequence(HashMap<WordSequence, Integer> grams, WordSequence wordSequence) {
        Integer count = grams.get(wordSequence);
        if (count != null) {
            grams.put(wordSequence, count + 1);
        } else {
            grams.put(wordSequence, 1);
        }
    }

    public void deallocate() {
    }

    public float getProbability(WordSequence wordSequence) {
        float prob;
        if (logProbs.containsKey(wordSequence)) {
            prob = logProbs.get(wordSequence);
        } else if (wordSequence.size() > 1) {
            float backoff = logBackoffs.getIfAbsent(wordSequence.getOldest(), Float.NaN);
            if (backoff != backoff  /* fast NaN test */) {
                prob = LogMath.LOG_ONE + getProbability(wordSequence.getNewest());
            } else {
                prob = backoff + getProbability(wordSequence.getNewest());
            }
        } else {
            prob = LogMath.LOG_ZERO;
        }
        return prob;
    }

    public float getSmear(WordSequence wordSequence) {
        // TODO: implement
        return 0;
    }

    public Set<String> getVocabulary() {
        return vocabulary;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    @Override
    public void onUtteranceEnd() {
        //TODO not implemented
    }

    public void setText(List<String> sentences) {
        this.sentences = sentences;
    }
}
