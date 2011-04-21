/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.File;
import java.io.PrintStream;
import java.util.Collection;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.arachna.ant.AntHelper;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.nwdi.checkstyle.CheckstyleBuilder.DescriptorImpl;

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
    private static final String CHECKSTYLE_RESULT_LOCATION_TEMPLATE = "%s/checkstyle/%s~%s/checkstyle-result.xml";

    /**
     * Checkstyle configuration file.
     */
    private final File config;

    /**
     * Logger.
     */
    private final PrintStream logger;

    /**
     * descriptor of checkstyle builder.
     */
    private DescriptorImpl descriptor;

    /**
     * helper for populating properties of the checkstyle ant task
     */
    private AntHelper antHelper;

    /**
     * Create an instance of an {@link CheckStyleExecutor} with the given
     * workspace location and checkstyle configuration file.
     *
     * @param listener
     *            build listener for logging
     * @param antHelper
     *            helper class for populating the checkstyle ant task's file
     *            sets, class path etc.
     * @param config
     *            the checkstyle configuration file
     * @param descriptor
     *            the descriptor of checkstyle builder
     *
     */
    CheckStyleExecutor(final PrintStream logger, final AntHelper antHelper, final File config,
        final DescriptorImpl descriptor) {
        this.logger = logger;
        this.antHelper = antHelper;
        this.config = config;
        this.descriptor = descriptor;
    }

    /**
     * Executes the checkstyle check for the given development component.
     *
     * @param component
     *            the development component to execute a checkstyle check on.
     */
    void execute(final DevelopmentComponent component) {
        Collection<File> existingSourceFolders = this.antHelper.getExistingSourceFolders(component);

        if (!existingSourceFolders.isEmpty()) {
            File resultFile = createResultFile(component);

            if (!resultFile.getParentFile().exists()) {
                if (!resultFile.getParentFile().mkdirs()) {
                    this.logger.append(resultFile.getParentFile().getAbsolutePath() + " could not be created!\n");
                    return;
                }
            }

            final CheckStyleTask task = createCheckStyleTask(component, resultFile);

            this.logger.append(String.format("Running checkstyle analysis on %s:%s...", component.getVendor(),
                component.getName()));
            long start = System.currentTimeMillis();
            task.execute();
            this.logger.append(String.format(" (%f sec.)\n", (System.currentTimeMillis() - start) / 1000f));
        }
    }

    /**
     * Sets up an instance of a checkstyle ant task with the source folders
     * associated with the given development component.
     *
     * @param component
     * @param resultFile
     * @return
     */
    private CheckStyleTask createCheckStyleTask(final DevelopmentComponent component, File resultFile) {
        final CheckStyleTask task = new CheckStyleTask();

        final Project project = new Project();
        task.setProject(project);

        for (FileSet sources : antHelper.createSourceFileSets(component, this.descriptor.getExcludes(),
            this.descriptor.getExcludeContainsRegexps())) {
            task.addFileset(sources);
        }

        task.setClasspath(this.antHelper.createClassPath(project, component));
        task.addFormatter(createFormatter(component, resultFile));
        task.setConfig(this.config);
        task.setFailOnViolation(false);

        return task;
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
     * Create a file object where the checkstyle result shall be written to.
     *
     * @param component
     *            the development component which is used to determine the place
     *            to write the result to.
     * @return file object where checkstyle result shall be written to.
     */
    private File createResultFile(final DevelopmentComponent component) {
        return new File(String.format(CHECKSTYLE_RESULT_LOCATION_TEMPLATE, this.antHelper.getPathToWorkspace(),
            component.getVendor(), component.getName().replace('/', '~')).replace('/', File.separatorChar));
    }
}