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

package stroom.dictionary;

import stroom.dictionary.shared.Dictionary;
import stroom.dictionary.shared.DictionaryService;
import stroom.dictionary.shared.FindDictionaryCriteria;
import stroom.entity.server.MockDocumentService;
import stroom.entity.shared.DocumentType;
import stroom.util.spring.StroomSpringProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile(StroomSpringProfiles.TEST)
@Component
public class MockDictionaryService extends MockDocumentService<Dictionary, FindDictionaryCriteria>
        implements DictionaryService {
    @Override
    public DocumentType getDocumentType() {
        return getDocumentType(9, "Dictionary", "Dictionary");
    }

    @Override
    public Class<Dictionary> getEntityClass() {
        return Dictionary.class;
    }
}
