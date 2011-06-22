/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.PublicPartReference;
import org.arachna.xml.DomHelper;
import org.w3c.dom.Element;

/**
 * Build <code>&lt;paths&gt;&lt;path/&gt;&lt;/paths&gt;</code> DOM
 * representation from a set of {@link DevelopmentComponent} and their used DCs.
 * 
 * @author Dirk Weigenand
 */
public final class PathsBuilder {
    /**
     * helper dealing with DOM.
     */
    private final DomHelper domHelper;

    /**
     * development components to create paths of their used DCs.
     */
    private final Collection<DevelopmentComponent> components;

    /**
     * helper for creating ant build scripts.
     */
    private final AntHelper antHelper;

    /**
     * Builder for paths in an ant build script.
     * 
     * @param domHelper
     *            helper for creating the elements.
     * @param antHelper
     *            helper for building ant scripts.
     * @param components
     *            DCs for whose used DCs the paths should be created.
     */
    public PathsBuilder(final DomHelper domHelper, final AntHelper antHelper,
        final Collection<DevelopmentComponent> components) {
        this.domHelper = domHelper;
        this.antHelper = antHelper;
        this.components = components;
    }

    /**
     * Creates a <code>&lt;paths&gt;</code> element containing the information
     * to construct ant paths.
     * 
     * @return an {@link Element} representing the paths built from the used DCs
     *         and their used public parts.
     */
    public Element build() {
        final Element paths = domHelper.createElement("paths");

        for (final PublicPartReference ppRef : collectPublicPartReferences()) {
            final String refId = createRefId(ppRef);

            paths.appendChild(domHelper.createElement("path", new String[] { "id", "location", "include" },
                new String[] { refId, antHelper.getLocation(ppRef), "*.jar" }));
        }

        return paths;
    }

    /**
     * Collect the used public parts.
     * 
     * @return the set of referenced public parts
     */
    private Collection<PublicPartReference> collectPublicPartReferences() {
        final Set<PublicPartReference> refs = new HashSet<PublicPartReference>();

        for (final DevelopmentComponent component : components) {
            for (final PublicPartReference ppRef : component.getUsedDevelopmentComponents()) {
                if (ppRef.isAtBuildTime()) {
                    refs.add(ppRef);
                }
            }
        }

        return refs;
    }

    /**
     * Create an ID for the referenced public part.
     * 
     * @param ppRef
     *            the referenced public part an ID is to created for.
     * @return an ID for the referenced public part.
     */
    private String createRefId(final PublicPartReference ppRef) {
        return String.format("%s~%s~%s", ppRef.getVendor(), ppRef.getComponentName(), ppRef.getName());
    }
}
