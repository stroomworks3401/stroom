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

package stroom.template.set.client.gin;

import stroom.core.client.gin.PluginModule;
import stroom.template.set.client.TemplateSetPlugin;
import stroom.template.set.client.presenter.TemplateSetPresenter;
import stroom.template.set.client.presenter.TemplateSetSettingsPresenter;
import stroom.template.set.client.presenter.TemplateSetSettingsPresenter.TemplateSetSettingsView;
import stroom.template.set.client.presenter.TemplateSetFieldListPresenter;
import stroom.template.set.client.presenter.TemplateSetFieldListPresenter.TemplateSetFieldListView;
import stroom.template.set.client.presenter.TemplateSetFieldEditPresenter;
import stroom.template.set.client.presenter.TemplateSetFieldEditPresenter.TemplateSetFieldEditView;
import stroom.template.set.client.view.TemplateSetSettingsViewImpl;
import stroom.template.set.client.view.TemplateSetFieldListViewImpl;
import stroom.template.set.client.view.TemplateSetFieldEditViewImpl;

public class TemplateSetModule extends PluginModule {

    @Override
    protected void configure() {
        // Main plugin & presenter
        bindPlugin(TemplateSetPlugin.class);
        bind(TemplateSetPresenter.class);

        // Settings presenter & view
        bind(TemplateSetSettingsPresenter.class);
        bind(TemplateSetSettingsView.class).to(TemplateSetSettingsViewImpl.class);

        // Field list presenter & view
        bind(TemplateSetFieldListPresenter.class);
        bind(TemplateSetFieldListView.class).to(TemplateSetFieldListViewImpl.class);

        // Field edit presenter & view
        bind(TemplateSetFieldEditPresenter.class);
        bind(TemplateSetFieldEditView.class).to(TemplateSetFieldEditViewImpl.class);
    }
}
