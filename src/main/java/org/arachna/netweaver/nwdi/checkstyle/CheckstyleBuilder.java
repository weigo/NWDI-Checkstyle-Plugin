package org.arachna.netweaver.nwdi.checkstyle;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.velocity.app.VelocityEngine;
import org.arachna.netweaver.dc.types.DevelopmentComponent;
import org.arachna.netweaver.hudson.nwdi.AntTaskBuilder;
import org.arachna.netweaver.hudson.nwdi.DCWithJavaSourceAcceptingFilter;
import org.arachna.netweaver.hudson.nwdi.NWDIBuild;
import org.arachna.netweaver.hudson.nwdi.NWDIProject;
import org.arachna.netweaver.hudson.util.FilePathHelper;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Builder for executing checkstyle checks on SAP NetWeaver development components.
 * 
 * @author Dirk Weigenand
 */
public class CheckstyleBuilder extends AntTaskBuilder {
    /**
     * Descriptor for {@link CheckstyleBuilder}.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * Name of checkstyle configuration file in workspace.
     */
    private static final String CHECKSTYLE_CONFIG_XML = "checkstyle-config.xml";

    /**
     * Data bound constructor. Used for populating a {@link CheckstyleBuilder} instance from form fields in <code>config.jelly</code>.
     */
    @DataBoundConstructor
    public CheckstyleBuilder() {
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
        boolean result = true;
        final PrintStream logger = listener.getLogger();

        try {
            final NWDIBuild nwdiBuild = (NWDIBuild)build;
            final FilePath workspace = nwdiBuild.getWorkspace();
            final FilePath checkstyleConfig = workspace.child(CHECKSTYLE_CONFIG_XML);
            checkstyleConfig.write(getDescriptor().getConfiguration(), "UTF-8");

            final VelocityEngine engine = getVelocityEngine();

            final Collection<DevelopmentComponent> components =
                nwdiBuild.getAffectedDevelopmentComponents(new DCWithJavaSourceAcceptingFilter());
            final BuildFileGenerator generator = createBuildFileGenerator(engine, checkstyleConfig);

            for (final DevelopmentComponent component : components) {
                final BuildDescriptor descriptor = generator.execute(component);

                if (descriptor != null) {
                    result |= execute(nwdiBuild, launcher, listener, descriptor.getDefaultTarget(), descriptor.getBuildFile(), null);
                }
            }

        }
        catch (final IOException e) {
            e.printStackTrace(logger);
            result = false;
        }
        catch (final InterruptedException e) {
            e.printStackTrace(logger);
            // finish.
        }

        return result;
    }

    /**
     * Get the properties to use calling ant.
     * 
     * @return the properties to use calling ant.
     */
    @Override
    protected String getAntProperties() {
        return "checkstyle.dir=" + new File(Hudson.getInstance().root, "plugins/NWDI-Checkstyle-Plugin/WEB-INF/lib").getAbsolutePath();
    }

    /**
     * Create a {@link BuildFileGenerator} using the given {@link NWDIBuild}.
     * 
     * @param engine
     *            velocity engine to use for creating build files.
     * @param checkstyleConfig
     *            path to global checkstyle configuration.
     * @return the checkstyle build file generator.
     */
    protected BuildFileGenerator createBuildFileGenerator(final VelocityEngine engine, final FilePath checkstyleConfig) {
        final DescriptorImpl descriptor = getDescriptor();

        return new BuildFileGenerator(engine, getAntHelper(), FilePathHelper.makeAbsolute(checkstyleConfig), descriptor.getExcludes(),
            descriptor.getExcludeContainsRegexps());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor for {@link CheckstyleBuilder}.
     */
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * Persistent checkstyle configuration.
         * 
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String configuration;

        /**
         * set of filename patterns to exclude from checkstyle checks.
         */
        private final Collection<String> excludes = new HashSet<String>();

        /**
         * set of regular expressions to exclude files from checkstyle checks via their content.
         */
        private final Collection<String> excludeRegexps = new HashSet<String>();

        /**
         * Create descriptor for NWDI-CheckStyle-Builder and load global configuration data.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckConfiguration(@QueryParameter final String value) throws IOException, ServletException {
            return value.isEmpty() ? FormValidation.error(Messages.checkstyle_builder_checkconfiguration()) : FormValidation.ok();

            // FIXME: Add further configuration validation!
        }

        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return NWDIProject.class.equals(aClass);
        }

        /**
         * This human readable name is used in the configuration screen.
         * 
         * @return return the builder name.
         */
        @Override
        public String getDisplayName() {
            return "NWDI Checkstyle Builder";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            configuration = formData.getString("configuration");

            final JSONObject advancedConfig = (JSONObject)formData.get("advancedConfiguration");

            if (advancedConfig != null) {
                excludes.clear();
                excludes.addAll(getExcludeItemDescriptions(formData, "excludes", "exclude"));

                excludeRegexps.clear();
                excludeRegexps.addAll(getExcludeItemDescriptions(advancedConfig, "excludeContainsRegexps", "regexp"));
            }

            save();

            return super.configure(req, formData);
        }

        /**
         * Extract item descriptions for exclusion from analysis.
         * 
         * @param advancedConfig
         *            JSON form containing advanced configuration data.
         * @param formName
         *            name of JSON form element to extract item descriptions from.
         * @param itemName
         *            name of configuration form item.
         * 
         * @return collection of item descriptions to exclude from checkstyle analysis
         */
        protected Collection<String> getExcludeItemDescriptions(final JSONObject advancedConfig, final String formName,
            final String itemName) {
            final Collection<String> descriptions = new HashSet<String>();

            final JSONArray excludes = JSONArray.fromObject(advancedConfig.get(formName));

            for (int i = 0; i < excludes.size(); i++) {
                final JSONObject param = excludes.getJSONObject(i);

                if (!param.isNullObject()) {
                    final String exclude = param.getString(itemName);

                    if (exclude.length() > 0) {
                        descriptions.add(exclude);
                    }
                }
            }

            return descriptions;

        }

        /**
         * Returns the checkstyle configuration.
         * 
         * @return the checkstyle configuration as XML.
         */
        public String getConfiguration() {
            return configuration;
        }

        /**
         * Sets the checkstyle configuration to be used for all NWDI checkstyle builders.
         * 
         * @param configuration
         *            the configuration to set
         */
        public void setConfiguration(final String configuration) {
            this.configuration = configuration;
        }

        /**
         * Returns the list of file name patterns to exclude from checkstyle checks.
         * 
         * @return the list of file name patterns to exclude from checkstyle checks.
         */
        public Collection<String> getExcludes() {
            return excludes;
        }

        /**
         * Sets the list of file name patterns to exclude from checkstyle checks.
         * 
         * @param excludes
         *            the list of file name patterns to exclude from checkstyle checks.
         */
        public void setExcludes(final Collection<String> excludes) {
            this.excludes.clear();

            if (excludes != null) {
                this.excludes.addAll(excludes);
            }
        }

        /**
         * @return the excludeContainsRegexps
         */
        public Collection<String> getExcludeContainsRegexps() {
            return excludeRegexps;
        }

        /**
         * @param excludeContainsRegexps
         *            the excludeContainsRegexps to set
         */
        public void setExcludeContainsRegexps(final Collection<String> excludeContainsRegexps) {
            excludeRegexps.clear();

            if (excludeContainsRegexps != null) {
                excludeRegexps.addAll(excludeContainsRegexps);
            }
        }
    }
}
