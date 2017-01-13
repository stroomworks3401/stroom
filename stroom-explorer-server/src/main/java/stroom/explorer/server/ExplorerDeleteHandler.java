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

package stroom.explorer.server;

import stroom.entity.shared.DocumentService;
import stroom.entity.shared.DocumentServiceLocator;
import stroom.explorer.shared.ExplorerDeleteAction;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;
import stroom.util.shared.VoidResult;

import javax.inject.Inject;

@TaskHandlerBean(task = ExplorerDeleteAction.class)
class ExplorerDeleteHandler
        extends AbstractTaskHandler<ExplorerDeleteAction, VoidResult> {
    private final DocumentServiceLocator documentServiceLocator;
    private final ExplorerTreeDao explorerTreeDao;

    @Inject
    ExplorerDeleteHandler(final DocumentServiceLocator documentServiceLocator, final ExplorerTreeDao explorerTreeDao) {
        this.documentServiceLocator = documentServiceLocator;
        this.explorerTreeDao = explorerTreeDao;
    }

    @SuppressWarnings("unchecked")
    @Override
    public VoidResult exec(final ExplorerDeleteAction action) {
        final DocumentService documentService = documentServiceLocator.locate(action.getDocument().getType());

        if (documentService.deleteDocument(action.getDocument().getDocRef())) {
            // Remove the explorer entry.
            final ExplorerTreeNode node = ExplorerTreeNode.create(action.getDocument());
            explorerTreeDao.remove(node);
        }

        return VoidResult.INSTANCE;
    }

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
//
//
//
//
//
//        extends AbstractTaskHandler<ExplorerDeleteAction<BaseEntity>, BaseEntity> {
//    private final GenericEntityService entityService;
//    private final EntityEventLog entityEventLog;
//
//    @Inject
//    ExplorerDeleteHandler(final GenericEntityService entityService, final EntityEventLog entityEventLog) {
//        this.entityService = entityService;
//        this.entityEventLog = entityEventLog;
//    }
//
//    @Override
//    public BaseEntity exec(final ExplorerDeleteAction<BaseEntity> action) {
//        final BaseEntity entity = action.getEntity();
//        try {
//            entityService.delete(entity);
//            if (entity != null) {
//                entityEventLog.delete(entity);
//            }
//        } catch (final RuntimeException e) {
//            if (entity != null) {
//                entityEventLog.delete(entity, e);
//            }
//            throw e;
//        }
//
//        return entity;
//    }
}
