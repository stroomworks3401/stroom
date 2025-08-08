/*
 * Copyright 2025 Crown Copyright
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
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.template.set.client.presenter.TemplateSetSettingsPresenter.TemplateSetSettingsView;
import stroom.template.set.shared.TemplateSetDoc;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.HasUiHandlers;
import com.gwtplatform.mvp.client.View;

public class TemplateSetSettingsPresenter
        extends DocumentEditPresenter<TemplateSetSettingsView, TemplateSetDoc>
        implements TemplateSetSettingsUiHandlers {

    @Inject
    public TemplateSetSettingsPresenter(final EventBus eventBus,
                                        final TemplateSetSettingsView view) {
        super(eventBus, view);
        view.setUiHandlers(this);
    }

    @Override
    protected void onBind() {
        // No extra handlers needed yet
    }

    @Override
    public void onChange() {
        setDirty(true);
    }

    @Override
    protected void onRead(final DocRef docRef, final TemplateSetDoc templateSet, final boolean readOnly) {
        getView().setDescription(templateSet.getDescription());
    }

    @Override
    protected TemplateSetDoc onWrite(final TemplateSetDoc templateSet) {
        templateSet.setDescription(getView().getDescription());
        return templateSet;
    }

    public interface TemplateSetSettingsView
            extends View, ReadOnlyChangeHandler, HasUiHandlers<TemplateSetSettingsUiHandlers> {

        String getDescription();
        void setDescription(String description);
    }
}
