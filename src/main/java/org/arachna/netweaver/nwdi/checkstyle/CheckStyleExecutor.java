/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.ContainsRegexpSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
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
    private static final String LOCATION_TEMPLATE = "%s/.dtc/DCs/%s/%s/_comp/%s";

    /**
     * template for path to source folder.
     */
    private static final String CHECKSTYLE_RESULT_LOCATION_TEMPLATE =
        "%s/.dtc/DCs/%s/%s/_comp/gen/default/logs/checkstyle_errors.xml";

    /**
     * workspace folder.
     */
    private final String workspace;

    /**
     * checkstyle configuration file.
     */
    private final File config;

    /**
     * exclude patterns.
     */
    private final Collection<String> excludes = new HashSet<String>();

    /**
     * regular expressions for exclusion via file content.
     */
    private final Collection<String> regexps = new HashSet<String>();

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
    CheckStyleExecutor(final String workspace, final File config, final Collection<String> excludes,
        final Collection<String> regexps) {
        this.workspace = workspace;
        this.config = config;
        this.excludes.addAll(excludes);
        this.regexps.addAll(regexps);
    }

    /**
     * Executes the checkstyle check for the given development component.
     * 
     * @param component
     *            the development component to execute a checkstyle check on.
     */
    void execute(final DevelopmentComponent component) {
        if (!component.getSourceFolders().isEmpty()) {
            final CheckStyleTask task = new CheckStyleTask();

            final Project project = new Project();
            task.setProject(project);

            for (final String srcFolder : component.getSourceFolders()) {
                final FileSet fileSet = new FileSet();
                fileSet.setDir(new File(this.getSourceFolderLocation(component, srcFolder)));
                fileSet.setIncludes("**/*.java");
                fileSet.appendExcludes(this.excludes.toArray(new String[this.excludes.size()]));

                if (this.regexps.size() > 0) {
                    fileSet.add(this.createContainsRegexpSelectors());
                }

                task.addFileset(fileSet);
            }

            task.addFormatter(createFormatter(component));
            task.setConfig(this.config);
            task.setFailOnViolation(false);
            task.execute();
        }
    }

    private FileSelector createContainsRegexpSelectors() {
        final OrSelector or = new OrSelector();

        for (final String containsRegexp : this.regexps) {
            final ContainsRegexpSelector selector = new ContainsRegexpSelector();
            selector.setExpression(containsRegexp);
            or.add(selector);
        }

        return new NotSelector(or);
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
        final CheckStyleTask.Formatter formatter = new CheckStyleTask.Formatter();
        formatter.setTofile(createResultFile(component));
        formatter.setUseFile(true);

        final CheckStyleTask.FormatterType formatterType = new CheckStyleTask.FormatterType();
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
