package stroom.template.set.impl;

import stroom.docref.DocRef;
import stroom.docref.DocRefInfo;
import stroom.docstore.api.AuditFieldFilter;
import stroom.docstore.api.Store;
import stroom.docstore.api.StoreFactory;
import stroom.docstore.api.UniqueNameUtil;
import stroom.importexport.shared.ImportSettings;
import stroom.importexport.shared.ImportState;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.util.shared.Message;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class TemplateSetStoreImpl implements TemplateSetStore {

    private final Store<TemplateSetDoc> store;

    @Inject
    public TemplateSetStoreImpl(final StoreFactory storeFactory,
                                  final TemplateSetSerialiser documentationSerialiser) {
        this.store = storeFactory.createStore(documentationSerialiser, TemplateSetDoc.TYPE, TemplateSetDoc.class);
    }

    ////////////////////////////////////////////////////////////////////////
    // START OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public DocRef createDocument(final String name) {
        return store.createDocument(name);
    }

    @Override
    public DocRef copyDocument(final DocRef docRef,
                               final String name,
                               final boolean makeNameUnique,
                               final Set<String> existingNames) {
        final String newName = UniqueNameUtil.getCopyName(name, makeNameUnique, existingNames);
        return store.copyDocument(docRef.getUuid(), newName);
    }

    @Override
    public DocRef moveDocument(final DocRef docRef) {
        return store.moveDocument(docRef);
    }

    @Override
    public DocRef renameDocument(final DocRef docRef, final String name) {
        return store.renameDocument(docRef, name);
    }

    @Override
    public void deleteDocument(final DocRef docRef) {
        store.deleteDocument(docRef);
    }

    @Override
    public DocRefInfo info(final DocRef docRef) {
        return store.info(docRef);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ExplorerActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Map<DocRef, Set<DocRef>> getDependencies() {
        // TemplateSet has no deps forwards or backwards
        return Collections.emptyMap();
    }

    @Override
    public Set<DocRef> getDependencies(final DocRef docRef) {
        // TemplateSet has no deps forwards or backwards
        return Collections.emptySet();
    }

    @Override
    public void remapDependencies(final DocRef docRef,
                                  final Map<DocRef, DocRef> remappings) {
        // TemplateSet has no deps forwards or backwards
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF HasDependencies
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public TemplateSetDoc readDocument(final DocRef docRef) {
        return store.readDocument(docRef);
    }

    @Override
    public TemplateSetDoc writeDocument(final TemplateSetDoc document) {
        return store.writeDocument(document);
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF DocumentActionHandler
    ////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////
    // START OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public Set<DocRef> listDocuments() {
        return store.listDocuments();
    }

    @Override
    public DocRef importDocument(final DocRef docRef,
                                 final Map<String, byte[]> dataMap,
                                 final ImportState importState,
                                 final ImportSettings importSettings) {
        return store.importDocument(docRef, dataMap, importState, importSettings);
    }

    @Override
    public Map<String, byte[]> exportDocument(final DocRef docRef,
                                              final boolean omitAuditFields,
                                              final List<Message> messageList) {
        if (omitAuditFields) {
            return store.exportDocument(docRef, messageList, new AuditFieldFilter<>());
        }
        return store.exportDocument(docRef, messageList, d -> d);
    }

    @Override
    public String getType() {
        return store.getType();
    }

    @Override
    public Set<DocRef> findAssociatedNonExplorerDocRefs(final DocRef docRef) {
        return null;
    }

    ////////////////////////////////////////////////////////////////////////
    // END OF ImportExportActionHandler
    ////////////////////////////////////////////////////////////////////////

    @Override
    public List<DocRef> findByNames(final List<String> names, final boolean allowWildCards) {
        return store.findByNames(names, allowWildCards);
    }

    @Override
    public Map<String, String> getIndexableData(final DocRef docRef) {
        return store.getIndexableData(docRef);
    }
}
