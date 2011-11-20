/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import hudson.Util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.apache.velocity.app.VelocityEngine;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.PublicPart;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Node;
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
            dcFactory.create("vendor.com", "dc1", new PublicPart[] { new PublicPart("api", "", ""),
                new PublicPart("assembly", "", "") }, new PublicPartReference[] {});
        dc1.addSourceFolder(antHelper.getBaseLocation(dc1) + "/src/packages");
        components.add(dc1);

        final PublicPartReference api = new PublicPartReference("vendor.com", "dc1", "api");
        api.setAtBuildTime(true);
        final PublicPartReference assembly = new PublicPartReference("vendor.com", "dc1", "assembly");
        assembly.setAtBuildTime(true);

        final DevelopmentComponent dc2 =
            dcFactory.create("vendor.com", "dc2", new PublicPart[] { new PublicPart("defLib", "", "") },
                new PublicPartReference[] { api, assembly });
        dc2.addSourceFolder(antHelper.getBaseLocation(dc2) + "/src/packages");
        components.add(dc2);

        final PublicPartReference defLib = new PublicPartReference("vendor.com", "dc1", "defLib");
        defLib.setAtRunTime(true);

        final DevelopmentComponent dc3 =
            dcFactory.create("vendor.com", "dc3", new PublicPart[] {}, new PublicPartReference[] { defLib });
        dc3.addSourceFolder(antHelper.getBaseLocation(dc3) + "/src/packages");
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
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testProjectHasBeenCreated() throws XpathException, SAXException, IOException {
        final String result = createBuildFile();
        assertXpathEvaluatesTo("1", "count(/project[@default='all'])", result);
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)}
     * .
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testCreatedPaths() throws XpathException, SAXException, IOException {
        final String result = createBuildFile();
        assertXpathEvaluatesTo("2", "count(/project/path)", result);
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.BuildFileGenerator#write(java.io.Writer)}
     * .
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testDefaultTarget() throws XpathException, SAXException, IOException {
        final String result = createBuildFile();
        assertXpathEvaluatesTo("1", "count(/project/target[@name='all'])", result);
    }

    /**
     * @return
     * @throws IOException
     */
    protected String createBuildFile() throws IOException {
        final DevelopmentComponent component = dcFactory.get("vendor.com", "dc1");
        generator.execute(component);

        return Util.loadFile(new File(String.format(BuildFileGenerator.BUILD_XML_PATH_TEMPLATE,
            antHelper.getBaseLocation(component))));
    }

    /**
     * @param node
     */
    protected void assertNodeIsNotNullAndHasGivenElementName(final Node node, final String name) {
        assertThat(node, is(not(equalTo(null))));
        assertThat(node.getNodeName(), is(equalTo(name)));
    }
}
