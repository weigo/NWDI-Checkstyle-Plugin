/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.File;

import org.apache.tools.ant.types.FileSet;
import org.arachna.netweaver.dc.types.DevelopmentComponent;

import com.puppycrawl.tools.checkstyle.CheckStyleTask;

/**
 * @author Dirk Weigenand
 * 
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
     * 
     * @param workspace
     */
    CheckStyleExecutor(String workspace, File config) {
        this.workspace = workspace;
        this.config = config;
    }

    void execute(DevelopmentComponent component) {
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

    private CheckStyleTask.Formatter createFormatter(DevelopmentComponent component) {
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
    private String getSourceFolderLocation(DevelopmentComponent component, String sourceFolder) {
        return String.format(LOCATION_TEMPLATE, this.workspace, component.getVendor(), component.getName(),
            sourceFolder).replace('/', File.separatorChar);
    }

    private File createResultFile(DevelopmentComponent component) {
        return new File(String.format(CHECKSTYLE_RESULT_LOCATION_TEMPLATE, this.workspace, component.getVendor(),
            component.getName()).replace('/', File.separatorChar));
    }
}
