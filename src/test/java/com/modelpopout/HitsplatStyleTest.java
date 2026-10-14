package com.modelpopout;

import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assume;
import org.junit.Test;
import net.runelite.api.HitsplatID;
import static org.junit.Assert.*;

public class HitsplatStyleTest
{
    static byte[] hex(String text)
    {
        byte[] data=new byte[text.length()/2];for(int i=0;i<data.length;i++) data[i]=(byte)Integer.parseInt(text.substring(i*2,i*2+2),16);
        return data;
    }
    @Test public void publicArchiveFormatPreservesNativeColorPartsTimingFadeAndText()
    {
        HitsplatStyle style=HitsplatStyle.decode(hex("01007f0212345603000a04000b05000c06000d07fff808002b2531000900320a000c0c010d00020e001e00"));
        assertEquals(0x123456,style.color);assertArrayEquals(new int[]{10,11,12,13},style.sprites);
        assertEquals(50,style.duration);assertEquals(30,style.fadeStart);assertEquals(1,style.replaceMode);
        assertEquals(-8,style.offsetX);assertEquals(12,style.offsetY);assertEquals(2,style.textOffset);assertEquals("+0",style.text(0));
        HudSnapshot.Hit hit=new HudSnapshot.Hit(6,10,150,0,style,null);
        assertEquals(1,hit.alpha(100),0);assertEquals(.5,hit.alpha(140),0);assertEquals(0,hit.alpha(150),0);
        assertEquals(.8,hit.progress(140),.00001);
    }
    @Test public void smartIdsTransformsAndSentinelsDecodeWithoutInternalClientObjects()
    {
        HitsplatStyle style=HitsplatStyle.decode(hex("0380001234047fff1227fcffff001c01001a001b00"));
        assertEquals(0x1234,style.sprites[0]);assertEquals(-1,style.sprites[1]);
        assertEquals(10236,style.varbit);assertEquals(-1,style.varp);assertArrayEquals(new int[]{26,27,28},style.transforms);
        HitsplatStyle hidden=HitsplatStyle.decode(hex("11ffff007b00ffff00"));
        assertEquals(-1,hidden.varbit);assertEquals(123,hidden.varp);assertArrayEquals(new int[]{-1,-1},hidden.transforms);
    }
    @Test public void fallbacksDistinguishZeroDamageHealingPoisonVenomAndSpecialColors()
    {
        assertEquals("0",HitsplatStyle.fallback(HitsplatID.BLOCK_ME).text(0));
        assertNotEquals(HitsplatStyle.fallback(HitsplatID.BLOCK_ME).color,HitsplatStyle.fallback(HitsplatID.DAMAGE_ME).color);
        assertNotEquals(HitsplatStyle.fallback(HitsplatID.POISON).color,HitsplatStyle.fallback(HitsplatID.VENOM).color);
        assertNotEquals(HitsplatStyle.fallback(HitsplatID.HEAL).color,HitsplatStyle.fallback(HitsplatID.DAMAGE_ME).color);
        assertNotEquals(HitsplatStyle.fallback(HitsplatID.DAMAGE_ME_CYAN).color,HitsplatStyle.fallback(HitsplatID.DAMAGE_ME_ORANGE).color);
        assertEquals("2147483647",HitsplatStyle.fallback(999).text(Integer.MAX_VALUE));
    }
    @Test public void malformedOrOversizedDefinitionsAreRejected()
    {
        for(byte[] bytes:new byte[][]{new byte[0],new byte[4097],hex("6300"),hex("08002531"),hex("0900"),hex("080125310000"),hex("090032")})
        {
            try { HitsplatStyle.decode(bytes);fail("invalid metadata accepted"); }
            catch(IllegalArgumentException|BufferUnderflowException expected) { }
        }
    }
    @Test public void actualLocalGameDefinitionsResolveMissDamagePoisonVenomHealingAndMaxHit() throws Exception
    {
        String directory=System.getProperty("modelpopout.hudFixtures");Assume.assumeNotNull(directory);
        Map<Integer,byte[]> definitions=new HashMap<>();
        for(int id:new int[]{12,16,43,65,26,27,28,29,48,68,5,6,74})
            definitions.put(id,Files.readAllBytes(Path.of(directory,"hitsplat-"+id+".bin")));
        HudAssets assets=new HudAssets(definitions::get,id -> null,id -> 0,id -> 0);
        assertEquals(1358,assets.style(12).sprites[2]);assertEquals(1359,assets.style(16).sprites[2]);
        assertEquals(1360,assets.style(65).sprites[2]);assertEquals(1632,assets.style(5).sprites[2]);
        assertEquals(1629,assets.style(6).sprites[2]);assertEquals(3571,assets.style(43).sprites[2]);
        assertEquals(50,assets.style(16).duration);assertEquals(30,assets.style(74).duration);
        assertEquals(-1,assets.style(16).replaceMode);assertEquals("0",assets.style(12).text(0));
    }
}
