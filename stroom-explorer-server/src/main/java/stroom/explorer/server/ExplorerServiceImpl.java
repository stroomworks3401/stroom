/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.explorer.server;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentServiceLocator;
import stroom.entity.shared.DocumentType;
import stroom.explorer.shared.DocumentTypes;
import stroom.explorer.shared.ExplorerNode;
import stroom.explorer.shared.ExplorerTreeFilter;
import stroom.explorer.shared.FetchExplorerDataResult;
import stroom.explorer.shared.FindExplorerDataCriteria;
import stroom.security.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.shared.HasNodeState;
import stroom.util.spring.StroomScope;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Scope(StroomScope.THREAD)
class ExplorerServiceImpl implements ExplorerService {
    private final SecurityContext securityContext;
    private final DocumentServiceLocator documentServiceLocator;
    private final ExplorerTreeModel explorerTreeModel;

    @Inject
    ExplorerServiceImpl(final SecurityContext securityContext,final DocumentServiceLocator documentServiceLocator, final ExplorerTreeModel explorerTreeModel) {
        this.securityContext = securityContext;
        this.documentServiceLocator = documentServiceLocator;
        this.explorerTreeModel = explorerTreeModel;
    }

    @Override
    public FetchExplorerDataResult getData(final FindExplorerDataCriteria criteria) {
        final FetchExplorerDataResult result = new FetchExplorerDataResult();

        // Get the master tree model.
        final TreeModel masterTreeModel = explorerTreeModel.getModel();

        // See if we need to open any more folders to see nodes we want to ensure are visible.
        final Set<ExplorerNode> forcedOpen = getForcedOpenItems(masterTreeModel, criteria);

        final TreeModel filteredModel = new TreeModelImpl();
        final List<ExplorerNode> roots = masterTreeModel.getChildMap().get(null);
        for (final ExplorerNode root : roots) {
            addDescendants(root, masterTreeModel, filteredModel, criteria.getFilter());

            // Add root node.
            result.getTreeStructure().add(null, root);
            addChildren(root, filteredModel, criteria.getOpenItems(), forcedOpen, criteria.getMinDepth(), 0, result);
        }

        return result;
    }

    private Set<ExplorerNode> getForcedOpenItems(final TreeModel masterTreeModel, final FindExplorerDataCriteria criteria) {
        final Set<ExplorerNode> forcedOpen = new HashSet<>();
        if (criteria.getEnsureVisible() != null && criteria.getEnsureVisible().size() > 0) {
            for (final ExplorerNode ensureVisible : criteria.getEnsureVisible()) {

                ExplorerNode parent = masterTreeModel.getParentMap().get(ensureVisible);
                while (parent != null) {
                    forcedOpen.add(parent);
                    parent = masterTreeModel.getParentMap().get(parent);
                }
            }
        }
        return forcedOpen;
    }

    private boolean addDescendants(final ExplorerNode parent, final TreeModel treeModelIn, final TreeModel treeModelOut, final ExplorerTreeFilter filter) {
        boolean added = false;

        final List<ExplorerNode> children = treeModelIn.getChildMap().get(parent);
        if (children != null) {
            for (final ExplorerNode child : children) {
                // Recurse right down to find out if a descendant is being added and therefore if we need to include this as an ancestor.
                final boolean hasChildren = addDescendants(child, treeModelIn, treeModelOut, filter);
                if (hasChildren) {
                    treeModelOut.add(parent, child);
                    added = true;
                    //                    child.setNodeState(HasNodeState.NodeState.CLOSED);
                    //                    parent.setNodeState(HasNodeState.NodeState.CLOSED);

                } else if (checkSecurity(child, filter.getRequiredPermissions())
                        && checkType(child, filter.getIncludedTypes())
                        && checkTags(child, filter.getTags())
                        && checkName(child, filter.getNameFilter())) {
                    treeModelOut.add(parent, child);
                    added = true;
                    //                    child.setNodeState(HasNodeState.NodeState.LEAF);
                }
            }
        }

        return added;
    }

    private boolean checkSecurity(final ExplorerNode explorerNode, final Set<String> requiredPermissions) {
        if (requiredPermissions == null || requiredPermissions.size() == 0) {
            return false;
        }

        final String type = explorerNode.getType();
        final String uuid = explorerNode.getDocRef().getUuid();
        for (final String permission : requiredPermissions) {
            if (!securityContext.hasDocumentPermission(type, uuid, permission)) {
                return false;
            }
        }

        return true;
    }

