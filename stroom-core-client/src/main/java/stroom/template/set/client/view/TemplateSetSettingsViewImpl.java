package stroom.template.set.client.view;

import stroom.entity.client.presenter.ReadOnlyChangeHandler;
import stroom.template.set.client.presenter.TemplateSetSettingsPresenter.TemplateSetSettingsView;
import stroom.template.set.client.presenter.TemplateSetSettingsUiHandlers;
import stroom.svg.shared.SvgImage;
import stroom.widget.button.client.Button;

import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.ViewWithUiHandlers;

public class TemplateSetSettingsViewImpl
        extends ViewWithUiHandlers<TemplateSetSettingsUiHandlers>
        implements TemplateSetSettingsView, ReadOnlyChangeHandler {

    private final Widget widget;

    @UiField
    TextBox name;
    @UiField
    TextArea description;
    @UiField
    SimplePanel defaultPipeline;
    @UiField
    Button testTemplate;

    @Inject
    public TemplateSetSettingsViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
        testTemplate.setIcon(SvgImage.OK);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    private void fireChange() {
        if (getUiHandlers() != null) {
            getUiHandlers().onChange();
        }
    }

    @Override
    public String getDescription() {
        return description.getText().trim();
    }

    @Override
    public void setDescription(final String description) {
        this.description.setText(description != null ? description : "");
    }

    @Override
    public void onReadOnly(final boolean readOnly) {
        name.setEnabled(!readOnly);
        description.setEnabled(!readOnly);
        testTemplate.setEnabled(!readOnly);
    }

    @UiHandler("name")
    public void onNameKeyDown(final KeyDownEvent event) {
        fireChange();
    }

    @UiHandler("description")
    public void onDescriptionKeyDown(final KeyDownEvent event) {
        fireChange();
    }

    public interface Binder extends UiBinder<Widget, TemplateSetSettingsViewImpl> {
    }
}
