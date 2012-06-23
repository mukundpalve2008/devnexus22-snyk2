/*
 *
 *  * Copyright (C) 2010-2012 Serge Rieder
 *  * serge@jkiss.org
 *  *
 *  * This library is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 2.1 of the License, or (at your option) any later version.
 *  *
 *  * This library is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this library; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jkiss.dbeaver.ui.views.session;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.runtime.load.LoadingUtils;
import org.jkiss.dbeaver.runtime.load.jobs.LoadingJob;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

/**
 * Session table
 */
class SessionTable extends DatabaseObjectListControl<DBAServerSession> {

    private DBAServerSessionManager<DBAServerSession> sessionManager;

    public SessionTable(Composite parent, int style, DBAServerSessionManager<DBAServerSession> sessionManager)
    {
        super(parent, style, CONTENT_PROVIDER);
        this.sessionManager = sessionManager;
        //setFitWidth(true);
    }

    @Override
    protected LoadingJob<Collection<DBAServerSession>> createLoadService()
    {
        return LoadingUtils.createService(
                new LoadSessionsService(),
                new ObjectsLoadVisualizer());
    }

    protected LoadingJob<Void> createAlterService(DBAServerSession session, Map<String, Object> options)
    {
        return LoadingUtils.createService(
                new KillSessionService(session, options),
                new ObjectActionVisualizer());
    }

    public void init(DBAServerSessionManager<DBAServerSession> sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    private static IStructuredContentProvider CONTENT_PROVIDER = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement)
        {
            if (inputElement instanceof Collection) {
                return ((Collection<?>)inputElement).toArray();
            }
            return null;
        }

        @Override
        public void dispose()
        {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
        {
        }

    };

    private class LoadSessionsService extends DatabaseLoadService<Collection<DBAServerSession>> {

        protected LoadSessionsService()
        {
            super("Load sessions", sessionManager.getDataSource());
        }

        @Override
        public Collection<DBAServerSession> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                DBCExecutionContext context = sessionManager.getDataSource().openContext(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Retrieve server sessions");
                try {
                    return sessionManager.getSessions(context, null);
                }
                finally {
                    context.close();
                }
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }
    }

    private class KillSessionService extends DatabaseLoadService<Void> {
        private final DBAServerSession session;
        private final Map<String, Object> options;

        protected KillSessionService(DBAServerSession session, Map<String, Object> options)
        {
            super("Kill session", sessionManager.getDataSource());
            this.session = session;
            this.options = options;
        }

        @Override
        public Void evaluate()
            throws InvocationTargetException, InterruptedException
        {
            try {
                DBCExecutionContext context = sessionManager.getDataSource().openContext(getProgressMonitor(), DBCExecutionPurpose.UTIL, "Kill server session");
                try {
                    sessionManager.alterSession(context, session, options);
                    return null;
                }
                finally {
                    context.close();
                }
            } catch (Throwable ex) {
                if (ex instanceof InvocationTargetException) {
                    throw (InvocationTargetException)ex;
                } else {
                    throw new InvocationTargetException(ex);
                }
            }
        }

    }

}
