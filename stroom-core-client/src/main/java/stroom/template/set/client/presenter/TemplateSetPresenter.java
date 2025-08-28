package stroom.template.set.client.presenter;

import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditTabPresenter;
import stroom.entity.client.presenter.DocumentEditTabProvider;
import stroom.entity.client.presenter.LinkTabPanelView;
import stroom.entity.client.presenter.MarkdownEditPresenter;
import stroom.entity.client.presenter.MarkdownTabProvider;
import stroom.security.client.presenter.DocumentUserPermissionsTabProvider;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetItem;
import stroom.template.set.shared.TemplateSetResource;
import stroom.widget.tab.client.presenter.TabData;
import stroom.widget.tab.client.presenter.TabDataImpl;
import stroom.dispatch.client.RestFactory;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import javax.inject.Provider;
import java.util.Collections;
import java.util.List;

public class TemplateSetPresenter
        extends DocumentEditTabPresenter<LinkTabPanelView, TemplateSetDoc> {

    private static final TabData TEMPLATES = new TabDataImpl("Templates");
    private static final TabData SETTINGS = new TabDataImpl("Settings");
    private static final TabData DOCUMENTATION = new TabDataImpl("Documentation");
    private static final TabData PERMISSIONS = new TabDataImpl("Permissions");

    private final TemplateSetTemplateListPresenter templateListPresenter;
    private final TemplateSetResource templateSetResource;
    private final RestFactory restFactory;

    @Inject
    public TemplateSetPresenter(final EventBus eventBus,
                                final LinkTabPanelView view,
                                final Provider<TemplateSetSettingsPresenter> settingsPresenterProvider,
                                final Provider<TemplateSetTemplateListPresenter> templateListPresenterProvider,
                                final Provider<MarkdownEditPresenter> markdownEditPresenterProvider,
                                final DocumentUserPermissionsTabProvider<TemplateSetDoc> documentUserPermissionsTabProvider,
                                final TemplateSetResource templateSetResource,
                                final RestFactory restFactory) {
        super(eventBus, view);
        this.templateListPresenter = templateListPresenterProvider.get();
        this.templateSetResource = templateSetResource;
        this.restFactory = restFactory;

        // Templates tab
        addTab(TEMPLATES, new DocumentEditTabProvider<>(() -> templateListPresenter));

        // Settings tab
        addTab(SETTINGS, new DocumentEditTabProvider<>(settingsPresenterProvider::get));

        // Documentation tab
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

        // Permissions tab
        addTab(PERMISSIONS, documentUserPermissionsTabProvider);

        selectTab(TEMPLATES);
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

    @Override
    protected void onRead(final DocRef docRef,
                          final TemplateSetDoc document,
                          final boolean readOnly) {
        super.onRead(docRef, document, readOnly);

        if (document.getSetUuid() != null) {
            restFactory
                    .create(templateSetResource)
                    .method(res -> res.getTemplatesForSet(document.getSetUuid()))
                    .onSuccess((List<TemplateSetItem> items) ->
                            templateListPresenter.setTemplates(items))
                    .onFailure(caught ->
                            templateListPresenter.setTemplates(Collections.emptyList()))
                    .taskMonitorFactory(null, "Fetching templates")
                    .exec();
        } else {
            templateListPresenter.setTemplates(Collections.emptyList());
        }
    }
}
