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
class BuildFileGenerator {
    protected static final String BUILD_XML_PATH_TEMPLATE = "%s/checkstyle-build.xml";

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
    private final AntHelper antHelper;

    /**
     * Factory for exclude patterns depending on DC type.
     */
    private final ExcludesFactory excludesFactory = new ExcludesFactory();

    /**
     * Excludes configured in project.
     */
    private final Set<String> excludes = new HashSet<String>();

    /**
     * Excludes by regexp over content configured in project.
     */
    private final Set<String> excludeContainsRegexps = new HashSet<String>();

    /**
     * Template to use for generating build files.
     */
    private final VelocityEngine engine;

    /**
     * Paths to generated build files.
     */
    private final Collection<String> buildFilePaths = new HashSet<String>();

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
        final String pathToGlobalCheckstyleConfig, final Collection<String> excludes,
        final Collection<String> excludeContainsRegexps) {
        this.engine = engine;
        this.logger = logger;
        this.antHelper = antHelper;
        this.pathToGlobalCheckstyleConfig = pathToGlobalCheckstyleConfig;
        this.excludes.addAll(excludes);
        this.excludeContainsRegexps.addAll(excludeContainsRegexps);
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
            final Collection<String> sources =
                antHelper.createSourceFileSets(component, new ExcludeDataDictionarySourceDirectoryFilter());

            if (!sources.isEmpty()) {
                final Context context = createContext(component, sources);
                String location = getBuildXmlLocation(component);
                buildFile = new FileWriter(location);
                evaluateContext(buildFile, context);
                buildFilePaths.add(location);
            }
        }
        catch (final Exception e) {
            e.printStackTrace(logger);
        }
        finally {
            if (buildFile != null) {
                try {
                    buildFile.close();
                }
                catch (final IOException e) {
                    e.printStackTrace(logger);
                }
            }
        }
    }

    /**
     * @param component
     * @return
     */
    private String getBuildXmlLocation(DevelopmentComponent component) {
        return String.format(BUILD_XML_PATH_TEMPLATE, antHelper.getBaseLocation(component));
    }

    /**
     * @param buildFile
     * @param context
     * @throws IOException
     */
    void evaluateContext(Writer buildFile, final Context context) throws IOException {
        engine.evaluate(context, buildFile, "checkstyle-build", new InputStreamReader(this.getClass()
            .getResourceAsStream("/org/arachna/netweaver/nwdi/checkstyle/checkstyle-build.vm")));
    }

    /**
     * @param component
     * @param sources
     * @return
     */
    Context createContext(final DevelopmentComponent component, final Collection<String> sources) {
        final Context context = new VelocityContext();
        context.put("sourcePaths", sources);
        context.put("checkstyleconfig", pathToGlobalCheckstyleConfig);
        context.put("excludes", excludesFactory.create(component, excludes));
        context.put("excludeContainsRegexps", excludeContainsRegexps);
        context.put("classpaths", antHelper.createClassPath(component));
        context.put("classes", component.getOutputFolder());
        context.put("vendor", component.getVendor());
        context.put("component", component.getName().replaceAll("/", "~"));
        context.put("componenBase", this.antHelper.getBaseLocation(component));

        return context;
    }
}