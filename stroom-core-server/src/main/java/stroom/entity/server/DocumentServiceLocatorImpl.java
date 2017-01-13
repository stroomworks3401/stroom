/*
 * Copyright 2016 Crown Copyright
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

package stroom.entity.server;

import org.springframework.stereotype.Component;
import stroom.entity.shared.DocumentService;
import stroom.entity.shared.DocumentServiceLocator;
import stroom.entity.shared.DocumentType;
import stroom.entity.shared.EntityServiceException;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
class DocumentServiceLocatorImpl implements DocumentServiceLocator {
    private final EntityServiceBeanRegistry beanRegistry;
    private volatile List<DocumentType> documentTypes;

    @Inject
    DocumentServiceLocatorImpl(final EntityServiceBeanRegistry beanRegistry) {
        this.beanRegistry = beanRegistry;
    }

    @Override
    public DocumentService locate(final String type) {
        final Object bean = beanRegistry.getEntityService(type);
        if (bean == null) {
            throw new EntityServiceException("No entity service can be found");
        }
        if (!(bean instanceof DocumentService)) {
            throw new EntityServiceException("Bean is not a document entity service");
        }

        return (DocumentService) bean;
    }

    @Override
    public List<DocumentType> getTypes() {
        if (this.documentTypes == null) {
            final Collection<Object> services = beanRegistry
                    .getAllServicesByParent(DocumentService.class);
            final List<DocumentType> list = services.stream().map(service -> ((DocumentService) service).getDocumentType()).collect(Collectors.toList());
            list.sort(Comparator.comparingInt(DocumentType::getPriority));
            this.documentTypes = list;
        }

        return documentTypes;
    }
}