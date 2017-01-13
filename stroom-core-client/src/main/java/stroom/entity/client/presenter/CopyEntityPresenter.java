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

package stroom.entity.client.presenter;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.MyPresenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.proxy.Proxy;
import stroom.alert.client.event.AlertEvent;
import stroom.entity.client.event.CopyEntityEvent;
import stroom.entity.client.event.ShowCopyEntityDialogEvent;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.Folder;
import stroom.explorer.client.presenter.EntityTreePresenter;
import stroom.explorer.shared.ExplorerNode;
import stroom.security.shared.DocumentPermissionNames;
import stroom.widget.popup.client.event.HidePopupEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupUiHandlers;
import stroom.widget.popup.client.presenter.PopupView.PopupType;

import java.util.List;

public class CopyEntityPresenter
        extends MyPresenter<CopyEntityPresenter.CopyEntityView, CopyEntityPresenter.CopyEntityProxy>
        implements ShowCopyEntityDialogEvent.Handler, PopupUiHandlers {
    private final EntityTreePresenter entityTreePresenter;
    private List<ExplorerNode> explorerNodeList;
    private ExplorerNode entity;

    @Inject
    public CopyEntityPresenter(final EventBus eventBus, final CopyEntityView view, final CopyEntityProxy proxy,
                               final EntityTreePresenter entityTreePresenter) {
        super(eventBus, view, proxy);
        this.entityTreePresenter = entityTreePresenter;
        view.setUiHandlers(this);

        view.setFolderView(entityTreePresenter.getView());

        entityTreePresenter.setIncludedTypes(Folder.ENTITY_TYPE);
        entityTreePresenter.setRequiredPermissions(DocumentPermissionNames.USE, DocumentPermissionNames.READ);
    }

    @ProxyEvent
    @Override
    public void onCopy(final ShowCopyEntityDialogEvent event) {
        explorerNodeList = event.getExplorerNodeList();
        copyNextEntity();
    }

    private void copyNextEntity() {
        entity = getNextEntity();
        if (entity != null) {
            entityTreePresenter.setSelectedItem(null);

            entityTreePresenter.setSelectedItem(entity);
            entityTreePresenter.getModel().reset();
            entityTreePresenter.getModel().setEnsureVisible(entity);
            entityTreePresenter.getModel().refresh();

            forceReveal();
        }
    }

    private ExplorerNode getNextEntity() {
        while (explorerNodeList.size() > 0) {
            return explorerNodeList.remove(0);
        }
        return null;
    }

    @Override
    protected void revealInParent() {
        final String caption = "Copy " + entity.getDisplayValue();
        getView().setName(entity.getDisplayValue());
        final PopupSize popupSize = new PopupSize(350, 400, 350, 350, 2000, 2000, true);
        ShowPopupEvent.fire(this, this, PopupType.OK_CANCEL_DIALOG, popupSize, caption, this);
        getView().focus();
    }

    @Override
    public void onHideRequest(final boolean autoClose, final boolean ok) {
        if (ok) {
            final ExplorerNode folder = getFolder();
            // if (!allowNullFolder && folder == null) {
            // AlertEvent.fireWarn(CopyEntityPresenter.this,
            // "No parent group has been selected", null);
            // } else {
            String entityName = getView().getName();
            if (entityName != null) {
                entityName = entityName.trim();
            }

            if (entityName == null || entityName.length() == 0) {
                AlertEvent.fireWarn(CopyEntityPresenter.this,
                        "You must provide a name for the new " + entity.getType().toLowerCase(), null);
            } else {
                CopyEntityEvent.fire(CopyEntityPresenter.this, CopyEntityPresenter.this, entity,
                        folder, entityName);
            }
            // }
        } else {
            HidePopupEvent.fire(CopyEntityPresenter.this, CopyEntityPresenter.this, autoClose, ok);
        }
    }

    @Override
    public void onHide(final boolean autoClose, final boolean ok) {
        // If there are any more entities that are to be copied then go through the whole process again.
        copyNextEntity();
    }

    private ExplorerNode getFolder() {
        return entityTreePresenter.getSelectedItem();
    }

    public interface CopyEntityView extends View, HasUiHandlers<PopupUiHandlers> {
        String getName();

        void setName(String name);

        void setFolderView(View view);

        void focus();
    }

    @ProxyCodeSplit
    public interface CopyEntityProxy extends Proxy<CopyEntityPresenter> {
    }
}
