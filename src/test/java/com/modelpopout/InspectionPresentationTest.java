package com.modelpopout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class InspectionPresentationTest
{
    @Test public void inspectorHudAndCameraRepaintsUseOnlyTheirOwnWholeSurface() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            RepaintManager previous=RepaintManager.currentManager((JComponent)null);
            Map<JComponent,Integer> requests=new HashMap<>();
            RepaintManager.setCurrentManager(new RepaintManager()
            { @Override public void addDirtyRegion(JComponent component,int x,int y,int width,int height)
              { if (width>0 && height>0) requests.merge(component,1,Integer::sum); } });
            try
            {
                ModelPopoutConfig config=new ModelPopoutConfig() {};
                ViewerWindow.Surface first=new ViewerWindow.Surface(),second=new ViewerWindow.Surface();
                first.setSize(512,512);second.setSize(512,512);
                InspectionPanel a=new InspectionPanel(config,new LiveViewState(),() -> {});
                InspectionPanel b=new InspectionPanel(config,new LiveViewState(),() -> {});
                first.add(a);second.add(b);a.setSize(500,500);b.setSize(500,500);requests.clear();
                a.repaint(0,5,7,10,12);assertEquals(1,requests.size());assertTrue(requests.containsKey(first));
                requests.clear();b.repaint();assertEquals(1,requests.size());assertTrue(requests.containsKey(second));
                first.remove(a);requests.clear();a.repaint();assertTrue(requests.containsKey(a));
            }
            finally { RepaintManager.setCurrentManager(previous); }
        });
    }

    @Test public void repeatedInspectorCompositingPreservesModelAlphaAcrossOpacityAndResize() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ModelPopoutConfig config=new ModelPopoutConfig() {};
            InspectionPanel panel=new InspectionPanel(config,new LiveViewState(),() -> {});
            panel.status=null;panel.image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);
            Graphics2D fill=panel.image.createGraphics();fill.setColor(Color.RED);fill.fillRect(192,192,128,128);fill.dispose();
            ViewerWindow.Surface surface=new ViewerWindow.Surface();surface.add(panel);
            surface.modelPainter=g -> panel.paintModels(g);
            for (int size:new int[]{512,640}) for (int opacity:new int[]{100,50,0,100,0})
            {
                surface.setSize(size,size);panel.setSize(size,size);surface.opacity=opacity;
                BufferedImage frame=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);
                for (int repeat=0;repeat<20;repeat++)
                {
                    Graphics2D output=frame.createGraphics();surface.paint(output);output.dispose();
                    assertEquals("Background opacity must not fade the model",0xffff0000,frame.getRGB(size/2,size/2));
                    assertEquals(BackgroundOpacity.alpha(opacity),frame.getRGB(2,2)>>>24);
                }
            }
        });
    }
}
