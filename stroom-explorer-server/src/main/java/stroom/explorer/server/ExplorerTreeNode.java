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

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import stroom.entity.shared.BaseEntitySmall;
import stroom.entity.shared.HasName;
import stroom.entity.shared.HasUuid;
import stroom.entity.shared.SQLNameConstants;
import stroom.util.shared.HasDisplayValue;
import stroom.util.shared.HasType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlTransient;

@Entity
@Table(name = "EXPLORER_TREE_NODE", uniqueConstraints = @UniqueConstraint(columnNames = { "TYPE", "UUID" }) )
public class ExplorerTreeNode implements ClosureTableTreeNode {
    private Long id;
    private String type;
    private String uuid;
    private String name;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID", columnDefinition = "INT")
    @XmlTransient
    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Column(name = "TYPE", nullable = false)
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Column(name = "UUID", nullable = false)
    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Column(name = "NAME", nullable = false)
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

//    @Override
//    protected void toString(final StringBuilder sb) {
//        super.toString(sb);
//        sb.append(", type=");
//        sb.append(type);
//        sb.append(", uuid=");
//        sb.append(uuid);
//        sb.append(", name=");
//        sb.append(name);
//    }

    @Override
    public ExplorerTreeNode clone() {
        final ExplorerTreeNode clone = new ExplorerTreeNode();
        clone.type = type;
        clone.uuid = uuid;
        clone.name = name;
        return clone;
    }
}