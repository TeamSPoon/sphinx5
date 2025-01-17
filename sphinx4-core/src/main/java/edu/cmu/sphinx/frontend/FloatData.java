/*
 * Copyright 1999-2002 Carnegie Mellon University.
 * Portions Copyright 2002 Sun Microsystems, Inc.
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package edu.cmu.sphinx.frontend;

import edu.cmu.sphinx.util.MatrixUtils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Data object that holds data of primitive type float.
 *
 * @see Data
 */
public class FloatData implements Data, Cloneable {

    public final float[] values;
    public final int sampleRate;
    private static final AtomicInteger serial = new AtomicInteger(0);


    /**
     * @return the position of the first sample in the original data. The very first sample number is zero.
     */
    public final long firstSampleNumber;

    /**
     * Returns the time in milliseconds at which the audio data is collected.
     *
     * @return the difference, in milliseconds, between the time the audio data is collected and midnight, January 1,
     *         1970
     */
    public final long collectTime;
    private final int hash;

    /**
     * Constructs a Data object with the given values, sample rate, collect time, and first sample number.
     *
     * @param values            the data values
     * @param sampleRate        the sample rate of the data
     * @param firstSampleNumber the position of the first sample in the original data
     */
    public FloatData(float[] values, int sampleRate, long firstSampleNumber) {
        this(values, sampleRate, firstSampleNumber * 1000 / sampleRate, firstSampleNumber);
    }

    /**
     * Constructs a Data object with the given values, sample rate, collect time, and first sample number.
     *
     * @param values            the data values
     * @param sampleRate        the sample rate of the data
     * @param collectTime       the time at which this data is collected
     * @param firstSampleNumber the position of the first sample in the original data
     */
    public FloatData(float[] values, int sampleRate,
                     long collectTime, long firstSampleNumber) {
        this.values = values;
        this.sampleRate = sampleRate;
        this.collectTime = collectTime;
        this.firstSampleNumber = firstSampleNumber;
        this.hash = serial.getAndIncrement();
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public FloatData clone() {
        try {
            FloatData data = (FloatData)super.clone();
            return data;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e.toString());
        }
    }
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof FloatData && Arrays.equals(values, ((FloatData) obj).values));
    }


    /** 
     * Converts a given Data-object into a <code>FloatData</code> if possible.
     * @param data data to convert
     * @return converted data
     */
    public static FloatData toFloatData(Data data) {
        FloatData convertData;
        if (data instanceof FloatData)
            convertData = (FloatData) data;
        else if (data instanceof DoubleData) {
            DoubleData dd = (DoubleData) data;
            convertData = new FloatData(MatrixUtils.double2float(dd.getValues()), dd.getSampleRate(),
                    dd.getFirstSampleNumber());
        } else
            throw new IllegalArgumentException("data type '" + data.getClass() + "' is not supported");

        return convertData;
    }
}
