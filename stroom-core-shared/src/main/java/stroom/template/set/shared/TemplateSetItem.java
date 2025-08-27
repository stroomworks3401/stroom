package stroom.template.set.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import java.util.Objects;

@JsonInclude(Include.NON_NULL)
public class TemplateSetItem {

    @JsonProperty
    private String uuid;       // unique id of the template
    @JsonProperty
    private String setUuid;    // id of the parent template set
    @JsonProperty
    private String name;
    @JsonProperty
    private String version;
    @JsonProperty
    private Long createTimeMs;
    @JsonProperty
    private Long updateTimeMs;
    @JsonProperty
    private String createUser;
    @JsonProperty
    private String updateUser;
    @JsonProperty
    private String description;
    @JsonProperty
    private String fields; // stored as JSON string (maps to MySQL JSON column)

    public TemplateSetItem() {}

    public TemplateSetItem(String uuid,
                           String setUuid,
                           String name,
                           String version,
                           Long createTimeMs,
                           Long updateTimeMs,
                           String createUser,
                           String updateUser,
                           String description,
                           String fields) {
        this.uuid = uuid;
        this.setUuid = setUuid;
        this.name = name;
        this.version = version;
        this.createTimeMs = createTimeMs;
        this.updateTimeMs = updateTimeMs;
        this.createUser = createUser;
        this.updateUser = updateUser;
        this.description = description;
        this.fields = fields;
    }

    // Getters
    public String getUuid() { return uuid; }
    public String getSetUuid() { return setUuid; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public Long getCreateTimeMs() { return createTimeMs; }
    public Long getUpdateTimeMs() { return updateTimeMs; }
    public String getCreateUser() { return createUser; }
    public String getUpdateUser() { return updateUser; }
    public String getDescription() { return description; }
    public String getFields() { return fields; }

    // Setters
    public void setUuid(String uuid) { this.uuid = uuid; }
    public void setSetUuid(String setUuid) { this.setUuid = setUuid; }
    public void setName(String name) { this.name = name; }
    public void setVersion(String version) { this.version = version; }
    public void setCreateTimeMs(Long createTimeMs) { this.createTimeMs = createTimeMs; }
    public void setUpdateTimeMs(Long updateTimeMs) { this.updateTimeMs = updateTimeMs; }
    public void setCreateUser(String createUser) { this.createUser = createUser; }
    public void setUpdateUser(String updateUser) { this.updateUser = updateUser; }
    public void setDescription(String description) { this.description = description; }
    public void setFields(String fields) { this.fields = fields; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TemplateSetItem)) return false;
        TemplateSetItem that = (TemplateSetItem) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
