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

package stroom.explorer.shared;

import stroom.entity.shared.AbstractEntityAction;
import stroom.entity.shared.Action;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.Entity;

public class ExplorerCopyAction extends Action<ExplorerNode> {
    private static final long serialVersionUID = 800905016214418723L;

    private ExplorerNode document;
    private ExplorerNode folder;
    private String name;

    public ExplorerCopyAction() {
        // Default constructor necessary for GWT serialisation.
    }

    public ExplorerCopyAction(final ExplorerNode document, final ExplorerNode folder, final String name) {
        this.document = document;
        this.folder = folder;
        this.name = name;
    }

    public ExplorerNode getDocument() {
        return document;
    }

    public ExplorerNode getFolder() {
        return folder;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getTaskName() {
        return "Copy: '" + document.getType() + " " + document.getName() + "' -> '" + name + "'";
    }
}