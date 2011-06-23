/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.util.Collection;
import java.util.LinkedList;

import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.arachna.xml.DomHelper;
import org.w3c.dom.Element;

/**
 * @author Dirk Weigenand
 * 
 */
public final class CheckStyleTaskBuilder {
    /**
     * List of generated targets.
     */
    private final Collection<String> targets = new LinkedList<String>();

    /**
     * List of generated targets.
     */
    private final Collection<DevelopmentComponent> components = new LinkedList<DevelopmentComponent>();

    /**
     * helper for creating DOM elements.
     */
    private final DomHelper domHelper;

    /**
     * Generator for Ids used in build file.
     */
    private final IdGenerator idGenerator;

    public CheckStyleTaskBuilder(final DomHelper domHelper, final IdGenerator idGenerator,
        final Collection<DevelopmentComponent> components) {
        this.domHelper = domHelper;
        this.idGenerator = idGenerator;
        this.components.addAll(components);
    }

    /**
     * Build a list of checkstyle DOM elements to be used to create an ant build
     * file.
     * 
     * @param components
     *            list of development components to create checkstyle DOM
     *            elements for.
     * @return a list of DOM elements for creating checkstyle ant targets from.
     */
    public Element build() {
        final Element targets = domHelper.createElement("targets");

        for (final DevelopmentComponent component : components) {
            final String[] attributeNames = new String[] { "name" };
            final String[] attributeValues = new String[] { idGenerator.createId("checkstyle-", component) };

            final Element checkstyle = domHelper.createElement("checkstyle", attributeNames, attributeValues);

            checkstyle.appendChild(createSources(component));
            checkstyle.appendChild(createClassPath(component));

            targets.appendChild(checkstyle);
        }

        return targets;
    }

    /**
     * @param component
     * @return
     */
    protected Element createClassPath(final DevelopmentComponent component) {
        final Element classPath = domHelper.createElement("classpath");

        for (final PublicPartReference reference : component.getUsedDevelopmentComponents()) {
            classPath.appendChild(domHelper.createElement("path", "refid", idGenerator.createId(reference)));
        }
        return classPath;
    }

    /**
     * @param component
     * @return
     */
    protected Element createSources(final DevelopmentComponent component) {
        final Element sources = domHelper.createElement("sources");

        for (final String folder : component.getSourceFolders()) {
            sources.appendChild(domHelper.createElement("src", "folder", folder));
        }

        return sources;
    }
}
