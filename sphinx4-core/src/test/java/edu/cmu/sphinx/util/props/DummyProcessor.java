package edu.cmu.sphinx.util.props;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class DummyProcessor implements DummyFrontEndProcessor {


    public void newProperties(PropertySheet ps) throws PropertyException {
    }


    public String getName() {
        return this.getClass().getName();
    }
}
