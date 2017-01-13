package stroom.explorer.shared;

import stroom.util.shared.SharedObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeStructure implements SharedObject {
    private ExplorerNode root;
    private Map<ExplorerNode, ExplorerNode> parentMap = new HashMap<>();
    private Map<ExplorerNode, List<ExplorerNode>> childMap = new HashMap<>();

    public TreeStructure() {
        // Default constructor necessary for GWT serialisation.
    }

    public void add(final ExplorerNode parent, final ExplorerNode child) {
        if (parent == null) {
            root = child;
        }

        parentMap.put(child, parent);

        List<ExplorerNode> children = childMap.get(parent);
        if (children == null) {
            children = new ArrayList<>();
            childMap.put(parent, children);
        }
        children.add(child);
    }

    public ExplorerNode getRoot() {
        return root;
    }

    public ExplorerNode getParent(final ExplorerNode child) {
        return parentMap.get(child);
    }

    public List<ExplorerNode> getChildren(final ExplorerNode parent) {
        return childMap.get(parent);
    }
}
