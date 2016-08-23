/*
* Copyright 2014 Carnegie Mellon University.
* All Rights Reserved.  Use is subject to license terms.
*
* See the file "license.terms" for information on usage and
* redistribution of this file, and for a DISCLAIMER OF ALL
* WARRANTIES.
*
*/

package edu.cmu.sphinx.linguist.acoustic.tiedstate;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Structure to store weights for all gaussians in AM. 
 * Supposed to provide faster access in case of large models */
public class GaussianWeights {

    public final float[][] weights;
    public final int states;
    public final int gauPerState;
    public final int streams;
    public final String name;

    public GaussianWeights(String name, int states, int gauPerState, int streams) {
        this.states = states;
        this.gauPerState = gauPerState;
        this.streams = streams;
        this.name = name;
        weights = new float[gauPerState][states * streams];
    }
    
    public void put(int stateId, int streamId, float[] gauWeights) {
        assert gauWeights.length == gauPerState;
        int s = stateId * streams + streamId;
        for (int i = 0; i < gauPerState; i++) {
            weights[i][s] = gauWeights[i];
        }
    }
    
    public float get(int stateId, int streamId, int gaussianId) {
        return weights[gaussianId][stateId * streams + streamId];
    }

    public void logInfo(Logger logger) {
        if (logger.isLoggable(Level.INFO))
            logger.info(name + " Gaussian weights: " + states * streams);
    }
    
    public static Pool<float[]> convertToPool() {
        return null;
    }
}
