package stroom.explorer.server;

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.closuretable.TreePath;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "EXPLORER_TREE_PATH")
@IdClass(CompositeExplorerTreePathId.class)    // has a composite primary key
public class ExplorerTreePath implements TreePath {
    private ClosureTableTreeNode ancestor;
    private ClosureTableTreeNode descendant;
    private int depth;
    private int orderIndex;

    @Id
    @ManyToOne(targetEntity = ExplorerTreeNode.class)
    @JoinColumn(name = "ANCESTOR", columnDefinition = "INT", nullable = false)    // the name of the database foreign key column
    @Override
    public ClosureTableTreeNode getAncestor() {
        return ancestor;
    }

    @Override
    public void setAncestor(ClosureTableTreeNode ancestor) {
        this.ancestor = ancestor;
    }

    @Id
    @ManyToOne(targetEntity = ExplorerTreeNode.class)
    @JoinColumn(name = "DESCENDANT", columnDefinition = "INT", nullable = false)    // the name of the database foreign key column
    @Override
    public ClosureTableTreeNode getDescendant() {
        return descendant;
    }

    @Override
    public void setDescendant(ClosureTableTreeNode descendant) {
        this.descendant = descendant;
    }

    @Column(name = "DEPTH", columnDefinition = "INT", nullable = false)
    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Column(name = "ORDER_INDEX", columnDefinition = "INT", nullable = false)
    @Override
    public int getOrderIndex() {
        return orderIndex;
    }

    @Override
    public void setOrderIndex(int position) {
        this.orderIndex = position;
    }
}