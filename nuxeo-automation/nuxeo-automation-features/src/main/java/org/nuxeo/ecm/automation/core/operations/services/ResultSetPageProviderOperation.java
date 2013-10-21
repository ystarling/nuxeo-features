/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.ecm.automation.core.operations.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.RecordSet;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;

/**
 * Operation to execute a query or a named provider with support for Pagination
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.7
 */
@Operation(id = ResultSetPageProviderOperation.ID, category = Constants.CAT_FETCH, label = "QueryAndFetch", description = "Perform "
        + "a query or a named provider query on the repository. Result is "
        + "paginated. The result is returned as a RecordSet (QueryAndFetch) rather than as a List of Document"
        + "The query result will become the input for the next "
        + "operation. If no query or provider name is given, a query returning "
        + "all the documents that the user has access to will be executed.", addToStudio = false)
public class ResultSetPageProviderOperation {

    public static final String ID = "Resultset.PageProvider";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    @Context
    protected OperationContext context;

    @Context
    protected CoreSession session;

    @Param(name = "providerName", required = false)
    protected String providerName;

    @Param(name = "query", required = false)
    protected String query;

    @Param(name = "language", required = false, widget = Constants.W_OPTION, values = { NXQL.NXQL })
    protected String lang = NXQL.NXQL;

    @Param(name = "page", required = false)
    protected Integer page;

    @Param(name = "pageSize", required = false)
    protected Integer pageSize;

    @Param(name = "sortInfo", required = false)
    protected StringList sortInfoAsStringList;

    @Param(name = "queryParams", required = false)
    protected StringList strParameters;

    @Param(name = "documentLinkBuilder", required = false)
    protected String documentLinkBuilder;

    /**
     * @since 5.7
     */
    @Param(name = "maxResults", required = false)
    protected String maxResults = "100";

    @SuppressWarnings("unchecked")
    @OperationMethod
    public RecordSet run() throws Exception {

        PageProviderService pps = Framework.getLocalService(PageProviderService.class);

        List<SortInfo> sortInfos = null;
        if (sortInfoAsStringList != null) {
            sortInfos = new ArrayList<SortInfo>();
            for (String sortInfoDesc : sortInfoAsStringList) {
                SortInfo sortInfo;
                if (sortInfoDesc.contains("|")) {
                    String[] parts = sortInfoDesc.split("|");
                    sortInfo = new SortInfo(parts[0],
                            Boolean.parseBoolean(parts[1]));
                } else {
                    sortInfo = new SortInfo(sortInfoDesc, true);
                }
                sortInfos.add(sortInfo);
            }
        }

        Object[] parameters = null;

        if (strParameters != null && !strParameters.isEmpty()) {
            parameters = strParameters.toArray(new String[strParameters.size()]);
            // expand specific parameters
            for (int idx = 0; idx < parameters.length; idx++) {
                String value = (String) parameters[idx];
                if (value.equals(CURRENT_USERID_PATTERN)) {
                    parameters[idx] = session.getPrincipal().getName();
                } else if (value.equals(CURRENT_REPO_PATTERN)) {
                    parameters[idx] = session.getRepositoryName();
                }
            }
        }

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);

        if (query == null
                && (providerName == null || providerName.length() == 0)) {
            // provide a defaut query
            query = "SELECT * from Document";
        }

        Long targetPage = null;
        if (page != null) {
            targetPage = Long.valueOf(page.longValue());
        }
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = Long.valueOf(pageSize.longValue());
        }

        final class QueryAndFetchProviderDescriptor extends
                GenericPageProviderDescriptor {
            private static final long serialVersionUID = 1L;

            public QueryAndFetchProviderDescriptor() {
                super();
                try {
                    this.klass = (Class<PageProvider<?>>) Class.forName(CoreQueryAndFetchPageProvider.class.getName());
                } catch (ClassNotFoundException e) {

                }
            }
        }

        PageProvider<Map<String, Serializable>> pp = null;
        if (query != null) {
            QueryAndFetchProviderDescriptor desc = new QueryAndFetchProviderDescriptor();
            desc.setPattern(query);
            if (maxResults != null && !maxResults.isEmpty()
                    && !maxResults.equals("-1")) {
                // set the maxResults to avoid slowing down queries
                desc.getProperties().put("maxResults", maxResults);
            }
            pp = (CoreQueryAndFetchPageProvider) pps.getPageProvider("", desc,
                    null, sortInfos, targetPageSize, targetPage, props,
                    parameters);

        } else {
            pp = (PageProvider<Map<String, Serializable>>) pps.getPageProvider(
                    providerName, sortInfos, targetPageSize, targetPage, props,
                    parameters);
        }
        return new PaginableRecordSetImpl(pp);

    }
}
