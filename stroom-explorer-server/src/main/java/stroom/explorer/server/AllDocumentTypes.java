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

package stroom.explorer.server;

import stroom.explorer.shared.DocumentType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllDocumentTypes {
    private final Map<String, DocumentType> typeMap = new HashMap<>();
    private final List<DocumentType> documentTypes;

    public AllDocumentTypes() {
        addDocumentType(1, "Folder", "Folder");
        addDocumentType(11, "StatisticStore", "Statistic");
        addDocumentType(6, "Pipeline", "Pipeline");
        addDocumentType(13, "XMLSchema", "XML Schema");
        addDocumentType(10, "Index", "Index");
        addDocumentType(99, "Script", "Script");
        addDocumentType(9, "Visualisation", "Visualisation");
        addDocumentType(3, "Feed", "Feed");
        addDocumentType(4, "TextConverter", "Text Converter");
        addDocumentType(9, "Dictionary", "Dictionary");
        addDocumentType(7, "Dashboard", "Dashboard");
        addDocumentType(5, "XSLT", "XSLT");

        documentTypes = getDocumentTypes();
    }

    private void addDocumentType(final int priority, final String type, final String displayType) {
        final String url = getIconUrl(type);
        final DocumentType documentType = new DocumentType(priority, type, displayType, url);
        typeMap.put(type, documentType);
    }

    public String getIconUrl(final String type) {
        return DocumentType.DOC_IMAGE_URL + type + ".png";
    }

    private List<DocumentType> getDocumentTypes() {
        final List<DocumentType> documentTypes = new ArrayList<>(typeMap.size());
        for (final DocumentType type : typeMap.values()) {
            documentTypes.add(type);
        }

        // Sort types by priority.
        return sort(documentTypes);
    }

    private List<DocumentType> sort(final List<DocumentType> list) {
        // Sort types by priority.
        Collections.sort(list, (o1, o2) -> {
            final int comparison = Integer.compare(o1.getPriority(), o2.getPriority());
            if (comparison != 0) {
                return comparison;
            }

            return o1.getType().compareTo(o2.getType());
        });

        return list;
    }

    public List<DocumentType> getAllTypes() {
        return documentTypes;
    }

    public int getPriority(final String type) {
        final DocumentType documentType = typeMap.get(type);
        return documentType.getPriority();
    }
}
