/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.File;

import org.apache.tools.ant.types.FileSet;
import org.arachna.netweaver.dc.types.DevelopmentComponent;

import com.puppycrawl.tools.checkstyle.CheckStyleTask;

/**
 * Execute a checkstyle check on a given development component.
 * 
 * @author Dirk Weigenand
 */
final class CheckStyleExecutor {
    /**
     * template for path to source folder.
     */
    private static final String LOCATION_TEMPLATE = "%s/DCs/%s/%s/_comp/%s";

    /**
     * template for path to source folder.
     */
    private static final String CHECKSTYLE_RESULT_LOCATION_TEMPLATE =
        "%s/DCs/%s/%s/_comp/gen/default/logs/checkstyle_errors.xml";

    /**
     * workspace folder.
     */
    private final String workspace;

    /**
     * checkstyle configuration file.
     */
    private final File config;

    /**
     * Create an instance of an {@link CheckStyleExecutor} with the given
     * workspace location and checkstyle configuration file.
     * 
     * @param workspace
     *            the workspace where to operate from
     * @param config
     *            the checkstyle configuration file
     * 
     */
    CheckStyleExecutor(final String workspace, final File config) {
        this.workspace = workspace;
        this.config = config;
    }

    /**
     * Executes the checkstyle check for the given development component.
     * 
     * @param component
     *            the development component to execute a checkstyle check on.
     */
    void execute(final DevelopmentComponent component) {
        CheckStyleTask task = new CheckStyleTask();

        for (String srcFolder : component.getSourceFolders()) {
            // TODO: add excludes
            FileSet fileSet = new FileSet();
            fileSet.setDir(new File(this.getSourceFolderLocation(component, srcFolder)));
            fileSet.setIncludes("**/*.java");
            task.addFileset(fileSet);
        }

        task.addFormatter(createFormatter(component));
        task.setConfig(this.config);
        task.setFailOnViolation(false);
        task.perform();

    }

    /**
     * Creates a {@link CheckStyleTask.Formatter} object based on the given
     * development component.
     * 
     * The development component determines where the checkstyle result file can
     * be found.
     * 
     * @param component
     *            development component the formatter should be created for.
     * @return a checkstyle formatter object.
     */
    private CheckStyleTask.Formatter createFormatter(final DevelopmentComponent component) {
        CheckStyleTask.Formatter formatter = new CheckStyleTask.Formatter();
        formatter.setTofile(createResultFile(component));
        formatter.setUseFile(true);

        CheckStyleTask.FormatterType formatterType = new CheckStyleTask.FormatterType();
        formatterType.setValue("xml");

        return formatter;
    }

    /**
     * Get the base path of the current component (its '_comp' folder).
     * 
     * @return the base path of the current component (its '_comp' folder).
     */
    private String getSourceFolderLocation(final DevelopmentComponent component, final String sourceFolder) {
        return String.format(LOCATION_TEMPLATE, this.workspace, component.getVendor(), component.getName(),
            sourceFolder).replace('/', File.separatorChar);
    }

    /**
     * Create a file object where the checkstlye result shall be written to.
     * 
     * @param component
     *            the development component which is used to determine the place
     *            to write the result to.
     * @return file object where checkstyle result shall be written to.
     */
    private File createResultFile(final DevelopmentComponent component) {
        return new File(String.format(CHECKSTYLE_RESULT_LOCATION_TEMPLATE, this.workspace, component.getVendor(),
            component.getName()).replace('/', File.separatorChar));
    }
}
