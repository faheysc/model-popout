package com.modelpopout;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelScenePainterTest
{
    @Test public void npcFacesTowardPlayerAcrossCameraSidesWithoutChangingPlayerView()
    {
        assertFalse(ModelScenePainter.shouldMirrorNpc(CameraView.DEFAULT));
        assertFalse(ModelScenePainter.shouldMirrorNpc(new CameraView(Math.PI/2,0,1))); // already faces left
        assertFalse(ModelScenePainter.shouldMirrorNpc(new CameraView(Math.PI,0,1)));
        assertTrue(ModelScenePainter.shouldMirrorNpc(new CameraView(3*Math.PI/2,0,1))); // otherwise faces right
        assertTrue(ModelScenePainter.shouldMirrorNpc(new CameraView(-Math.PI/2,.5,2)));
    }

    @Test public void mirrorReflectsOnlyNpcPixelsPreservingIntrinsicAlphaAndSourceAcrossAnimationFrames()
    {
        for (int pose = 0; pose < 3; pose++)
        {
            BufferedImage scene = new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
            scene.setRGB(100+pose,250,0xffff0000);
            scene.setRGB(512+70+pose,250,0xff000000);
            scene.setRGB(512+120+pose,250,0x8000ff00);
            int[] source = scene.getRGB(0,0,1024,512,null,0,1024);
            BufferedImage mirrored = paint(scene,true,true,1024,512);
            BufferedImage original = paint(scene,true,false,1024,512);
            for (int x = 0; x < 512; x++)
            {
                assertEquals(scene.getRGB(x,250),mirrored.getRGB(x,250));
                assertEquals(original.getRGB(512+x,250),mirrored.getRGB(1023-x,250));
            }
            assertEquals(255,mirrored.getRGB(1023-70-pose,250) >>> 24);
            assertEquals(128,mirrored.getRGB(1023-120-pose,250) >>> 24);
            assertArrayEquals(source,scene.getRGB(0,0,1024,512,null,0,1024));
        }
    }

    @Test public void mirrorRespectsCenteredSlotBoundsDuringResizeAndDoesNotAffectPlayerOnlyScene()
    {
        BufferedImage scene = new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
        scene.setRGB(0,0,0xffff0000);
        scene.setRGB(512,0,0xff000000);
        BufferedImage resized = paint(scene,true,true,1024,600);
        assertEquals(0xffff0000,resized.getRGB(0,44));
        assertEquals(0xff000000,resized.getRGB(1023,44));
        assertEquals(0,resized.getRGB(0,43));
        assertArrayEquals(paint(scene,false,false,512,512).getRGB(0,0,512,512,null,0,512),
            paint(scene,false,true,512,512).getRGB(0,0,512,512,null,0,512));
    }

    static BufferedImage paint(BufferedImage scene, boolean npc, boolean mirror, int width, int height)
    {
        BufferedImage target = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = target.createGraphics();
        try { ModelScenePainter.paint(g,scene,npc,mirror,width,height); }
        finally { g.dispose(); }
        return target;
    }
}
