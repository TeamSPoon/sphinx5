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

package edu.cmu.sphinx.util;

/**
 * Provides a set of methods for performing simple math in the log domain.
 *
 * The logarithmic base can be set by the
 * property: <code>edu.cmu.sphinx.util.LogMath.logBase</code>
 */
public enum LogMath { ;

    public static final float LOG_ZERO = -Float.MAX_VALUE;
    public static final float LOG_ONE = 0.f;



    private static final float logBase = 1.0001f;
    //private static boolean useTable = false;

    private static final double naturalLogBase = Math.log(logBase);
    private static final double inverseNaturalLogBase = 1.0 / naturalLogBase;

    //private float theAddTable[];

    LogMath() {

//        if (useTable) {
//            // Now create the addTable table.
//            // summation needed in the loop
//            float innerSummation;
//            // First decide number of elements.
//            int entriesInTheAddTable;
//            final int veryLargeNumberOfEntries = 150000;
//            final int verySmallNumberOfEntries = 0;
//            // To decide size of table, take into account that a base
//            // of 1.0001 or 1.0003 converts probabilities, which are
//            // numbers less than 1, into integers. Therefore, a good
//            // approximation for the smallest number in the table,
//            // therefore the value with the highest index, is an
//            // index that maps into 0.5: indices higher than that, if
//            // they were present, would map to less values less than
//            // 0.5, therefore they would be mapped to 0 as
//            // integers. Since the table implements the expression:
//            //
//            // log(1.0 + base^(-index)))
//            //
//            // then the highest index would be:
//            //
//            // topIndex = - log(logBase^(0.5) - 1)
//            //
//            // where log is the log in the appropriate base.
//            //
//            // Added -Math.rint(...) to round to nearest
//            // integer. Added the negation to match the preceding
//            // documentation
//            entriesInTheAddTable = (int) -Math
//                .rint(linearToLog(logToLinear(0.5f) - 1));
//            // We reach this max if the log base is 1.00007. The
//            // closer you get to 1, the higher the number of entries
//            // in the table.
//            if (entriesInTheAddTable > veryLargeNumberOfEntries) {
//                entriesInTheAddTable = veryLargeNumberOfEntries;
//            }
//            if (entriesInTheAddTable <= verySmallNumberOfEntries) {
//                throw new IllegalArgumentException("The log base " + logBase
//                        + " yields a very small addTable. "
//                        + "Either choose not to use the addTable, "
//                        + "or choose a logBase closer to 1.0");
//            }
//            // PBL added this just to see how many entries really are
//            // in the table
//            theAddTable = new float[entriesInTheAddTable];
//            for (int index = 0; index < entriesInTheAddTable; ++index) {
//                // This loop implements the expression:
//                //
//                // log( 1.0 + power(base, index))
//                //
//                // needed to add two numbers in the log domain.
//                innerSummation = (float) logToLinear(-index);
//                innerSummation += 1.0f;
//                theAddTable[index] = linearToLog(innerSummation);
//            }
//        }
    }


    /**
     * Sets log base.
     * <p>
     * According to forum discussions a value between 1.00001 and 1.0004 should
     * be used for speech recognition. Going above 1.0005 will probably hurt.
     *
     * @param logBase Log base
     */
//    public static void setLogBase(float logBase) {
//        synchronized(LogMath.class) {
//            assert instance == null;
//            LogMath.logBase = logBase;
//        }
//    }

//    /**
//     * The property that controls whether we use the old, slow (but correct)
//     * method of performing the LogMath.add by doing the actual computation.
//     * @param useTable to configure table lookups
//     */
//    public static void setUseTable(boolean useTable) {
//        synchronized(LogMath.class) {
//            assert instance == null;
//            LogMath.useTable = useTable;
//        }
//    }

