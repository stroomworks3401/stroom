package stroom.template.set.client.presenter;

import stroom.template.set.shared.TemplateSetItem;

import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;

import java.util.ArrayList;
import java.util.List;

/**
 * Data provider for TemplateSetItem lists.
 */
public class TemplateSetTemplateDataProvider extends AsyncDataProvider<TemplateSetItem> {

    private List<TemplateSetItem> list = new ArrayList<>();
    private Range requestedRange;

    @Override
    protected void onRangeChanged(final HasData<TemplateSetItem> display) {
        fetch(display.getVisibleRange());
    }

    public void setList(final List<TemplateSetItem> list) {
        this.list = list != null ? list : new ArrayList<>();
    }

    private void fetch(final Range range) {
        if (range != null) {
            requestedRange = range;

            final List<TemplateSetItem> subList = new ArrayList<>();
            for (int i = range.getStart(); i < range.getStart() + range.getLength() && i < list.size(); i++) {
                subList.add(list.get(i));
            }

            updateRowData(range.getStart(), subList);
            updateRowCount(list.size(), true);
        }
    }

    public void refresh() {
        fetch(requestedRange);
    }
}
