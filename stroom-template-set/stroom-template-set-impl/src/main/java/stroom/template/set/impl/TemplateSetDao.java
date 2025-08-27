package stroom.template.set.impl;

import stroom.template.set.shared.TemplateSetItem;

import java.util.List;
import java.util.Optional;

public interface TemplateSetDao {

    // Create
    void addTemplate(TemplateSetItem item);

    // Read
    Optional<TemplateSetItem> getTemplateByUuid(String templateUuid);

    List<TemplateSetItem> getTemplatesForSet(String setUuid);

    // Update
    boolean updateTemplate(TemplateSetItem item);

    // Delete
    boolean deleteTemplate(String templateUuid);

    int deleteAllTemplatesForSet(String setUuid);
}
