/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.Writer;
import java.util.Collection;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.xml.DomHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generator for an Ant build file.
 * 
 * @author Dirk Weigenand
 */
public final class BuildFileGenerator {
    /**
     * List of DCs that should be scrutinized with CheckStyle.
     */
    private Collection<DevelopmentComponent> components = new LinkedList<DevelopmentComponent>();

    private DomHelper domHelper;

    private final AntHelper antHelper;

    public BuildFileGenerator(final AntHelper antHelper, final Collection<DevelopmentComponent> components) {
        this.antHelper = antHelper;
        this.components = components;
    }

    public void write(final Writer target) {
        try {
            transform(createBuildTemplate(), target);
        }
        catch (final TransformerException e) {
            throw new RuntimeException(e);
        }
        catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Document createBuildTemplate() throws ParserConfigurationException {
        domHelper = createDomHelper();
        final Document document = domHelper.getDocument();
        final Element project = domHelper.createElement("project");
        project.appendChild(new PathsBuilder(domHelper, antHelper, components).build());
        project.appendChild(new CheckStyleTaskBuilder(domHelper, new IdGenerator(), components).build());
        document.appendChild(project);

        return document;
    }

    /**
     * Create and configure the transformer object to use generating the report.
     * Throws a <code>RuntimeException</code> when there is an error creating
     * the transformer.
     * 
     * @return the transformer to use for generating the report.
     */
    private Transformer getTransformer() {
        final StreamSource source =
            new StreamSource(this.getClass().getResourceAsStream(
                "/org/arachna/netweaver/nwdi/checkstyle/CheckstyleBuilder/checkstyle-project.xsl"));
        final TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = null;

        try {
            transformer = factory.newTransformer(source);
            transformer.setOutputProperty("method", "xml");
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("encoding", "UTF-8");
        }
        catch (final TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        return transformer;
    }

    /**
     * @return
     * @throws ParserConfigurationException
     */
    protected DomHelper createDomHelper() throws ParserConfigurationException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        return new DomHelper(builder.newDocument());
    }

    protected void transform(final Document document, final Writer writer) throws TransformerException {
        getTransformer().transform(new DOMSource(document), new StreamResult(writer));
    }

}
