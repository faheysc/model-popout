package com.modelpopout;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlayerFrameTraceTest
{
    @Test public void distinguishesNullBlankAlphaAndPlayerSlotFromNpcPixels()
    {
        assertEquals("null",PlayerFrameTrace.pixels(null));
        BufferedImage scene=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
        String blank=PlayerFrameTrace.pixels(scene);
        assertTrue(blank.contains("visible=0 opaque=0 alpha=0"));
        scene.setRGB(600,10,0xffffffff);assertEquals(blank,PlayerFrameTrace.pixels(scene));
        scene.setRGB(10,10,0x80ff0000);
        assertTrue(PlayerFrameTrace.pixels(scene).contains("visible=1 opaque=0 alpha=128"));
        scene.setRGB(10,10,0xffff0000);
        assertTrue(PlayerFrameTrace.pixels(scene).contains("visible=1 opaque=1 alpha=255"));
    }
    @Test public void traceIsBoundedAndDrainingDoesNotRetainRecords()
    {
        PlayerFrameTrace trace=new PlayerFrameTrace();
        for(int i=0;i<5000;i++)trace.record("capture","id="+i);
        String records=trace.drain();
        assertTrue(records.startsWith("dropped=904\n"));
        assertFalse(records.contains("id=0\n"));assertTrue(records.contains("id=4999"));
        assertEquals(4097,records.split("\n").length);
        assertEquals("dropped=0\n",trace.drain());
    }
    @Test public void fixedAndLiveCompletedScenesRemainImmutableWithAndWithoutHud()
    {
        ModelSnapshot mesh=ModelSnapshot.capture(new TextureRenderingTest.TexturedModel(),null);
        ModelFrame frame=ModelFrame.initial(mesh);CameraView camera=new CameraView(0,0,1);
        ModelRenderer renderer=new ModelRenderer();
        BufferedImage fixed=renderer.render(mesh,camera,frame,TextureSet.EMPTY,0);
        String fixedPixels=PlayerFrameTrace.pixels(fixed);
        assertFalse(fixedPixels.contains("visible=0 "));
        for(boolean frozen:new boolean[]{false,true})for(boolean npc:new boolean[]{false,true})for(boolean hud:new boolean[]{false,true})for(int cycle=100;cycle<110;cycle++)
        {
            TextureRenderingTest.TexturedModel animated=new TextureRenderingTest.TexturedModel();
            animated.y[0] += cycle-100;
            BufferedImage source=frozen?fixed:renderer.render(ModelSnapshot.capture(animated,null),camera,frame,TextureSet.EMPTY,0);
            BufferedImage scene=new BufferedImage(npc?1024:512,512,BufferedImage.TYPE_INT_ARGB);
            Graphics2D compose=scene.createGraphics();
            try{compose.drawImage(source,0,0,null);if(npc)compose.drawImage(source,512,0,null);}finally{compose.dispose();}
            String before=PlayerFrameTrace.pixels(scene);
            BufferedImage destination=new BufferedImage(scene.getWidth(),512,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g=destination.createGraphics();
            try
            {
                ModelScenePainter.paint(g,scene,npc,true,destination.getWidth(),512);
                if(hud)
                {
                    HudSnapshot packet=new HudSnapshot(1,cycle,8,
                        new HudSnapshot.ActorHud(7,HudHealth.scaled(cycle-100,30),java.util.List.of()),
                        new HudSnapshot.ActorHud(8,HudHealth.scaled(15,30),java.util.List.of()),HudAssets.HealthSkin.FALLBACK);
                    HudAnchor anchor=HudAnchor.initial(mesh,frame);
                    HudPainter.paint(g,packet,anchor,anchor,camera,destination.getWidth(),512,npc,true,7,8,true,true);
                }
            }finally{g.dispose();}
            assertEquals(before,PlayerFrameTrace.pixels(scene));assertEquals(fixedPixels,PlayerFrameTrace.pixels(fixed));
            for(int y=0;y<512;y++)for(int x=0;x<512;x++)
                assertTrue("HUD cannot lower player alpha",(destination.getRGB(x,y)>>>24)>=(scene.getRGB(x,y)>>>24));
            // A camera-only render also must not modify the previously published cached image.
            renderer.render(mesh,new CameraView(0,.01*(cycle-100),1),frame,TextureSet.EMPTY,0);
            assertEquals(fixedPixels,PlayerFrameTrace.pixels(fixed));
        }
    }
}
