package com.modelpopout;

/** Recover the game's planar UV basis from copied texture-mapping triangles, using a Gram solve. */
final class TextureCoordinates
{
    static double[] compute(ModelSnapshot mesh)
    {
        double[] uv = new double[mesh.a.length*6];
        for (int face = 0; face < mesh.a.length; face++)
        {
            int offset = face*6;
            if (mesh.textureFaces == null || mesh.textureFaces[face] == -1)
            {
                uv[offset+2] = 1; uv[offset+5] = 1;
                continue;
            }
            int mapping = mesh.textureFaces[face] & 255;
            if (mesh.texA == null || mapping >= mesh.texA.length)
            { invalid(uv,offset); continue; }
            int origin = mesh.texA[mapping], b = mesh.texB[mapping], c = mesh.texC[mapping];
            if (origin < 0 || b < 0 || c < 0 || origin >= mesh.x.length || b >= mesh.x.length || c >= mesh.x.length)
            { invalid(uv,offset); continue; }
            double[] e = {mesh.x[b]-mesh.x[origin],mesh.y[b]-mesh.y[origin],mesh.z[b]-mesh.z[origin]};
            double[] f = {mesh.x[c]-mesh.x[origin],mesh.y[c]-mesh.y[origin],mesh.z[c]-mesh.z[origin]};
            double ee = dot(e,e), ff = dot(f,f), ef = dot(e,f), determinant = ee*ff-ef*ef;
            if (Math.abs(determinant) < 1e-12) { invalid(uv,offset); continue; }
            int[] vertices = {mesh.a[face],mesh.b[face],mesh.c[face]};
            for (int vertex = 0; vertex < 3; vertex++)
            {
                int index = vertices[vertex];
                double[] p = {mesh.x[index]-mesh.x[origin],mesh.y[index]-mesh.y[origin],mesh.z[index]-mesh.z[origin]};
                double pe = dot(p,e), pf = dot(p,f);
                uv[offset+vertex*2] = (pe*ff-pf*ef)/determinant;
                uv[offset+vertex*2+1] = (pf*ee-pe*ef)/determinant;
            }
        }
        return uv;
    }
    private static double dot(double[] a,double[] b) { return a[0]*b[0]+a[1]*b[1]+a[2]*b[2]; }
    private static void invalid(double[] uv,int offset) { for (int i=0;i<6;i++) uv[offset+i] = Double.NaN; }
}
