package com.modelpopout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LatestHudMailboxTest
{
    static HudSnapshot packet(int cycle)
    { return new HudSnapshot(1,cycle,-1,new HudSnapshot.ActorHud(1,HudHealth.UNKNOWN,List.of()),
        new HudSnapshot.ActorHud(-1,HudHealth.UNKNOWN,List.of()),HudAssets.HealthSkin.FALLBACK); }
    @Test public void aThousandUpdatesQueueOneDeliveryOfLatestCompletedPacket()
    {
        ArrayDeque<Runnable> queue=new ArrayDeque<>();List<HudSnapshot> received=new ArrayList<>();
        LatestHudMailbox box=new LatestHudMailbox(queue::add,received::add);
        for(int i=0;i<1000;i++) box.offer(packet(i));
        assertEquals(1,queue.size());queue.remove().run();assertEquals(1,received.size());
        assertEquals(999,received.get(0).cycle);assertTrue(queue.isEmpty());
    }
    @Test public void racingOfferDuringDeliverySchedulesOneFollowUpWithoutLosingLatest()
    {
        ArrayDeque<Runnable> queue=new ArrayDeque<>();List<HudSnapshot> received=new ArrayList<>();
        LatestHudMailbox[] box=new LatestHudMailbox[1];
        box[0]=new LatestHudMailbox(queue::add,next -> {
            received.add(next);if(next.cycle==1) {box[0].offer(packet(2));box[0].offer(packet(3));assertTrue(queue.isEmpty());}
        });
        box[0].offer(packet(1));queue.remove().run();assertEquals(1,queue.size());queue.remove().run();
        assertEquals(2,received.size());assertEquals(3,received.get(1).cycle);assertTrue(queue.isEmpty());
    }
    @Test public void closeDropsPendingDataAndReopeningUsesSameBoundedDelivery()
    {
        ArrayDeque<Runnable> queue=new ArrayDeque<>();List<HudSnapshot> received=new ArrayList<>();
        LatestHudMailbox box=new LatestHudMailbox(queue::add,received::add);
        box.offer(packet(1));box.clear();queue.remove().run();assertTrue(received.isEmpty());
        box.offer(packet(2));box.clear();box.offer(packet(3));assertEquals(1,queue.size());
        queue.remove().run();assertEquals(3,received.get(0).cycle);
    }
}
