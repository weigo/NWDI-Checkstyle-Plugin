/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
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
     * instance under test.
     */
    private PathsBuilder pathsBuilder;

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

        final AntHelper antHelper = new AntHelper("/tmp", dcFactory, new ExcludesFactory());
        pathsBuilder = new PathsBuilder(domHelper, antHelper, components);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception {
        pathsBuilder = null;
    }

    /**
     * Test method for
     * {@link org.arachna.netweaver.nwdi.checkstyle.PathsBuilder#build()}.
     * 
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     * @throws TransformerConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XpathException
     */
    @Test
    public final void testBuild() throws XpathException, SAXException, IOException, TransformerConfigurationException,
        TransformerException, TransformerFactoryConfigurationError {
        final Element paths = pathsBuilder.build();
        assertThat(paths, is(not(equalTo(null))));
        final StringWriter result = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(paths), new StreamResult(result));
        System.err.println(result.toString());
        this.assertXpathEvaluatesTo("vendor.com~dc1~assembly", "/paths/path[1]/@id", result.toString());
        this.assertXpathEvaluatesTo("/tmp/.dtc/DCs/vendor.com/dc1/_comp/gen/default/public/assembly/lib/java",
            "/paths/path[1]/@location", result.toString());
    }
}
