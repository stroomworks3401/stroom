package stroom.template.set.client.view;

import stroom.template.set.client.presenter.TemplateSetTemplateEditPresenter.TemplateSetTemplateEditView;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextArea;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewImpl;

public class TemplateSetTemplateEditViewImpl extends ViewImpl implements TemplateSetTemplateEditView {

    private final Widget widget;

    @UiField
    TextBox templateName;

    @UiField
    TextArea fieldsJson;  // new field for JSON editing

    @Inject
    public TemplateSetTemplateEditViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void focus() {
        templateName.setFocus(true);
    }

    @Override
    public void setTemplateName(final String name) {
        templateName.setText(name);
    }

    @Override
    public String getTemplateName() {
        return templateName.getText();
    }

    @Override
    public void setFieldsJson(final String json) {
        fieldsJson.setText(json);
    }

    @Override
    public String getFieldsJson() {
        return fieldsJson.getText();
    }

    @Override
    public void showError(final String msg) {
        com.google.gwt.user.client.Window.alert(msg);
    }

    public interface Binder extends UiBinder<Widget, TemplateSetTemplateEditViewImpl> {}
}
