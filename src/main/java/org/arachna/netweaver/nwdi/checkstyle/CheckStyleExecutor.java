/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import hudson.model.BuildListener;

import java.io.File;
import java.util.ArrayList;
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
    private static final String CHECKSTYLE_RESULT_LOCATION_TEMPLATE = "%s/checkstyle/%s~%s/checkstyle-result.xml";

    /**
     * workspace folder.
     */
    private final String workspace;

    /**
     * checkstyle configuration file.
     */
    private final File config;

    /**
     * build listener for log messages.
     */
    private final BuildListener listener;

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
     * @param listener
     *            build listener for logging
     * @param workspace
     *            the workspace where to operate from
     * @param config
     *            the checkstyle configuration file
     * @param excludes
     *            collection of ant exclude expressions for exclusion based on
     *            file name patterns
     * @param regexps
     *            collection of regular expression for exclusion based on file
     *            contents
     *
     */
    CheckStyleExecutor(final BuildListener listener, final String workspace, final File config,
        final Collection<String> excludes, final Collection<String> regexps) {
        this.listener = listener;
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
        Collection<File> existingSourceFolders = getExistingSourceFolders(component);

        if (!existingSourceFolders.isEmpty()) {
            final CheckStyleTask task = new CheckStyleTask();

            final Project project = new Project();
            task.setProject(project);

            createFileSet(existingSourceFolders, task);
            File resultFile = createResultFile(component);

            if (!resultFile.getParentFile().exists()) {
                if (!resultFile.getParentFile().mkdirs()) {
                    this.listener.getLogger().append(
                        resultFile.getParentFile().getAbsolutePath() + " could not be created!");
                    return;
                }
            }

            task.addFormatter(createFormatter(component, resultFile));
            task.setConfig(this.config);
            task.setFailOnViolation(false);
            this.listener.getLogger().append(
                String.format("Running checkstyle analysis on %s/%s...", component.getVendor(), component.getName()));
            long start = System.currentTimeMillis();
            task.execute();
            this.listener.getLogger().append(
                String.format(" (%f sec.)\n", (System.currentTimeMillis() - start) / 1000f));
        }
    }

    private void createFileSet(Collection<File> existingSourceFolders, final CheckStyleTask task) {
        for (final File srcFolder : existingSourceFolders) {
            final FileSet fileSet = new FileSet();
            fileSet.setDir(srcFolder);
            fileSet.setIncludes("**/*.java");
            fileSet.appendExcludes(this.excludes.toArray(new String[this.excludes.size()]));

            if (this.regexps.size() > 0) {
                fileSet.add(this.createContainsRegexpSelectors());
            }

            task.addFileset(fileSet);
        }
    }

    /**
     * Iterate over the DCs source folders and return those that actually exist
     * in the file system.
     *
     * @param component
     *            development component to get the existing source folders for.
     * @return source folders that exist in the DCs directory structure.
     */
    private Collection<File> getExistingSourceFolders(DevelopmentComponent component) {
        Collection<File> sourceFolders = new ArrayList<File>();

        for (String sourceFolder : component.getSourceFolders()) {
            File folder = this.getSourceFolderLocation(component, sourceFolder);

            if (folder.exists()) {
                sourceFolders.add(folder);
            }
            else {
                this.listener.getLogger().append(
                    String.format("Source folder %s does not exist in %s/%s!", folder.getName(), component.getVendor(),
                        component.getName()));
            }
        }

        return sourceFolders;
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
    private CheckStyleTask.Formatter createFormatter(final DevelopmentComponent component, File resultFile) {
        final CheckStyleTask.Formatter formatter = new CheckStyleTask.Formatter();
        formatter.setTofile(resultFile);
        formatter.setUseFile(true);

        final CheckStyleTask.FormatterType formatterType = new CheckStyleTask.FormatterType();
        formatterType.setValue("xml");
        formatter.setType(formatterType);

        return formatter;
    }

    /**
     * Get a file object pointing to the given source folders location in the
     * development components directory structure.
     *
     * @return the file object representing the source folder in the DCs
     *         directory structure.
     */
    private File getSourceFolderLocation(final DevelopmentComponent component, final String sourceFolder) {
        return new File(String.format(LOCATION_TEMPLATE, this.workspace, component.getVendor(), component.getName(),
            sourceFolder).replace('/', File.separatorChar));
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
            component.getName().replace('/', '~')).replace('/', File.separatorChar));
    }
}