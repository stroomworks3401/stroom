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

import stroom.alert.client.event.AlertEvent;
import stroom.query.api.datasource.FieldType;
import stroom.template.set.client.presenter.TemplateSetFieldEditPresenter.TemplateSetFieldEditView;
import stroom.template.set.shared.TemplateSetField;
import stroom.widget.popup.client.event.HidePopupRequestEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;

import com.google.gwt.user.client.ui.Focus;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

import java.util.List;
import java.util.Set;

public class TemplateSetFieldEditPresenter
        extends MyPresenterWidget<TemplateSetFieldEditView> {

    private Set<String> otherFieldNames;

    @Inject
    public TemplateSetFieldEditPresenter(final EventBus eventBus,
                                         final TemplateSetFieldEditView view) {
        super(eventBus, view);
    }

    public void read(final TemplateSetField field,
                     final Set<String> otherFieldNames,
                     final List<String> fieldTypes) {
        getView().setFieldTypes(fieldTypes);

        this.otherFieldNames = otherFieldNames;
        getView().setType(field.getFldType());
        getView().setFieldName(field.getFldName());
        getView().setFieldType(field.getNativeType());
        getView().setDefaultValue(field.getDefaultValue());
        getView().setStored(field.isStored());
        getView().setIndexed(field.isIndexed());
        getView().setMultiValued(field.isMultiValued());
        getView().setRequired(field.isRequired());
        getView().setDescription(field.getDescription());
    }

    public TemplateSetField write() {
        String name = getView().getFieldName();
        name = name != null ? name.trim() : "";

        if (name.isEmpty()) {
            AlertEvent.fireWarn(this, "A template set field must have a name", null);
            return null;
        }
        if (otherFieldNames.contains(name)) {
            AlertEvent.fireWarn(this, "A template set field with this name already exists", null);
            return null;
        }

        String defaultValue = null;
        if (getView().getDefaultValue() != null &&
            !getView().getDefaultValue().trim().isEmpty()) {
            defaultValue = getView().getDefaultValue();
        }

        return TemplateSetField
                .builder()
                .fldType(getView().getType())
                .fldName(name)
                .nativeType(getView().getFieldType())
                .defaultValue(defaultValue)
                .stored(getView().isStored())
                .indexed(getView().isIndexed())
                .multiValued(getView().isMultiValued())
                .required(getView().isRequired())
                .description(getView().getDescription())
                .build();
    }

    public void show(final String caption, final HidePopupRequestEvent.Handler handler) {
        final PopupSize popupSize = PopupSize.resizable(300, 350);
        ShowPopupEvent.builder(this)
                .popupType(PopupType.OK_CANCEL_DIALOG)
                .popupSize(popupSize)
                .caption(caption)
                .onShow(e -> getView().focus())
                .onHideRequest(handler)
                .fire();
    }

    public interface TemplateSetFieldEditView extends View, Focus {

        FieldType getType();
        void setType(FieldType type);

        String getFieldName();
        void setFieldName(String fieldName);

        String getFieldType();
        void setFieldType(String fieldType);

        String getDefaultValue();
        void setDefaultValue(String defaultValue);

        boolean isStored();
        void setStored(boolean stored);

        boolean isIndexed();
        void setIndexed(boolean indexed);

        boolean isMultiValued();
        void setMultiValued(boolean multiValued);

        boolean isRequired();
        void setRequired(boolean required);

        String getDescription();
        void setDescription(String description);

        void setFieldTypes(List<String> fieldTypes);
    }
}
