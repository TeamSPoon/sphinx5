/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import edu.cmu.sphinx.decoder.adaptation.ClusteredDensityFileData;
import edu.cmu.sphinx.decoder.adaptation.Transform;
import edu.cmu.sphinx.linguist.acoustic.Unit;
import edu.cmu.sphinx.util.props.Configurable;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/** Generic interface for a loader of acoustic models */
public interface Loader extends Configurable {

    /**
     * Loads the acoustic model.
     *
     * @throws IOException if an error occurs while loading the model
     */
    void load() throws IOException;

    /**
     * Gets the pool of means for this loader.
     *
     * @return the pool
     */
    Pool<float[]> getMeansPool();

    /**
     * Gets the means transformation matrix pool.
     *
     * @return the pool
     */
    Pool<float[][]> getMeansTransformationMatrixPool();

    /**
     * Gets the means transformation vectors pool.
     *
     * @return the pool
     */
    Pool<float[]> getMeansTransformationVectorPool();

    /**
     * Gets the variance pool.
     *
     * @return the pool
     */
    Pool<float[]> getVariancePool();

    /**
     * Gets the variance transformation matrix pool.
     *
     * @return the pool
     */
    Pool<float[][]> getVarianceTransformationMatrixPool();

    /**
     * Gets the variance transformation vectors pool.
     *
     * @return the pool
     */
    Pool<float[]> getVarianceTransformationVectorPool();

    /**
     * Gets the mixture weight pool.
     *
     * @return the pool
     */
    GaussianWeights getMixtureWeights();

    /**
     * Gets the transition matrix pool.
     *
     * @return the pool
     */
    Pool<float[][]> getTransitionMatrixPool();

    /**
     * Gets the transformation matrix.
     *
     * @return the matrix
     */
    float[][] getTransformMatrix();

    /**
     * Gets the senone pool for this loader.
     *
     * @return the pool
     */
    Pool<Senone> getSenonePool();

    /**
     * Returns the HMM Manager for this loader.
     *
     * @return the HMM Manager
     */
    HMMManager getHMMManager();

    /**
     * Returns the map of context indepent units. The map can be accessed by unit name.
     *
     * @return the map of context independent units
     */
    Map<String, Unit> getContextIndependentUnits();

    /** logs information about this loader */
    void logInfo();

    /**
     * Returns the size of the left context for context dependent units.
     *
     * @return the left context size
     */
    int getLeftContextSize();

    /**
     * Returns the size of the right context for context dependent units.
     *
     * @return the left context size
     */
    int getRightContextSize();
    
    /**
     * @return the model properties
     */
    Properties getProperties();

    /**
     * Apply the transform
     * @param transform transform to apply to the model
     * @param clusters transform clusters
     */
    void update(Transform transform, ClusteredDensityFileData clusters);
}
