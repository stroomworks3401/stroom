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

package stroom.index.server;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import stroom.query.shared.IndexField;
import stroom.query.shared.IndexFieldType;
import stroom.util.date.DateUtil;
import stroom.util.logging.StroomLogger;

public class DocumentUtil {
    private static final StroomLogger LOGGER = StroomLogger.getLogger(DocumentUtil.class);

    public static int add(final Document document, final IndexField indexField, final String value) {
        if (indexField.getFieldType().isNumeric()) {
            final long val = Long.parseLong(value);
            return add(document, indexField, val);

        } else if (IndexFieldType.DATE_FIELD.equals(indexField.getFieldType())) {
            Long val = null;

            try {
                val = DateUtil.parseUnknownString(value);
            } catch (final Exception e) {
                LOGGER.trace(e.getMessage(), e);
            }

            if (val != null) {
                return add(document, indexField, val);
            }
        } else {
            document.add(new Field(indexField.getFieldName(), value, FieldTypeFactory.create(indexField)));
            return 1;
        }

        return 0;
    }

    public static int add(final Document document, final IndexField indexField, final long value) {
        // As of Lucene 6 longs must be indexed as points and stored in a separate field.
        if (indexField.isIndexed()) {
            document.add(new LongPoint(indexField.getFieldName(), value));
        }
        if (indexField.isStored()) {
            document.add(new StoredField(indexField.getFieldName(), value));
        }

        if (indexField.isIndexed() || indexField.isStored()) {
            return 1;
        }

        return 0;
    }

    /**
     * Utility function to get a string from a field as Lucene 6 now requires us to add more than one field to index and store a number.
     * This means we have to look at more than one field with the same name to get the one that stores the value.
     *
     * @param document
     * @param fieldName
     * @return
     */
    public static String getStringValue(final Document document, final String fieldName) {
        final IndexableField[] fields = document.getFields(fieldName);
        if (fields != null) {
            for (final IndexableField field : fields) {
                final String value = field.stringValue();
                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }
}
