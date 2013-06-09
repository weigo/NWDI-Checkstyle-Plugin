/**
 *
 */
package org.arachna.netweaver.nwdi.checkstyle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
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
    /**
     * path template for a 'checkstyle-build.xml' for a development component.
     */
    protected static final String BUILD_XML_PATH_TEMPLATE = "%s/checkstyle-build.xml";

    /**
     * Encoding to use for writing build files and reading their templates.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Checkstyle configuration file.
     */
    private final String pathToGlobalCheckstyleConfig;

    /**
     * helper for populating properties of the checkstyle ant task.
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
     * Excludes 'by regexp over content' configured in project.
     */
    private final Set<String> excludeContainsRegexps = new HashSet<String>();

    /**
     * Template to use for generating build files.
     */
    private final VelocityEngine engine;

    /**
     * Create an instance of an {@link BuildFileGenerator} with the given workspace location and checkstyle configuration file.
     * 
     * @param engine
     *            VelocityEngine for build file generation.
     * @param antHelper
     *            helper class for populating the checkstyle ant task's file sets, class path etc.
     * @param pathToGlobalCheckstyleConfig
     *            the path to the global checkstyle configuration file
     * @param excludes
     *            Ant exclude patterns.
     * @param excludeContainsRegexps
     *            regular expressions to be used to exclude sources that match the given expressions with their content.
     * 
     */
    BuildFileGenerator(final VelocityEngine engine, final AntHelper antHelper, final String pathToGlobalCheckstyleConfig,
        final Collection<String> excludes, final Collection<String> excludeContainsRegexps) {
        this.engine = engine;
        this.antHelper = antHelper;
        this.pathToGlobalCheckstyleConfig = pathToGlobalCheckstyleConfig;
        this.excludes.addAll(excludes);
        this.excludeContainsRegexps.addAll(excludeContainsRegexps);
    }

    /**
     * Generates the 'checkstyle-build.xml' for the given development component.
     * 
     * @param component
     *            the development component to generate the 'checkstyle-build.xml' for.
     * @return path to generated build file.
     */
    String execute(final DevelopmentComponent component) {
        Writer buildFile = null;
        String buildFilePath = null;

        try {
            final Collection<String> sources = antHelper.createSourceFileSets(component, new ExcludeDataDictionarySourceDirectoryFilter());
            sources.addAll(component.getTestSourceFolders());

            if (!sources.isEmpty()) {
                final Context context = createContext(component, sources);
                final String location = getBuildXmlLocation(component);
                buildFile = new OutputStreamWriter(new FileOutputStream(location), Charset.forName(ENCODING));
                evaluateContext(buildFile, context);
                buildFilePath = location;
            }
        }
        catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        finally {
            if (buildFile != null) {
                try {
                    buildFile.close();
                }
                catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        return buildFilePath;
    }

    /**
     * Calculate the location of the 'checkstyle-build.xml'.
     * 
     * @param component
     *            development component to calculate the location of the 'checkstyle-build.xml' for.
     * @return the location of the 'checkstyle-build.xml' for the given component.
     */
    private String getBuildXmlLocation(final DevelopmentComponent component) {
        return String.format(BUILD_XML_PATH_TEMPLATE, antHelper.getBaseLocation(component));
    }

    /**
     * Evaluate the given context and write the transformation result into the given writer instance.
     * 
     * @param buildFile
     *            writer to receive the transformation result.
     * @param context
     *            velocity context to be evaluated.
     */
    void evaluateContext(final Writer buildFile, final Context context) {
        engine.evaluate(context, buildFile, "checkstyle-build",
            new InputStreamReader(this.getClass().getResourceAsStream("/org/arachna/netweaver/nwdi/checkstyle/checkstyle-build.vm"),
                Charset.forName(ENCODING)));
    }

    /**
     * Fill in the velocity context to be used to create the 'checkstyle-build.xml'.
     * 
     * @param component
     *            development component the build file shall be created for.
     * @param sources
     *            a collection of source paths to be checked with checkstyle.
     * @return the velocity context produced.
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
        context.put("componenBase", antHelper.getBaseLocation(component));

        return context;
    }
}