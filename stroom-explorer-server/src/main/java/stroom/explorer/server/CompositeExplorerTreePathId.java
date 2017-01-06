package stroom.explorer.server;

import stroom.pipeline.shared.data.PipelineProperty;
import stroom.util.shared.EqualsBuilder;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

public class CompositeExplorerTreePathId implements Serializable {
    private long ancestor;
    private long descendant;

    public long getAncestor() {
        return ancestor;
    }

    public void setAncestor(long ancestorId) {
        this.ancestor = ancestorId;
    }

    public long getDescendant() {
        return descendant;
    }

    public void setDescendant(long descendantId) {
        this.descendant = descendantId;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof CompositeExplorerTreePathId)) {
            return false;
        }

        final CompositeExplorerTreePathId compositeExplorerTreePathId = (CompositeExplorerTreePathId) o;
        final EqualsBuilder builder = new EqualsBuilder();
        builder.append(ancestor, compositeExplorerTreePathId.ancestor);
        builder.append(descendant, compositeExplorerTreePathId.descendant);
        return builder.isEquals();
    }

    @Override
    public int hashCode() {
        return Long.hashCode(getDescendant()) * 31 + Long.hashCode(getAncestor());
    }
}