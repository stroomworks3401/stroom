package stroom.template.set.client.presenter;

import stroom.alert.client.event.ConfirmEvent;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.template.set.shared.TemplateSetItem;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.data.grid.client.MyDataGrid;
import stroom.data.grid.client.PagerView;
import stroom.template.set.shared.TemplateSetResource;
import stroom.widget.util.client.MultiSelectionModelImpl;
import stroom.widget.button.client.ButtonView;
import stroom.svg.client.SvgPresets;
import stroom.document.client.event.DirtyEvent;
import stroom.widget.util.client.MouseUtil;

import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;

import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateSetTemplateListPresenter
        extends DocumentEditPresenter<TemplateSetTemplateListPresenter.TemplateSetTemplateListView, TemplateSetDoc> {

    private final PagerView pagerView;
    private final MyDataGrid<TemplateSetItem> dataGrid;
    private final MultiSelectionModelImpl<TemplateSetItem> selectionModel;
    private final ButtonView addButton;
    private final ButtonView removeButton;

    private List<TemplateSetItem> templates;
    private TemplateSetTemplateDataProvider dataProvider;
    private final TemplateSetResource templateSetResource;
    private TemplateSetDoc templateSet;
    private final RestFactory restFactory;
    private final TemplateSetTemplateEditPresenter templateSetTemplateEditPresenter;

    @Inject
    public TemplateSetTemplateListPresenter(final EventBus eventBus,
                                            final TemplateSetTemplateListView view,
                                            final PagerView pagerView, final TemplateSetResource templateSetResource,
                                            final RestFactory restFactory,
                                            final TemplateSetTemplateEditPresenter templateSetTemplateEditPresenter) {
        super(eventBus, view);
        this.pagerView = pagerView;
        this.templateSetResource = templateSetResource;
        this.restFactory = restFactory;
        this.templateSetTemplateEditPresenter = templateSetTemplateEditPresenter;

        dataGrid = new MyDataGrid<>(this);
        selectionModel = dataGrid.addDefaultSelectionModel(true);
        pagerView.setDataWidget(dataGrid);

        view.setDataGridView(pagerView);
        addButton = pagerView.addButton(SvgPresets.NEW_ITEM);
        removeButton = pagerView.addButton(SvgPresets.DELETE);

        addColumns();
        enableButtons();
    }

    @Override
    protected void onBind() {
        super.onBind();
        registerHandler(addButton.addClickHandler(event -> {
            if (!isReadOnly() && MouseUtil.isPrimary(event)) onAdd();
        }));
        registerHandler(removeButton.addClickHandler(event -> {
            if (!isReadOnly() && MouseUtil.isPrimary(event)) onRemove();
        }));
        registerHandler(selectionModel.addSelectionHandler(event -> enableButtons()));
    }

    private void enableButtons() {
        addButton.setEnabled(!isReadOnly());
        removeButton.setEnabled(!isReadOnly() && templates != null && !selectionModel.getSelectedItems().isEmpty());
    }

    private void addColumns() {
        dataGrid.addResizableColumn(new Column<TemplateSetItem, String>(new TextCell()) {
            @Override
            public String getValue(final TemplateSetItem row) {
                return row != null ? row.getName() : "";
            }
        }, "Template Name", 200);

        dataGrid.addResizableColumn(new Column<TemplateSetItem, String>(new TextCell()) {
            @Override
            public String getValue(final TemplateSetItem row) {
                return row != null ? row.getUuid() : "";
            }
        }, "UUID", 300);
    }

    private void onAdd() {
        if (templateSet == null) return;

        // Create a new TemplateSetItem using the builder
        TemplateSetItem item = TemplateSetItem.builder().build();

        // Open the edit presenter
        templateSetTemplateEditPresenter.read(item);
        templateSetTemplateEditPresenter.show("New Template", e -> {
            if (e.isOk()) {
                TemplateSetItem newItem = templateSetTemplateEditPresenter.write();
                if (newItem != null) {
                    // Save the template using the resource
                    restFactory
                            .create(templateSetResource)
                            .method(res -> res.addTemplateToSet(templateSet.getSetUuid(), newItem))
                            .onSuccess(savedItem -> {
                                // Add to the local list and refresh the data provider
                                templates.add(savedItem);
                                dataProvider.setList(templates);
                                dataProvider.refresh();

                                e.hide();
                                DirtyEvent.fire(this, true);
                            })
                            .taskMonitorFactory(pagerView)
                            .exec();
                } else {
                    e.reset();
                }
            } else {
                e.hide();
            }
        });
    }

    private void onRemove() {
        final List<TemplateSetItem> selected = selectionModel.getSelectedItems();
        if (selected == null || selected.isEmpty()) return;

        ConfirmEvent.fire(this, "Are you sure you want to delete the selected templates?", result -> {
            if (result) {
                for (TemplateSetItem item : selected) {
                    restFactory.create(templateSetResource)
                            .method(res -> {
                                res.deleteTemplate(item.getUuid());
                                return null;
                            })
                            .onSuccess(v -> {
                                templates.remove(item);
                                dataProvider.setList(templates);
                                dataProvider.refresh();
                                DirtyEvent.fire(this, true);
                            })
                            .taskMonitorFactory(pagerView, "Deleting template")
                            .exec();
                }
                selectionModel.clear();
            }
        });
    }

    public void setTemplates(final List<TemplateSetItem> templates) {
        this.templates = new ArrayList<>(templates);
        this.templates.sort(Comparator.comparing(TemplateSetItem::getName, String.CASE_INSENSITIVE_ORDER));
        refreshDataGrid();
    }

    private void refreshDataGrid() {
        if (templates == null) {
            templates = new ArrayList<>();
        }
        if (dataProvider == null) {
            dataProvider = new TemplateSetTemplateDataProvider();
            dataProvider.addDataDisplay(dataGrid);
        }
        dataProvider.setList(templates);
        dataProvider.refresh();
    }

    @Override
    protected void onRead(final DocRef docRef, final TemplateSetDoc document, final boolean readOnly) {
        this.templateSet = document;

        if (document.getSetUuid() != null) {
            restFactory
                    .create(templateSetResource)
                    .method(res -> res.getTemplatesForSet(document.getSetUuid()))
                    .onSuccess(items -> setTemplates(items))
                    .onFailure(caught -> setTemplates(Collections.emptyList()))
                    .taskMonitorFactory(null, "Fetching templates")
                    .exec();
        } else {
            setTemplates(Collections.emptyList());
        }
        enableButtons();
    }

    @Override
    protected TemplateSetDoc onWrite(final TemplateSetDoc document) {
        return document;
    }

    public interface TemplateSetTemplateListView extends View {
        void setDataGridView(View view);
    }
}
