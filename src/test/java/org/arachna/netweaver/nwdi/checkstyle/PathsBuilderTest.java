/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludesFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.DevelopmentComponentFactory;
import org.arachna.netweaver.dc.types.PublicPart;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.arachna.xml.DomHelper;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * @author Dirk Weigenand
 * 
 */
public class PathsBuilderTest extends XMLTestCase {
    /**
     * XML for the paths.
     */
    private String paths;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        final DomHelper domHelper =
            new DomHelper(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
        final DevelopmentComponentFactory dcFactory = new DevelopmentComponentFactory();
        final Collection<DevelopmentComponent> components = new LinkedList<DevelopmentComponent>();
        final DevelopmentComponent dc1 =
            dcFactory.create("vendor.com", "dc1", new PublicPart[] { new PublicPart("api", "", ""),
                new PublicPart("assembly", "", "") }, new PublicPartReference[] {});
        components.add(dc1);

        final PublicPartReference api = new PublicPartReference("vendor.com", "dc1", "api");
        api.setAtBuildTime(true);
        final PublicPartReference assembly = new PublicPartReference("vendor.com", "dc1", "assembly");
        assembly.setAtBuildTime(true);

        final DevelopmentComponent dc2 =
            dcFactory.create("vendor.com", "dc2", new PublicPart[] { new PublicPart("defLib", "", "") },
                new PublicPartReference[] { api, assembly });
        components.add(dc2);

        final PublicPartReference defLib = new PublicPartReference("vendor.com", "dc1", "defLib");
        defLib.setAtRunTime(true);

        final DevelopmentComponent dc3 =
            dcFactory.create("vendor.com", "dc3", new PublicPart[] {}, new PublicPartReference[] { defLib });
        components.add(dc3);

        final AntHelper antHelper = new AntHelper("/tmp", dcFactory, new ExcludesFactory());
        final PathsBuilder pathsBuilder = new PathsBuilder(domHelper, antHelper, components);
        final Element paths = pathsBuilder.build();
        final StringWriter result = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(paths), new StreamResult(result));
        this.paths = result.toString();
        System.err.println(this.paths);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        paths = null;
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.PathsBuilder#build()}.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testGeneratePathId() throws XpathException, SAXException, IOException {
        this.assertXpathEvaluatesTo("vendor.com~dc1~assembly", "/paths/path[1]/@id", paths);
        this.assertXpathEvaluatesTo("vendor.com~dc1~api", "/paths/path[2]/@id", paths);
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.PathsBuilder#build()}.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testGeneratedPathsCount() throws XpathException, SAXException, IOException {
        this.assertXpathEvaluatesTo("2", "count(/paths/path)", paths);
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.PathsBuilder#build()}.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testGenerateLocation() throws XpathException, SAXException, IOException {
        this.assertXpathEvaluatesTo("/tmp/.dtc/DCs/vendor.com/dc1/_comp/gen/default/public/assembly/lib/java",
            "/paths/path[1]/@location", paths);
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.PathsBuilder#build()}.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testDoNotGeneratePathForRuntimeReferences() throws XpathException, SAXException, IOException {
        this.assertXpathEvaluatesTo("0", "count(/paths/path[@id = 'vendor.com~dc3~defLib'])", paths);
    }
}
