package stroom.template.set.impl.db;

import stroom.db.util.JooqUtil;
import stroom.template.set.impl.TemplateSetDao;
import stroom.template.set.impl.db.jooq.tables.records.TemplateSetRecord;
import stroom.template.set.shared.TemplateSetItem;
import stroom.util.json.JsonUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import jakarta.inject.Inject;
import org.jooq.JSON;

import java.util.List;
import java.util.Optional;

//TODO actually use the TemplateSetFields to build proper objects from the db col
import static stroom.template.set.impl.db.jooq.tables.TemplateSet.TEMPLATE_SET;

public class TemplateSetDaoImpl implements TemplateSetDao {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TemplateSetDaoImpl.class);

    private final TemplateSetDbConnProvider dbConnProvider;

    @Inject
    public TemplateSetDaoImpl(final TemplateSetDbConnProvider dbConnProvider) {
        this.dbConnProvider = dbConnProvider;
    }

    @Override
    public void addTemplate(final TemplateSetItem item) {
        final String fieldsJson = item.getFields() != null
                ? JsonUtil.writeValueAsString(item.getFields())
                : null;

        JooqUtil.context(dbConnProvider, ctx -> ctx
                .insertInto(TEMPLATE_SET)
                .columns(
                        TEMPLATE_SET.SET_UUID,
                        TEMPLATE_SET.UUID,           // template UUID
                        TEMPLATE_SET.NAME,
                        TEMPLATE_SET.DESCRIPTION,
                        TEMPLATE_SET.FIELDS,
                        TEMPLATE_SET.CREATE_TIME_MS,
                        TEMPLATE_SET.UPDATE_TIME_MS,
                        TEMPLATE_SET.CREATE_USER,
                        TEMPLATE_SET.UPDATE_USER
                )
                .values(
                        item.getSetUuid(),
                        item.getUuid(),
                        item.getName(),
                        item.getDescription(),
                        fieldsJson != null ? JSON.valueOf(fieldsJson) : null,
                        item.getCreateTimeMs(),
                        item.getUpdateTimeMs(),
                        item.getCreateUser(),
                        item.getUpdateUser()
                )
                .onDuplicateKeyUpdate()
                .set(TEMPLATE_SET.SET_UUID, item.getSetUuid())
                .set(TEMPLATE_SET.NAME, item.getName())
                .set(TEMPLATE_SET.DESCRIPTION, item.getDescription())
                .set(TEMPLATE_SET.FIELDS, fieldsJson != null ? JSON.valueOf(fieldsJson) : null)
                .set(TEMPLATE_SET.UPDATE_TIME_MS, item.getUpdateTimeMs())
                .set(TEMPLATE_SET.UPDATE_USER, item.getUpdateUser())
                .execute());
    }

    @Override
    public Optional<TemplateSetItem> getTemplateByUuid(final String templateUuid) {
        return JooqUtil.contextResult(dbConnProvider, ctx -> ctx
                        .selectFrom(TEMPLATE_SET)
                        .where(TEMPLATE_SET.UUID.eq(templateUuid))
                        .fetchOptional())
                .map(this::mapRecord);
    }

    @Override
    public List<TemplateSetItem> getTemplatesForSet(final String setUuid) {
        return JooqUtil.contextResult(dbConnProvider, ctx -> ctx
                .selectFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.SET_UUID.eq(setUuid))
                .orderBy(TEMPLATE_SET.NAME.asc())
                .fetch(this::mapRecord));
    }

    @Override
    public boolean updateTemplate(final TemplateSetItem item) {
        final String fieldsJson = item.getFields() != null
                ? JsonUtil.writeValueAsString(item.getFields())
                : null;

        final int updated = JooqUtil.contextResult(dbConnProvider, ctx -> ctx
                .update(TEMPLATE_SET)
                .set(TEMPLATE_SET.SET_UUID, item.getSetUuid())
                .set(TEMPLATE_SET.NAME, item.getName())
                .set(TEMPLATE_SET.DESCRIPTION, item.getDescription())
                .set(TEMPLATE_SET.FIELDS, fieldsJson != null ? JSON.valueOf(fieldsJson) : null)
                .set(TEMPLATE_SET.UPDATE_TIME_MS, item.getUpdateTimeMs())
                .set(TEMPLATE_SET.UPDATE_USER, item.getUpdateUser())
                .where(TEMPLATE_SET.UUID.eq(item.getUuid()))
                .execute());

        return updated > 0;
    }

    @Override
    public boolean deleteTemplate(final String templateUuid) {
        final int count = JooqUtil.contextResult(dbConnProvider, ctx -> ctx
                .deleteFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.UUID.eq(templateUuid))
                .execute());
        return count > 0;
    }

    @Override
    public int deleteAllTemplatesForSet(final String setUuid) {
        return JooqUtil.contextResult(dbConnProvider, ctx -> ctx
                .deleteFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.SET_UUID.eq(setUuid))
                .execute());
    }

    private TemplateSetItem mapRecord(final TemplateSetRecord r) {
        final TemplateSetItem item = new TemplateSetItem();
        item.setUuid(r.get(TEMPLATE_SET.UUID));
        item.setSetUuid(r.get(TEMPLATE_SET.SET_UUID));
        item.setName(r.get(TEMPLATE_SET.NAME));
        item.setDescription(r.get(TEMPLATE_SET.DESCRIPTION));

        final JSON json = r.get(TEMPLATE_SET.FIELDS);
        try {
            if (json != null) {
                item.setFields(json.data());
            }
        } catch (final Exception ex) {
            LOGGER.error("Bad JSON in TEMPLATE_SET.FIELDS for uuid {}: {}",
                    r.get(TEMPLATE_SET.UUID), json, ex);
            item.setFields("[]"); // fallback
        }


        item.setCreateTimeMs(r.get(TEMPLATE_SET.CREATE_TIME_MS));
        item.setUpdateTimeMs(r.get(TEMPLATE_SET.UPDATE_TIME_MS));
        item.setCreateUser(r.get(TEMPLATE_SET.CREATE_USER));
        item.setUpdateUser(r.get(TEMPLATE_SET.UPDATE_USER));

        return item;
    }
}