    /**
     * Returns the summation of two numbers when the arguments and the result are in log. <p>  That is, it returns
     * log(a + b) given log(a) and log(b) </p> <p>  This method makes use of the equality: </p> <p>  <b>log(a
     * + b) = log(a) + log (1 + exp(log(b) - log(a))) </b> </p> <p>  which is derived from: </p> <p>  <b>a + b
     * = a * (1 + (b / a)) </b> </p> <p>  which in turns makes use of: </p> <p>  <b>b / a = exp (log(b) -
     * log(a)) </b> </p> <p>  Important to notice that <code>subtractAsLinear(a, b)</code> is *not* the same as
     * <code>addAsLinear(a, -b)</code>, since we're in the log domain, and -b is in fact the inverse. </p> <p>  No
     * underflow/overflow check is performed. </p>
     *
     * @param logVal1 value in log domain (i.e. log(val1)) to add
     * @param logVal2 value in log domain (i.e. log(val2)) to add
     * @return sum of val1 and val2 in the log domain
     */
    public static float addAsLinear(double logVal1, double logVal2) {
        double logHighestValue = logVal1;
        double logDifference = logVal1 - logVal2;
        /*
         * [ EBG: maybe we should also have a function to add many numbers, *
         * say, return the summation of all terms in a given vector, if *
         * efficiency becomes an issue.
         */
        // difference is always a positive number
        if (logDifference < 0) {
            logHighestValue = logVal2;
            logDifference = -logDifference;
        }
        return (float) (logHighestValue + addTable(logDifference));
    }

    /**
     * Method used by add() internally. It returns the difference between the highest number and the total summation of
     * two numbers. <p> Considering the expression (in which we assume natural log) <p>  <b>log(a + b) = log(a) +
     * log(1 + exp(log(b) - log(a))) </b> </p>
     * <p>
     * the current function returns the second term of the right hand side of the equality above, generalized for the
     * case of any log base. This function can be constructed as a table, if table lookup is faster than actual
     * computation.
     *
     * @param index the index into the addTable
     * @return the value pointed to by index
     */
    @SuppressWarnings("unused")
    private static double addTableActualComputation(double index) {
        double logInnerSummation;
        // Negate index, since the derivation of this formula implies
        // the smallest number as a numerator, therefore the log of the
        // ratio is negative
        logInnerSummation = logToLinear(-index);
        logInnerSummation += 1.0;
        return linearToLog(logInnerSummation);
    }

//    /**
//     * Method used by add() internally. It returns the difference between the highest number and the total summation of
//     * two numbers. <p> Considering the expression (in which we assume natural log) <p>  <b>log(a + b) = log(a) +
//     * log(1 + exp(log(b) - log(a))) </b> </p>
//     * <p>
//     * the current function returns the second term of the right hand side of the equality above, generalized for the
//     * case of any log base. This function is constructed as a table lookup.
//     *
//     * @param index the index into the addTable
//     * @return the value pointed to by index
//     * @throws IllegalArgumentException
//     */
//    private float addTable(float index) throws IllegalArgumentException {
//            // int intIndex = (int) Math.rint(index);
//            int intIndex = (int) (index + 0.5);
//            // When adding two numbers, the highest one should be
//            // preserved, and therefore the difference should always
//            // be positive.
//            if (intIndex < theAddTable.length) {
//                return theAddTable[intIndex];
//            } else {
//                return 0.0f;
//            }
//
//
//    }

    static double addTable(double index) {
        double innerSummation = logToLinear(-index);
        innerSummation += 1.0f;
        return linearToLog(innerSummation);
    }

    /**
     * Returns the difference between two numbers when the arguments and the result are in log. <p>  That is, it
     * returns log(a - b) given log(a) and log(b) </p> <p>  Implementation is less efficient than add(), since
     * we're less likely to use this function, provided for completeness. Notice however that the result only makes
     * sense if the minuend is higher than the subtrahend. Otherwise, we should return the log of a negative number.
     * </p> <p>  It implements the subtraction as: </p> <p>  <b>log(a - b) = log(a) + log(1 - exp(log(b) -
     * log(a))) </b> </p> <p>  No need to check for underflow/overflow. </p>
     *
     * @param logMinuend    value in log domain (i.e. log(minuend)) to be subtracted from
     * @param logSubtrahend value in log domain (i.e. log(subtrahend)) that is being subtracted
     * @return difference between minuend and the subtrahend in the log domain
     * @throws IllegalArgumentException <p> This is a very slow way to do this, but this method should rarely be used.
     *                                  </p>
     */
    public static float subtractAsLinear(double logMinuend, double logSubtrahend)
            throws IllegalArgumentException {
        double logInnerSummation;
        if (logMinuend < logSubtrahend) {
            throw new IllegalArgumentException("Subtraction results in log "
                    + "of a negative number: " + logMinuend + " - "
                    + logSubtrahend);
        }
        logInnerSummation = 1.0;
        logInnerSummation -= logToLinear(logSubtrahend - logMinuend);
        return (float) (logMinuend + linearToLog(logInnerSummation));
    }

