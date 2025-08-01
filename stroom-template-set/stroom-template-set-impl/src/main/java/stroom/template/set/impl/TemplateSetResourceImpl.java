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
import stroom.util.io.StreamUtil;
import stroom.util.shared.EntityServiceException;
import stroom.util.shared.FetchWithUuid;
import stroom.util.shared.ResourceGeneration;
import stroom.util.shared.ResourceKey;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@AutoLogged
class TemplateSetResourceImpl implements TemplateSetResource, FetchWithUuid<TemplateSetDoc> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateSetResourceImpl.class);

    private final Provider<TemplateSetStore> templateSetStoreProvider;
    private final Provider<DocumentResourceHelper> documentResourceHelperProvider;
    private final Provider<ResourceStore> resourceStoreProvider;

    @Inject
    TemplateSetResourceImpl(final Provider<TemplateSetStore> TemplateSetStoreProvider,
                              final Provider<DocumentResourceHelper> documentResourceHelperProvider,
                              final Provider<ResourceStore> resourceStoreProvider) {
        this.templateSetStoreProvider = TemplateSetStoreProvider;
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
        return documentResourceHelperProvider.get().update(templateSetStoreProvider.get(), doc);
    }

    private DocRef getDocRef(final String uuid) {
        return DocRef.builder()
                .uuid(uuid)
                .type(TemplateSetDoc.TYPE)
                .build();
    }

    @Override
    public ResourceGeneration download(final DocRef dictionaryRef) {
        // Get dictionary.
        final TemplateSetDoc templateSetDoc = templateSetStoreProvider.get().readDocument(dictionaryRef);
        if (templateSetDoc == null) {
            throw new EntityServiceException("Unable to find dictionary");
        }

        final ResourceKey resourceKey = resourceStoreProvider.get().createTempFile("dictionary.txt");
        final Path tempFile = resourceStoreProvider.get().getTempFile(resourceKey);
        try {
            Files.writeString(tempFile, templateSetDoc.getData(), StreamUtil.DEFAULT_CHARSET);
        } catch (final IOException e) {
            LOGGER.error("Unable to download Dictionary", e);
            throw new UncheckedIOException(e);
        }
        return new ResourceGeneration(resourceKey, new ArrayList<>());
    }
}
