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

package stroom.template.set.shared;

import stroom.query.api.datasource.Field;
import stroom.query.api.datasource.FieldType;
import stroom.query.api.datasource.IndexField;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Objects;

/**
 * <p>
 * Wrapper for index field info
 * </p>
 */
@JsonPropertyOrder({
        "fldName",
        "fldType",
        "nativeType",
        "indexed",
        "stored",
        "defaultValue",
        "multiValued",
        "required",
        "description"
})

@JsonInclude(Include.NON_NULL)
public class TemplateSetField implements IndexField {

    @JsonProperty
    private String fldName;

    @JsonProperty
    private FieldType fldType = FieldType.TEXT;

    @JsonProperty
    private String nativeType;

    @JsonProperty
    private String defaultValue;

    @JsonProperty
    private boolean multiValued;

    @JsonProperty
    private boolean required;

    @JsonProperty
    private boolean indexed = true;  // default true, adjust as needed

    @JsonProperty
    private boolean stored;

    @JsonProperty
    private String description;

    public TemplateSetField() {
    }

    @JsonCreator
    public TemplateSetField(
            @JsonProperty("fldName") final String fldName,
            @JsonProperty("fldType") final FieldType fldType,
            @JsonProperty("nativeType") final String nativeType,
            @JsonProperty("indexed") final boolean indexed,
            @JsonProperty("stored") final boolean stored,
            @JsonProperty("defaultValue") final String defaultValue,
            @JsonProperty("multiValued") final boolean multiValued,
            @JsonProperty("required") final boolean required,
            @JsonProperty("description") final String description) {

        this.fldName = fldName;
        this.fldType = fldType != null ? fldType : FieldType.TEXT;
        this.nativeType = nativeType;
        this.indexed = indexed;
        this.stored = stored;
        this.defaultValue = defaultValue;
        this.multiValued = multiValued;
        this.required = required;
        this.description = description;
    }

    // Getters and setters for new fields
    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(final boolean indexed) {
        this.indexed = indexed;
    }

    public boolean isStored() {
        return stored;
    }

    public void setStored(final boolean stored) {
        this.stored = stored;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public String getFldName() {
        return fldName;
    }

    public void setFldName(final String fldName) {
        this.fldName = fldName;
    }

    @Override
    public FieldType getFldType() {
        return fldType;
    }

    public void setFldType(final FieldType fldType) {
        this.fldType = fldType;
    }

    public String getNativeType() {
        return nativeType;
    }

    public void setNativeType(final String nativeType) {
        this.nativeType = nativeType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public void setMultiValued(final boolean multiValued) {
        this.multiValued = multiValued;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    @Override
    public String getDisplayValue() {
        return fldName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateSetField)) return false;
        TemplateSetField that = (TemplateSetField) o;
        return multiValued == that.multiValued &&
               required == that.required &&
               Objects.equals(fldName, that.fldName) &&
               fldType == that.fldType &&
               Objects.equals(nativeType, that.nativeType) &&
               Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int compareTo(final Field o) {
        return fldName.compareToIgnoreCase(o.getFldName());
    }


    @Override
    public int hashCode() {
        return Objects.hash(fldName, fldType, nativeType, defaultValue, multiValued, required);
    }

    @Override
    public String toString() {
        return fldName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder {

        private String fldName;
        private FieldType fldType = FieldType.TEXT;
        private String nativeType;
        private String defaultValue;
        private boolean multiValued;
        private boolean required;
        private boolean indexed = true;
        private boolean stored;
        private String description;

        private Builder() {
        }

        private Builder(final TemplateSetField field) {
            this.fldName = field.fldName;
            this.fldType = field.fldType;
            this.nativeType = field.nativeType;
            this.defaultValue = field.defaultValue;
            this.multiValued = field.multiValued;
            this.required = field.required;
            this.indexed = field.indexed;
            this.stored = field.stored;
            this.description = field.description;
        }

        public Builder fldName(final String fldName) {
            this.fldName = fldName;
            return this;
        }

        public Builder fldType(final FieldType fldType) {
            this.fldType = fldType;
            return this;
        }

        public Builder nativeType(final String nativeType) {
            this.nativeType = nativeType;
            return this;
        }

        public Builder defaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder multiValued(final boolean multiValued) {
            this.multiValued = multiValued;
            return this;
        }

        public Builder required(final boolean required) {
            this.required = required;
            return this;
        }

        public Builder indexed(final boolean indexed) {
            this.indexed = indexed;
            return this;
        }

        public Builder stored(final boolean stored) {
            this.stored = stored;
            return this;
        }

        public Builder description(final String description) {
            this.description = description;
            return this;
        }

        public TemplateSetField build() {
            return new TemplateSetField(
                    fldName,
                    fldType,
                    nativeType,
                    indexed,
                    stored,
                    defaultValue,
                    multiValued,
                    required,
                    description
            );
        }
    }

}
