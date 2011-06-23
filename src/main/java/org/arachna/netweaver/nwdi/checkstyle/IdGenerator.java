/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.dc.types.PublicPartReference;

/**
 * Generator for Ids used in ant build files.
 * 
 * @author Dirk Weigenand
 */
public final class IdGenerator {
    /**
     * Generate an Id for the given development component and prepend the prefix
     * given.
     * 
     * @param prefix
     *            Prefix to prepend to the generated id.
     * @param component
     *            development to generate an Id from.
     * @return the generated Id.
     */
    public String createId(final String prefix, final DevelopmentComponent component) {
        return String.format("%s%s~%s", prefix, component.getVendor(), component.getName());
    }

    /**
     * Create an ID for the referenced public part.
     * 
     * @param ppRef
     *            the referenced public part an ID is to created for.
     * @return an ID for the referenced public part.
     */
    public String createId(final PublicPartReference ppRef) {
        return String.format("%s~%s~%s", ppRef.getVendor(), ppRef.getComponentName(), ppRef.getName());
    }
}
