package stroom.template.set.shared;

import stroom.docref.DocRef;
import stroom.docref.HasName;
import stroom.docref.HasType;
import stroom.docref.HasUuid;
import stroom.docs.shared.Description;
import stroom.docstore.shared.Doc;
import stroom.docstore.shared.DocumentType;
import stroom.docstore.shared.DocumentTypeRegistry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Description(
        "My epic new feature which allows you to use templates for event enriching among other things."
)
@JsonPropertyOrder({
        "Type",
        "uuid",
        "version",
        "createTimeMs",
        "updateTimeMs",
        "createUser",
        "updateUser",
        "description",
        "setUuid"
})
@JsonInclude(Include.NON_NULL)
public class TemplateSetDoc extends Doc implements HasType, HasUuid, HasName {

    public static final String TYPE = "TemplateSet";
    public static final DocumentType DOCUMENT_TYPE = DocumentTypeRegistry.TEMPLATE_SET_DOCUMENT_TYPE;

    @JsonProperty
    private String description;

    @JsonProperty
    private String setUuid;  // link to the template set row

    public TemplateSetDoc() {
        this.setUuid = UUID.randomUUID().toString();
    }

    @JsonCreator
    public TemplateSetDoc(@JsonProperty("type") final String type,
                          @JsonProperty("uuid") final String uuid,
                          @JsonProperty("name") final String name,
                          @JsonProperty("version") final String version,
                          @JsonProperty("createTimeMs") final Long createTimeMs,
                          @JsonProperty("updateTimeMs") final Long updateTimeMs,
                          @JsonProperty("createUser") final String createUser,
                          @JsonProperty("updateUser") final String updateUser,
                          @JsonProperty("description") final String description,
                          @JsonProperty("setUuid") final String setUuid) {
        super(type, uuid, name, version, createTimeMs, updateTimeMs, createUser, updateUser);
        this.description = description;
        this.setUuid = setUuid != null
                ? setUuid
                : UUID.randomUUID().toString();
    }

    public String getDescription() { return description; }
    public void setDescription(final String description) { this.description = description; }

    public String getSetUuid() { return setUuid; }
    public void setSetUuid(final String setUuid) { this.setUuid = setUuid; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        final TemplateSetDoc that = (TemplateSetDoc) o;
        return Objects.equals(description, that.description) &&
               Objects.equals(setUuid, that.setUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, setUuid);
    }

    public static DocRef getDocRef(final String uuid) {
        return DocRef.builder(TYPE).uuid(uuid).build();
    }

    public static DocRef.TypedBuilder buildDocRef() {
        return DocRef.builder(TYPE);
    }
}
