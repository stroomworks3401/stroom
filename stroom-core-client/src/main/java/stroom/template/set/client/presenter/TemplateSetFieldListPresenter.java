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

import stroom.alert.client.event.ConfirmEvent;
import stroom.data.grid.client.EndColumn;
import stroom.data.grid.client.MyDataGrid;
import stroom.data.grid.client.PagerView;
import stroom.dispatch.client.RestFactory;
import stroom.docref.DocRef;
import stroom.document.client.event.DirtyEvent;
import stroom.entity.client.presenter.DocumentEditPresenter;
import stroom.preferences.client.DateTimeFormatter;
import stroom.query.api.datasource.FieldType;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetField;
import stroom.template.set.shared.TemplateSetResource;
import stroom.svg.client.SvgPresets;

import stroom.widget.button.client.ButtonView;
import stroom.widget.util.client.MouseUtil;
import stroom.widget.util.client.MultiSelectionModelImpl;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.Column;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.gwtplatform.mvp.client.View;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TemplateSetFieldListPresenter
        extends DocumentEditPresenter<TemplateSetFieldListPresenter.TemplateSetFieldListView, TemplateSetDoc> {

    private static final TemplateSetResource TEMPLATE_SET_RESOURCE = GWT.create(TemplateSetResource.class);

    private final PagerView pagerView;
    private final MyDataGrid<TemplateSetField> dataGrid;
    private final MultiSelectionModelImpl<TemplateSetField> selectionModel;
    private final TemplateSetFieldEditPresenter fieldEditPresenter;
    private final RestFactory restFactory;
    private final DateTimeFormatter dateTimeFormatter;
    private final ButtonView newButton;
    private final ButtonView editButton;
    private final ButtonView removeButton;

    private TemplateSetDoc templateSet;
    private List<TemplateSetField> fields;
    private TemplateSetFieldDataProvider<TemplateSetField> dataProvider;

    @Inject
    public TemplateSetFieldListPresenter(final EventBus eventBus,
                                         final TemplateSetFieldListView view,
                                         final PagerView pagerView,
                                         final TemplateSetFieldEditPresenter fieldEditPresenter,
                                         final RestFactory restFactory,
                                         final DateTimeFormatter dateTimeFormatter) {
        super(eventBus, view);
        this.pagerView = pagerView;
        this.fieldEditPresenter = fieldEditPresenter;
        this.restFactory = restFactory;
        this.dateTimeFormatter = dateTimeFormatter;

        dataGrid = new MyDataGrid<>(this);
        selectionModel = dataGrid.addDefaultSelectionModel(true);
        pagerView.setDataWidget(dataGrid);

        view.setDataGridView(pagerView);
        newButton = pagerView.addButton(SvgPresets.NEW_ITEM);
        editButton = pagerView.addButton(SvgPresets.EDIT);
        removeButton = pagerView.addButton(SvgPresets.DELETE);

        addColumns();
        enableButtons();
    }

    @Override
    protected void onBind() {
        super.onBind();

        registerHandler(newButton.addClickHandler(event -> {
            if (!isReadOnly() && MouseUtil.isPrimary(event)) {
                onAdd();
            }
        }));
        registerHandler(editButton.addClickHandler(event -> {
            if (!isReadOnly() && MouseUtil.isPrimary(event)) {
                onEdit();
            }
        }));
        registerHandler(removeButton.addClickHandler(event -> {
            if (!isReadOnly() && MouseUtil.isPrimary(event)) {
                onRemove();
            }
        }));
        registerHandler(selectionModel.addSelectionHandler(event -> {
            if (!isReadOnly()) {
                enableButtons();
                if (event.getSelectionType().isDoubleSelect()) {
                    onEdit();
                }
            }
        }));
    }

    private void enableButtons() {
        newButton.setEnabled(!isReadOnly());
        if (!isReadOnly() && fields != null) {
            final TemplateSetField selected = selectionModel.getSelected();
            final boolean enabled = selected != null;
            editButton.setEnabled(enabled);
            removeButton.setEnabled(enabled);
        } else {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
        }
    }

    private void addColumns() {
        addStringColumn("Name", 150, TemplateSetField::getFldName);
        addStringColumn("Type", row -> row.getFldType().getDisplayValue());
        addStringColumn("Field Type", TemplateSetField::getNativeType);
        addStringColumn("Default Value", TemplateSetField::getDefaultValue);
        addBooleanColumn("Stored", TemplateSetField::isStored);
        addBooleanColumn("Indexed", TemplateSetField::isIndexed);
        addBooleanColumn("Multi Valued", TemplateSetField::isMultiValued);
        addBooleanColumn("Required", TemplateSetField::isRequired);
        addStringColumn("Description", TemplateSetField::getDescription);
        dataGrid.addEndColumn(new EndColumn<>());
    }

    private void addStringColumn(final String name, final Function<TemplateSetField, String> function) {
        addStringColumn(name, 100, function);
    }

    private void addStringColumn(final String name, final int width, final Function<TemplateSetField, String> function) {
        dataGrid.addResizableColumn(new Column<TemplateSetField, String>(new TextCell()) {
            @Override
            public String getValue(final TemplateSetField row) {
                return function.apply(row);
            }
        }, name, width);
    }

    private void addBooleanColumn(final String name, final Function<TemplateSetField, Boolean> function) {
        dataGrid.addResizableColumn(new Column<TemplateSetField, String>(new TextCell()) {
            @Override
            public String getValue(final TemplateSetField row) {
                return function.apply(row) ? "Yes" : "No";
            }
        }, name, 100);
    }

    private void onAdd() {
        final Set<String> otherNames = fields.stream()
                .map(TemplateSetField::getFldName)
                .collect(Collectors.toSet());

        final List<String> fieldTypes = Arrays.stream(FieldType.values())
                .map(Enum::name) // gives TEXT, NUMERIC, DATE, etc.
                .toList();

        fieldEditPresenter.read(
                TemplateSetField.builder().build(),
                otherNames,
                fieldTypes
        );

        fieldEditPresenter.show("New Template Field", e -> {
            if (e.isOk()) {
                final TemplateSetField field = fieldEditPresenter.write();
                if (field != null) {
                    fields.add(field);
                    fields.sort(Comparator.comparing(TemplateSetField::getFldName, String.CASE_INSENSITIVE_ORDER));
                    selectionModel.setSelected(field);
                    refresh();
                    DirtyEvent.fire(this, true);
                    e.hide();
                } else {
                    e.reset();
                }
            } else {
                e.hide();
            }
        });
    }

    private void onEdit() {
        final TemplateSetField existing = selectionModel.getSelected();
        if (existing != null) {
            final Set<String> otherNames = fields.stream()
                    .map(TemplateSetField::getFldName)
                    .collect(Collectors.toSet());
            otherNames.remove(existing.getFldName());

            final List<String> fieldTypes = Arrays.stream(FieldType.values())
                    .map(Enum::name) // or FieldType::toString if UI expects nicer labels
                    .toList();

            fieldEditPresenter.read(existing, otherNames, fieldTypes);
            fieldEditPresenter.show("Edit Template Field", e -> {
                if (e.isOk()) {
                    final TemplateSetField updated = fieldEditPresenter.write();
                    if (updated != null && !updated.equals(existing)) {
                        fields.remove(existing);
                        fields.add(updated);
                        fields.sort(Comparator.comparing(
                                TemplateSetField::getFldName,
                                String.CASE_INSENSITIVE_ORDER));
                        selectionModel.setSelected(updated);
                        refresh();
                        DirtyEvent.fire(this, true);
                        e.hide();
                    } else {
                        e.hide();
                    }
                } else {
                    e.hide();
                }
            });
        }
    }

    private void onRemove() {
        final List<TemplateSetField> selected = selectionModel.getSelectedItems();
        if (selected != null && !selected.isEmpty()) {
            final String message = selected.size() > 1
                    ? "Are you sure you want to delete the selected fields?"
                    : "Are you sure you want to delete the selected field?";

            ConfirmEvent.fire(this, message, result -> {
                if (result) {
                    fields.removeAll(selected);
                    selectionModel.clear();
                    refresh();
                    DirtyEvent.fire(this, true);
                }
            });
        }
    }

    private void refresh() {
        if (fields == null) {
            fields = new ArrayList<>();
        }
        if (dataProvider == null) {
            dataProvider = new TemplateSetFieldDataProvider<>();
            dataProvider.addDataDisplay(dataGrid);
        }
        dataProvider.setList(fields);
        dataProvider.refresh();
    }

    @Override
    protected void onRead(final DocRef docRef, final TemplateSetDoc document, final boolean readOnly) {
        this.templateSet = document;
        if (document != null) {
            fields = document.getFields().stream()
                    .sorted(Comparator.comparing(TemplateSetField::getFldName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
        }
        refresh();
        enableButtons();
    }

    @Override
    protected TemplateSetDoc onWrite(final TemplateSetDoc document) {
        document.setFields(fields);
        return document;
    }

    public interface TemplateSetFieldListView extends View {
        void setDataGridView(View view);
    }
}
