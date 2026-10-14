package com.modelpopout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Native-style priority groups, including depth interleaving for priorities 10 and 11. */
final class FaceOrdering
{
    static int[] order(ModelSnapshot mesh, double[] z)
    {
        Integer[] sorted = new Integer[mesh.a.length];
        double[] distance = new double[sorted.length];
        for (int i=0;i<sorted.length;i++) { sorted[i]=i; distance[i]=(z[mesh.a[i]]+z[mesh.b[i]]+z[mesh.c[i]])/3; }
        Arrays.sort(sorted,Comparator.comparingDouble((Integer i) -> -distance[i]));
        if (mesh.priorities == null) return Arrays.stream(sorted).mapToInt(Integer::intValue).toArray();
        List<List<Integer>> groups = new ArrayList<>();
        for (int i=0;i<12;i++) groups.add(new ArrayList<>());
        for (int face : sorted) groups.get(Math.min(11,mesh.priorities[face]&255)).add(face);
        List<Integer> special = new ArrayList<>(groups.get(10)); special.addAll(groups.get(11));
        double[] thresholds = {average(groups,distance,1,2),average(groups,distance,3,4),average(groups,distance,6,8)};
        int[] result = new int[sorted.length]; int out=0, dynamic=0;
        for (int priority=0;priority<10;priority++)
        {
            int threshold = priority == 0 ? 0 : priority == 3 ? 1 : priority == 5 ? 2 : -1;
            if (threshold >= 0) while (dynamic < special.size() && distance[special.get(dynamic)] > thresholds[threshold])
                result[out++] = special.get(dynamic++);
            for (int face : groups.get(priority)) result[out++] = face;
        }
        while (dynamic < special.size()) result[out++] = special.get(dynamic++);
        return result;
    }
    private static double average(List<List<Integer>> groups,double[] depth,int a,int b)
    {
        double sum=0; int count=0;
        for (int group : new int[]{a,b}) for (int face : groups.get(group)) { sum+=depth[face]; count++; }
        return count == 0 ? 0 : sum/count;
    }
}
