/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Progress monitor default implementation
 */
public class DefaultProgressMonitor implements DBRProgressMonitor {

    static final Log log = LogFactory.getLog(DefaultProgressMonitor.class);

    private IProgressMonitor nestedMonitor;
    private List<DBRBlockingObject> blocks = null;
    private boolean taskRunning = false;

    public DefaultProgressMonitor(IProgressMonitor nestedMonitor)
    {
        this.nestedMonitor = nestedMonitor;
    }

    public IProgressMonitor getNestedMonitor()
    {
        return nestedMonitor;
    }

    public void beginTask(String name, int totalWork)
    {
        nestedMonitor.beginTask(name, totalWork);
        taskRunning = true;
    }

    public void done()
    {
        nestedMonitor.done();
        taskRunning = false;
    }

    public void subTask(String name)
    {
        nestedMonitor.subTask(name);
    }

    public void worked(int work)
    {
        nestedMonitor.worked(work);
    }

    public boolean isCanceled()
    {
        return nestedMonitor.isCanceled();
    }

    public synchronized void startBlock(DBRBlockingObject object, String taskName)
    {
        if (taskRunning) {
            subTask(taskName);
        } else {
            beginTask(taskName, 1);
        }
        if (blocks == null) {
            blocks = new ArrayList<DBRBlockingObject>();
        }
        blocks.add(object);
    }

    public synchronized void endBlock()
    {
        if (blocks == null || blocks.isEmpty()) {
            log.warn("End block invoked while no blocking objects are in stack");
            return;
        }
        //if (blocks.size() == 1) {
        //    this.done();
        //}
        blocks.remove(blocks.size() - 1);
    }

    public DBRBlockingObject getActiveBlock()
    {
        return blocks == null || blocks.isEmpty() ? null : blocks.get(blocks.size() - 1);
    }

}
