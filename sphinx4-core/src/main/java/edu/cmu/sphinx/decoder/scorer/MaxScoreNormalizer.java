package edu.cmu.sphinx.decoder.scorer;

import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

/**
 * Performs a simple normalization of all token-scores by
 *
 * @author Holger Brandl
 */
public class MaxScoreNormalizer implements ScoreNormalizer {


    public void newProperties(PropertySheet ps) throws PropertyException {
    }

    public MaxScoreNormalizer() {
    }


    public Scoreable normalize(Iterable<? extends Scoreable> scoreableList, Scoreable bestToken) {
        for (Scoreable scoreable : scoreableList) {
            scoreable.normalizeScore(bestToken.score());
        }

        return bestToken;
    }
}
