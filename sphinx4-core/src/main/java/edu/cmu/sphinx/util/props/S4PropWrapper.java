package edu.cmu.sphinx.util.props;

import java.lang.annotation.Annotation;

/**
 * Wraps annotations
 *
 * @author Holger Brandl
 */
public class S4PropWrapper {

    public final Annotation annotation;


    S4PropWrapper(Annotation annotation) {
        this.annotation = annotation;
    }


}
