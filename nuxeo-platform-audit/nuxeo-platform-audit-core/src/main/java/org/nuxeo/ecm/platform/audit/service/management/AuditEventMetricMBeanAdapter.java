/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.platform.audit.service.management;

import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;

/**
 * @author matic
 */
public class AuditEventMetricMBeanAdapter implements AuditEventMetricMBean {

    protected final NXAuditEventsService service;

    protected final String eventName;

    protected AuditEventMetricMBeanAdapter(NXAuditEventsService service,
            String name) {
        this.service = service;
        this.eventName = name;
    }

    public Long getCount() {
        return service.getEventsCount(eventName);
    }

}
