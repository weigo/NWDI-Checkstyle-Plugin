/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.arachna.ant.AntHelper;
import org.arachna.ant.ExcludeDataDictionarySourceDirectoryFilter;
import org.arachna.ant.ExcludesFactory;
import org.arachna.netweaver.dc.types.DevelopmentComponent;

/**
 * Execute a checkstyle check on a given development component.
 * 
 * @author Dirk Weigenand
 */
final class BuildFileGenerator {
    /**
     * Checkstyle configuration file.
     */
    private final String pathToGlobalCheckstyleConfig;

    /**
     * Logger.
     */
    private final PrintStream logger;

    /**
     * helper for populating properties of the checkstyle ant task
     */
    private AntHelper antHelper;

    /**
     * Factory for exclude patterns depending on DC type.
     */
    private ExcludesFactory excludesFactory = new ExcludesFactory();

    /**
     * Excludes configured in project.
     */
    private Set<String> excludes = new HashSet<String>();

    /**
     * Template to use for generating build files.
     */
    private VelocityEngine engine;

    /**
     * Paths to generated build files.
     */
    private Collection<String> buildFilePaths = new HashSet<String>();

    /**
     * @return the buildFilePaths
     */
    public final Collection<String> getBuildFilePaths() {
        return buildFilePaths;
    }

    /**
     * Create an instance of an {@link BuildFileGenerator} with the given
     * workspace location and checkstyle configuration file.
     * 
     * @param listener
     *            build listener for logging
     * @param antHelper
     *            helper class for populating the checkstyle ant task's file
     *            sets, class path etc.
     * @param pathToGlobalCheckstyleConfig
     *            the path to the global checkstyle configuration file
     * @param descriptor
     *            the descriptor of checkstyle builder
     * 
     */
    BuildFileGenerator(final VelocityEngine engine, final PrintStream logger, final AntHelper antHelper,
        final String pathToGlobalCheckstyleConfig, final Collection<String> excludes) {
        this.engine = engine;
        this.logger = logger;
        this.antHelper = antHelper;
        this.pathToGlobalCheckstyleConfig = pathToGlobalCheckstyleConfig;
        this.excludes.addAll(excludes);
    }

    /**
     * Executes the checkstyle check for the given development component.
     * 
     * @param component
     *            the development component to execute a checkstyle check on.
     */
    void execute(final DevelopmentComponent component) {
        Writer buildFile = null;

        try {
            Context context = new VelocityContext();
            context.put("sourcePaths",
                antHelper.createSourceFileSets(component, new ExcludeDataDictionarySourceDirectoryFilter()));
            context.put("checkstyleconfig", pathToGlobalCheckstyleConfig);
            context.put("excludes", this.excludesFactory.create(component, this.excludes));
            context.put("classpaths", antHelper.createClassPath(component));
            context.put("vendor", component.getVendor());
            context.put("component", component.getName().replaceAll("/", "~"));
            // FIXME: add containsexcludes

            String baseLocation = antHelper.getBaseLocation(component);
            context.put("componentBase", baseLocation);
            String location = String.format("%s/checkstyle-build.xml", baseLocation);
            buildFile = new FileWriter(location);
            engine.evaluate(context, buildFile, "checkstyle-build", new InputStreamReader(this.getClass()
                .getResourceAsStream("/org/arachna/netweaver/nwdi/checkstyle/checkstyle-build.vm")));
            this.buildFilePaths.add(location);
        }
        catch (Exception e) {
            e.printStackTrace(logger);
        }
        finally {
            if (buildFile != null) {
                try {
                    buildFile.close();
                }
                catch (IOException e) {
                    e.printStackTrace(logger);
                }
            }
        }
    }
}