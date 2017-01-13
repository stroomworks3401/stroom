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

import stroom.alert.client.event.AlertEvent;
import stroom.entity.client.event.CreateEntityEvent;
import stroom.entity.client.event.ShowCreateEntityDialogEvent;
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
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.MyPresenter;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.annotations.ProxyCodeSplit;
import com.gwtplatform.mvp.client.annotations.ProxyEvent;
import com.gwtplatform.mvp.client.proxy.Proxy;

public class CreateEntityPresenter
        extends MyPresenter<CreateEntityPresenter.CreateEntityView, CreateEntityPresenter.CreateEntityProxy>
        implements ShowCreateEntityDialogEvent.Handler, PopupUiHandlers {
    private final EntityTreePresenter entityTreePresenter;
    private String entityType;
    private String caption;
    private boolean allowNullFolder;
    @Inject
    public CreateEntityPresenter(final EventBus eventBus, final CreateEntityView view, final CreateEntityProxy proxy,
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
    public void onCreate(final ShowCreateEntityDialogEvent event) {
        entityType = event.getEntityType();

        entityTreePresenter.setSelectedItem(null);

        entityTreePresenter.setSelectedItem(event.getSelected());
        entityTreePresenter.getModel().reset();
        entityTreePresenter.getModel().setEnsureVisible(event.getSelected());
        entityTreePresenter.getModel().refresh();

        caption = "New " + event.getEntityDisplayType();
        allowNullFolder = event.isAllowNullFolder();

        forceReveal();
    }

    @Override
    protected void revealInParent() {
        getView().setName("");
        final PopupSize popupSize = new PopupSize(350, 400, 350, 350, 2000, 2000, true);
        ShowPopupEvent.fire(this, this, PopupType.OK_CANCEL_DIALOG, popupSize, caption, this);
        getView().focus();
    }

    @Override
    public void onHideRequest(final boolean autoClose, final boolean ok) {
        if (ok) {
            final ExplorerNode folder = getFolder();
            if (!allowNullFolder && folder == null) {
                AlertEvent.fireWarn(CreateEntityPresenter.this, "No parent folder has been selected", null);
            } else {
                String entityName = getView().getName();
                if (entityName != null) {
                    entityName = entityName.trim();
                }

                if (entityName == null || entityName.length() == 0) {
                    AlertEvent.fireWarn(CreateEntityPresenter.this,
                            "You must provide a name for the new " + entityType.toLowerCase(), null);
                } else {
                    CreateEntityEvent.fire(this, this, entityType, folder, entityName);
                }
            }
        } else {
            HidePopupEvent.fire(this, this, autoClose, ok);
        }
    }

    @Override
    public void onHide(final boolean autoClose, final boolean ok) {
        // Do nothing.
    }

    private ExplorerNode getFolder() {
        return entityTreePresenter.getSelectedItem();
    }

    public interface CreateEntityView extends View, HasUiHandlers<PopupUiHandlers> {
        String getName();

        void setName(String name);

        void setFolderView(View view);

        void setFoldersVisible(final boolean visible);

        void focus();
    }

    @ProxyCodeSplit
    public interface CreateEntityProxy extends Proxy<CreateEntityPresenter> {
    }
}
