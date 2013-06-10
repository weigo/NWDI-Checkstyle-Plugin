/**
 * 
 */
package org.arachna.netweaver.nwdi.checkstyle;

/**
 * Descriptor for build files (file name and default target).
 * 
 * @author Dirk Weigenand
 */
class BuildDescriptor {
    /**
     * name of build file.
     */
    private final String buildFile;

    /**
     * name of default target.
     */
    private final String defaultTarget;

    /**
     * Create new descriptor instance with name of build file and default target.
     * 
     * @param buildFile
     *            name of build file.
     * @param defaultTarget
     *            name of default target.
     */
    BuildDescriptor(final String buildFile, final String defaultTarget) {
        this.buildFile = buildFile;
        this.defaultTarget = defaultTarget;
    }

    /**
     * @return the buildFile
     */
    String getBuildFile() {
        return buildFile;
    }

    /**
     * @return the defaultTarget
     */
    String getDefaultTarget() {
        return defaultTarget;
    }

}