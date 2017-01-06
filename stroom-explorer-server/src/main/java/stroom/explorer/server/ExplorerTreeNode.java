package stroom.explorer.server;

import stroom.entity.shared.BaseEntitySmall;
import stroom.entity.shared.HasName;
import stroom.entity.shared.HasUuid;
import stroom.entity.shared.SQLNameConstants;
import stroom.util.shared.HasDisplayValue;
import stroom.util.shared.HasType;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

@MappedSuperclass
public class ExplorerTreeNode implements Cloneable {
    private static final long serialVersionUID = -6752927142242673318L;

    // Value of a long to represent an undefined id.
    private static final long UNDEFINED_ID = -1;

    public static final String TYPE = SQLNameConstants.TYPE;
    public static final String UUID = SQLNameConstants.UUID;
    public static final String NAME = SQLNameConstants.NAME;

    private long id = UNDEFINED_ID;
    private String type;
    private String uuid;
    private String name;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "INT")
    @XmlTransient
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Column(name = TYPE, nullable = false)
    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Column(name = UUID, nullable = false)
    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Column(name = NAME, nullable = false)
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