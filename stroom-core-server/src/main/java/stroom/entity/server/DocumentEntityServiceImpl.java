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

package stroom.entity.server;

import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.shared.BaseEntityService;
import stroom.entity.shared.BaseResultList;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentEntity;
import stroom.entity.shared.DocumentEntityService;
import stroom.entity.shared.DocumentService;
import stroom.entity.shared.DocumentType;
import stroom.entity.shared.EntityServiceException;
import stroom.entity.shared.FindDocumentEntityCriteria;
import stroom.entity.shared.FindService;
import stroom.entity.shared.Folder;
import stroom.entity.shared.HasLoadByUuid;
import stroom.entity.shared.PageRequest;
import stroom.entity.shared.PermissionException;
import stroom.entity.shared.ProvidesNamePattern;
import stroom.logging.EntityEventLog;
import stroom.security.SecurityContext;
import stroom.security.shared.DocumentPermissionNames;
import stroom.util.config.StroomProperties;
import event.logging.BaseAdvancedQueryItem;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
@AutoMarshal
public abstract class DocumentEntityServiceImpl<E extends DocumentEntity, C extends FindDocumentEntityCriteria> implements DocumentEntityService<E, C>, BaseEntityService<E>, ProvidesNamePattern, SupportsCriteriaLogging<C> {
    public static final String NAME_PATTERN_PROPERTY = "stroom.namePattern";
    public static final String NAME_PATTERN_VALUE = "^[a-zA-Z0-9_\\- \\.\\(\\)]{1,}$";
    public static final String ID = "@ID@";
    public static final String TYPE = "@TYPE@";
    public static final String NAME = "@NAME@";

    protected static final String[] STANDARD_PERMISSIONS = new String[]{DocumentPermissionNames.USE,
            DocumentPermissionNames.READ, DocumentPermissionNames.UPDATE, DocumentPermissionNames.DELETE, DocumentPermissionNames.OWNER};

    private final StroomEntityManager entityManager;
    private final SecurityContext securityContext;
    private final EntityEventLog entityEventLog;
    private final EntityServiceHelper<E> entityServiceHelper;
    private final FindServiceHelper<E, C> findServiceHelper;

    private final QueryAppender<E, C> queryAppender;

    private String entityType;

    protected DocumentEntityServiceImpl(final StroomEntityManager entityManager, final SecurityContext securityContext, final EntityEventLog entityEventLog) {
        this.entityManager = entityManager;
        this.securityContext = securityContext;
        this.entityEventLog = entityEventLog;
        this.queryAppender = createQueryAppender(entityManager);
        this.entityServiceHelper = new EntityServiceHelper<>(entityManager, getEntityClass(), queryAppender);
        this.findServiceHelper = new FindServiceHelper<>(entityManager, getEntityClass(), queryAppender);
    }

    public StroomEntityManager getEntityManager() {
        return entityManager;
    }

    public EntityServiceHelper<E> getEntityServiceHelper() {
        return entityServiceHelper;
    }

    // =======================
    // START DocumentService
    // =======================
    @Override
    public DocRef createDocument(final DocRef folder, final String name) {
        return DocRef.create(create(folder, name));
    }

    @Override
    public DocRef copyDocument(final DocRef document, final DocRef folder, final String name) {
        final E original = loadByUuid(document.getUuid());
        return DocRef.create(copy(original, folder, name));
    }

    @Override
    public DocRef moveDocument(final DocRef document, final DocRef folder, final String name) {
        final E before = loadByUuid(document.getUuid());
        return DocRef.create(move(before, folder, name));
    }

    @Override
    public Boolean deleteDocument(final DocRef document) {
        final E entity = loadByUuid(document.getUuid());
        if (entity != null) {
            return delete(entity);
        }

        // If we couldn't find the entity then it must have been deleted already so return true.
        return true;
    }

    @Override
    public DocRef importDocument(final DocRef folder, final String name, final String data) {
        return null;
    }

    @Override
    public String exportDocument(final DocRef document) {
        return null;
    }

//    @Override
//    public DocumentType getDocumentType() {
//        return null;
//    }

    protected DocumentType getDocumentType(final int priority, final String type, final String displayType) {
        final String url = getIconUrl(type);
        return new DocumentType(priority, type, displayType, url);
    }

    private String getIconUrl(final String type) {
        return DocumentType.DOC_IMAGE_URL + type + ".png";
    }

    // =======================
    // END DocumentService
    // =======================

