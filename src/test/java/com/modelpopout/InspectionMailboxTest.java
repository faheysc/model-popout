package com.modelpopout;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class InspectionMailboxTest
{
    private InspectionCapture.Packet packet(int cycle)
    { return new InspectionCapture.Packet(null,TextureSet.EMPTY,null,cycle,0,true); }

    @Test public void slowEdtRetainsOnlyTheLatestPacketAndClosingFencesQueuedAndNewOffers()
    {
        Queue<Runnable> edt=new ArrayDeque<>();AtomicInteger delivered=new AtomicInteger(-1);
        InspectionMailbox mailbox=new InspectionMailbox(edt::add,value -> delivered.set(value.cycle));
        for (int i=0;i<10000;i++) mailbox.offer(packet(i));
        assertEquals(1,edt.size());edt.remove().run();assertEquals(9999,delivered.get());assertTrue(edt.isEmpty());
        mailbox.offer(packet(10000));mailbox.close();mailbox.offer(packet(10001));
        edt.remove().run();assertEquals(9999,delivered.get());assertTrue(edt.isEmpty());
    }

    @Test public void racingOfferDuringDeliverySchedulesOnlyOneFollowup()
    {
        Queue<Runnable> edt=new ArrayDeque<>();AtomicInteger delivered=new AtomicInteger();InspectionMailbox[] holder=new InspectionMailbox[1];
        holder[0]=new InspectionMailbox(edt::add,value -> {
            delivered.incrementAndGet();if (value.cycle==1) { holder[0].offer(packet(2));holder[0].offer(packet(3)); }
        });
        holder[0].offer(packet(1));edt.remove().run();assertEquals(1,edt.size());edt.remove().run();
        assertEquals(2,delivered.get());assertTrue(edt.isEmpty());
    }
}
