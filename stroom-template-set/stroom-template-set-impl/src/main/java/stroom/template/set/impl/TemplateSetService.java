package stroom.template.set.impl;

import stroom.docref.DocRef;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetItem;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class TemplateSetService {

    private final TemplateSetStore store;   // <-- use your store instead of raw DocStore
    private final TemplateSetDao dao;

    @Inject
    public TemplateSetService(final TemplateSetStore store,
                              final TemplateSetDao dao) {
        this.store = store;
        this.dao = dao;
    }

    // ---- Doc operations ----

    public TemplateSetDoc createDoc(final TemplateSetDoc doc) {
        Objects.requireNonNull(doc);
        if (doc.getUuid() == null) {
            doc.setUuid(UUID.randomUUID().toString());
        }
        return store.writeDocument(doc);
    }

    public TemplateSetDoc updateDoc(final TemplateSetDoc doc) {
        Objects.requireNonNull(doc);
        return store.writeDocument(doc);
    }

    public Optional<TemplateSetDoc> readDoc(final String uuid) {
        return Optional.ofNullable(
                store.readDocument(DocRef.builder(TemplateSetDoc.TYPE)
                        .uuid(uuid)
                        .build()));
    }

    // ---- Template row operations ----

    public TemplateSetItem addTemplateToSet(final String setUuid,
                                            final TemplateSetItem item,
                                            final String user) {
        Objects.requireNonNull(setUuid);
        Objects.requireNonNull(item);

        if (item.getUuid() == null) {
            item.setUuid(UUID.randomUUID().toString());
        }
        item.setSetUuid(setUuid);
        final long now = System.currentTimeMillis();
        item.setCreateTimeMs(now);
        item.setUpdateTimeMs(now);
        item.setCreateUser(user);
        item.setUpdateUser(user);

        dao.addTemplate(item);
        return item;
    }

    public boolean updateTemplate(final TemplateSetItem item, final String user) {
        Objects.requireNonNull(item);
        item.setUpdateTimeMs(System.currentTimeMillis());
        item.setUpdateUser(user);
        return dao.updateTemplate(item);
    }

    public boolean deleteTemplate(final String templateUuid) {
        return dao.deleteTemplate(templateUuid);
    }

    public int deleteAllTemplatesForSet(final String setUuid) {
        return dao.deleteAllTemplatesForSet(setUuid);
    }

    public List<TemplateSetItem> getTemplatesForSet(final String setUuid) {
        return dao.getTemplatesForSet(setUuid);
    }

    public Optional<TemplateSetItem> getTemplateByUuid(final String templateUuid) {
        return dao.getTemplateByUuid(templateUuid);
    }
}
