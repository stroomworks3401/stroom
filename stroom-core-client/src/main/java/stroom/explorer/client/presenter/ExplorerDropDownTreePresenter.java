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

package stroom.explorer.client.presenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;
import stroom.data.client.event.DataSelectionEvent;
import stroom.data.client.event.DataSelectionEvent.DataSelectionHandler;
import stroom.data.client.event.HasDataSelectionHandlers;
import stroom.dispatch.client.ClientDispatchAsync;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.Folder;
import stroom.explorer.client.event.SelectionType;
import stroom.explorer.shared.ExplorerNode;
import stroom.widget.dropdowntree.client.presenter.DropDownTreePresenter;
import stroom.widget.popup.client.event.HidePopupEvent;

public class ExplorerDropDownTreePresenter extends DropDownTreePresenter
        implements HasDataSelectionHandlers<ExplorerNode> {
    private final ExtendedExplorerTree explorerTree;
    private boolean allowFolderSelection;
    @Inject
    public ExplorerDropDownTreePresenter(final EventBus eventBus, final DropDownTreeView view,
                                         final ClientDispatchAsync dispatcher) {
        super(eventBus, view);
        setUnselectedText("None");

        explorerTree = new ExtendedExplorerTree(this, dispatcher);

        // Add views.
        view.setCellTree(explorerTree);
    }

    private void setSelectedTreeItem(final ExplorerNode selectedItem,
                                     final SelectionType selectionType, final boolean fireEvents) {
        if (selectionType.isDoubleSelect()) {
            if (selectedItem == null) {
                DataSelectionEvent.fire(this, null, true);
                HidePopupEvent.fire(this, this);
            } else {
                // Is the selection type valid?
                if (isSelectionAllowed(selectedItem)) {
                    DataSelectionEvent.fire(this, selectedItem, true);
                    HidePopupEvent.fire(this, this);
                }
            }
        } else {
            if (selectedItem == null) {
                // Has the selected item changed to null.
                if (fireEvents) {
                    DataSelectionEvent.fire(this, null, false);
                }
            } else {
                // Has the selected item changed.
                if (isSelectionAllowed(selectedItem)) {
                    if (fireEvents) {
                        DataSelectionEvent.fire(this, selectedItem, false);
                    }
                }
            }
        }
    }

    private boolean isSelectionAllowed(final ExplorerNode selected) {
        if (allowFolderSelection) {
            return true;
        }

        return !Folder.ENTITY_TYPE.equals(selected.getType());
    }

    @Override
    public void nameFilterChanged(final String text) {
        explorerTree.changeNameFilter(text);
    }

    @Override
    public void unselect() {
        explorerTree.setSelectedItem(null);
    }

    public void reset() {
        explorerTree.getTreeModel().reset();
    }

    @Override
    public void refresh() {
        explorerTree.getTreeModel().refresh();
    }

    @Override
    public void focus() {
        explorerTree.setFocus(true);
    }

    public void setIncludedTypes(final String... includedTypes) {
        explorerTree.getTreeModel().setIncludedTypes(includedTypes);
    }

    public void setTags(final String... tags) {
        explorerTree.getTreeModel().setTags(tags);
    }

    public void setRequiredPermissions(final String... requiredPermissions) {
        explorerTree.getTreeModel().setRequiredPermissions(requiredPermissions);
    }

    public DocRef getSelectedEntityReference() {
        final ExplorerNode explorerNode = getSelectedExplorerData();
        if (explorerNode == null) {
            return null;
        }
        return explorerNode.getDocRef();
    }

    public void setSelectedEntityReference(final DocRef docRef) {
        if (docRef != null) {
            final ExplorerNode explorerNode = new ExplorerNode(null, docRef.getType(), docRef.getUuid(), docRef.getName(), null);
            setSelectedExplorerData(explorerNode);
        } else {
            setSelectedExplorerData(null);
        }
    }

    private ExplorerNode getSelectedExplorerData() {
        final ExplorerNode selected = explorerTree.getSelectionModel().getSelected();
        if (selected != null) {
            return selected;
        }
        return null;
    }

    private void setSelectedExplorerData(final ExplorerNode explorerNode) {
        if (explorerNode != null) {
            explorerTree.setSelectedItem(explorerNode);
            explorerTree.getTreeModel().reset();
            explorerTree.getTreeModel().setEnsureVisible(explorerNode);
            explorerTree.getTreeModel().refresh();
        } else {
            explorerTree.getTreeModel().reset();
            explorerTree.setSelectedItem(null);
            explorerTree.getTreeModel().refresh();
        }
    }

    public void setAllowFolderSelection(final boolean allowFolderSelection) {
        this.allowFolderSelection = allowFolderSelection;
    }

    @Override
    public HandlerRegistration addDataSelectionHandler(final DataSelectionHandler<ExplorerNode> handler) {
        return addHandlerToSource(DataSelectionEvent.getType(), handler);
    }

    @Override
    public void onHideRequest(final boolean autoClose, final boolean ok) {
        if (ok) {
            DataSelectionEvent.fire(this, explorerTree.getSelectionModel().getSelected(), false);
        }
        super.onHideRequest(autoClose, ok);
    }

    private static class ExtendedExplorerTree extends ExplorerTree {
        private final ExplorerDropDownTreePresenter explorerDropDownTreePresenter;

        public ExtendedExplorerTree(final ExplorerDropDownTreePresenter explorerDropDownTreePresenter, final ClientDispatchAsync dispatcher) {
            super(dispatcher, false);
            this.explorerDropDownTreePresenter = explorerDropDownTreePresenter;
        }

        @Override
        protected void setInitialSelectedItem(final ExplorerNode selection) {
            super.setInitialSelectedItem(selection);
            explorerDropDownTreePresenter.setSelectedTreeItem(selection, new SelectionType(false, false), true);
        }

        @Override
        protected void doSelect(final ExplorerNode selection, final SelectionType selectionType) {
            super.doSelect(selection, selectionType);
            explorerDropDownTreePresenter.setSelectedTreeItem(selection, selectionType, false);
        }
    }
}
