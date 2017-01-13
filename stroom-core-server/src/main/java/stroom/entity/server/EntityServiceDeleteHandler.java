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

import stroom.entity.shared.BaseEntity;
import stroom.entity.shared.EntityService;
import stroom.entity.shared.EntityServiceDeleteAction;
import stroom.entity.shared.EntityServiceException;
import stroom.entity.shared.EntityServiceSaveAction;
import stroom.logging.EntityEventLog;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;

import javax.inject.Inject;

@TaskHandlerBean(task = EntityServiceDeleteAction.class)
class EntityServiceDeleteHandler extends AbstractTaskHandler<EntityServiceDeleteAction<BaseEntity>, BaseEntity> {
    private final EntityServiceBeanRegistry beanRegistry;
    private final EntityEventLog entityEventLog;

    @Inject
    EntityServiceDeleteHandler(final EntityServiceBeanRegistry beanRegistry, final EntityEventLog entityEventLog) {
        this.beanRegistry = beanRegistry;
        this.entityEventLog = entityEventLog;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BaseEntity exec(final EntityServiceDeleteAction<BaseEntity> action) {
        final Object bean = beanRegistry.getEntityService(action.getEntity().getClass());
        if (bean == null) {
            throw new EntityServiceException("No entity service can be found");
        }
        if (!(bean instanceof EntityService<?>)) {
            throw new EntityServiceException("Bean is not an entity service");
        }

        final EntityService<BaseEntity> entityService = (EntityService<BaseEntity>) bean;
        final BaseEntity entity = action.getEntity();

        try {
            entityService.delete(entity);
                entityEventLog.delete(entity);
        } catch (final RuntimeException e) {
                entityEventLog.delete(entity, e);
            throw e;
        }

        return entity;
    }
}
