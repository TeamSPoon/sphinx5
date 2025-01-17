/*
 * Created on Jan 21, 2005
 */
package edu.cmu.sphinx.linguist.language.ngram;

import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4ComponentList;
import edu.cmu.sphinx.util.props.S4StringList;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Simple interpolated LM implementation.
 *
 * @author Tanel Alumae
 */
public class InterpolatedLanguageModel implements LanguageModel {

    /**
     * The property that defines the language models to be interpolated.
     */
    @S4ComponentList(type = LanguageModel.class)
    public final static String PROP_LANGUAGE_MODELS = "languageModels";

    /**
     * The property that defines the language models weights
     */
    @S4StringList
    public final static String PROP_LANGUAGE_MODEL_WEIGHTS =
        "languageModelWeights";


    private boolean allocated = false;

    private List<LanguageModel> languageModels;
    private float weights[];
    private int numberOfLanguageModels;
    private Set<String> vocabulary;

    private static final double EPSILON = 0.001;

    public InterpolatedLanguageModel(List<LanguageModel> languageModels, float [] floats ) {

        this.languageModels = languageModels;
        this.numberOfLanguageModels = languageModels.size();

        this.weights = new float[floats.length];
        float weightSum = 0;
        for (int i = 0; i < floats.length; i++) {
            weightSum += floats[i];
            this.weights[i] = LogMath.linearToLog(floats[i]);
        }
        if (weightSum < 1.0 - EPSILON || weightSum > 1.0 + EPSILON) {
            throw new PropertyException(
                                        InterpolatedLanguageModel.class.getName(),
                                        PROP_LANGUAGE_MODEL_WEIGHTS,
                                        "Weights do not sum to 1.0");
        }
    }

    public InterpolatedLanguageModel() {

    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        if (allocated) {
            throw new RuntimeException("Can't change properties after allocation");
        }
        languageModels =
            ps.getComponentList(PROP_LANGUAGE_MODELS, LanguageModel.class);
        numberOfLanguageModels = languageModels.size();

        // read weights as a String List.
        List<String> items = ps.getStringList(PROP_LANGUAGE_MODEL_WEIGHTS);
        if (items.size() != numberOfLanguageModels) {
            throw new RuntimeException("Number of weights not equal to number of language models");
        }

        // convert Strings to floats and assign weights.
        float[] floats = new float[items.size()];
        weights = new float[floats.length];
        float weightSum = 0;
        for (int i = 0; i < items.size(); i++) {
            try {
                floats[i] = Float.parseFloat(items.get(i));
                weightSum += floats[i];
                weights[i] = LogMath.linearToLog(floats[i]);
            } catch (NumberFormatException e) {
                throw new PropertyException(
                                            InterpolatedLanguageModel.class.getName(),
                                            PROP_LANGUAGE_MODEL_WEIGHTS,
                                            "Float value expected from the property list. But found:" +
                                                    items.get(i));
            }
        }
        if (weightSum < 1.0 - EPSILON || weightSum > 1.0 + EPSILON) {
            throw new PropertyException(
                                        InterpolatedLanguageModel.class.getName(),
                                        PROP_LANGUAGE_MODEL_WEIGHTS,
                                        "Weights do not sum to 1.0");
        }
    }

    public void allocate() throws IOException {
        if (!allocated) {
            allocated = true;
            vocabulary = new HashSet<>();
            for (LanguageModel model : languageModels) {
                model.allocate();
                vocabulary.addAll(model.getVocabulary());
            }
        }
    }

    public void deallocate() throws IOException {
        allocated = false;
        for (LanguageModel model : languageModels) {
            model.deallocate();
        }
    }

    /**
     * Calculates probability p = w[1]*p[1] + w[2]*p[2] + ... (in log domain)
     *
     * @see edu.cmu.sphinx.linguist.language.ngram.LanguageModel#getProbability(edu.cmu.sphinx.linguist.WordSequence)
     */
    public float getProbability(WordSequence wordSequence) {
        float prob = 0;
        for (int i = 0; i < numberOfLanguageModels; i++) {
            float p =
                weights[i] +
                        (languageModels.get(i)).getProbability(wordSequence);
            if (i == 0) {
                prob = p;
            } else {
                prob = LogMath.addAsLinear(prob, p);
            }
        }
        return prob;

    }

    /*
     * (non-Javadoc)
     * @see
     * edu.cmu.sphinx.linguist.language.ngram.LanguageModel#getSmear(edu.cmu
     * .sphinx.linguist.WordSequence)
     */
    public float getSmear(WordSequence wordSequence) {
        return 1.0f; // TODO not implemented
    }

    /*
     * (non-Javadoc)
     * @see
     * edu.cmu.sphinx.linguist.language.ngram.LanguageModel#getVocabulary()
     */
    public Set<String> getVocabulary() {
        return vocabulary;
    }

    /*
     * (non-Javadoc)
     * @see edu.cmu.sphinx.linguist.language.ngram.LanguageModel#getMaxDepth()
     */
    public int getMaxDepth() {
        int maxDepth = 0;
        for (LanguageModel languageModel : languageModels) {
            int d = languageModel.getMaxDepth();
            if (d > maxDepth) {
                maxDepth = d;
            }
        }
        return maxDepth;
    }

    @Override
    public void onUtteranceEnd() {
    //TODO not implemented
    }
}
