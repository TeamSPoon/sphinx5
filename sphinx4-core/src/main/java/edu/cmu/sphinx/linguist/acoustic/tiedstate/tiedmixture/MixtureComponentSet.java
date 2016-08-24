/*
 * Copyright 2014 Carnegie Mellon University.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.linguist.acoustic.tiedstate.tiedmixture;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.acoustic.tiedstate.MixtureComponent;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MixtureComponentsSet - phonetically tied set of gaussians
 */
public class MixtureComponentSet {
    
    private int scoresQueueLen;
    private boolean toStoreScore;
    //private final Deque<MixtureComponentSetScores> storedScores;
    private final Map<Long,MixtureComponentSetScores> storedScores;

    private final PrunableMixtureComponent[][] components;
    private final PrunableMixtureComponent[][] topComponents;
    public final int numStreams;
    public final int topGauNum;
    public final int gauNum;
    private long gauCalcSampleNumber;

    private final ConcurrentSkipListSet<Long> sampleTimes = new ConcurrentSkipListSet<>();
    private final AtomicLong lastSample = new AtomicLong(Long.MIN_VALUE);
    
    public MixtureComponentSet(PrunableMixtureComponent[][] components, int topGauNum) {
        this.components = components;
        this.numStreams = components.length;
        this.topGauNum = topGauNum;
        this.gauNum = components[0].length;
        topComponents = new PrunableMixtureComponent[numStreams][];
        for (int i = 0; i < numStreams; i++) {
            PrunableMixtureComponent[] featTopComponents = new PrunableMixtureComponent[topGauNum];
            PrunableMixtureComponent[] ci = components[i];
            System.arraycopy(ci, 0, featTopComponents, 0, topGauNum);
            topComponents[i] = featTopComponents;
        }
        gauCalcSampleNumber = -1;
        toStoreScore = false;
        storedScores = //new ConcurrentHashMap<>();
                        new HashMap();
    }
    
    private void add(MixtureComponentSetScores scores) {
        long start = scores.getFrameStartSample();
        storedScores.put(start, scores);
        sampleTimes.add(start);
        if (start > lastSample.get())
            lastSample.set(start);
    }

    private MixtureComponentSetScores removeFirst() {
        if (!sampleTimes.isEmpty()) {
            long when = sampleTimes.pollFirst();
            //if (when != null) {
                MixtureComponentSetScores s = storedScores.remove(when);
                return s;
            //}
        }
        return null;
    }
    
    private MixtureComponentSetScores getStoredScores(long frameFirstSample) {
        if (storedScores.isEmpty())
            return null;
        if (lastSample.get() < frameFirstSample)
            //new frame
            return null;

        return storedScores.get(frameFirstSample);
    }
    
    private MixtureComponentSetScores createFromTopGau(long firstFrameSample, MixtureComponentSetScores recycle) {

        int s = this.numStreams;
        int n = this.topGauNum;
        MixtureComponentSetScores scores =
                (recycle == null ? new MixtureComponentSetScores() : recycle)
                        .clear(s, n, firstFrameSample);

        PrunableMixtureComponent[][] topComponents = this.topComponents;
        for (int i = 0; i < s; i++) {

            PrunableMixtureComponent[] topI = topComponents[i];
            float[] scoreRow = scores.scores[i];
            int[] idRow = scores.ids[i];

            for (int j = 0; j < n; j++) {
                PrunableMixtureComponent topIJ = topI[j];
                scoreRow[j] = topIJ.score;
                idRow[j] = topIJ.id;
            }
        }
        return scores;
    }
    
    private static void insertTopComponent(PrunableMixtureComponent[] topComponents, PrunableMixtureComponent component) {
        int i;
        int l = topComponents.length;
        float cScore = component.getPartialScore();
        for (i = 0; i < l - 1; i++) {
            if (i >= 1 && cScore < topComponents[i].partScore) {
                topComponents[i - 1] = component;
                return;
            }
            topComponents[i] = topComponents[i + 1];
        }

        if (l >= 2 && cScore < topComponents[l - 1].partScore)
            topComponents[l - 2] = component;
        else if (l >= 1)
            topComponents[l - 1] = component;
    }
    
    private static boolean isInTopComponents(PrunableMixtureComponent[] topComponents, PrunableMixtureComponent component) {
        for (PrunableMixtureComponent topComponent : topComponents)
            if (topComponent.id == component.id)
                return true;
        return false;
    }
    
