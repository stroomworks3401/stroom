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

import stroom.core.client.LocationManager;
import stroom.dispatch.client.ExportFileCompleteUtil;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetResource;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.MarkdownEditPresenter;
import stroom.entity.client.presenter.MarkdownTabProvider;
import stroom.security.client.presenter.DocumentUserPermissionsTabProvider;
import stroom.svg.client.SvgPresets;
import stroom.widget.button.client.ButtonView;
import stroom.widget.button.client.SvgButton;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Provider;

public class TemplateSetPresenter
        extends DocumentEditTabPresenter<LinkTabPanelView, TemplateSetDoc> {

    private static final TemplateSetResource TEMPLATE_SET_RESOURCE = GWT.create(TemplateSetResource.class);

    private static final TabData DOCUMENTATION = new TabDataImpl("Documentation");
    private static final TabData PERMISSIONS = new TabDataImpl("Permissions");

    private final ButtonView downloadButton;
    private final RestFactory restFactory;
    private final LocationManager locationManager;

    private DocRef docRef;

    @Inject
    public TemplateSetPresenter(final EventBus eventBus,
                                final LinkTabPanelView view,
                                final Provider<MarkdownEditPresenter> markdownEditPresenterProvider,
                                final DocumentUserPermissionsTabProvider<TemplateSetDoc>
                                          documentUserPermissionsTabProvider,
                                final RestFactory restFactory,
                                final LocationManager locationManager) {
        super(eventBus, view);
        this.restFactory = restFactory;
        this.locationManager = locationManager;

        downloadButton = SvgButton.create(SvgPresets.DOWNLOAD);
        toolbar.addButton(downloadButton);

        addTab(DOCUMENTATION, new MarkdownTabProvider<TemplateSetDoc>(eventBus, markdownEditPresenterProvider) {
            @Override
            public void onRead(final MarkdownEditPresenter presenter,
                               final DocRef docRef,
                               final TemplateSetDoc document,
                               final boolean readOnly) {
                presenter.setText(document.getData());
                presenter.setReadOnly(readOnly);
                // Select the tab here to ensure the markdown editor toolbar display state (based
                // on the readOnly value) is updated.
                selectTab(DOCUMENTATION);
            }

            @Override
            public TemplateSetDoc onWrite(final MarkdownEditPresenter presenter,
                                            final TemplateSetDoc document) {
                document.setData(presenter.getText());
                return document;
            }
        });
        addTab(PERMISSIONS, documentUserPermissionsTabProvider);
        selectTab(DOCUMENTATION);
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(downloadButton.addClickHandler(clickEvent -> {
            restFactory
                    .create(TEMPLATE_SET_RESOURCE)
                    .method(res -> res.download(docRef))
                    .onSuccess(result -> ExportFileCompleteUtil.onSuccess(locationManager, this, result))
                    .taskMonitorFactory(this)
                    .exec();
        }));
    }

    @Override
    public void onRead(final DocRef docRef, final TemplateSetDoc doc, final boolean readOnly) {
        super.onRead(docRef, doc, readOnly);
        this.docRef = docRef;
        downloadButton.setEnabled(true);
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
