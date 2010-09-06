/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.registry.event.RegistryEvent;

/**
 * DataSourceEvent
 */
public class DBNEvent extends RegistryEvent
{
    public enum Action
    {
        ADD,
        REMOVE,
        UPDATE,
    }

    public enum NodeChange {
        LOAD,
        UNLOAD,
        REFRESH,
        LOCK,
        UNLOCK,
    }

    private Action action;
    private NodeChange nodeChange;
    private DBNNode node;

    public DBNEvent(Object source, Action action, DBNNode node)
    {
        this(source, action, NodeChange.REFRESH, node);
        this.action = action;
        this.node = node;
    }

    public DBNEvent(Object source, Action action, NodeChange nodeChange, DBNNode node)
    {
        super(source);
        this.action = action;
        this.nodeChange = nodeChange;
        this.node = node;
    }

    public Action getAction()
    {
        return action;
    }

    public NodeChange getNodeChange()
    {
        return nodeChange;
    }

    public DBNNode getNode()
    {
        return node;
    }
}