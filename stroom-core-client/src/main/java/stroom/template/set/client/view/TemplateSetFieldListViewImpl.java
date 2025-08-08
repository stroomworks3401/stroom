package stroom.template.set.client.view;

import stroom.template.set.client.presenter.TemplateSetFieldListPresenter.TemplateSetFieldListView;

import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.View;
import com.gwtplatform.mvp.client.ViewImpl;

public class TemplateSetFieldListViewImpl extends ViewImpl implements TemplateSetFieldListView {

    private final Widget widget;

    @UiField
    SimplePanel dataGrid;
    @UiField
    HTML syncState;

    @Inject
    public TemplateSetFieldListViewImpl(final Binder binder) {
        widget = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return widget;
    }

    @Override
    public void setDataGridView(final View view) {
        dataGrid.setWidget(view.asWidget());
    }

    public interface Binder extends UiBinder<Widget, TemplateSetFieldListViewImpl> {
    }
}