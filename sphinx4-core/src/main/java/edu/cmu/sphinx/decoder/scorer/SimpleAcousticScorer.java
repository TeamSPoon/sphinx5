package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.decoder.search.ActiveList;
import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.util.DataUtil;
import edu.cmu.sphinx.util.props.ConfigurableAdapter;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements some basic scorer functionality, including a simple default
 * acoustic scoring implementation which scores within the current thread, that
 * can be changed by overriding the {@link #doScoring} method.
 * <p>
 * <p>
 * Note that all scores are maintained in LogMath log base.
 *
 * @author Holger Brandl
 */
public class SimpleAcousticScorer extends ConfigurableAdapter implements AcousticScorer {

    /**
     * Property the defines the frontend to retrieve features from for scoring
     */
    @S4Component(type = BaseDataProcessor.class)
    public final static String FEATURE_FRONTEND = "frontend";
    protected BaseDataProcessor frontEnd;

    /**
     * An optional post-processor for computed scores that will normalize
     * scores. If not set, no normalization will applied and the token scores
     * will be returned unchanged.
     */
    @S4Component(type = ScoreNormalizer.class, mandatory = false)
    public final static String SCORE_NORMALIZER = "scoreNormalizer";
    protected ScoreNormalizer scoreNormalizer;

    private LinkedList<Data> storedData;
    private boolean seenEnd = false;

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        this.frontEnd = (BaseDataProcessor) ps.getComponent(FEATURE_FRONTEND);
        this.scoreNormalizer = (ScoreNormalizer) ps.getComponent(SCORE_NORMALIZER);
        storedData = new LinkedList<>();
    }

    /**
     * @param frontEnd        the frontend to retrieve features from for scoring
     * @param scoreNormalizer optional post-processor for computed scores that will
     *                        normalize scores. If not set, no normalization will applied
     *                        and the token scores will be returned unchanged.
     */
    public SimpleAcousticScorer(BaseDataProcessor frontEnd, ScoreNormalizer scoreNormalizer) {
        initLogger();
        this.frontEnd = frontEnd;
        this.scoreNormalizer = scoreNormalizer;
        storedData = new LinkedList<>();
    }

    public SimpleAcousticScorer() {
    }

    /**
     * Scores the given set of states.
     *
     * @param scoreableList A list containing scoreable objects to be scored
     * @return The best scoring scoreable, or <code>null</code> if there are no
     * more features to score
     */
    public Data calculateScores(ActiveList scoreableList) {
        Data data;
        if (storedData.isEmpty()) {
            while ((data = getNextData()) instanceof Signal) {
                if (data instanceof SpeechEndSignal) {
                    seenEnd = true;
                    break;
                }
                if (data instanceof DataEndSignal) {
                    if (seenEnd)
                        return null;
                    else
                        break;
                }
            }
            if (data == null)
                return null;
        } else {
            data = storedData.poll();
        }

        return calculateScoresForData(scoreableList, data);
    }

    public Data calculateScoresAndStoreData(ActiveList scoreableList) {
        Data data;
        while ((data = getNextData()) instanceof Signal) {
            if (data instanceof SpeechEndSignal) {
                seenEnd = true;
                break;
            }
            if (data instanceof DataEndSignal) {
                if (seenEnd)
                    return null;
                else
                    break;
            }
        }
        if (data == null)
            return null;

        storedData.add(data);

        return calculateScoresForData(scoreableList, data);

    }

    protected Data calculateScoresForData(ActiveList scoreableList, Data data) {
        if (data instanceof SpeechEndSignal || data instanceof DataEndSignal) {
            return data;
        }

        //if (scoreableList.isEmpty())
            //return null;

        // convert the data to FloatData if not yet done
        if (data instanceof DoubleData)
            data = DataUtil.DoubleData2FloatData((DoubleData) data);

        Scoreable bestToken = doScoring(scoreableList, data);
        //Scoreable bestToken = scoreableList.best();

        // apply optional score normalization
        if (scoreNormalizer != null && bestToken instanceof Token)
            bestToken = scoreNormalizer.normalize(scoreableList, bestToken);

        return bestToken;
    }

    protected Data getNextData() {
        Data data = frontEnd.getData();
        return data;
    }

    public void startRecognition() {
        storedData.clear();
    }

    public void stopRecognition() {
        // nothing needs to be done here
    }

    protected <T extends Scoreable> T doScoring(Iterable<T> scoreableList, Data data) {
        //return doScoring(scoreableList, 0, scoreableList.size(), data);


        AtomicReference<Double> bestScore = new AtomicReference(Double.NEGATIVE_INFINITY);
        Scoreable[] best = new Scoreable[1];

        scoreableList./*stream().parallel().*/forEach(
                (x) -> {
                    if (x == null)
                        return;

                    float s = x.calculateScore(data);
                    bestScore.updateAndGet((Double b) -> {
                        if (b < s) {
                            best[0] = x;
                            return (double) s;
                        }
                        return b;
                    });
                });

        return (T) best[0];


//        T best = null;
//        float bestScore = -Float.MAX_VALUE;
//        for (int i = 0; i < scoreableList.size(); i++) {
//            T item = scoreableList.get(i);
//            float s = item.calculateScore(data);
//            if (s > bestScore) {
//                bestScore = s;
//                best = item;
//            }
//        }
//        return best;
    }

//    /**
//     * Scores a a list of <code>Scoreable</code>s given a <code>Data</code>
//     * -object.
//     *
//     * @param scoreableList The list of Scoreables to be scored
//     * @param data          The <code>Data</code>-object to be used for scoring.
//     * @param <T>           type for scorables
//     * @return the best scoring <code>Scoreable</code> or <code>null</code> if
//     * the list of scoreables was empty.
//     */
//    protected <T extends Scoreable> T doScoring(List<T> scoreableList, int from, int to, Data data) {
//

//    }

    // Even if we don't do any meaningful allocation here, we implement the
    // methods because most extending scorers do need them either.

    public void allocate() {
    }

    public void deallocate() {
    }

}
