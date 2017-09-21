package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import edu.cmu.sphinx.frontend.Data;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMapUnsafe;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements a Senone that contains a cache of the last scored data.
 * <p>
 * Subclasses should implement the abstract {@link #calculateScore} method,
 * which is called by the {@link #getScore} method to calculate the score
 * for each cache miss.
 * <p>
 * Note: this implementation is thread-safe and can be safely used
 * across different threads without external synchronization.
 *
 * @author Yaniv Kunda
 */
@SuppressWarnings("serial")
public abstract class ScoreCachingSenone implements Senone {


    final Map<Data,Float> scoreCache =

            //new ConcurrentHashMapUnsafe<>(1024*8);
            new ConcurrentHashMap<>(1024*8);


    //long tries, miss;

    /**
     * Gets the cached score for this senone based upon the given feature.
     * If the score was not cached, it is calculated using {@link #calculateScore},
     * cached, and then returned.  
     */
    public final float getScore(Data feature) {
//        ScoreCache cached = scoreCache;
//        if (feature != cached.feature) {
//            cached = new ScoreCache(feature, calculateScore(feature));
//            scoreCache = cached;
//        }
//        return cached.score;

        //System.out.println("scorecache: " + scoreCache.size());


        return scoreCache.computeIfAbsent(feature, this::calculateScore);


        /*
        tries++;
        float score = scoreCache.computeIfAbsent(feature, (f) -> {
            miss++;
            return calculateScore(f);
        });*/
        //System.out.println( "hit rate: " + (tries-miss) + " / " + (tries) );
        //return score;

        //return calculateScore(feature);
    }

    /**
     * Calculates the score for this senone based upon the given feature.
     *
     * @param feature the feature vector to score this senone against
     * @return the score for this senone in LogMath log base
     */
    protected abstract float calculateScore(Data feature);

}
