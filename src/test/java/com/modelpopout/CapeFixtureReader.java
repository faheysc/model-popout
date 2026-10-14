package com.modelpopout;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Explicit test-data parsing: no reflection or private snapshot deserialization. */
final class CapeFixtureReader
{
    @SuppressWarnings("deprecation")
    static CapeSurfaceTest.Fixture read(String source)
    {
        JsonObject root=new JsonParser().parse(source).getAsJsonObject();
        JsonObject mesh=root.getAsJsonObject("mesh");
        CapeSurfaceTest.Fixture fixture=new CapeSurfaceTest.Fixture();
        fixture.pixels=ints(root,"pixels");
        fixture.textureId=root.get("textureId").getAsInt();
        fixture.direction=root.get("direction").getAsInt();
        fixture.speed=root.get("speed").getAsInt();
        int[] assignments=ints(mesh,"textureIds");
        TestModel model=new TestModel()
        {
            public int getVerticesCount(){return mesh.getAsJsonArray("x").size();}
            public int getFaceCount(){return mesh.getAsJsonArray("a").size();}
            public float[] getVerticesX(){return floats(mesh,"x");}
            public float[] getVerticesY(){return floats(mesh,"y");}
            public float[] getVerticesZ(){return floats(mesh,"z");}
            public int[] getFaceIndices1(){return ints(mesh,"a");}
            public int[] getFaceIndices2(){return ints(mesh,"b");}
            public int[] getFaceIndices3(){return ints(mesh,"c");}
            public int[] getFaceColors1(){return colors(mesh,"colorA","lightA",assignments);}
            public int[] getFaceColors2(){return colors(mesh,"colorB","lightB",assignments);}
            public int[] getFaceColors3(){return colors(mesh,"colorC","lightC",assignments);}
            public short[] getFaceTextures(){return shorts(assignments);}
            public byte[] getFaceTransparencies(){return bytes(ints(mesh,"alpha"));}
            public byte[] getTextureFaces(){return bytes(ints(mesh,"textureFaces"));}
            public byte[] getFaceRenderPriorities(){return bytes(ints(mesh,"priorities"));}
            public int[] getTexIndices1(){return ints(mesh,"texA");}
            public int[] getTexIndices2(){return ints(mesh,"texB");}
            public int[] getTexIndices3(){return ints(mesh,"texC");}
        };
        fixture.mesh=ModelSnapshot.capture(model,null);
        return fixture;
    }
    private static int[] colors(JsonObject mesh,String color,String light,int[] assignments)
    {
        int[] values=ints(mesh,color),lights=ints(mesh,light);
        for(int i=0;i<values.length;i++) if(assignments[i]>=0) values[i]=lights[i];
        return values;
    }
    private static int[] ints(JsonObject root,String name)
    {
        JsonElement element=root.get(name);
        if(element==null || element.isJsonNull()) return null;
        JsonArray array=element.getAsJsonArray();int[] values=new int[array.size()];
        for(int i=0;i<values.length;i++) values[i]=array.get(i).getAsInt();
        return values;
    }
    private static float[] floats(JsonObject root,String name)
    {
        JsonArray array=root.getAsJsonArray(name);float[] values=new float[array.size()];
        for(int i=0;i<values.length;i++) values[i]=array.get(i).getAsFloat();
        return values;
    }
    private static short[] shorts(int[] values)
    {
        if(values==null) return null;
        short[] result=new short[values.length];
        for(int i=0;i<values.length;i++) result[i]=(short)values[i];
        return result;
    }
    private static byte[] bytes(int[] values)
    {
        if(values==null) return null;
        byte[] result=new byte[values.length];
        for(int i=0;i<values.length;i++) result[i]=(byte)values[i];
        return result;
    }
}
