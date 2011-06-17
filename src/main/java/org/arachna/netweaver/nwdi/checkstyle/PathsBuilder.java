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
 * Build <paths><path/></paths> DOM representation from a set of
 * {@link DevelopmentComponent} and their used DCs.
 *
 * @author Dirk Weigenand
 */
public final class PathsBuilder {
    private final DomHelper domHelper;
    private final Collection<DevelopmentComponent> components;
    private final Set<String> references = new HashSet<String>();
    private final AntHelper antHelper;

    public PathsBuilder(DomHelper domHelper, AntHelper antHelper, Collection<DevelopmentComponent> components) {
        this.domHelper = domHelper;
        this.antHelper = antHelper;
        this.components = components;
    }

    public Element build() {
        Element paths = this.domHelper.createElement("paths");

        for (PublicPartReference ppRef : this.collectPublicPartReferences()) {
            String refId = this.createRefId(ppRef);

            paths.appendChild(this.domHelper.createElement("path", new String[] { "id", "location", "include" },
                new String[] { refId, this.antHelper.getLocation(ppRef), "*.jar" }));
            this.references.add(refId);
        }

        return paths;
    }

    private Set<PublicPartReference> collectPublicPartReferences() {
        Set<PublicPartReference> refs = new HashSet<PublicPartReference>();

        for (DevelopmentComponent component : this.components) {
            for (PublicPartReference ppRef : component.getUsedDevelopmentComponents()) {
                if (ppRef.isAtBuildTime()) {
                    refs.add(ppRef);
                }
            }
        }

        return refs;
    }

    private String createRefId(PublicPartReference ppRef) {
        return String.format("%s:%s:%s", ppRef.getVendor(), ppRef.getComponentName(), ppRef.getName());
    }
}
