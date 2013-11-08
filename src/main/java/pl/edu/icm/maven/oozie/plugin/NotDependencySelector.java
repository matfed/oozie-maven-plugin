package pl.edu.icm.maven.oozie.plugin;

import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;

/**
 * A dependency selector that negates the one it contains.
 *
 * @author Mateusz Fedoryszak (m.fedoryszak@icm.edu.pl)
 */
public class NotDependencySelector implements DependencySelector {

    private final DependencySelector selector;

    public NotDependencySelector(DependencySelector selector) {
        this.selector = selector;
    }

    public boolean selectDependency(Dependency dependency) {
        return !selector.selectDependency(dependency);
    }

    public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
        return new NotDependencySelector(selector.deriveChildSelector(context));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }

        NotDependencySelector that = (NotDependencySelector) obj;
        return selector.equals(that.selector);
    }

    @Override
    public int hashCode() {
        int hash = getClass().hashCode();
        hash = hash * 31 + selector.hashCode();
        return hash;
    }
}