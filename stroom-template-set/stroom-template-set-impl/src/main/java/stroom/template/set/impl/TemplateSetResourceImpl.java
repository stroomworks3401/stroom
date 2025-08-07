/*
 * Copyright 2017 Crown Copyright
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

package stroom.template.set.impl;

import stroom.docref.DocRef;
import stroom.docstore.api.DocumentResourceHelper;
import stroom.event.logging.rs.api.AutoLogged;
import stroom.resource.api.ResourceStore;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetResource;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.FetchWithUuid;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoLogged
public class TemplateSetResourceImpl implements TemplateSetResource, FetchWithUuid<TemplateSetDoc> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateSetResourceImpl.class);

    private final Provider<TemplateSetStore> templateSetStoreProvider;
    private final Provider<DocumentResourceHelper> documentResourceHelperProvider;
    private final Provider<ResourceStore> resourceStoreProvider;

    @Inject
    public TemplateSetResourceImpl(final Provider<TemplateSetStore> templateSetStoreProvider,
                                   final Provider<DocumentResourceHelper> documentResourceHelperProvider,
                                   final Provider<ResourceStore> resourceStoreProvider) {
        this.templateSetStoreProvider = templateSetStoreProvider;
        this.documentResourceHelperProvider = documentResourceHelperProvider;
        this.resourceStoreProvider = resourceStoreProvider;
    }

    @Override
    public TemplateSetDoc fetch(final String uuid) {
        return documentResourceHelperProvider.get()
                .read(templateSetStoreProvider.get(), getDocRef(uuid));
    }

    @Override
    public TemplateSetDoc update(final String uuid, final TemplateSetDoc doc) {
        if (doc.getUuid() == null || !doc.getUuid().equals(uuid)) {
            throw new EntityServiceException("The document UUID must match the update UUID");
        }
        return documentResourceHelperProvider.get()
                .update(templateSetStoreProvider.get(), doc);
    }

    private DocRef getDocRef(final String uuid) {
        return DocRef.builder()
                .uuid(uuid)
                .type(TemplateSetDoc.TYPE)  // make sure this constant exists in TemplateSetDoc
                .build();
    }
}


