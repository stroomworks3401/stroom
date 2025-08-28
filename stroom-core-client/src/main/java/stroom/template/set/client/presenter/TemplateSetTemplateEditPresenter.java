package stroom.template.set.client.presenter;

import stroom.docref.DocRef;
import stroom.template.set.shared.TemplateSetItem;
import stroom.widget.popup.client.event.HidePopupRequestEvent;
import stroom.widget.popup.client.event.ShowPopupEvent;
import stroom.widget.popup.client.presenter.PopupSize;
import stroom.widget.popup.client.presenter.PopupType;

import com.google.gwt.user.client.ui.Focus;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.MyPresenterWidget;
import com.gwtplatform.mvp.client.View;

public class TemplateSetTemplateEditPresenter extends MyPresenterWidget<TemplateSetTemplateEditPresenter.TemplateSetTemplateEditView> {

    private String existingName;
    private String parentSetUuid;

    @Inject
    public TemplateSetTemplateEditPresenter(final EventBus eventBus, final TemplateSetTemplateEditView view) {
        super(eventBus, view);
    }

    public void read(final TemplateSetItem templateItem) {
        existingName = templateItem.getName();
        parentSetUuid = templateItem.getSetUuid(); // remember parent set
        getView().setTemplateName(templateItem.getName());
        getView().setFieldsJson(templateItem.getFields() != null ? templateItem.getFields() : "");
    }

    public TemplateSetItem write() {
        String name = getView().getTemplateName().trim();
        if (name.isEmpty()) {
            getView().showError("Template name cannot be empty");
            return null;
        }

        String fieldsJson = getView().getFieldsJson().trim();

        TemplateSetItem item = TemplateSetItem.builder()
                .uuid(templateItemUuid()) // generate if null
                .name(name)
                .fields(fieldsJson.isEmpty() ? null : fieldsJson) // set JSON
                .setUuid(parentSetUuid) // ensure template links to parent set
                .build();

        return item;
    }

    public void show(final String caption, final HidePopupRequestEvent.Handler handler) {
        ShowPopupEvent.builder(this)
                .popupType(PopupType.OK_CANCEL_DIALOG)
                .popupSize(PopupSize.resizable(400, 300)) // increased size for JSON editing
                .caption(caption)
                .onShow(e -> getView().focus())
                .onHideRequest(handler)
                .fire();
    }

    private String templateItemUuid() {
        return existingName != null ? existingName : java.util.UUID.randomUUID().toString();
    }

    public interface TemplateSetTemplateEditView extends View, Focus {
        void setTemplateName(String name);
        String getTemplateName();

        void setFieldsJson(String json);
        String getFieldsJson();
        void showError(String msg);
    }
}

