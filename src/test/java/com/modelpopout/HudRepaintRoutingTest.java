package com.modelpopout;

import java.awt.Container;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercises the real ModelPanel repaint entry point, not a manually selected surface painter. */
public class HudRepaintRoutingTest
{
    @Test public void hudDirtyRequestsUseTheWholeCompositorRatherThanTheChildRectangle() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            RepaintManager previous=RepaintManager.currentManager((JComponent)null);
            Requests requests=new Requests();RepaintManager.setCurrentManager(requests);
            Fixture f=new Fixture();
            try
            {
                for(int[] size:new int[][]{{480,300},{1024,512},{640,480}})
                    for(int opacity:new int[]{0,30,50,100})
                    {
                        f.resize(size[0],size[1]);f.surface.opacity=opacity;requests.dirty.clear();
                        f.model.repaint();
                        assertEquals("HUD must invalidate the same component as completed model frames",1,requests.dirty.size());
                        assertTrue("Child-only dirty regions reintroduce a separate native presentation path",requests.dirty.containsKey(f.surface));
                        Rectangle region=requests.dirty.get(f.surface);
                        assertEquals(new Rectangle(0,0,size[0],size[1]),region);
                        f.nativeParent.last=null;f.surface.paintImmediately(region);
                        assertEquals("Native update must include the entire composed surface",region,f.nativeParent.last);
                    }
            }
            finally {f.root.removeNotify();RepaintManager.setCurrentManager(previous);}
        });
    }

    @Test public void interleavedHudAndCompletedModelRequestsShareOneDirtySource() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            RepaintManager previous=RepaintManager.currentManager((JComponent)null);
            Requests requests=new Requests();RepaintManager.setCurrentManager(requests);
            Fixture f=new Fixture();
            try
            {
                f.resize(1024,512);requests.dirty.clear();
                for(int i=0;i<1000;i++)
                {
                    f.model.repaint(5,7,13,17); // HUD/config/expiration invalidation.
                    f.surface.repaint();       // Existing completed-model publication path.
                }
                assertEquals("One component can coalesce both update sources",1,requests.dirty.size());
                assertEquals(new Rectangle(0,0,1024,512),requests.dirty.get(f.surface));
                assertFalse(requests.dirty.containsKey(f.model));
            }
            finally {f.root.removeNotify();RepaintManager.setCurrentManager(previous);}
        });
    }

    @Test public void reopenedViewUsesItsOwnSurfaceAndDetachedPanelsKeepNormalSwingBehavior() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            RepaintManager previous=RepaintManager.currentManager((JComponent)null);
            Requests requests=new Requests();RepaintManager.setCurrentManager(requests);
            Fixture oldView=new Fixture(),reopened=new Fixture();
            try
            {
                requests.dirty.clear();oldView.model.repaint();
                assertTrue(requests.dirty.containsKey(oldView.surface));assertFalse(requests.dirty.containsKey(reopened.surface));
                requests.dirty.clear();reopened.model.repaint();
                assertTrue(requests.dirty.containsKey(reopened.surface));assertFalse(requests.dirty.containsKey(oldView.surface));
                oldView.surface.remove(oldView.model);requests.dirty.clear();oldView.model.repaint();
                assertTrue(requests.dirty.containsKey(oldView.model));assertFalse(requests.dirty.containsKey(oldView.surface));
                javax.swing.JPanel otherParent=new javax.swing.JPanel();otherParent.add(oldView.model);
                requests.dirty.clear();oldView.model.repaint();
                assertTrue(requests.dirty.containsKey(oldView.model));assertFalse(requests.dirty.containsKey(otherParent));
            }
            finally {oldView.root.removeNotify();reopened.root.removeNotify();RepaintManager.setCurrentManager(previous);}
        });
    }

    @Test public void opaqueNativeFallbackMustNotStopSwingAtTheSurface() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            Fixture f=new Fixture();
            try
            {
                f.resize(1024,512);
                // Reproduce Window.setBackground's alpha-to-255 setLayersOpaque side effect.
                f.root.setOpaque(true);f.root.getLayeredPane().setOpaque(true);f.surface.setOpaque(true);
                f.nativeParent.last=null;f.surface.paintImmediately(0,0,1024,512);
                assertNull("Opaque Swing ancestor bypasses the native Window.paint path",f.nativeParent.last);
                ViewerWindow.keepPresentationNonOpaque(f.root);
                assertFalse(f.root.isOpaque());assertFalse(f.root.getLayeredPane().isOpaque());assertFalse(f.surface.isOpaque());
                f.surface.paintImmediately(0,0,1024,512);
                assertEquals(new Rectangle(0,0,1024,512),f.nativeParent.last);
                // Model and HUD invalidations, root updates and resized fallback remain routed.
                for(int width:new int[]{480,1024,640})
                {
                    f.resize(width,300);
                    for(JComponent component:new JComponent[]{f.model,f.surface,f.root,f.root.getLayeredPane()})
                    {
                        f.nativeParent.last=null;component.paintImmediately(0,0,component.getWidth(),component.getHeight());
                        assertNotNull("Every Swing origin must ascend to the native presentation",f.nativeParent.last);
                    }
                }
            }
            finally {f.root.removeNotify();}
        });
    }

    private static final class Fixture
    {
        final NativeParent nativeParent=new NativeParent();
        final JRootPane root=new JRootPane();
        final ViewerWindow.Surface surface=new ViewerWindow.Surface();
        final ModelPopoutPlugin.ModelPanel model=new ModelPopoutPlugin().new ModelPanel(new ModelPopoutConfig() {});
        Fixture()
        {
            root.setOpaque(false);root.getLayeredPane().setOpaque(false);
            root.setContentPane(surface);surface.setBorder(javax.swing.BorderFactory.createEmptyBorder(6,6,6,6));
            surface.add(model);nativeParent.add(root);resize(480,300);root.addNotify();
        }
        void resize(int width,int height)
        {
            nativeParent.setSize(width,height);root.setSize(width,height);root.doLayout();
            surface.setSize(width,height);surface.doLayout();
        }
    }
    private static final class NativeParent extends Container
    {
        Rectangle last;
        @Override public boolean isShowing(){return true;}
        @Override public void repaint(long delay,int x,int y,int width,int height){last=new Rectangle(x,y,width,height);}
    }
    private static final class Requests extends RepaintManager
    {
        // Capture Swing's submitted target/rectangle before the native-window visibility filter.
        // A real Window cannot be constructed headlessly. paintImmediately above uses JDK routing.
        final Map<JComponent,Rectangle> dirty=new LinkedHashMap<>();
        @Override public void addDirtyRegion(JComponent component,int x,int y,int width,int height)
        {
            if(width<=0 || height<=0)return;
            Rectangle next=new Rectangle(x,y,width,height);
            dirty.merge(component,next,(old,value) -> old.union(value));
        }
    }
}
