/**
 * Copyright 2014 Alpha Cephei Inc.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.alignment;

import edu.cmu.sphinx.util.Utilities;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class SpeechAlignerTest {

    @Test
    public void shouldAlignText() {
        align(Collections.singletonList("foo"), Collections.singletonList("bar"), -1);
        align(Collections.singletonList("foo"), Collections.singletonList("foo"), 0);
        align(asList("foo", "bar"), Collections.singletonList("foo"), 0);
        align(asList("foo", "bar"), Collections.singletonList("bar"), 1);
        align(Collections.singletonList("foo"), asList("foo", "bar"), 0, -1);
        align(Collections.singletonList("bar"), asList("foo", "bar"), -1, 0);
        align(asList("foo", "bar", "baz"), asList("foo", "baz"), 0, 2);
        align(asList("foo", "bar", "42", "baz", "qux"), asList("42", "baz"), 2,
                3);
    }

    private static void align(List<String> database, List<String> query,
                              Integer... result) {
        LongTextAligner aligner = new LongTextAligner(database, 1);
        int[] alignment = aligner.align(query);

        assertThat(Utilities.asList(alignment), contains(result));
    }
}
