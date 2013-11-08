package pl.edu.icm.maven.oozie.plugin;

import org.apache.maven.plugin.logging.Log;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorException;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.util.graph.DefaultDependencyNode;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a full dependency tree of a given artifact.
 *
 * @author Mateusz Fedoryszak (m.fedoryszak@icm.edu.pl)
 */
public class FullWorkflowDependencyTreeBuilder {
    private final Log log;
    private final RepositorySystem repoSystem;
    private final List<RemoteRepository> remoteRepos;
    private final RepositorySystemSession repoSession;

    public FullWorkflowDependencyTreeBuilder(RepositorySystem repoSystem, RepositorySystemSession repoSession,
                                             List<RemoteRepository> remoteRepos, Log log) {
        this.repoSystem = repoSystem;
        this.remoteRepos = remoteRepos;
        this.repoSession = repoSession;
        this.log = log;
    }

    /**
     * Builds a full dependency tree of a given artifact. Doesn't prune duplicates nor conflicts. In case of cyclic
     * dependencies, stops resolution of a given dependency.
     *
     * Beware: such tree may be enormous!
     */
    public DependencyNode buildDependencyTree(Artifact af, DependencySelector selector) {
        DependencyNode root = new DefaultDependencyNode(new Dependency(af, null));
        augmentWithChildren(root, Collections.<Artifact>emptySet(), selector);
        return root;
    }

    private void augmentWithChildren(DependencyNode node, Set<Artifact> ancestors, DependencySelector selector) {
        if (ancestors.contains(node.getDependency().getArtifact()))
            return;

        Set<Artifact> ancestorsAndMe = new HashSet<Artifact>(ancestors);
        ancestorsAndMe.add(node.getDependency().getArtifact());

        for (Dependency d : getImmediateDependencies(node.getDependency().getArtifact())) {
            if (!selector.selectDependency(d))
                continue;
            DependencyNode child = new DefaultDependencyNode(d);
            augmentWithChildren(child, ancestorsAndMe, selector);
            node.getChildren().add(child);
        }
    }

    public List<Dependency> getImmediateDependencies(Artifact af) {

        ArtifactDescriptorRequest req = new ArtifactDescriptorRequest(af, remoteRepos, null);
        try {
            ArtifactDescriptorResult res = repoSystem.readArtifactDescriptor(repoSession, req);
            return res.getDependencies();
        } catch (ArtifactDescriptorException e) {
            log.error(e);

            return Collections.emptyList();
        }

    }
}
