package stroom.template.set.impl.db;

import stroom.template.set.impl.TemplateSetDao;

import com.google.inject.AbstractModule;

public class TemplateSetDaoModule extends AbstractModule {

    @Override
    protected void configure() {
        super.configure();

        bind(TemplateSetDao.class).to(TemplateSetDaoImpl.class);
    }
}
