package edu.cmu.sphinx.util.props;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */
public class ComponentListTests {


    @Test
    public void testInvalidList() {
        ConfigurationManager cm = new ConfigurationManager();

        Map<String, Object> props = new HashMap<>();
        cm.addConfigurable(DummyProcessor.class, "dummyA");
        props.put(DummyFrontEnd.DATA_PROCS, Collections.singletonList("dummyA, dummyB"));
        cm.addConfigurable(DummyFrontEnd.class, "dfe", props);

        cm.lookup("dfe");
    }

}
