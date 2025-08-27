package stroom.template.set.client.gin;

import stroom.core.client.gin.PluginModule;
import stroom.template.set.client.TemplateSetPlugin;
import stroom.template.set.client.presenter.TemplateSetPresenter;
import stroom.template.set.client.presenter.TemplateSetSettingsPresenter;
import stroom.template.set.client.presenter.TemplateSetSettingsPresenter.TemplateSetSettingsView;
import stroom.template.set.client.presenter.TemplateSetTemplateListPresenter;
import stroom.template.set.client.presenter.TemplateSetTemplateListPresenter.TemplateSetTemplateListView;
import stroom.template.set.client.view.TemplateSetSettingsViewImpl;
import stroom.template.set.client.view.TemplateSetTemplateListViewImpl;

public class TemplateSetModule extends PluginModule {

    @Override
    protected void configure() {
        // Main plugin & presenter
        bindPlugin(TemplateSetPlugin.class);
        bind(TemplateSetPresenter.class);

        // Settings presenter & view
        bind(TemplateSetSettingsPresenter.class);
        bind(TemplateSetSettingsView.class).to(TemplateSetSettingsViewImpl.class);

        // Template list presenter & view
        bind(TemplateSetTemplateListPresenter.class);
        bind(TemplateSetTemplateListView.class).to(TemplateSetTemplateListViewImpl.class);
    }
}
