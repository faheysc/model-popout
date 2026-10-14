package com.modelpopout;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Temporary, opt-in investigation. Bounded records; no images or client objects retained. */
final class PlayerFrameTrace
{
    private final ArrayDeque<String> records = new ArrayDeque<>();
    private long sequence, dropped;

    synchronized void record(String stage, String detail)
    {
        if (records.size() == 4096) { records.removeFirst(); dropped++; }
        records.addLast(++sequence + " " + System.nanoTime() + " " + Thread.currentThread().getName()
            + " " + stage + " " + detail);
    }

    synchronized String drain()
    {
        String result = "dropped=" + dropped + "\n" + String.join("\n",records);
        records.clear(); dropped = 0;
        return result;
    }

    /** Player slot only: distinguishes blank frames, changed alpha and changed colors. */
    static String pixels(BufferedImage image)
    {
        if (image == null) return "null";
        int width = Math.min(ModelRenderer.SIZE,image.getWidth());
        int height = Math.min(ModelRenderer.SIZE,image.getHeight());
        long alpha = 0, hash = 1;
        int visible = 0, opaque = 0;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++)
        {
            int pixel = image.getRGB(x,y), a = pixel >>> 24;
            alpha += a;
            if (a != 0) visible++;
            if (a == 255) opaque++;
            hash = hash * 31 + pixel;
        }
        return "image=" + System.identityHashCode(image) + " size=" + image.getWidth() + "x" + image.getHeight()
            + " visible=" + visible + " opaque=" + opaque + " alpha=" + alpha + " hash=" + Long.toHexString(hash);
    }
}
