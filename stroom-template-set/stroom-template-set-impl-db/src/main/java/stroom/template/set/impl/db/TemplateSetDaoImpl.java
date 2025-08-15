package stroom.template.set.impl.db;

import stroom.db.util.JooqUtil;
import stroom.template.set.impl.TemplateSetDao;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetField;
import stroom.util.exception.DataChangedException;
import stroom.util.json.JsonUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.NullSafe;
import stroom.util.shared.UserRef;

import jakarta.inject.Inject;
import org.jooq.Condition;
import org.jooq.Record;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static stroom.template.set.impl.db.jooq.tables.TemplateSet.TEMPLATE_SET;

public abstract class TemplateSetDaoImpl implements TemplateSetDao {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TemplateSetDaoImpl.class);

    private final TemplateSetDbConnProvider templateSetDbConnProvider;

    private static final Function<Record, TemplateSetDoc> RECORD_TO_TEMPLATE_SET_DOC_MAPPER = record -> TemplateSetDoc.builder()
            .uuid(record.get(TEMPLATE_SET.UUID))
            .name(record.get(TEMPLATE_SET.NAME))
            .version(record.get(TEMPLATE_SET.VERSION))
            .createTimeMs(record.get(TEMPLATE_SET.CREATE_TIME_MS))
            .createUser(record.get(TEMPLATE_SET.CREATE_USER))
            .updateTimeMs(record.get(TEMPLATE_SET.UPDATE_TIME_MS))
            .updateUser(record.get(TEMPLATE_SET.UPDATE_USER))
            .description(record.get(TEMPLATE_SET.DESCRIPTION))
            .fields(JsonUtil.readValue(record.get(TEMPLATE_SET.FIELDS), TemplateSetField.class))
            .build();


    @Inject
    TemplateSetDaoImpl(final TemplateSetDbConnProvider templateSetDbConnProvider) {
        this.templateSetDbConnProvider = templateSetDbConnProvider;
    }

    private String getFieldsJson(final TemplateSet templateSet) {
        return templateSet.getFields() != null ? JsonUtil.writeValueAsString(templateSet.getFields()) : null;
    }

    @Override
    public TemplateSet create(final TemplateSet templateSet) {
        final String fieldsJson = getFieldsJson(templateSet);
        final UUID uuid = UUID.randomUUID();

        JooqUtil.context(templateSetDbConnProvider, context -> context
                .insertInto(TEMPLATE_SET)
                .columns(
                        TEMPLATE_SET.UUID,
                        TEMPLATE_SET.NAME,
                        TEMPLATE_SET.VERSION,
                        TEMPLATE_SET.CREATE_TIME_MS,
                        TEMPLATE_SET.CREATE_USER,
                        TEMPLATE_SET.UPDATE_TIME_MS,
                        TEMPLATE_SET.UPDATE_USER,
                        TEMPLATE_SET.DESCRIPTION,
                        TEMPLATE_SET.FIELDS
                )
                .values(
                        uuid,
                        templateSet.getName(),
                        templateSet.getVersion(),
                        templateSet.getCreateTimeMs(),
                        templateSet.getCreateUser(),
                        templateSet.getUpdateTimeMs(),
                        templateSet.getUpdateUser(),
                        templateSet.getDescription(),
                        fieldsJson
                )
                .execute()
        );

        return templateSet.copy().uuid(uuid).build();
    }

    @Override
    public TemplateSet update(final TemplateSet templateSet) {
        final String fieldsJson = getFieldsJson(templateSet);

        final int count = JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                .update(TEMPLATE_SET)
                .set(TEMPLATE_SET.NAME, templateSet.getName())
                .set(TEMPLATE_SET.VERSION, templateSet.getVersion())
                .set(TEMPLATE_SET.UPDATE_TIME_MS, templateSet.getUpdateTimeMs())
                .set(TEMPLATE_SET.UPDATE_USER, templateSet.getUpdateUser())
                .set(TEMPLATE_SET.DESCRIPTION, templateSet.getDescription())
                .set(TEMPLATE_SET.FIELDS, fieldsJson)
                .where(TEMPLATE_SET.UUID.eq(templateSet.getUuid()))
                .execute()
        );

        if (count == 0) {
            throw new DataChangedException("Unable to update templateSet with UUID " + templateSet.getUuid());
        }

        return templateSet;
    }

    @Override
    public boolean delete(final UUID uuid) {
        return JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                .deleteFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.UUID.eq(uuid))
                .execute()) > 0;
    }

    @Override
    public Optional<TemplateSet> fetch(final UUID uuid) {
        return JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                        .select()
                        .from(TEMPLATE_SET)
                        .where(TEMPLATE_SET.UUID.eq(uuid))
                        .fetchOptional())
                .map(RECORD_TO_TEMPLATE_SET_MAPPER);
    }

    @Override
    public List<TemplateSet> find(final FindTemplateSetCriteria criteria) {
        final Collection<Condition> conditions = JooqUtil.conditions(
                NullSafe.getAsOptional(criteria.getName(), TEMPLATE_SET.NAME::eq));

        final Integer offset = JooqUtil.getOffset(criteria.getPageRequest());
        final Integer limit = JooqUtil.getLimit(criteria.getPageRequest(), true);

        return JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                        .select()
                        .from(TEMPLATE_SET)
                        .where(conditions)
                        .limit(offset, limit)
                        .fetch())
                .stream()
                .map(RECORD_TO_TEMPLATE_SET_MAPPER)
                .toList();
    }

    @Override
    public int deleteAllByOwner(final UserRef ownerRef) {
        Objects.requireNonNull(ownerRef);
        final int delCount = JooqUtil.contextResult(templateSetDbConnProvider, dslContext -> dslContext
                .deleteFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.CREATE_USER.eq(ownerRef.getName()))
                .execute());

        LOGGER.debug(() -> LogUtil.message("Deleted {} {} records for user {}",
                delCount, TEMPLATE_SET.getName(), ownerRef.toInfoString()));

        return delCount;
    }
}
