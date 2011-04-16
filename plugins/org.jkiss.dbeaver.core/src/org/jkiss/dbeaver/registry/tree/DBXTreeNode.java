/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.tree;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.AbstractDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DBXTreeNode
 */
public abstract class DBXTreeNode
{
    static final Log log = LogFactory.getLog(DBXTreeNode.class);

    private final AbstractDescriptor source;
    private final DBXTreeNode parent;
    private List<DBXTreeNode> children;
    private DBXTreeNode recursiveLink;
    private Image defaultIcon;
    private List<DBXTreeIcon> icons;
    private final boolean navigable;
    private final boolean inline;
    private Expression visibleIf;

    public DBXTreeNode(AbstractDescriptor source, DBXTreeNode parent, boolean navigable, boolean inline, String visibleIf)
    {
        this.source = source;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
        this.navigable = navigable;
        this.inline = inline;
        if (!CommonUtils.isEmpty(visibleIf)) {
            try {
                this.visibleIf = RuntimeUtils.parseExpression(visibleIf);
            } catch (DBException e) {
                log.warn(e);
            }
        }
    }

    public AbstractDescriptor getSource()
    {
        return source;
    }

    public abstract String getLabel();
    
    public String getItemLabel()
    {
        return getLabel();
    }

    public boolean isNavigable()
    {
        return navigable;
    }

    public boolean isInline()
    {
        return inline;
    }

    public DBXTreeNode getParent()
    {
        return parent;
    }

    public boolean hasChildren(DBNNode context)
    {
        if (CommonUtils.isEmpty(children)) {
            return false;
        }
        if (context == null) {
            return true;
        }
        for (DBXTreeNode child : children) {
            if (child.isVisible(context)) {
                return true;
            }
        }
        return false;
    }

    public List<DBXTreeNode> getChildren(DBNNode context)
    {
        if (context != null && !CommonUtils.isEmpty(children)) {
            boolean hasExpr = false;
            for (DBXTreeNode child : children) {
                if (child.getVisibleIf() != null) {
                    hasExpr = true;
                    break;
                }
            }
            if (hasExpr) {
                List<DBXTreeNode> filteredChildren = new ArrayList<DBXTreeNode>(children.size());
                for (DBXTreeNode child : children) {
                    if (child.isVisible(context)) {
                        filteredChildren.add(child);
                    }
                }
                return filteredChildren;
            }
        }
        return children;
    }

    private boolean isVisible(DBNNode context)
    {
        return visibleIf == null || Boolean.TRUE.equals(visibleIf.evaluate(makeContext(context)));
    }

    private void addChild(DBXTreeNode child)
    {
        if (this.children == null) {
            this.children = new ArrayList<DBXTreeNode>();
        }
        this.children.add(child);
    }

    public DBXTreeNode getRecursiveLink()
    {
        return recursiveLink;
    }

    public Image getDefaultIcon()
    {
        return defaultIcon;
    }

    public void setDefaultIcon(Image defaultIcon)
    {
        this.defaultIcon = defaultIcon;
    }

    public List<DBXTreeIcon> getIcons()
    {
        return icons;
    }

    public void addIcon(DBXTreeIcon icon)
    {
        if (this.icons == null) {
            this.icons = new ArrayList<DBXTreeIcon>();
        }
        this.icons.add(icon);
    }

    public Image getIcon(DBNNode context)
    {
        List<DBXTreeIcon> extIcons = getIcons();
        if (!CommonUtils.isEmpty(extIcons)) {
            // Try to get some icon depending on it's condition
            for (DBXTreeIcon icon : extIcons) {
                if (icon.getExpression() == null) {
                    continue;
                }
                try {
                    Object result = icon.getExpression().evaluate(makeContext(context));
                    if (Boolean.TRUE.equals(result)) {
                        return icon.getIcon();
                    }
                } catch (JexlException e) {
                    // do nothing
                    log.debug("Error evaluating expression '" + icon.getExprString() + "'", e);
                }
            }
        }
        return getDefaultIcon();
    }

    public Expression getVisibleIf()
    {
        return visibleIf;
    }

    private static JexlContext makeContext(final DBNNode node)
    {
        return new JexlContext() {

            public Object get(String name)
            {
                return node instanceof DBNDatabaseNode && name.equals("object") ? ((DBNDatabaseNode) node).getObject() : null;
            }

            public void set(String name, Object value)
            {
                log.warn("Set is not implemented in DBX model");
            }

            public boolean has(String name)
            {
                return node instanceof DBNDatabaseNode && name.equals("object") && ((DBNDatabaseNode) node).getObject() != null;
            }
        };
    }

}
