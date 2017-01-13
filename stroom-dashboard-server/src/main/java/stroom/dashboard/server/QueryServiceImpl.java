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

package stroom.dashboard.server;

import event.logging.BaseAdvancedQueryItem;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import stroom.dashboard.shared.Dashboard;
import stroom.dashboard.shared.FindQueryCriteria;
import stroom.dashboard.shared.Query;
import stroom.dashboard.shared.QueryService;
import stroom.entity.server.AutoMarshal;
import stroom.entity.server.CriteriaLoggingUtil;
import stroom.entity.server.DocumentEntityServiceImpl;
import stroom.entity.server.QueryAppender;
import stroom.entity.server.util.SQLBuilder;
import stroom.entity.server.util.SQLUtil;
import stroom.entity.server.util.StroomEntityManager;
import stroom.entity.shared.DocRef;
import stroom.entity.shared.DocumentType;
import stroom.logging.EntityEventLog;
import stroom.query.shared.QueryData;
import stroom.security.SecurityContext;
import stroom.util.spring.StroomSpringProfiles;

import javax.inject.Inject;
import java.util.List;

@Profile(StroomSpringProfiles.PROD)
@Component("queryService")
@Transactional
@AutoMarshal
public class QueryServiceImpl extends DocumentEntityServiceImpl<Query, FindQueryCriteria> implements QueryService {
    private final SecurityContext securityContext;

    @Inject
    QueryServiceImpl(final StroomEntityManager entityManager, final SecurityContext securityContext, final EntityEventLog entityEventLog) {
        super(entityManager, securityContext, entityEventLog);
        this.securityContext = securityContext;
    }

    @Override
    public DocumentType getDocumentType() {
        return null;
    }

    @Override
    public Class<Query> getEntityClass() {
        return Query.class;
    }

    @Override
    public FindQueryCriteria createCriteria() {
        return new FindQueryCriteria();
    }

    @Override
    protected void checkCreatePermission(final DocRef folder) {
        // Do nothing as users can always create queries because they don't belong in a folder.
    }

//    @Override
//    public Query create(final DocRef folder, final String name) {
//        return super.create(folder, name);
//    }
//
//    @Override
//    public void create(final String name, final Dashboard dashboard, final QueryData queryData) {
//        final Query query = internalCreate(null, name);
//        query.setDashboard(dashboard);
//        query.setQueryData(queryData);
//        internalSave(query);
//    }

    @Override
    public void appendCriteria(final List<BaseAdvancedQueryItem> items, final FindQueryCriteria criteria) {
        CriteriaLoggingUtil.appendEntityIdSet(items, "dashboardIdSet", criteria.getDashboardIdSet());
        super.appendCriteria(items, criteria);
    }

    @Override
    protected QueryAppender<Query, FindQueryCriteria> createQueryAppender(final StroomEntityManager entityManager) {
        return new QueryQueryAppender(entityManager);
    }

    @Override
    public String getNamePattern() {
        // Unnamed queries are valid.
        return null;
    }

    private static class QueryQueryAppender extends QueryAppender<Query, FindQueryCriteria> {
        public QueryQueryAppender(final StroomEntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected void appendBasicCriteria(final SQLBuilder sql, final String alias, final FindQueryCriteria criteria) {
            super.appendBasicCriteria(sql, alias, criteria);

            if (criteria.getNameCriteria() != null) {
                SQLUtil.appendValueQuery(sql, alias + ".name", criteria.getNameCriteria());
            }

            SQLUtil.appendSetQuery(sql, true, alias + ".dashboard", criteria.getDashboardIdSet());
        }
    }
}