    private boolean checkType(final ExplorerNode explorerNode, final Set<String> types) {
        return types == null || types.contains(explorerNode.getType()) || "System".equals(explorerNode.getType());
    }

    private boolean checkTags(final ExplorerNode explorerNode, final Set<String> tags) {
        if (tags == null) {
            return true;
        } else if (explorerNode.getTags() != null && explorerNode.getTags().length() > 0 && tags.size() > 0) {
            for (final String tag : tags) {
                if (explorerNode.getTags().contains(tag)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkName(final ExplorerNode explorerNode, final String nameFilter) {
        return nameFilter == null || explorerNode.getDisplayValue().toLowerCase().contains(nameFilter.toLowerCase());
    }

    private void addChildren(final ExplorerNode parent, final TreeModel filteredModel, final Set<ExplorerNode> openItems, final Set<ExplorerNode> forcedOpen, final Integer minDepth, final int currentDepth, final FetchExplorerDataResult result) {
        parent.setDepth(currentDepth);

        // See if we need to force this item open.
        boolean force = false;
        if (forcedOpen.contains(parent)) {
            force = true;
            result.getOpenedItems().add(parent);
        }

        final List<ExplorerNode> children = filteredModel.getChildMap().get(parent);
        if (children == null) {
            parent.setNodeState(HasNodeState.NodeState.LEAF);

        } else if (force || openItems.contains(parent) || currentDepth < minDepth) {
            parent.setNodeState(HasNodeState.NodeState.OPEN);
            for (final ExplorerNode child : children) {
                result.getTreeStructure().add(parent, child);
                addChildren(child, filteredModel, openItems, forcedOpen, minDepth, currentDepth + 1, result);
            }

        } else {
            parent.setNodeState(HasNodeState.NodeState.CLOSED);
        }
    }

    public DocumentTypes getAllDocumentTypes() {
        final List<DocumentType> allTypes = documentServiceLocator.getTypes();
        final List<DocumentType> visibleTypes = getVisibleTypes();
        return new DocumentTypes(allTypes, visibleTypes);
    }

    private List<DocumentType> getVisibleTypes() {
        // Get the master tree model.
        final TreeModel masterTreeModel = explorerTreeModel.getModel();

        // Filter the model by user permissions.
        final Set<String> requiredPermissions = new HashSet<>();
        requiredPermissions.add(DocumentPermissionNames.READ);

        final Set<String> visibleTypes = new HashSet<>();
        final List<ExplorerNode> roots = masterTreeModel.getChildMap().get(null);
        for (final ExplorerNode root : roots) {
            addTypes(root, masterTreeModel, visibleTypes, requiredPermissions);
        }

        final List<DocumentType> documentTypes = documentServiceLocator.getTypes();
        final List<DocumentType> filtered = documentTypes.stream().filter(documentType -> visibleTypes.contains(documentType.getType())).collect(Collectors.toList());
        return filtered;
    }

    private boolean addTypes(final ExplorerNode parent, final TreeModel treeModel, final Set<String> types, final Set<String> requiredPermissions) {
        boolean added = false;

        final List<ExplorerNode> children = treeModel.getChildMap().get(parent);
        if (children != null) {
            for (final ExplorerNode child : children) {
                // Recurse right down to find out if a descendant is being added and therefore if we need to include this type as it is an ancestor.
                final boolean hasChildren = addTypes(child, treeModel, types, requiredPermissions);
                if (hasChildren) {
                    types.add(child.getType());
                    added = true;
                } else if (checkSecurity(child, requiredPermissions)) {
                    types.add(child.getType());
                    added = true;
                }
            }
        }

        return added;
    }

    @Override
    public List<DocRef> getDescendants(final DocRef ancestor) {
        final List<DocRef> descendants = new ArrayList<>();
        final ExplorerNode root = new ExplorerNode(null, ancestor.getType(), ancestor.getUuid(), ancestor.getName(), null);
        addRefs(root, descendants);
        return descendants;
    }

    private void addRefs(final ExplorerNode parent, final List<DocRef> refs) {
        final List<ExplorerNode> children = explorerTreeModel.getModel().getChildMap().get(parent);
        if (children != null) {
            for (final ExplorerNode child : children) {
                refs.add(child.getDocRef());
                addRefs(child, refs);
            }
        }
    }
}