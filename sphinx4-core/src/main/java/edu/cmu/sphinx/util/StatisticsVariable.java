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

package edu.cmu.sphinx.util;

import java.util.HashMap;

/**
 * Represents a named value. A StatisticsVariable may be used to track data in a fashion that will allow the data to be
 * viewed or dumped at any time.  Statistics are kept in a pool and are grouped in contexts. Statistics can be dumped
 * as a whole or by context.
 */
public class StatisticsVariable {

    private static final HashMap<String, StatisticsVariable> pool = new HashMap<>();

    /** the value of this StatisticsVariable. It can be manipulated directly by the application. */
    public double value;

    public final String name;        // the name of this value
    private boolean enabled;        // if true this var is enabled


    /**
     * Gets the StatisticsVariable with the given name from the given context. If the statistic does not currently
     * exist, it is created. If the context does not currently exist, it is created.
     *
     * @param statName the name of the StatisticsVariable
     * @return the StatisticsVariable with the given name and context
     */
    static public final StatisticsVariable the(String statName) {
        return pool.computeIfAbsent(statName, StatisticsVariable::new);
    }


    /**
     * Gets the StatisticsVariable with the given name for the given instance and context. This is a convenience
     * function.
     *
     * @param instanceName the instance name of creator
     * @param statName     the name of the StatisticsVariable
     * @return new variable
     */
    static public StatisticsVariable the(String instanceName, String statName) {
        return the(instanceName + '.' + statName);
    }


    /** Dump all of the StatisticsVariable in the given context */
    static public void dumpAll() {
        System.out.println(" ========= statistics  " + "=======");
        for (StatisticsVariable stats : pool.values()) {
            stats.dump();
        }
    }


    /** Resets all of the StatisticsVariables in the given context */
    static public void resetAll() {
        pool.values().forEach(StatisticsVariable::reset);
    }


    /**
     * Contructs a StatisticsVariable with the given name and context
     *
     * @param statName the name of this StatisticsVariable
     */
    private StatisticsVariable(String statName) {
        this.name = statName;
        this.value = 0.0;
    }


    /**
     * Retrieves the value for this StatisticsVariable
     *
     * @return the current value for this StatisticsVariable
     */
    public double val() {
        return value;
    }


    /**
     * Sets the value for this StatisticsVariable
     *
     * @param value the new value
     */
    public void val(double value) {
        this.value = value;
    }


    /** Resets this StatisticsVariable. The value is set to zero. */
    public void reset() {
        val(0.0);
    }


    /** Dumps this StatisticsVariable. */
    public void dump() {
        if (enabled) {
            System.out.println(name + ' ' + value);
        }
    }

//
//    /**
//     * Determines if this StatisticsVariable is enabled
//     *
//     * @return true if enabled
//     */
//    public boolean isEnabled() {
//        return enabled;
//    }
//
//
//    /**
//     * Sets the enabled state of this StatisticsVariable
//     *
//     * @param enabled the new enabled state
//     */
//    public void setEnabled(boolean enabled) {
//        this.enabled = enabled;
//    }


//    public static void main(String[] args) {
//        StatisticsVariable loops =
//                StatisticsVariable.the("main", "loops");
//        StatisticsVariable sum =
//                StatisticsVariable.the("main", "sum");
//
//        StatisticsVariable foot =
//                StatisticsVariable.the("body", "foot");
//        StatisticsVariable leg =
//                StatisticsVariable.the("body", "leg");
//        StatisticsVariable finger =
//                StatisticsVariable.the("body", "finger");
//
//        foot.val(2);
//        leg.val(2);
//        finger.val(10);
//
//        StatisticsVariable.dumpAll();
//        StatisticsVariable.dumpAll();
//
//        for (int i = 0; i < 1000; i++) {
//            loops.value++;
//            sum.value += i;
//        }
//
//        StatisticsVariable.dumpAll();
//
//
//        StatisticsVariable loopsAlias =
//                StatisticsVariable.the("main", "loops");
//        StatisticsVariable sumAlias =
//                StatisticsVariable.the("main", "sum");
//
//        for (int i = 0; i < 1000; i++) {
//            loopsAlias.value++;
//            sumAlias.value += i;
//        }
//
//        StatisticsVariable.dumpAll();
//        StatisticsVariable.resetAll();
//        StatisticsVariable.dumpAll();
//    }
}
