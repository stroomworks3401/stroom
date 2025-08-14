package stroom.template.set.impl.db;

import stroom.db.util.AbstractFlyWayDbModule;
import stroom.db.util.DataSourceProxy;

import java.util.List;
import javax.sql.DataSource;

public class TemplateSetDbModule extends AbstractFlyWayDbModule <TemplateSetDbConnProvider> {

    private static final String MODULE = "stroom-template-set";
    private static final String FLYWAY_LOCATIONS = "stroom/template/set/impl/db/migration";
    private static final String FLYWAY_TABLE = "template_set_schema_history";

    @Override
    protected String getFlyWayTableName() {
        return FLYWAY_TABLE;
    }

    @Override
    protected String getModuleName() {
        return MODULE;
    }

    @Override
    protected List<String> getFlyWayLocations() {
        return List.of(FLYWAY_LOCATIONS);
    }

    @Override
    protected Class<TemplateSetDbConnProvider> getConnectionProviderType() {
        return TemplateSetDbConnProvider.class;
    }

    @Override
    protected TemplateSetDbConnProvider createConnectionProvider(final DataSource dataSource) {
        return new DataSourceImpl(dataSource);
    }

    private static class DataSourceImpl extends DataSourceProxy implements TemplateSetDbConnProvider {

        private DataSourceImpl(final DataSource dataSource) {
            super(dataSource, MODULE);
        }
    }
}
