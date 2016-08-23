/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.result;

import edu.cmu.sphinx.util.LogMath;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the posterior score computation code. Sets up a simple lattice, and
 * dumps out the posterior probabilities of each node.
 */
public class PosteriorTest {

	@Test
	public void testPosterior() {

	    
		Lattice lattice = new Lattice();

		Node a = lattice.addNode("A", "A", 0, 0);
		Node b = lattice.addNode("B", "B", 0, 0);
		Node c = lattice.addNode("C", "C", 0, 0);
		Node d = lattice.addNode("D", "D", 0, 0);

		double acousticAB = 4;
		double acousticAC = 6;
		double acousticCB = 1;
		double acousticBD = 5;
		double acousticCD = 2;

		lattice.setInitialNode(a);
		lattice.setTerminalNode(d);

		lattice.addEdge(a, b, LogMath.linearToLog(acousticAB), 0);
		lattice.addEdge(a, c, LogMath.linearToLog(acousticAC), 0);
		lattice.addEdge(c, b, LogMath.linearToLog(acousticCB), 0);
		lattice.addEdge(b, d, LogMath.linearToLog(acousticBD), 0);
		lattice.addEdge(c, d, LogMath.linearToLog(acousticCD), 0);

		lattice.computeNodePosteriors(1.0f);
		double pathABD = acousticAB * acousticBD;
		double pathACBD = acousticAC * acousticCB * acousticBD;
		double pathACD = acousticAC * acousticCD;
		double allPaths = pathABD + pathACBD + pathACD;

		double bPosterior = (pathABD + pathACBD) / allPaths;
		double cPosterior = (pathACBD + pathACD) / allPaths;

		double delta = 1e-4;
		Assert.assertEquals (LogMath.logToLinear((float) a.getPosterior()), 1.0, delta);
		Assert.assertEquals (LogMath.logToLinear((float) b.getPosterior()), bPosterior, delta);
		Assert.assertEquals (LogMath.logToLinear((float) c.getPosterior()), cPosterior, delta);
		Assert.assertEquals (LogMath.logToLinear((float) d.getPosterior()), 1.0, delta);
	}
}