    /**
     * Converts the source, which is assumed to be a log value whose base is sourceBase, to a log value whose base is
     * resultBase. Possible values for both the source and result bases include Math.E, 10.0, LogMath.getLogBase(). If a
     * source or result base is not supported, an IllegalArgumentException will be thrown. <p>  It takes advantage
     * of the relation: </p> <p>  <b>log_a(b) = log_c(b) / lob_c(a) </b> </p> <p>  or: </p> <p> 
     * <b>log_a(b) = log_c(b) * lob_a(c) </b> </p> <p>  where <b>log_a(b) </b> is logarithm of <b>b </b> base <b>a
     * </b> etc. </p>
     *
     * @param logSource  log value whose base is sourceBase
     * @param sourceBase the base of the log the source
     * @param resultBase the base to convert the source log to
     * @return converted value
     * @throws IllegalArgumentException if arguments out of bounds
     */
    public static double logToLog(double logSource, double sourceBase,
                                 double resultBase) throws IllegalArgumentException {
        //  TODO: This is slow, but it probably doesn't need
	//  to be too fast.
        // It can be made more efficient if one of the bases is
        // Math.E. So maybe we should consider two functions logToLn and
        // lnToLog instead of a generic function like this??
        double lnSourceBase = Math.log(sourceBase);
        double lnResultBase = Math.log(resultBase);
        return (logSource * lnSourceBase / lnResultBase);
    }

    /**
     * Converts the source, which is a number in base Math.E, to a log value which base is the LogBase of this LogMath.
     *
     * @return converted value
     * @param logSource the number in base Math.E to convert
     */
    public static final float lnToLog(float logSource) {
        return logSource * (float)inverseNaturalLogBase;
    }
    public static final double lnToLog(double logSource) {
        return (logSource * inverseNaturalLogBase);
    }

    /**
     * Converts the source, which is a number in base 10, to a log value which base is the LogBase of this LogMath.
     *
     * @return converted value
     * @param logSource the number in base Math.E to convert
     */
    public static float log10ToLog(double logSource) {
        return (float) logToLog(logSource, 10.0, logBase);
    }

    /**
     * Converts the source, whose base is the LogBase of this LogMath, to a log value which is a number in base Math.E.
     *
     * @param logSource the number to convert to base Math.E
     * @return converted value
     */
    public static final double logToLn(double logSource) {
        return logSource * naturalLogBase;
    }

    /**
     * Converts the value from linear scale to log scale. The log scale numbers are limited by the range of the type
     * float. The linear scale numbers can be any double value.
     *
     * @param linearValue the value to be converted to log scale
     * @return the value in log scale
     * @throws IllegalArgumentException if value out of range
     */
    public static float linearToLog(double linearValue) throws IllegalArgumentException {
       return (float)(Math.log(linearValue) * inverseNaturalLogBase);
    }
//    public static float linearToLog(float linearValue) throws IllegalArgumentException {
//        return (float)(Math.log(linearValue) * inverseNaturalLogBase);
//    }

    /**
     * Converts the value from log scale to linear scale.
     *
     * @param logValue the value to be converted to the linear scale
     * @return the value in the linear scale
     */
    public static final double logToLinear(double logValue) {
        return Math.exp(logToLn(logValue));
    }

    /** @return the actual log base. 
     */
    public static float logBase() {
        return logBase;
    }

//    public static boolean isUseTable() {
//        return useTable;
//    }

    /**
     * Returns the log (base 10) of value
     *
     * @param value the value to take the log of
     * @return the log (base 10) of value
     */
    // [ EBG: Shouldn't we be using something like logToLog(value, base, 10)
    // for this? ]
    public static double log10(double value) {
        return (0.4342944819 * java.lang.Math.log(value));
        // If you want to get rid of the constant:
        // return ((1.0f / Math.log(10.0f)) * Math.log(value));
    }

    /** Converts a vector from linear domain to log domain using a given <code>LogMath</code>-instance for conversion. 
     * @param vector to convert in-place
     */
    public static void linearToLog(float[] vector) {
        int nbGaussians = vector.length;
        for (int i = 0; i < nbGaussians; i++) {
            vector[i] = linearToLog(vector[i]);
        }
    }


    /** Converts a vector from log to linear domain using a given <code>LogMath</code>-instance for conversion. 
     * @param vector to convert
     * @param out result
     */
    public static void logToLinear(float[] vector, float[] out) {
        for (int i = 0; i < vector.length; i++) {
            out[i] = (float)logToLinear(vector[i]);
        }
    }
}
