package stroom.template.set.client.view;

import stroom.item.client.SelectionBox;
import stroom.query.api.datasource.FieldType;
import stroom.template.set.client.presenter.TemplateSetFieldEditPresenter.TemplateSetFieldEditView;
import stroom.widget.tickbox.client.view.CustomCheckBox;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

import java.util.List;

public class TemplateSetFieldEditViewImpl extends ViewImpl implements TemplateSetFieldEditView {

    private final Widget widget;

    @UiField
    TextBox fieldName;
    @UiField
    SelectionBox<String> fieldType;
    @UiField
    TextBox defaultValue;
    @UiField
    CustomCheckBox required;

    @Inject
    public TemplateSetFieldEditViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void focus() {
        fieldName.setFocus(true);
    }

    @Override
    public FieldType getType() {
        return null;
    }

    @Override
    public void setType(final FieldType type) {

    }

    @Override
    public String getFieldName() {
        return fieldName.getText();
    }

    @Override
    public void setFieldName(final String fieldName) {
        this.fieldName.setText(fieldName);
    }

    @Override
    public String getFieldType() {
        return fieldType.getValue();
    }

    @Override
    public void setFieldType(final String fieldType) {
        this.fieldType.setValue(fieldType);
    }

    @Override
    public void setFieldTypes(final List<String> fieldTypes) {
        this.fieldType.clear();
        this.fieldType.addItems(fieldTypes);
    }

    @Override
    public String getDefaultValue() {
        return defaultValue.getText();
    }

    @Override
    public void setDefaultValue(final String defaultValue) {
        this.defaultValue.setText(defaultValue);
    }

    @Override
    public boolean isStored() {
        return false;
    }

    @Override
    public void setStored(final boolean stored) {

    }

    @Override
    public boolean isIndexed() {
        return false;
    }

    @Override
    public void setIndexed(final boolean indexed) {

    }

    @Override
    public boolean isMultiValued() {
        return false;
    }

    @Override
    public void setMultiValued(final boolean multiValued) {

    }

    @Override
    public boolean isRequired() {
        return required.getValue();
    }

    @Override
    public void setRequired(final boolean required) {
        this.required.setValue(required);
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void setDescription(final String description) {

    }

    public interface Binder extends UiBinder<Widget, TemplateSetFieldEditViewImpl> {
    }
}
