package pl.edu.icm.maven.oozie.plugin;

import com.google.common.io.Files;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.Exclusion;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import pl.edu.icm.maven.oozie.plugin.pigscripts.ConfigurationReader;
import pl.edu.icm.maven.oozie.plugin.pigscripts.PigScriptExtractor;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "prepare-package", requiresDependencyResolution = ResolutionScope.COMPILE)
public class PreparePackageMojo extends AbstractOozieMojo {
    @Component
    private RepositorySystem repoSystem;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepos;

	PigScriptExtractor  psh = null;
	
    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
        preparePackageWorkflow();
        preparePackageJob();
    }

    private void preparePackageJob() throws MojoExecutionException {

    	psh = new PigScriptExtractor( new ConfigurationReader(descriptors, getLog(), buildDirectory).readConfiguration() , getLog(), omp_debbug );
    	
        if (!jobPackage && (skipTests || skipITs)) {
            getLog().info("Ozzie job package has not been prepared.");
            return;
        }

        org.apache.maven.shared.dependency.tree.DependencyNode dependencyTree;
        try {
            dependencyTree = dependencyTreeBuilder.buildDependencyTree(mavenProject, localRepository, null);
        } catch (DependencyTreeBuilderException ex) {
            throw new MojoExecutionException("Failed to build dependency tree", ex);
        }

        unpackWorkflows(
                new File("${project.build.directory}", OoziePluginConstants.OOZIE_WF_PREPARE_PACKAGE_DIR),
                new File("${project.build.directory}", "dependency-maven-plugin-markers"),
                mavenProject.getArtifact());

        /*
         * This step can be omitted when the following problem with "exclude"
         * option for tar.gz files http://jira.codehaus.org/browse/MDEP-242 for
         * maven-dependency-plugin is resolved. Instead the 'exclude' option
         * should be used in previous step.
         */
        executeMojo(
                plugin(groupId("org.apache.maven.plugins"),
                artifactId("maven-clean-plugin"),
                version(OoziePluginConstants.MAVEN_CLEAN_PLUGIN_VERSION)),
                goal("clean"),
                configuration(
                element(name("excludeDefaultDirectories"), "true"),
                element(name("filesets"),
                element(name("fileset"),
                element(name("directory"),
                "${project.build.directory}/"
                + OoziePluginConstants.OOZIE_WF_PREPARE_PACKAGE_DIR),
                element(name("includes"),
                element(name("include"),
                "**/lib/*"),
                element(name("include"),
                "**/lib/"))))),
                environment);

        executeMojo(
                plugin(groupId("org.apache.maven.plugins"),
                artifactId("maven-dependency-plugin"),
                version(OoziePluginConstants.MAVEN_DEPENDENCY_PLUGIN_VERSION)),
                goal("copy-dependencies"),
                configuration(
                element(name("outputDirectory"),
                "${project.build.directory}/"
                + OoziePluginConstants.OOZIE_WF_PREPARE_PACKAGE_DIR
                + "/lib/"),
                element(name("excludeClassifiers"),
                OoziePluginConstants.OOZIE_WF_CLASSIFIER),
                element(name("excludeScope"), "provided")), environment);

        String mainWorkflowDirectory = buildDirectory + "/" + OoziePluginConstants.OOZIE_WF_PREPARE_PACKAGE_DIR;
        String globalLibDirectory = mainWorkflowDirectory + "/lib/";
        File tmpDir = Files.createTempDir();
        tmpDir.deleteOnExit();
        getLog().info("============================================");
        getLog().info(" Start of Pig scripts and associated libs extraction");
        unpackPigScripts(globalLibDirectory, mainWorkflowDirectory, dependencyTree, tmpDir);
        getLog().info(" End of Pig scripts and associated libs extraction");
        getLog().info("============================================");
    }

    private void preparePackageWorkflow() throws MojoExecutionException {

        executeMojo(
                plugin(groupId("org.apache.maven.plugins"),
                artifactId("maven-resources-plugin"),
                version(OoziePluginConstants.MAVEN_RESOURCES_PLUGIN_VERSION)),
                goal("copy-resources"),
                configuration(
                element("outputDirectory",
                "${project.build.directory}/"
                + OoziePluginConstants.OOZIE_WF_PREPARE_PACKAGE_DIR),
                element("resources",
                element("resource",
                element("directory", oozieDirectory),
                element("filtering", String.valueOf(filtering))))), environment);
    }

    private void unpackWorkflows(File outputDirectory, File markersDirectory, Artifact af) throws MojoExecutionException {
        FullWorkflowDependencyTreeBuilder treeBuilder =
                new FullWorkflowDependencyTreeBuilder(repoSystem, repoSession, remoteRepos, getLog());

        org.sonatype.aether.artifact.Artifact aetherArtifact =
                new DefaultArtifact(af.getGroupId(), af.getArtifactId(), af.getClassifier(), af.getType(), af.getVersion());

        DependencySelector scopeSelector = new ScopeDependencySelector(Arrays.asList("compile", "runtime"), null);
        DependencySelector workflowSelector = new NotDependencySelector(new ExclusionDependencySelector(
                Collections.singleton(new Exclusion("*", "*", OoziePluginConstants.OOZIE_WF_CLASSIFIER, "tar.gz"))));

        DependencySelector uberSelector = new AndDependencySelector(scopeSelector, workflowSelector);

        DependencyNode root = treeBuilder.buildDependencyTree(aetherArtifact, uberSelector);

        for(DependencyNode node: root.getChildren()) {
            UnpackingDependencyVisitor visitor =
                    new UnpackingDependencyVisitor(outputDirectory,  markersDirectory, environment);
            node.accept(visitor);
            visitor.throwIfFailed();
        }
    }

    private void unpackPigScripts(String globalLibDirectory, String currentTreePosition, org.apache.maven.shared.dependency.tree.DependencyNode dependencyTree, File tmpDir)
            throws MojoExecutionException {

        for (org.apache.maven.shared.dependency.tree.DependencyNode childNode : dependencyTree.getChildren()) {
            Artifact af = childNode.getArtifact();

            if ("jar".equals(af.getType()) && !Artifact.SCOPE_TEST.equals(af.getScope())) {

                // search for pig scripts:
                File afTmpDir = new File(tmpDir, af.getGroupId() + "-" + af.getArtifactId());
                executeMojo(
                        plugin(groupId("org.apache.maven.plugins"),
                        artifactId("maven-dependency-plugin"),
                        version(OoziePluginConstants.MAVEN_DEPENDENCY_PLUGIN_VERSION)),
                        goal("copy-dependencies"),
                        configuration(
                        element(name("outputDirectory"), afTmpDir.getPath()),
                        element(name("includeGroupIds"), af.getGroupId()),
                        element(name("includeArtifactIds"), af.getArtifactId())),
                        environment);

                psh.performExtraction(globalLibDirectory, currentTreePosition, af, afTmpDir);
            }

            if (OoziePluginConstants.OOZIE_WF_CLASSIFIER.equals(af.getClassifier())) {
                // recursive call for a subworkflow
                unpackPigScripts(globalLibDirectory, currentTreePosition + "/" + af.getGroupId() + "-" + af.getArtifactId(), childNode, tmpDir);
            }
        }
    }
}