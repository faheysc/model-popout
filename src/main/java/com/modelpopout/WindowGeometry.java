package com.modelpopout;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;

/** Geometry for invisible native-style resize edges. */
final class WindowGeometry
{
    static final int NORTH = 1, EAST = 2, SOUTH = 4, WEST = 8;
    static final int BORDER = 6;

    // All coordinates are AWT user-space units, including negative monitor origins.
    static Rectangle move(Rectangle start, java.awt.Point pressed, java.awt.Point current)
    {
        return new Rectangle(start.x+current.x-pressed.x,start.y+current.y-pressed.y,start.width,start.height);
    }

    static boolean titleReachable(Rectangle window, Rectangle monitor)
    {
        Rectangle title = new Rectangle(window.x,window.y,window.width,32);
        Rectangle intersection = title.intersection(monitor);
        return intersection.width >= Math.min(window.width,160) && intersection.height >= 26;
    }

    static Rectangle usable(Rectangle monitor, java.awt.Insets insets)
    {
        return new Rectangle(monitor.x+insets.left,monitor.y+insets.top,
            Math.max(1,monitor.width-insets.left-insets.right),
            Math.max(1,monitor.height-insets.top-insets.bottom));
    }

    static int edges(int x, int y, int width, int height)
    {
        int edges = 0;
        if (x < BORDER) edges |= WEST;
        else if (x >= width-BORDER) edges |= EAST;
        if (y < BORDER) edges |= NORTH;
        else if (y >= height-BORDER) edges |= SOUTH;
        return edges;
    }

    static int cursor(int edges)
    {
        switch (edges)
        {
            case NORTH: return Cursor.N_RESIZE_CURSOR;
            case EAST: return Cursor.E_RESIZE_CURSOR;
            case SOUTH: return Cursor.S_RESIZE_CURSOR;
            case WEST: return Cursor.W_RESIZE_CURSOR;
            case NORTH | EAST: return Cursor.NE_RESIZE_CURSOR;
            case NORTH | WEST: return Cursor.NW_RESIZE_CURSOR;
            case SOUTH | EAST: return Cursor.SE_RESIZE_CURSOR;
            case SOUTH | WEST: return Cursor.SW_RESIZE_CURSOR;
            default: return Cursor.DEFAULT_CURSOR;
        }
    }

    static Rectangle resize(Rectangle start, int edges, int dx, int dy, Dimension minimum)
    {
        Rectangle next = new Rectangle(start);
        if ((edges & EAST) != 0) next.width = Math.max(minimum.width,start.width+dx);
        if ((edges & SOUTH) != 0) next.height = Math.max(minimum.height,start.height+dy);
        if ((edges & WEST) != 0)
        {
            next.width = Math.max(minimum.width,start.width-dx);
            next.x = start.x+start.width-next.width;
        }
        if ((edges & NORTH) != 0)
        {
            next.height = Math.max(minimum.height,start.height-dy);
            next.y = start.y+start.height-next.height;
        }
        return next;
    }
}