    @Override
    public E create(final DocRef folder, final String name) throws RuntimeException {
        E entity;

        try {
            // Check create permissions of the parent folder.
            checkCreatePermission(folder);

            // Validate the entity name.
            NameValidationUtil.validate(this, name);

            // Create a new entity instance.
            try {
                entity = getEntityClass().newInstance();
            } catch (final IllegalAccessException | InstantiationException e) {
                throw new EntityServiceException(e.getMessage());
            }

            if (entity.getUuid() == null) {
                entity.setUuid(UUID.randomUUID().toString());
            }
            entity.setName(name);
            entity = internalSave(entity);

            // Create the initial user permissions for this new document.
            String folderUuid = null;
            if (folder != null) {
                folderUuid = folder.getUuid();
            }
            securityContext.createInitialDocumentPermissions(entity.getType(), entity.getUuid(), folderUuid);
            entityEventLog.create(entity);

        } catch (final RuntimeException e) {
            entityEventLog.create(getEntityType(), name, e);
            throw e;
        } catch (final Exception e) {
            entityEventLog.create(getEntityType(), name, e);
            throw new RuntimeException(e);
        }

        return entity;
    }

    @Override
    public E load(final E entity) throws RuntimeException {
        return load(entity, Collections.emptySet());
    }

    @Override
    public E load(final E entity, final Set<String> fetchSet) throws RuntimeException {
        if (entity == null) {
            return null;
        }
        return loadById(entity.getId(), fetchSet);
    }

