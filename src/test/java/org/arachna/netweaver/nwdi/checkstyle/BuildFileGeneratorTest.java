/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import hudson.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

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
     * Instance under test.
     */
    private BuildFileGenerator generator;
    private DevelopmentComponentFactory dcFactory;
    private File workspace;
    private AntHelper antHelper;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        dcFactory = new DevelopmentComponentFactory();
        workspace = Util.createTempDir();
        antHelper = new AntHelper(workspace.getAbsolutePath(), dcFactory);

        final Collection<DevelopmentComponent> components = new LinkedList<DevelopmentComponent>();
        final DevelopmentComponent dc1 =
            dcFactory.create("vendor.com", "dc1", new PublicPart[] {
                new PublicPart("api", "", "", PublicPartType.COMPILE),
                new PublicPart("assembly", "", "", PublicPartType.ASSEMBLY) }, new PublicPartReference[] {});
        dc1.addSourceFolder("src/packages");
        dc1.addSourceFolder("test/packages");
        dc1.setOutputFolder("classes");
        components.add(dc1);

        final PublicPartReference api = new PublicPartReference("vendor.com", "dc1", "api");
        api.setAtBuildTime(true);
        final PublicPartReference assembly = new PublicPartReference("vendor.com", "dc1", "assembly");
        assembly.setAtBuildTime(true);

        final DevelopmentComponent dc2 =
            dcFactory.create("vendor.com", "dc2", new PublicPart[] { new PublicPart("defLib", "", "",
                PublicPartType.COMPILE) }, new PublicPartReference[] { api, assembly });
        dc2.addSourceFolder("src/packages");
        components.add(dc2);

        final PublicPartReference defLib = new PublicPartReference("vendor.com", "dc1", "defLib");
        defLib.setAtRunTime(true);

        final DevelopmentComponent dc3 =
            dcFactory.create("vendor.com", "dc3", new PublicPart[] {}, new PublicPartReference[] { defLib });
        dc3.addSourceFolder("src/packages");
        components.add(dc3);

        generator =
            new BuildFileGenerator(new VelocityEngine(), new PrintStream(new ByteArrayOutputStream()), antHelper, "",
                new HashSet<String>(), new HashSet<String>());
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        generator = null;
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)}
     * .
     */
    @Test
    public final void testProjectHasBeenCreated() throws XpathException, SAXException, IOException {
        assertXpathEvaluatesTo("1", "count(/project)");
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)}
     * .
     */
    @Test
    public final void testClassPathWasCreated() throws XpathException, SAXException, IOException {
        assertXpathEvaluatesTo("1", "count(/project/path[@id='classpath'])");
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)}
     * .
     */
    @Test
    public final void testClassPathContainsOutputFolder() throws XpathException, SAXException, IOException {
        final DevelopmentComponent component = dcFactory.get("vendor.com", "dc1");

        assertXpathEvaluatesTo("1",
            String.format("count(/project//checkstyle[@classpath='%s'])", component.getOutputFolder()));
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)}
     * .
     */
    @Test
    public final void testDefaultTarget() throws XpathException, SAXException {
        assertXpathEvaluatesTo("1", "count(/project/target[@name='checkstyle-vendor.com~dc1'])");
    }

    private void assertXpathEvaluatesTo(String expected, String xPath) {
        try {
            assertXpathEvaluatesTo(expected, xPath, createBuildFile());
        }
        catch (XpathException e) {
            fail(e.getMessage());
        }
        catch (SAXException e) {
            fail(e.getMessage());
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
    }

    /**
     * @return
     * @throws IOException
     */
    protected String createBuildFile() {
        final DevelopmentComponent component = dcFactory.get("vendor.com", "dc1");
        Context context = generator.createContext(component, component.getSourceFolders());
        StringWriter content = new StringWriter();

        try {
            generator.evaluateContext(content, context);
        }
        catch (IOException e) {
            fail(e.getMessage());
        }

        return content.toString();
    }
}
