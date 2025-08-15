package stroom.template.set.impl.db;

import stroom.db.util.AbstractFlyWayDbModule;
import stroom.db.util.DataSourceProxy;

import java.util.List;
import javax.sql.DataSource;

/**
 * Guice module for the TemplateSet database.
 * Uses FlyWay for schema migration.
 */
public class TemplateSetDbModule extends AbstractFlyWayDbModule<TemplateSetConfig, TemplateSetDbConnProvider> {

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
        return new TemplateSetDataSource(dataSource);
    }

    /**
     * Concrete DataSource implementation.
     * Wraps the real DataSource and associates it with this module.
     */
    private static class TemplateSetDataSource extends DataSourceProxy implements TemplateSetDbConnProvider {

        private TemplateSetDataSource(final DataSource dataSource) {
            super(dataSource, MODULE);
        }
    }
}

