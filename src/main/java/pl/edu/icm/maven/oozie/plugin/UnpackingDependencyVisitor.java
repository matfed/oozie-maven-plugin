package pl.edu.icm.maven.oozie.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

import java.io.File;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;

/**
 * A dependency visitor that unpacks them into a directory tree.
 *
 * @author Mateusz Fedoryszak (m.fedoryszak@icm.edu.pl)
 */
public class UnpackingDependencyVisitor implements DependencyVisitor {
    private File relative = new File("");
    private MojoExecutionException exception = null;

    private final File outputDirectory;
    private final File markersDirectory;
    private final ExecutionEnvironment environment;

    public UnpackingDependencyVisitor(File outputDirectory, File markersDirectory, ExecutionEnvironment environment) {
        this.outputDirectory = outputDirectory;
        this.markersDirectory = markersDirectory;
        this.environment = environment;
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        Artifact artifact = node.getDependency().getArtifact();

        try {
            executeMojo(
                    plugin(groupId("org.apache.maven.plugins"),
                            artifactId("maven-dependency-plugin"),
                            version(OoziePluginConstants.MAVEN_DEPENDENCY_PLUGIN_VERSION)),
                    goal("unpack-dependencies"),
                    configuration(
                            element(name("outputDirectory"), new File(outputDirectory, relative.getPath()).getPath()),
                            element(name("markersDirectory"), new File(markersDirectory, relative.getPath()).getPath()),
                            element(name("includeGroupIds"), artifact.getGroupId()),
                            element(name("includeArtifactIds"), artifact.getArtifactId())),
                    environment);
        } catch (MojoExecutionException e) {
            exception = e;
            return false;
        }
        relative = new File(relative, artifact.getGroupId() + "-" + artifact.getArtifactId());

        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        relative = relative.getParentFile();
        return exception != null;
    }

    public void throwIfFailed() throws MojoExecutionException {
        if (exception != null)
            throw exception;
    }
}
