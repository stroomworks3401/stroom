/*
 * Copyright 2017 Crown Copyright
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

package stroom.template.set.client.presenter;

import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.DocumentEditTabProvider;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.MarkdownEditPresenter;
import stroom.entity.client.presenter.MarkdownTabProvider;
import stroom.security.client.presenter.DocumentUserPermissionsTabProvider;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Provider;

public class TemplateSetPresenter extends DocumentEditTabPresenter<LinkTabPanelView, TemplateSetDoc> {

    private static final TabData FIELDS = new TabDataImpl("Templates");
    private static final TabData SETTINGS = new TabDataImpl("Settings");
    private static final TabData DOCUMENTATION = new TabDataImpl("Documentation");
    private static final TabData PERMISSIONS = new TabDataImpl("Permissions");

    @Inject
    public TemplateSetPresenter(final EventBus eventBus,
                                final LinkTabPanelView view,
                                final Provider<TemplateSetSettingsPresenter> settingsPresenterProvider,
                                final Provider<TemplateSetFieldListPresenter> fieldListPresenterProvider,
                                final Provider<MarkdownEditPresenter> markdownEditPresenterProvider,
                                final DocumentUserPermissionsTabProvider<TemplateSetDoc>
                                        documentUserPermissionsTabProvider) {
        super(eventBus, view);

        addTab(FIELDS, new DocumentEditTabProvider<>(fieldListPresenterProvider::get));
        addTab(SETTINGS, new DocumentEditTabProvider<>(settingsPresenterProvider::get));
        addTab(DOCUMENTATION, new MarkdownTabProvider<TemplateSetDoc>(eventBus, markdownEditPresenterProvider) {
            @Override
            public void onRead(final MarkdownEditPresenter presenter,
                               final DocRef docRef,
                               final TemplateSetDoc document,
                               final boolean readOnly) {
                presenter.setText(document.getDescription());
                presenter.setReadOnly(readOnly);
            }

            @Override
            public TemplateSetDoc onWrite(final MarkdownEditPresenter presenter,
                                          final TemplateSetDoc document) {
                document.setDescription(presenter.getText());
                return document;
            }
        });
        addTab(PERMISSIONS, documentUserPermissionsTabProvider);
        selectTab(FIELDS);
    }

    @Override
    public String getType() {
        return TemplateSetDoc.TYPE;
    }

    @Override
    protected TabData getPermissionsTab() {
        return PERMISSIONS;
    }

    @Override
    protected TabData getDocumentationTab() {
        return DOCUMENTATION;
    }
}
