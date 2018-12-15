/*
 * Copyright 2013 Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.frontend.denoise;

import edu.cmu.sphinx.frontend.*;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.System.arraycopy;

/**
 * The noise filter, same as implemented in sphinxbase/sphinxtrain/pocketsphinx.
 * 
 * Noise removal algorithm is inspired by the following papers Computationally
 * Efficient Speech Enchancement by Spectral Minina Tracking by G. Doblinger
 * 
 * Power-Normalized Cepstral Coefficients (PNCC) for Robust Speech Recognition
 * by C. Kim.
 * 
 * For the recent research and state of art see papers about IMRCA and A
 * Minimum-Mean-Square-Error Noise Reduction Algorithm On Mel-Frequency Cepstra
 * For Robust Speech Recognition by Dong Yu and others
 * 
 */
public class Denoise extends BaseDataProcessor {

    private transient double[] power = new double[0];
    private transient double[] noise = new double[0];
    private transient double[] floor = new double[0];
    private transient double[] peak = new double[0];
    private transient double[] signal = new double[0];
    private transient double[] gain = new double[0];

    @S4Double(defaultValue = 0.7)
    public final static String LAMBDA_POWER = "lambdaPower";
    double lambdaPower;

    @S4Double(defaultValue = 0.995)
    public final static String LAMBDA_A = "lambdaA";
    double lambdaA;

    @S4Double(defaultValue = 0.5)
    public final static String LAMBDA_B = "lambdaB";
    double lambdaB;

    @S4Double(defaultValue = 0.85)
    public final static String LAMBDA_T = "lambdaT";
    double lambdaT;

    @S4Double(defaultValue = 0.2)
    public final static String MU_T = "muT";
    double muT;

    @S4Double(defaultValue = 20.0)
    public final static String MAX_GAIN = "maxGain";
    double maxGain;

    @S4Integer(defaultValue = 4)
    public final static String SMOOTH_WINDOW = "smoothWindow";
    int smoothWindow;

    private final static double EPS = 1.0e-10;

    public Denoise(double lambdaPower, double lambdaA, double lambdaB,
            double lambdaT, double muT,
            double maxGain, int smoothWindow) {
        this.lambdaPower = lambdaPower;
        this.lambdaA = lambdaA;
        this.lambdaB = lambdaB;
        this.lambdaT = lambdaT;
        this.muT = muT;
        this.maxGain = maxGain;
        this.smoothWindow = smoothWindow;
    }

    public Denoise() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * edu.cmu.sphinx.util.props.Configurable#newProperties(edu.cmu.sphinx.util
     * .props.PropertySheet)
     */
    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        lambdaPower = ps.getDouble(LAMBDA_POWER);
        lambdaA = ps.getDouble(LAMBDA_A);
        lambdaB = ps.getDouble(LAMBDA_B);
        lambdaT = ps.getDouble(LAMBDA_T);
        muT = ps.getDouble(MU_T);
        maxGain = ps.getDouble(MAX_GAIN);
        smoothWindow = ps.getInt(SMOOTH_WINDOW);
    }

    @Override
    public Data getData() throws DataProcessingException {
        Data inputData = getPredecessor().getData();
        int i;

        if (inputData instanceof DataStartSignal) {
            Arrays.fill(power, 0);
            Arrays.fill(noise, 0);
            Arrays.fill(floor, 0);
            Arrays.fill(peak, 0);
            return inputData;
        }
        if (!(inputData instanceof DoubleData)) {
            return inputData;
        }

        DoubleData inputDoubleData = (DoubleData) inputData;

        double[] input = inputDoubleData.getValues();
        int length = input.length;

        alloc(input);

        updatePower(input);

        estimateEnvelope(power, noise);

        for (i = 0; i < length; i++)
            signal[i] = max(power[i] - noise[i], 0.0);

        estimateEnvelope(signal, floor);

        tempMasking(signal);

        powerBoosting(signal);

        for (i = 0; i < length; i++)
            gain[i] = min(max(
                        signal[i] / (power[i] + EPS),
                            1.0 / maxGain),
                            maxGain);

        smooth(gain, input);

        return inputData;
    }


    private void alloc(double[] input) {
        int length = input.length;
        if (signal.length != /* < */ length) {
            signal = new double[length];
            gain = new double[length];
            floor = new double[length];
            peak = new double[length];
            power = new double[length];
            noise = new double[length];
        }
        arraycopy(input, 0, power, 0, length);
        arraycopy(input, 0, noise, 0, length);
        for (int i = 0; i < length; i++)
            floor[i] = input[i] / maxGain;
    }

    private void smooth(double[] gain, double[] target) {
        for (int i = 0; i < gain.length; i++) {
            int start = max(i - smoothWindow, 0);
            int end = min(i + smoothWindow + 1, gain.length);
            double sum = 0.0;
            for (int j = start; j < end; j++) {
                sum += gain[j];
            }
            double g = sum / (end - start); //sample gain
            target[i] *= g;
        }
    }

    private void powerBoosting(double[] signal) {
        for (int i = 0; i < signal.length; i++) {
            if (signal[i] < floor[i])
                signal[i] = floor[i];
        }
    }

    private void tempMasking(double[] signal) {
        for (int i = 0; i < signal.length; i++) {
            double in = signal[i];

            peak[i] *= lambdaT;
            if (signal[i] < lambdaT * peak[i])
                signal[i] = peak[i] * muT;

            if (in > peak[i])
                peak[i] = in;
        }
    }

    private void updatePower(double[] input) {
        for (int i = 0; i < input.length; i++) {
            power[i] = lambdaPower * power[i] + (1 - lambdaPower) * input[i];
        }
    }

    private void estimateEnvelope(double[] signal, double[] envelope) {
        for (int i = 0; i < signal.length; i++) {
            double si = signal[i];
            double ei = envelope[i];
            envelope[i] = (si > ei) ?
                    ((lambdaA * ei) + ((1 - lambdaA) * si)) :
                    ((lambdaB * ei) + ((1 - lambdaB) * si));
        }
    }

}
