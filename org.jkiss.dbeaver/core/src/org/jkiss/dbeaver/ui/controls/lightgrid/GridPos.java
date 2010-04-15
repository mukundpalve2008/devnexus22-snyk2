/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

/**
 * GridPos
 */
public class GridPos
{
    public final int col;
    public final int row;

    public GridPos(int col, int row)
    {
        this.col = col;
        this.row = row;
    }

    public boolean equals(Object object)
    {
        return object instanceof GridPos && equalsTo((GridPos) object);
    }

    public boolean equalsTo(GridPos pos)
    {
        return this.col == pos.col && this.row == pos.row;
    }

    public String toString()
    {
        return col + ":" + row;
    }

    public int hashCode()
    {
        return col ^ row;
    }
}
