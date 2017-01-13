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

import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentService;
import stroom.entity.shared.DocumentServiceLocator;
import stroom.explorer.shared.ExplorerMoveAction;
import stroom.explorer.shared.ExplorerNode;
import stroom.task.server.AbstractTaskHandler;
import stroom.task.server.TaskHandlerBean;

import javax.inject.Inject;

@TaskHandlerBean(task = ExplorerMoveAction.class)
class ExplorerMoveHandler
        extends AbstractTaskHandler<ExplorerMoveAction, ExplorerNode> {
    private final DocumentServiceLocator documentServiceLocator;
    private final ExplorerTreeDao explorerTreeDao;

    @Inject
    ExplorerMoveHandler(final DocumentServiceLocator documentServiceLocator, final ExplorerTreeDao explorerTreeDao) {
        this.documentServiceLocator = documentServiceLocator;
        this.explorerTreeDao = explorerTreeDao;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ExplorerNode exec(final ExplorerMoveAction action) {
        final DocumentService documentService = documentServiceLocator.locate(action.getDocument().getType());

        ExplorerNode explorerNode;

        try {
            final DocRef docRef = documentService.moveDocument(action.getDocument().getDocRef(), action.getFolder().getDocRef(), action.getName());

            // Remove the old explorer entry.
            final ExplorerTreeNode old = ExplorerTreeNode.create(action.getDocument());
            explorerTreeDao.remove(old);

            // Add the new explorer entry.
            final ExplorerTreeNode parent = ExplorerTreeNode.create(action.getFolder());
            ExplorerTreeNode child = new ExplorerTreeNode(docRef.getType(), docRef.getUuid(), docRef.getName(), null);
            child = explorerTreeDao.addChild(parent, child);

            explorerNode = new ExplorerNode(child.getId(), child.getType(), child.getUuid(), child.getName(), child.getTags());

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        return explorerNode;
    }
}
