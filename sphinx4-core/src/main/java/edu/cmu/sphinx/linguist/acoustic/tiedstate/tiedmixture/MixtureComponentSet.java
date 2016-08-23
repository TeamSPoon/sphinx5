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

import java.lang.reflect.Array;
import java.util.*;

/**
 * MixtureComponentsSet - phonetically tied set of gaussians
 */
public class MixtureComponentSet {
    
    private int scoresQueueLen;
    private boolean toStoreScore;
    private final Deque<MixtureComponentSetScores> storedScores;
    MixtureComponentSetScores curScores;

    private final ArrayList<PrunableMixtureComponent[]> components;
    private final ArrayList<PrunableMixtureComponent[]> topComponents;
    private final int numStreams;
    private final int topGauNum;
    private final int gauNum;
    private long gauCalcSampleNumber;
    
    public MixtureComponentSet(ArrayList<PrunableMixtureComponent[]> components, int topGauNum) {
        this.components = components;
        this.numStreams = components.size();
        this.topGauNum = topGauNum;
        this.gauNum = components.get(0).length;
        topComponents = new ArrayList<>();
        for (int i = 0; i < numStreams; i++) {
            PrunableMixtureComponent[] featTopComponents = new PrunableMixtureComponent[topGauNum];
            for (int j = 0; j < topGauNum; j++)
                featTopComponents[j] = components.get(i)[j];
            topComponents.add(featTopComponents);
        }
        gauCalcSampleNumber = -1;
        toStoreScore = false;
        storedScores = new ArrayDeque<>();
        curScores = null;
    }
    
    private void storeScores(MixtureComponentSetScores scores) {


        storedScores.add(scores);
    }
    
    private MixtureComponentSetScores getStoredScores(long frameFirstSample) {
        if (storedScores.isEmpty())
            return null;
        if (storedScores.peekLast().getFrameStartSample() < frameFirstSample)
            //new frame
            return null;
        for (MixtureComponentSetScores scores : storedScores) {
            if (scores.getFrameStartSample() == frameFirstSample)
                return scores;
        }
        //Failed to find score. Seems it wasn't calculated yet
        return null;
    }
    
    private MixtureComponentSetScores createFromTopGau(long firstFrameSample, MixtureComponentSetScores recycle) {

        int s = this.numStreams;
        int n = this.topGauNum;
        MixtureComponentSetScores scores =
                (recycle == null ? new MixtureComponentSetScores() : recycle)
                        .clear(s, n, firstFrameSample);

        for (int i = 0; i < s; i++) {
            ArrayList<PrunableMixtureComponent[]> topComponents = this.topComponents;

            PrunableMixtureComponent[] topI = topComponents.get(i);
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
            if (cScore < topComponents[i].getPartialScore()) {
                topComponents[i - 1] = component;
                return;
            }
            topComponents[i] = topComponents[i + 1];
        }

        if (cScore < topComponents[l - 1].getPartialScore())
            topComponents[l - 2] = component;
        else
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
            PrunableMixtureComponent[] featTopComponents = topComponents.get(i);
            PrunableMixtureComponent[] featComponents = components.get(i);
            
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
                    threshold = featTopComponents[0].getPartialScore();
                }
            }
        }
    }
    
    public void updateTopScores(Data feature) {
        
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");
        
        long firstSampleNumber = FloatData.toFloatData(feature).getFirstSampleNumber();
        if (toStoreScore) {
            curScores = getStoredScores(firstSampleNumber);
        } else {
            if (curScores != null && curScores.getFrameStartSample() != firstSampleNumber)
                curScores = null;
        }
        if (curScores != null)
            //component scores for this frame was already calculated
            return;
        float[] featureVector = FloatData.toFloatData(feature).getValues();
        updateTopScores(featureVector);
        //store just calculated score in list


        int size = storedScores.size();
        int toRemove = (size+1) - scoresQueueLen;
        MixtureComponentSetScores lastRemoved = null;
        for (int i = 0; i < toRemove; i++)
            lastRemoved = storedScores.poll();

        curScores = createFromTopGau(firstSampleNumber, lastRemoved);
        if (toStoreScore)
            storeScores(curScores);
    }
    
    private void updateScores(float[] featureVector) {
        int step = featureVector.length / numStreams;
        float[] streamVector = new float[step];
        for (int i = 0; i < numStreams; i++) {
            System.arraycopy(featureVector, i * step, streamVector, 0, step);
            for (PrunableMixtureComponent component : components.get(i)) {
                component.updateScore(streamVector);
            }
        }
    }
    
    public void updateScores(Data feature) {
        if (feature instanceof DoubleData)
            System.err.println("DoubleData conversion required on mixture level!");
        
        long firstSampleNumber = FloatData.toFloatData(feature).getFirstSampleNumber();
        if (gauCalcSampleNumber != firstSampleNumber) {
            float[] featureVector = FloatData.toFloatData(feature).getValues();
            updateScores(featureVector);
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
    
    public int getTopGauNum() {
        return topGauNum;
    }
    
    public int getGauNum() {
        return gauNum;
    }
    
    public float getTopGauScore(int streamId, int topGauId) {
        return curScores.getScore(streamId, topGauId);
    }
    
    public int getTopGauId(int streamId, int topGauId) {
        return curScores.getGauId(streamId, topGauId);
    }
    
    public float getGauScore(int streamId, int topGauId) {
        return components.get(streamId)[topGauId].getStoredScore();
    }
    
    public int getGauId(int streamId, int topGauId) {
        return components.get(streamId)[topGauId].id;
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
            total += components.get(i).length;

        PrunableMixtureComponent[] allComponents = new PrunableMixtureComponent[total];
        int p = 0;
        for (int i = 0; i < numStreams; i++) {
            PrunableMixtureComponent[] c = components.get(i);
            int cl = c.length;
            System.arraycopy(c, 0, allComponents, p, cl);
            p += cl;
        }
        return allComponents;
    }
    
    protected int dimension() {
        int dimension = 0;
        for (int i = 0; i < numStreams; i++) {
            dimension+= components.get(i)[0].getMean().length;
        }
        return dimension;
    }

    protected int size() {
        int size = 0;
        for (int i = 0; i < numStreams; i++) {
            size += components.get(0).length;
        }
        return size;
    }
    
    static final Comparator<PrunableMixtureComponent> componentComparator = (a, b) -> (int)(a.score - b.score);

}
