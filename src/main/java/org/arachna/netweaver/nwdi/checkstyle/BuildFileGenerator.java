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

    private DomHelper domHelper = new DomHelper(null);

    private final AntHelper antHelper;

    public BuildFileGenerator(AntHelper antHelper, Collection<DevelopmentComponent> components) {
        this.antHelper = antHelper;
        this.components = components;
    }

    public void write(Writer target) {
        try {
            this.transform(this.createBuildTemplate(), target);
        }
        catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected Document createBuildTemplate() throws ParserConfigurationException {
        this.domHelper = createDomHelper();
        this.domHelper.getDocument().appendChild(new PathsBuilder(domHelper, antHelper, components).build());

        return this.domHelper.getDocument();
    }

    /**
     * Create and configure the transformer object to use generating the report.
     * Throws a <code>RuntimeException</code> when there is an error creating
     * the transformer.
     *
     * @return the transformer to use for generating the report.
     */
    private Transformer getTransformer() {
        final StreamSource source = new StreamSource(this.getClass().getResourceAsStream(""));
        final TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = null;

        try {
            transformer = factory.newTransformer(source);
            transformer.setOutputProperty("method", "html");
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

    protected void transform(Document document, Writer writer) throws TransformerException {
        this.getTransformer().transform(new DOMSource(document), new StreamResult(writer));
    }

}
