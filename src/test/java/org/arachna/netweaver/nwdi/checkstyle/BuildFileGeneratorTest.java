/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.PublicPart;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.arachna.netweaver.dc.types.PublicPartType;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * @author Dirk Weigenand
 * 
 */
public class BuildFileGeneratorTest extends XMLTestCase {
    /**
     * public part name 'assembly'.
     */
    private static final String ASSEMBLY = "assembly";

    /**
     * public part name 'api'.
     */
    private static final String API = "api";

    /**
     * development component name.
     */
    private static final String DC1 = "dc1";

    /**
     * development component vendor.
     */
    private static final String VENDOR = "vendor.com";

    /**
     * Instance under test.
     */
    private BuildFileGenerator generator;

    /**
     * Registry for development components.
     */
    private DevelopmentComponentFactory dcFactory;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        dcFactory = new DevelopmentComponentFactory();
        final AntHelper antHelper = new AntHelper("/jenkins", dcFactory);

        final DevelopmentComponent dc1 =
            dcFactory.create(VENDOR, DC1, new PublicPart[] { new PublicPart(API, "", "", PublicPartType.COMPILE),
                new PublicPart(ASSEMBLY, "", "", PublicPartType.ASSEMBLY) }, new PublicPartReference[] {});
        dc1.addSourceFolder("src/packages");
        dc1.addTestSourceFolder("test/packages");
        dc1.setOutputFolder("classes");

        final PublicPartReference api = new PublicPartReference(VENDOR, DC1, API);
        api.setAtBuildTime(true);
        final PublicPartReference assembly = new PublicPartReference(VENDOR, DC1, ASSEMBLY);
        assembly.setAtBuildTime(true);

        // final DevelopmentComponent dc2 =
        // dcFactory.create(VENDOR, "dc2", new PublicPart[] { new PublicPart(DEF_LIB, "", "", PublicPartType.COMPILE) },
        // new PublicPartReference[] { api, assembly });
        // dc2.addSourceFolder("src/packages");
        //
        // final PublicPartReference defLib = new PublicPartReference(VENDOR, DC1, DEF_LIB);
        // defLib.setAtRunTime(true);
        //
        // final DevelopmentComponent dc3 = dcFactory.create(VENDOR, "dc3", new PublicPart[] {}, new PublicPartReference[] { defLib });
        // dc3.addSourceFolder("src/packages");

        generator = new BuildFileGenerator(new VelocityEngine(), antHelper, "", new HashSet<String>(), new HashSet<String>());
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() {
        generator = null;
    }

    /**
     * Test method for {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)} .
     */
    @Test
    public final void testProjectHasBeenCreated() {
        assertXpathEvaluatesTo("1", "count(/project)");
    }

    /**
     * Test method for {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)} .
     */
    @Test
    public final void testClassPathWasCreated() {
        assertXpathEvaluatesTo("1", "count(/project/path[@id='classpath'])");
    }

    /**
     * Test method for {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)} .
     */
    @Test
    public final void testClassPathContainsOutputFolder() {
        final DevelopmentComponent component = dcFactory.get(VENDOR, DC1);

        assertXpathEvaluatesTo("1", String.format("count(/project//checkstyle[@classpath='%s'])", component.getOutputFolder()));
    }

    /**
     * Test method for {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)} .
     */
    @Test
    public final void testDefaultTarget() {
        assertXpathEvaluatesTo("1", "count(/project/target[@name='checkstyle-vendor.com~dc1'])");
    }

    private void assertXpathEvaluatesTo(final String expected, final String xPath) {
        try {
            assertXpathEvaluatesTo(expected, xPath, createBuildFile());
        }
        catch (final XpathException e) {
            fail(e.getMessage());
        }
        catch (final SAXException e) {
            fail(e.getMessage());
        }
        catch (final IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * @return
     * @throws IOException
     */
    protected String createBuildFile() {
        final DevelopmentComponent component = dcFactory.get(VENDOR, DC1);
        final Context context = generator.createContext(component, component.getSourceFolders());
        final StringWriter content = new StringWriter();

        generator.evaluateContext(content, context);

        System.err.println(content.toString());
        return content.toString();
    }
}