    @Override
    public E loadById(final long id) throws RuntimeException {
        return loadById(id, Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    @Override
    public E loadById(final long id, final Set<String> fetchSet) throws RuntimeException {
        E entity = null;

        final SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT e");
        sql.append(" FROM ");
        sql.append(getEntityClass().getName());
        sql.append(" AS e");

        queryAppender.appendBasicJoin(sql, "e", fetchSet);

        sql.append(" WHERE e.id = ");
        sql.arg(id);

        final List<E> resultList = getEntityManager().executeQueryResultList(sql);
        if (resultList != null && resultList.size() > 0) {
            entity = resultList.get(0);
        }

        if (entity != null) {
            try {
                queryAppender.postLoad(entity);
                checkReadPermission(DocRef.create(entity));
                entityEventLog.view(entity);
            } catch (final RuntimeException e) {
                entityEventLog.view(entity, e);
                throw e;
            } catch (final Exception e) {
                entityEventLog.view(entity, e);
                throw new RuntimeException(e);
            }
        }

        return entity;
    }

    // TODO : Remove this method when the explorer service is broken out as a separate micro service.
    public E loadByIdInsecure(final long id, final Set<String> fetchSet) throws RuntimeException {
        E entity = null;

        final SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT e");
        sql.append(" FROM ");
        sql.append(getEntityClass().getName());
        sql.append(" AS e");

        queryAppender.appendBasicJoin(sql, "e", fetchSet);

        sql.append(" WHERE e.id = ");
        sql.arg(id);

        final List<E> resultList = getEntityManager().executeQueryResultList(sql);
        if (resultList != null && resultList.size() > 0) {
            entity = resultList.get(0);
        }

        if (entity != null) {
            queryAppender.postLoad(entity);
        }

        return entity;
    }

    @Override
    public final E loadByUuid(final String uuid) throws RuntimeException {
        return loadByUuid(uuid, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final E loadByUuid(final String uuid, final Set<String> fetchSet) throws RuntimeException {
        E entity = null;

        final SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT e FROM ");
        sql.append(getEntityClass().getName());
        sql.append(" AS e");
        queryAppender.appendBasicJoin(sql, "e", fetchSet);
        sql.append(" WHERE e.uuid = ");
        sql.arg(uuid);

        final List<E> resultList = getEntityManager().executeQueryResultList(sql);
        if (resultList != null && resultList.size() > 0) {
            entity = resultList.get(0);
        }

        if (entity != null) {
            try {
                queryAppender.postLoad(entity);
                checkReadPermission(DocRef.create(entity));
                entityEventLog.view(entity);
            } catch (final RuntimeException e) {
                entityEventLog.view(entity, e);
                throw e;
            } catch (final Exception e) {
                entityEventLog.view(entity, e);
                throw new RuntimeException(e);
            }
        }

        return entity;
    }

    // TODO : Remove this method when the explorer service is broken out as a separate micro service.
    public final E loadByUuidInsecure(final String uuid, final Set<String> fetchSet) throws RuntimeException {
        final SQLBuilder sql = new SQLBuilder();
        sql.append("SELECT e FROM ");
        sql.append(getEntityClass().getName());
        sql.append(" AS e");
        queryAppender.appendBasicJoin(sql, "e", fetchSet);
        sql.append(" WHERE e.uuid = ");
        sql.arg(uuid);

        final BaseResultList<E> list = BaseResultList.createUnboundedList(entityManager.executeQueryResultList(sql));
        final E entity = list.getFirst();

        if (entity != null) {
            queryAppender.postLoad(entity);
        }

        return entity;
    }
//
//    @Override
//    public final E loadByName(final DocRef folder, final String name) throws RuntimeException {
//        return loadByName(folder, name, null);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Override
//    public final E loadByName(final DocRef folder, final String name, final Set<String> fetchSet) throws RuntimeException {
//        final SQLBuilder sql = new SQLBuilder();
//        sql.append("SELECT e FROM ");
//        sql.append(getEntityClass().getName());
//        sql.append(" AS e");
//        queryAppender.appendBasicJoin(sql, "e", fetchSet);
//        sql.append(" WHERE e.name = ");
//        sql.arg(name);
//
//        final Class<?> clazz = getEntityClass();
//        if (DocumentEntity.class.isAssignableFrom(clazz)) {
//            // For some reason this doesn't work on folders themselves?
//            if (!getEntityClass().equals(Folder.class)) {
//                if (folder == null) {
//                    sql.append(" AND e.folder IS NULL");
//                } else {
//                    sql.append(" AND e.folder.uuid = ");
//                    sql.arg(folder.getUuid());
//                }
//            }
//        }
//
//        final BaseResultList<E> list = BaseResultList.createUnboundedList(entityManager.executeQueryResultList(sql));
//
//        // FIXME: Fix onOce folders have been removed from entities. For now filter by parent group id manually
//        E entity = null;
//        if (getEntityClass().equals(Folder.class)) {
//            for (final E e : list) {
//                if (folder == null) {
//                    if (e.getFolder() == null) {
//                        entity = e;
//                        break;
//                    }
//                } else {
//                    if (EqualsUtil.isEquals(folder.getUuid(), e.getFolder().getUuid())) {
//                        entity = e;
//                        break;
//                    }
//                }
//            }
//        } else {
//            entity = list.getFirst();
//        }
//
//        if (entity != null) {
//            queryAppender.postLoad(entity);
//            checkReadPermission(entity);
//        }
//
//        return entity;
//    }

    @Override
    public E save(final E entity) throws RuntimeException {
        if (!entity.isPersistent()) {
            throw new EntityServiceException("You cannot update an entity that has not been created");
        }

        checkUpdatePermission(DocRef.create(entity));

        return internalSave(entity);
    }

    protected E internalSave(final E entity) throws RuntimeException {
        if (entity.getUuid() == null) {
            entity.setUuid(UUID.randomUUID().toString());
        }
        return entityServiceHelper.save(entity);
    }

//    @Override
//    public DocRef copy(final DocRef item, final DocRef folder, final String name) {
//        DocRef result = null;
//
//        E original = loadByUuid(item.getUuid());
//        return copy(original, folder, name);
//    }

    @Override
    public E copy(final E original, final DocRef folder, final String name) {
        E copy = original;

        if (original != null) {
            // This is going to be a copy so clear the persistence so save will create a new DB entry.
            copy.clearPersistence();
            copy.setName(name);

            try {
                // Check create permission on target folder.
                checkCreatePermission(folder);

                // Validate the entity name.
                NameValidationUtil.validate(this, name);

                // Save the copy.
                copy = internalSave(copy);

                // Create the initial user permissions for this new document.
                String folderUuid = null;
                if (folder != null) {
                    folderUuid = folder.getUuid();
                }
                securityContext.createInitialDocumentPermissions(copy.getType(), copy.getUuid(), folderUuid);

                entityEventLog.copy(original, copy);

            } catch (final RuntimeException e) {
                entityEventLog.copy(original, copy, e);
                throw e;
            } catch (final Exception e) {
                entityEventLog.copy(original, copy, e);
                throw new RuntimeException(e);
            }
        }

        return copy;
    }

    @Override
    public E move(final E before, final DocRef folder, final String name) {
        E after = before;

        if (before != null) {
            after.setName(name);

            try {
                // Check update permission on item.
                checkUpdatePermission(DocRef.create(before));

                // Validate the entity name.
                NameValidationUtil.validate(this, name);

                // Save
                after = internalSave(after);

                entityEventLog.move(before, after);

            } catch (final RuntimeException e) {
                entityEventLog.move(before, after, e);
                throw e;
            } catch (final Exception e) {
                entityEventLog.move(before, after, e);
                throw new RuntimeException(e);
            }
        }

        return after;
    }

//    @Override
//    public Boolean delete(final DocRef item) {
//        final E entity = loadByUuid(item.getUuid());
//        if (entity != null) {
//            return delete(entity);
//        }
//
//        // If we couldn't find the entity then it must have been deleted already so return true.
//        return true;
//    }

    @Override
    public Boolean delete(final E entity) throws RuntimeException {
        Boolean success;
        try {
            checkDeletePermission(DocRef.create(entity));
            success = entityServiceHelper.delete(entity);
            entityEventLog.delete(entity);
        } catch (final RuntimeException e) {
            entityEventLog.delete(entity, e);
            throw e;
        } catch (final Exception e) {
            entityEventLog.delete(entity, e);
            throw new RuntimeException(e);
        }

        return success;
    }

//    @Override
//    @Transactional(readOnly = true)
//    @SuppressWarnings("unchecked")
//    public List<E> findByFolder(final DocRef folder, final Set<String> fetchSet) throws RuntimeException {
//        final SQLBuilder sql = new SQLBuilder();
//        sql.append("SELECT e FROM ");
//        sql.append(getEntityClass().getName());
//        sql.append(" AS e");
//
//        queryAppender.appendBasicJoin(sql, "e", fetchSet);
//
//        sql.append(" WHERE 1=1");
//
//        if (folder != null) {
//            sql.append(" AND e.folder.uuid = ");
//            sql.arg(folder.getUuid());
//        }
//
//        final List<E> list = entityManager.executeQueryResultList(sql);
//        return filterResults(list, DocumentPermissionNames.READ);
//    }

    @Override
    public BaseResultList<E> find(final C criteria) throws RuntimeException {
        // Make sure the required permission is a valid one.
        String permission = criteria.getRequiredPermission();
        if (permission == null) {
            permission = DocumentPermissionNames.READ;
        } else if (!DocumentPermissionNames.isValidPermission(permission)) {
            throw new IllegalArgumentException("Unknown permission " + permission);
        }

        BaseResultList<E> result = null;

        // Find documents using the supplied criteria.
        // We do not want to limit the results by offset or length at this point as we will filter out results later based on user permissions.
        // We will only limit the returned number of results once we have applied permission filtering.
        final PageRequest pageRequest = criteria.getPageRequest();
        criteria.setPageRequest(null);
        final List<E> list = findServiceHelper.find(criteria);
        criteria.setPageRequest(pageRequest);

        // Filter the results to only include documents that the current user has permission to see.
        final List<E> filtered = filterResults(list, permission);

        if (pageRequest != null) {
            int offset = 0;
            int length = filtered.size();

            if (pageRequest.getOffset() != null) {
                offset = pageRequest.getOffset().intValue();
            }

            if (pageRequest.getLength() != null) {
                length = Math.min(length, pageRequest.getLength());
            }

            // If the page request will lead to a limited number of results then apply that limit here.
            if (offset != 0 || length < filtered.size()) {
                final List<E> limited = new ArrayList<>(length);
                for (int i = offset; i < offset + length; i++) {
                    limited.add(filtered.get(i));
                }
                result = new BaseResultList<>(limited, (long) offset, (long) filtered.size(), offset + length < filtered.size());
            }
        }

        if (result == null) {
            result = new BaseResultList<>(filtered, (long) 0, (long) filtered.size(), false);
        }

        return result;
    }

    // TODO : Remove this method when the explorer service is broken out as a separate micro service.
    public BaseResultList<E> findInsecure(final C criteria) throws RuntimeException {
        final List<E> list = findServiceHelper.find(criteria);
        return BaseResultList.createCriterialBasedList(list, criteria);
    }

    private List<E> filterResults(final List<E> list, final String permission) {
        return list.stream().filter(e -> securityContext.hasDocumentPermission(e.getType(), e.getUuid(), permission)).collect(Collectors.toList());
    }

//    protected List<EntityReferenceQuery> getReferenceTableList() {
//        return Collections.emptyList();
//    }
//
//    @Override
//    public E importEntity(final DocRef folder, final E entity) {
//
//
//        // Check that the user is allowed to create items of this type in the parent folder.
//        checkCreatePermission(folder);
//
//        // Save directly so there is no marshalling of objects that would destroy imported data.
//        return getEntityManager().saveEntity(entity);
//
//
//
//
//
//
//
//
//
//
//
//
//
////        DocRef result;
////
////        try {
////            // Check that the user has import permissions for the parent folder.
////            if (!securityContext.hasDocumentPermission(folder.getType(), folder.getUuid(), DocumentPermissionNames.IMPORT)) {
////                throw new PermissionException("You do not have permission to import " + getDocReference(entity) + " into folder " + folder);
////            }
////
////            // Check create permissions of the parent folder.
////            checkCreatePermission(folder);
////
////            // Validate the entity name.
////            NameValidationUtil.validate(this, name);
////
////            // Create a new entity instance.
////            E entity;
////            try {
////                entity = getEntityClass().newInstance();
////            } catch (final IllegalAccessException | InstantiationException e) {
////                throw new EntityServiceException(e.getMessage());
////            }
////
////            entity.setName(name);
////            entity = entityServiceHelper.create(entity);
////
////            // Create the initial user permissions for this new document.
////            String folderUuid = null;
////            if (folder != null) {
////                folderUuid = folder.getUuid();
////            }
////            securityContext.createInitialDocumentPermissions(entity.getType(), entity.getUuid(), folderUuid);
////
////            result = DocRef.create(entity);
////
////            entityEventLog.create(entity);
////
////        } catch (final RuntimeException e) {
////            entityEventLog.create(getEntityType(), name, e);
////            throw e;
////        } catch (final Exception e) {
////            entityEventLog.create(getEntityType(), name, e);
////            throw new RuntimeException(e);
////        }
////
////        return result;
//    }
//
//    @Override
//    public E exportEntity(final E entity) {
//        if (!securityContext.hasDocumentPermission(entity.getType(), entity.getUuid(), DocumentPermissionNames.EXPORT)) {
//            throw new PermissionException("You do not have permission to export " + getDocReference(entity));
//        }
//        return entityServiceHelper.load(entity);
//    }

    @Transient
    @Override
    public String getNamePattern() {
        return StroomProperties.getProperty(NAME_PATTERN_PROPERTY, NAME_PATTERN_VALUE);
    }

//    protected void validateName(final String name) {
//        final String pattern = getNamePattern();
//        if (pattern != null && pattern.length() > 0) {
//            if (name == null || !name.matches(pattern)) {
//                throw new EntityServiceException("Invalid name \"" + name + "\" (" + getEntityType() + "  " + pattern + ")");
//            }
//        }
//    }
//
//    private String getDocReference(BaseEntity entity) {
//        return "(" + DocRef.create(entity).toString() + ")";
//    }

    public abstract Class<E> getEntityClass();

    public String getEntityType() {
        if (entityType == null) {
            try {
                entityType = getEntityClass().newInstance().getType();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
        return entityType;
    }

    @Override
    public String[] getPermissions() {
        return STANDARD_PERMISSIONS;
    }

    protected QueryAppender<E, C> createQueryAppender(final StroomEntityManager entityManager) {
        return new QueryAppender(entityManager);
    }

    protected final QueryAppender<E, C> getQueryAppender() {
        return queryAppender;
    }

    @Override
    public void appendCriteria(final List<BaseAdvancedQueryItem> items, final C criteria) {
        CriteriaLoggingUtil.appendPageRequest(items, criteria.getPageRequest());
    }

    protected void checkCreatePermission(final DocRef folder) {
        if (!securityContext.hasDocumentPermission(Folder.ENTITY_TYPE, folder.getUuid(), DocumentPermissionNames.getDocumentCreatePermission(getEntityType()))) {
            throw new PermissionException("You do not have permission to create " + getEntityType() + " in folder " + folder);
        }
    }

    protected final void checkUpdatePermission(final DocRef docRef) {
        if (!securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.UPDATE)) {
            throw new PermissionException("You do not have permission to update (" + docRef.toString() + ")");
        }
    }

    protected final void checkReadPermission(final DocRef docRef) {
        if (!securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.READ)) {
            throw new PermissionException("You do not have permission to read (" + docRef.toString() + ")");
        }
    }

    protected final void checkDeletePermission(final DocRef docRef) {
        if (!securityContext.hasDocumentPermission(docRef.getType(), docRef.getUuid(), DocumentPermissionNames.DELETE)) {
            throw new PermissionException("You do not have permission to delete (" + docRef.toString() + ")");
        }
    }

    public static final class EntityReferenceQuery {
        private final String entityType;
        private final String tableName;
        private final String whereClause;

        public EntityReferenceQuery(final String entityType, final String tableName, final String whereClause) {
            this.entityType = entityType;
            this.tableName = tableName;
            this.whereClause = whereClause;
        }

        public String getEntityType() {
            return entityType;
        }

        public String getTableName() {
            return tableName;
        }

        public String getWhereClause() {
            return whereClause;
        }
    }
}