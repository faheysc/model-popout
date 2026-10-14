package com.modelpopout;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudPainterTest
{
    private final ModelSnapshot mesh=ModelSnapshot.capture(new TextureRenderingTest.TexturedModel(),null);
    private final ModelFrame frame=ModelFrame.initial(mesh);
    private final HudAnchor anchor=HudAnchor.initial(mesh,frame);
    private final CameraView camera=new CameraView(0,0,1);
    private HudSnapshot packet(int cycle)
    {
        HitsplatStyle style=HitsplatStyle.decode(new byte[]{9,0,50,0});
        BufferedImage hit=HudAssetsTest.solid(8,8,0xff123456);hit.setRGB(0,0,0xffabcdef);
        return new HudSnapshot(1,cycle,8,
            new HudSnapshot.ActorHud(7,HudHealth.scaled(15,30),List.of(new HudSnapshot.Hit(16,10,150,0,style,hit))),
            new HudSnapshot.ActorHud(8,HudHealth.scaled(0,30),List.of(new HudSnapshot.Hit(12,0,150,0,style,hit))),
            HudAssets.HealthSkin.FALLBACK);
    }
    private BufferedImage paint(HudSnapshot packet,int width,int height,boolean mirror,long playerKey,long npcKey,boolean hits,boolean bars)
    {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        try { HudPainter.paint(g,packet,anchor,anchor,camera,width,height,true,mirror,playerKey,npcKey,hits,bars); }
        finally {g.dispose();}return image;
    }
    @Test public void identityFlagsAndUnknownHealthDoNotLeakHudOntoOtherModels()
    {
        assertEquals(0,count(paint(packet(100),1024,512,false,99,99,true,true)));
        BufferedImage image=paint(packet(100),1024,512,false,7,8,false,true);
        Point player=anchor.screen(camera,1024,512,true,0,false),npc=anchor.screen(camera,1024,512,true,1,false);
        assertEquals(0xff00ff00,image.getRGB(player.x-14,player.y-3));
        assertEquals(0xffff0000,image.getRGB(player.x+14,player.y-3));
        assertEquals(0xffff0000,image.getRGB(npc.x,npc.y-3));
        assertEquals(0,count(paint(packet(100),1024,512,false,7,8,false,false)));
        HudSnapshot missing=new HudSnapshot(1,100,8,new HudSnapshot.ActorHud(7,HudHealth.UNKNOWN,List.of()),
            new HudSnapshot.ActorHud(8,HudHealth.UNKNOWN,List.of()),HudAssets.HealthSkin.FALLBACK);
        assertEquals(0,count(paint(missing,1024,512,false,7,8,true,true)));
    }
    @Test public void npcMirroringDoesNotMirrorTextAndExpiredHitsLeaveNoPixels()
    {
        BufferedImage normal=paint(packet(100),1024,512,false,7,8,true,false);
        BufferedImage mirrored=paint(packet(100),1024,512,true,7,8,true,false);
        assertArrayEquals(normal.getRGB(0,0,1024,512,null,0,1024),mirrored.getRGB(0,0,1024,512,null,0,1024));
        Point npc=anchor.screen(camera,1024,512,true,1,true);
        assertEquals(0xffabcdef,mirrored.getRGB(npc.x-4,npc.y-20));
        assertEquals(0,count(paint(packet(150),1024,512,true,7,8,true,false)));
    }
    @Test public void anchorsTrackCompletedCameraAndViewportWithoutPoseBoundsJitter()
    {
        Point initial=anchor.screen(camera,1024,512,true,0,false);
        assertEquals(256,initial.x);
        Point bigger=anchor.screen(camera,2048,1024,true,0,false);
        assertEquals(initial.x*2,bigger.x);assertTrue(Math.abs(initial.y*2-bigger.y)<=1);
        Point zoomed=anchor.screen(new CameraView(0,0,2),1024,512,true,0,false);
        assertTrue(zoomed.y<initial.y);
        Point pitched=anchor.screen(new CameraView(Math.PI/2,.8,1),1024,512,true,0,false);
        assertTrue(pitched.y>initial.y);
        TextureRenderingTest.TexturedModel moving=new TextureRenderingTest.TexturedModel();moving.y[0]=-800;
        LiveViewState state=new LiveViewState();state.updateModel(mesh,false);LiveViewState.RenderRequest first=state.begin();state.finish(first);
        HudAnchor stable=HudAnchor.initial(first.model,first.framing);Point before=stable.screen(camera,1024,512,true,0,false);
        state.updateModel(ModelSnapshot.capture(moving,null),false);LiveViewState.RenderRequest next=state.begin();
        assertSame(first.framing,next.framing);assertEquals(before,stable.screen(camera,1024,512,true,0,false));
        for(int width:new int[]{480,640,1024}) for(double pitch:new double[]{CameraView.MIN_PITCH,0,CameraView.MAX_PITCH})
        {
            Point p=stable.screen(new CameraView(2,pitch,3),width,300,true,1,true);
            assertTrue(p.x>=48 && p.x<=width-48);assertTrue(p.y>=64 && p.y<=292);
        }
    }
    @Test public void nativeHealthSpritesClipTheForegroundAtScaledRatio()
    {
        HudAssets.HealthSkin skin=new HudAssets.HealthSkin(HudAssetsTest.solid(30,5,0xff112233),HudAssetsTest.solid(30,5,0xff445566));
        HudSnapshot packet=new HudSnapshot(1,100,-1,new HudSnapshot.ActorHud(7,HudHealth.scaled(15,30),List.of()),
            new HudSnapshot.ActorHud(-1,HudHealth.UNKNOWN,List.of()),skin);
        BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        try {HudPainter.paint(g,packet,anchor,null,camera,512,512,false,false,7,-1,false,true);}finally{g.dispose();}
        Point p=anchor.screen(camera,512,512,false,0,false);
        assertEquals(0xff112233,image.getRGB(p.x-1,p.y-3));assertEquals(0xff445566,image.getRGB(p.x,p.y-3));
    }
    @Test public void hudOpacityAndExpirationUseExistingBufferedPresentationWithoutMutatingModels() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage completed=HudAssetsTest.solid(3,3,0xffaa5500);int[] original=completed.getRGB(0,0,3,3,null,0,3);
            HudSnapshot[] next={packet(100)};ViewerWindow.Surface surface=new ViewerWindow.Surface();surface.setSize(1024,512);
            surface.modelPainter=g -> {g.drawImage(completed,500,400,null);HudPainter.paint(g,next[0],anchor,anchor,camera,1024,512,true,true,7,8,true,true);};
            WindowPresentation presentation=new WindowPresentation();BufferedImage screen=new BufferedImage(1024,512,BufferedImage.TYPE_INT_ARGB);
            Graphics2D target=screen.createGraphics();Point p=anchor.screen(camera,1024,512,true,0,false);
            try
            {
                for(int opacity:new int[]{0,50,100})
                {
                    surface.opacity=opacity;next[0]=packet(100);presentation.paint(target,1024,512,surface::paint);
                    assertEquals(0xff123456,screen.getRGB(p.x,p.y-16));assertEquals(255,screen.getRGB(p.x-14,p.y-3)>>>24);
                    assertEquals(0xffaa5500,screen.getRGB(500,400));
                    int previous=screen.getRGB(p.x,p.y-16);next[0]=packet(150);
                    presentation.paint(target,1024,512,g -> {surface.paint(g);assertEquals(previous,screen.getRGB(p.x,p.y-16));});
                    assertNotEquals(0xff123456,screen.getRGB(p.x,p.y-16));assertEquals(0xffaa5500,screen.getRGB(500,400));
                }
                assertArrayEquals(original,completed.getRGB(0,0,3,3,null,0,3));
            }
            finally {target.dispose();}
        });
    }
    private static int count(BufferedImage image)
    {int count=0;for(int value:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())) if((value>>>24)>0)count++;return count;}
}
