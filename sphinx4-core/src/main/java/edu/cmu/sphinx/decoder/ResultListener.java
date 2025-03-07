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

package edu.cmu.sphinx.decoder;

import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.Configurable;

import java.util.EventListener;
import java.util.function.Consumer;

/** The listener interface for being informed when new results are generated. */
public interface ResultListener extends EventListener, Configurable, Consumer<Result> {

}

