package com.modelpopout;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelViewportTest
{
    @Test public void leftDragReleaseOtherButtonsAndPreciseWheelRetainExistingCameraSemantics() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            LiveViewState state=new LiveViewState();AtomicInteger renders=new AtomicInteger();
            ModelViewport view=new ModelViewport(state,renders::incrementAndGet) {};
            view.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            mouse(view,MouseEvent.MOUSE_DRAGGED,20,20,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.NOBUTTON);
            mouse(view,MouseEvent.MOUSE_PRESSED,10,10,InputEvent.BUTTON3_DOWN_MASK,MouseEvent.BUTTON3);
            mouse(view,MouseEvent.MOUSE_DRAGGED,30,30,InputEvent.BUTTON3_DOWN_MASK,MouseEvent.NOBUTTON);
            assertEquals(0,renders.get());
            mouse(view,MouseEvent.MOUSE_PRESSED,10,10,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.BUTTON1);
            mouse(view,MouseEvent.MOUSE_DRAGGED,30,25,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.NOBUTTON);
            CameraView expected=CameraView.DEFAULT.drag(20,15);assertCamera(expected,state.camera());
            mouse(view,MouseEvent.MOUSE_RELEASED,30,25,0,MouseEvent.BUTTON3);
            mouse(view,MouseEvent.MOUSE_DRAGGED,35,30,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.NOBUTTON);
            expected=expected.drag(5,5);assertCamera(expected,state.camera());
            mouse(view,MouseEvent.MOUSE_DRAGGED,100,100,0,MouseEvent.NOBUTTON);
            assertEquals(2,renders.get());
            mouse(view,MouseEvent.MOUSE_RELEASED,35,30,0,MouseEvent.BUTTON1);
            mouse(view,MouseEvent.MOUSE_DRAGGED,50,50,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.NOBUTTON);
            assertEquals(2,renders.get());
            view.dispatchEvent(new MouseWheelEvent(view,MouseEvent.MOUSE_WHEEL,0,0,20,20,20,20,0,false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,1,-1,-.5));
            assertCamera(expected.wheel(-.5),state.camera());assertEquals(3,renders.get());
            assertEquals(Cursor.CROSSHAIR_CURSOR,view.getCursor().getType());
            assertFalse(view.isOpaque());assertFalse(view.isDoubleBuffered());
        });
    }

    @Test public void realMainAndInspectionPanelsUseIndependentCameraState() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            ModelPopoutPlugin plugin=new ModelPopoutPlugin();
            ModelPopoutPlugin.ModelPanel main=plugin.new ModelPanel(new ModelPopoutConfig() {});
            LiveViewState inspectorState=new LiveViewState();
            InspectionPanel inspector=new InspectionPanel(new ModelPopoutConfig() {},inspectorState,() -> {});
            try
            {
                LiveViewState mainState=ConfigTestSupport.get(plugin,"viewState");
                mouse(main,MouseEvent.MOUSE_PRESSED,10,10,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.BUTTON1);
                mouse(main,MouseEvent.MOUSE_DRAGGED,40,25,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.NOBUTTON);
                assertCamera(CameraView.DEFAULT.drag(30,15),mainState.camera());assertCamera(CameraView.DEFAULT,inspectorState.camera());
                mouse(inspector,MouseEvent.MOUSE_PRESSED,5,5,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.BUTTON1);
                mouse(inspector,MouseEvent.MOUSE_DRAGGED,-5,-5,InputEvent.BUTTON1_DOWN_MASK,MouseEvent.NOBUTTON);
                assertCamera(CameraView.DEFAULT.drag(-10,-10),inspectorState.camera());
                assertCamera(CameraView.DEFAULT.drag(30,15),mainState.camera());
            }
            catch (Exception failure) { throw new AssertionError(failure); }
        });
    }

    @Test public void mainAndInspectionRepaintsInvalidateOnlyTheirOwnCompositingBoundary() throws Exception
    {
        SwingUtilities.invokeAndWait(() -> {
            javax.swing.RepaintManager previous=javax.swing.RepaintManager.currentManager((javax.swing.JComponent)null);
            java.util.List<javax.swing.JComponent> dirty=new java.util.ArrayList<>();
            javax.swing.RepaintManager.setCurrentManager(new javax.swing.RepaintManager()
            {
                @Override public void addDirtyRegion(javax.swing.JComponent component,int x,int y,int width,int height)
                { if(width>0 && height>0) dirty.add(component); }
            });
            try
            {
                ModelPopoutPlugin plugin=new ModelPopoutPlugin();ModelPopoutConfig config=new ModelPopoutConfig() {};
                ModelViewport[] panels={plugin.new ModelPanel(config),new InspectionPanel(config,new LiveViewState(),() -> {})};
                for(ModelViewport panel:panels)
                {
                    ViewerWindow.Surface surface=new ViewerWindow.Surface();surface.setSize(600,600);
                    surface.add(panel);panel.setSize(512,512);dirty.clear();
                    panel.repaint(0,5,7,10,12);
                    assertEquals(java.util.Collections.singletonList(surface),dirty);
                    assertFalse(panel.isOpaque());assertFalse(panel.isDoubleBuffered());
                }
            }
            finally { javax.swing.RepaintManager.setCurrentManager(previous); }
        });
    }

    private static void mouse(ModelViewport view,int id,int x,int y,int modifiers,int button)
    { view.dispatchEvent(new MouseEvent(view,id,0,modifiers,x,y,x,y,1,false,button)); }

    private static void assertCamera(CameraView expected,CameraView actual)
    { assertEquals(expected.yaw,actual.yaw,1e-12);assertEquals(expected.pitch,actual.pitch,1e-12);assertEquals(expected.zoom,actual.zoom,1e-12); }
}