    private void updateTopScores(float[] featureVector) {
        int step = featureVector.length / numStreams;        
        
        float[] streamVector = new float[step];
        for (int i = 0; i < numStreams; i++) {
            System.arraycopy(featureVector, i * step, streamVector, 0, step);
            PrunableMixtureComponent[] featTopComponents = topComponents[i];
            PrunableMixtureComponent[] featComponents = components[i];
            
            //update scores in top gaussians from previous frame
            for (PrunableMixtureComponent topComponent : featTopComponents)
                topComponent.updateScore(streamVector);

            Arrays.sort(featTopComponents, componentComparator);
            
            //Check if there is any gaussians that should float into top
            float threshold = featTopComponents[0].getPartialScore();    
            for (PrunableMixtureComponent component : featComponents) {
                if (isInTopComponents(featTopComponents, component))
                    continue;
                if (component.isTopComponent(streamVector, threshold)) {
                    insertTopComponent(featTopComponents, component);
                    threshold = featTopComponents[0].partScore;
                }
            }
        }
    }
    
    public MixtureComponentSetScores updateTopScores(Data feature) {
        
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");

        FloatData featureFloat = FloatData.toFloatData(feature);

        long firstSampleNumber = featureFloat.firstSampleNumber;

        if (toStoreScore) {
            MixtureComponentSetScores s = getStoredScores(firstSampleNumber);
            if (s != null)
                //component scores for this frame was already calculated
                return s;
        }

        float[] featureVector = featureFloat.values;
        updateTopScores(featureVector);
        //store just calculated score in list

        int size = storedScores.size();
        int toRemove = (size+1) - scoresQueueLen;
        MixtureComponentSetScores lastRemoved = null;
        for (int i = 0; i < toRemove; i++) {
            lastRemoved = toRemove > 0 ? removeFirst() : null; //recycle the first
        }

        MixtureComponentSetScores s = createFromTopGau(firstSampleNumber, lastRemoved);
        if (toStoreScore)
            add(s);
        return s;
    }
    
    private void updateScores(float[] featureVector) {
        int step = featureVector.length / numStreams;
        float[] streamVector = new float[step];
        for (int i = 0; i < numStreams; i++) {
            System.arraycopy(featureVector, i * step, streamVector, 0, step);
            for (PrunableMixtureComponent component : components[i]) {
                component.updateScore(streamVector);
            }
        }
    }
    
    public void updateScores(Data feature) {
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");

        FloatData featureFloat = FloatData.toFloatData(feature);
        long firstSampleNumber = featureFloat.firstSampleNumber;
        if (gauCalcSampleNumber != firstSampleNumber) {
            updateScores(featureFloat.values);
            gauCalcSampleNumber = firstSampleNumber;
        }
    }
    
    /**
     * Should be called on each new utterance to scores for old frames
     */
    public void clearStoredScores() {
        storedScores.clear();
    }
    
    /**
     * How long scores for previous frames should be stored.
     * For fast match this value is lookahead_window_length + 1)
     * @param scoresQueueLen queue length
     */
    public void setScoreQueueLength(int scoresQueueLen) {
        toStoreScore = scoresQueueLen > 0;
        this.scoresQueueLen = scoresQueueLen;
    }

//    public float getTopGauScore(int streamId, int topGauId) {
//        return curScores.getScore(streamId, topGauId);
//    }
//
//    public int getTopGauId(int streamId, int topGauId) {
//        return curScores.getGauId(streamId, topGauId);
//    }
    
    public float getGauScore(int streamId, int topGauId) {
        return components[streamId][topGauId].getStoredScore();
    }
    
    public int getGauId(int streamId, int topGauId) {
        return components[streamId][topGauId].id;
    }
    
//    private static <T> T[] concatenate(T[] A, T[] B) {
//        int aLen = A.length;
//        int bLen = B.length;
//
//        @SuppressWarnings("unchecked")
//        T[] C = (T[]) Array.newInstance(A.getClass().getComponentType(), aLen+bLen);
//        System.arraycopy(A, 0, C, 0, aLen);
//        System.arraycopy(B, 0, C, aLen, bLen);
//
//        return C;
//    }
    
    protected MixtureComponent[] toArray() {
        int total = 0;
        for (int i = 0; i < numStreams; i++)
            total += components[i].length;

        PrunableMixtureComponent[] allComponents = new PrunableMixtureComponent[total];
        int p = 0;
        for (int i = 0; i < numStreams; i++) {
            PrunableMixtureComponent[] c = components[i];
            int cl = c.length;
            System.arraycopy(c, 0, allComponents, p, cl);
            p += cl;
        }
        return allComponents;
    }
    
    protected int dimension() {
        int dimension = 0;
        for (int i = 0; i < numStreams; i++) {
            dimension+= components[i][0].getMean().length;
        }
        return dimension;
    }

    protected int size() {
//        int size = 0;
//        for (int i = 0; i < numStreams; i++) {
//            size += components[0].length;
//        }
//        return size;
        return components[0].length * numStreams;
    }
    
    static final Comparator<PrunableMixtureComponent> componentComparator = (a, b) -> (int)(a.score - b.score);

}
