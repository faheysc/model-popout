package com.modelpopout;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class HudAssetsTest
{
    @Test public void authenticSpritePixelsAndDefinitionTextColorsSurviveComposition()
    {
        BufferedImage sprite=solid(32,32,0xff123456);
        HudAssets assets=new HudAssets(id -> HitsplatStyleTest.hex("02aabbcc05006309003200"),id -> sprite,id -> 0,id -> 0);
        HitsplatStyle style=assets.style(16);BufferedImage image=assets.hitImage(16,style,0);
        assertEquals(0xff123456,image.getRGB(0,image.getHeight()/2));
        boolean foundText=false;for(int pixel:image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth())) if(pixel==0xffaabbcc) foundText=true;
        assertTrue(foundText);assertEquals(0xff123456,sprite.getRGB(0,0));
    }
    @Test public void transformationsUsePublicVarbitsVarpsAndRespectHiddenStyles()
    {
        Map<Integer,byte[]> definitions=new HashMap<>();
        definitions.put(1,HitsplatStyleTest.hex("1227fcffff0004010002000300"));
        definitions.put(2,HitsplatStyleTest.hex("0211223300"));definitions.put(3,HitsplatStyleTest.hex("0244556600"));
        definitions.put(4,HitsplatStyleTest.hex("0277889900"));int[] value={0};
        HudAssets assets=new HudAssets(definitions::get,id -> null,id -> {assertEquals(10236,id);return value[0];},id -> failVarp());
        assertEquals(0x112233,assets.style(1).color);value[0]=1;assertEquals(0x445566,assets.style(1).color);
        value[0]=9;assertEquals(0x778899,assets.style(1).color);
        definitions.put(5,HitsplatStyleTest.hex("11ffff007b00ffff00"));
        HudAssets varp=new HudAssets(definitions::get,id -> null,id -> failVarp(),id -> {assertEquals(123,id);return 0;});
        assertNull(varp.style(5));
    }
    private static int failVarp() {fail("wrong variable API");return 0;}
    @Test public void unavailableMalformedHugeOrCyclicAssetsFallBackWithBoundedWork()
    {
        int[] reads={0};HudAssets looping=new HudAssets(id -> {reads[0]++;return HitsplatStyleTest.hex("110001ffff00000100");},id -> null,id -> 0,id -> 0);
        assertNotNull(looping.style(1));assertEquals(1,reads[0]); // Eight resolution steps, one cached definition.
        HudAssets broken=new HudAssets(id -> new byte[]{99},id -> solid(257,1,0xff000000),id -> 0,id -> 0);
        BufferedImage fallback=broken.hitImage(65,broken.style(65),12);assertTrue(fallback.getWidth()<=256);assertTrue(fallback.getHeight()<=128);
        int[] misses={0};HudAssets unavailable=new HudAssets(id -> null,id -> {misses[0]++;throw new UnsupportedOperationException();},id -> 0,id -> 0);
        assertSame(HudAssets.HealthSkin.FALLBACK,unavailable.healthSkin(10));assertEquals(2,misses[0]);
        unavailable.healthSkin(59);assertEquals(2,misses[0]);unavailable.healthSkin(60);assertEquals(4,misses[0]);
    }
    @Test public void nativeHealthSkinIsLoadedOnceAndStyleCacheIsBounded()
    {
        int[] spriteReads={0},definitionReads={0};
        HudAssets assets=new HudAssets(id -> {definitionReads[0]++;return new byte[]{0};},
            id -> {spriteReads[0]++;return solid(30,5,Color.GREEN.getRGB());},id -> 0,id -> 0);
        HudAssets.HealthSkin skin=assets.healthSkin(1);assertEquals(30,skin.width());assertEquals(5,skin.height());
        assertSame(skin,assets.healthSkin(10000));assertEquals(2,spriteReads[0]);
        for(int i=0;i<65;i++) assets.style(i);assets.style(0);assertEquals(66,definitionReads[0]);
    }
    static BufferedImage solid(int width,int height,int color)
    {
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++) for(int x=0;x<width;x++) image.setRGB(x,y,color);return image;
    }
    @Test public void unavailableNativeArtStillDistinguishesMissDamagePoisonAndHealing()
    {
        HudAssets unavailable=new HudAssets(id -> new byte[]{0},id -> null,id -> 0,id -> 0);
        for(int type:new int[]{12,16,65,5,6})
        {
            HitsplatStyle nativeStyle=unavailable.style(type);
            assertEquals(0xffffff,nativeStyle.color); // Native white text is not the fallback plate color.
            BufferedImage image=unavailable.hitImage(type,nativeStyle,0);
            assertEquals(0xff000000|HitsplatStyle.fallback(type).color,image.getRGB(image.getWidth()-3,image.getHeight()/2));
        }
    }
}
