package com.modelpopout;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowStackingTest
{
    @Test public void changesApplyImmediatelyToOnlyTheSelectedWindow()
    {
        AtomicBoolean viewer=new AtomicBoolean(),client=new AtomicBoolean();
        assertTrue(WindowStacking.apply(true,() -> true,viewer::set));
        assertTrue(viewer.get());assertFalse(client.get());
        assertTrue(WindowStacking.apply(false,() -> true,viewer::set));
        assertFalse(viewer.get());assertFalse(client.get());
    }

    @Test public void savedPreferenceIsReappliedWhenAWindowIsRecreated()
    {
        AtomicBoolean saved=new AtomicBoolean(true);
        for(int opening=0;opening<3;opening++)
        {
            AtomicBoolean viewer=new AtomicBoolean();
            assertTrue(WindowStacking.apply(saved.get(),() -> true,viewer::set));
            assertEquals(saved.get(),viewer.get());
            if(opening==1) saved.set(false);
        }
    }

    @Test public void unsupportedDesktopRejectsEnablingWithoutCallingTheSetter()
    {
        AtomicInteger calls=new AtomicInteger();
        assertFalse(WindowStacking.apply(true,() -> false,value -> calls.incrementAndGet()));
        assertEquals(0,calls.get());
        assertTrue(WindowStacking.apply(false,() -> false,value -> calls.incrementAndGet()));
        assertEquals(1,calls.get()); // Disabling still attempts to restore normal stacking.
    }

    @Test public void capabilityOrSetterFailureDoesNotEscapeIntoConfigurationHandling()
    {
        assertFalse(WindowStacking.apply(true,() -> {throw new SecurityException();},value -> fail()));
        assertFalse(WindowStacking.apply(true,() -> true,value -> {throw new UnsupportedOperationException();}));
        assertFalse(WindowStacking.apply(false,() -> true,value -> {throw new SecurityException();}));
    }
}
