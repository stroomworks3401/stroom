package stroom.template.set.impl.db;

import stroom.db.util.JooqUtil;
import org.jooq.impl.DSL;
import stroom.template.set.impl.TemplateSetDao;
import stroom.template.set.impl.db.jooq.tables.TemplateSet;
import stroom.template.set.impl.db.jooq.tables.records.TemplateSetRecord;
import stroom.template.set.shared.TemplateSetDoc;
import stroom.template.set.shared.TemplateSetField;
import stroom.util.exception.DataChangedException;
import stroom.util.json.JsonUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;
import stroom.util.logging.LogUtil;
import stroom.util.shared.NullSafe;
import stroom.util.shared.UserRef;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.inject.Inject;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.JSON;
import org.jooq.RecordMapper;
import org.jooq.impl.SQLDataType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static stroom.template.set.impl.db.jooq.tables.TemplateSet.TEMPLATE_SET;

public abstract class TemplateSetDaoImpl implements TemplateSetDao {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(TemplateSetDaoImpl.class);

    private final TemplateSetDbConnProvider templateSetDbConnProvider;

    private static final Function<Record, TemplateSetDoc> RECORD_TO_TEMPLATE_SET_MAPPER = record -> {
        TemplateSetDoc doc = new TemplateSetDoc();

        doc.setUuid(record.get(TemplateSet.TEMPLATE_SET.UUID));
        doc.setName(record.get(TemplateSet.TEMPLATE_SET.NAME));
        doc.setVersion(record.get(TemplateSet.TEMPLATE_SET.VERSION));
        doc.setCreateTimeMs(record.get(TemplateSet.TEMPLATE_SET.CREATE_TIME_MS));
        doc.setUpdateTimeMs(record.get(TemplateSet.TEMPLATE_SET.UPDATE_TIME_MS));
        doc.setCreateUser(record.get(TemplateSet.TEMPLATE_SET.CREATE_USER));
        doc.setUpdateUser(record.get(TemplateSet.TEMPLATE_SET.UPDATE_USER));
        doc.setDescription(record.get(TemplateSet.TEMPLATE_SET.DESCRIPTION));

        // Map the JSON fields column if it's not null
        final JSON fieldsJson = record.get(TemplateSet.TEMPLATE_SET.FIELDS);
        if (fieldsJson != null) {
            doc.setFields(List.of(JsonUtil.readValue(fieldsJson.data(), TemplateSetField[].class)));
        }

        return doc;
    };

    @Inject
    TemplateSetDaoImpl(final TemplateSetDbConnProvider templateSetDbConnProvider) {
        this.templateSetDbConnProvider = templateSetDbConnProvider;
    }

    private String getFieldsJson(final TemplateSetDoc templateSetDoc) {
        return templateSetDoc.getFields() != null ? JsonUtil.writeValueAsString(templateSetDoc.getFields()) : null;
    }

    @Override
    public TemplateSetDoc create(final TemplateSetDoc templateSetDoc) {
        final String fieldsJson = getFieldsJson(templateSetDoc);
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
                        uuid.toString(),
                        templateSetDoc.getName(),
                        templateSetDoc.getVersion(),
                        templateSetDoc.getCreateTimeMs(),
                        templateSetDoc.getCreateUser(),
                        templateSetDoc.getUpdateTimeMs(),
                        templateSetDoc.getUpdateUser(),
                        templateSetDoc.getDescription(),
                        JSON.valueOf(fieldsJson)
                )
                .execute()
        );

        return new TemplateSetDoc(
                templateSetDoc.getType(),
                uuid.toString(),
                templateSetDoc.getName(),
                templateSetDoc.getVersion(),
                templateSetDoc.getCreateTimeMs(),
                templateSetDoc.getUpdateTimeMs(),
                templateSetDoc.getCreateUser(),
                templateSetDoc.getUpdateUser(),
                templateSetDoc.getDescription(),
                templateSetDoc.getFields()
        );
    }

    @Override
    public TemplateSetDoc update(final TemplateSetDoc templateSetDoc) {
        final String fieldsJson = getFieldsJson(templateSetDoc);

        final int count = JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                .update(TEMPLATE_SET)
                .set(TEMPLATE_SET.NAME, templateSetDoc.getName())
                .set(TEMPLATE_SET.VERSION, templateSetDoc.getVersion())
                .set(TEMPLATE_SET.UPDATE_TIME_MS, templateSetDoc.getUpdateTimeMs())
                .set(TEMPLATE_SET.UPDATE_USER, templateSetDoc.getUpdateUser())
                .set(TEMPLATE_SET.DESCRIPTION, templateSetDoc.getDescription())
                .set(TEMPLATE_SET.FIELDS, JSON.valueOf(fieldsJson))
                .where(TEMPLATE_SET.UUID.eq(templateSetDoc.getUuid()))
                .execute()
        );

        if (count == 0) {
            throw new DataChangedException("Unable to update TemplateSetDoc with UUID " + templateSetDoc.getUuid());
        }

        return templateSetDoc;
    }

    public boolean delete(final UUID uuid) {
        return JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                .deleteFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.UUID.eq(String.valueOf(uuid)))
                .execute()) > 0;
    }

    public Optional<TemplateSetDoc> fetch(final UUID uuid) {
        return JooqUtil.contextResult(templateSetDbConnProvider, context -> context
                        .select()
                        .from(TEMPLATE_SET)
                        .where(TEMPLATE_SET.UUID.eq(String.valueOf(uuid)))
                        .fetchOptional())
                .map(RECORD_TO_TEMPLATE_SET_MAPPER);
    }

    @Override
    public int deleteAllByOwner(final UserRef ownerRef) {
        Objects.requireNonNull(ownerRef);
        final int delCount = JooqUtil.contextResult(templateSetDbConnProvider, dslContext -> dslContext
                .deleteFrom(TEMPLATE_SET)
                .where(TEMPLATE_SET.CREATE_USER.eq(ownerRef.getUuid()))
                .execute());

        LOGGER.debug(() -> LogUtil.message("Deleted {} {} records for user {}",
                delCount, TEMPLATE_SET.getName(), ownerRef.toInfoString()));

        return delCount;
    }
}
