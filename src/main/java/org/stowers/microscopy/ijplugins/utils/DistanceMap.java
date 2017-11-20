package org.stowers.microscopy.ijplugins.utils;


import org.stowers.microscopy.ij1plugins.findpeaks.FindPeaks3DLocalMax;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cjw on 11/8/17.
 */


public class DistanceMap {

    List<Long> pointsList1;
    List<Long> pointsList2;

//    HashMap<Long, SortedSet<Map.Entry<Long, Double>>> map;
    HashMap<Long, HashMap<Long, Double>> map;
    List<Long> close;
    int w, h, d;

    double maxDistance;

    public DistanceMap(List<Long> pointsList1, List<Long> pointsList2, int w, int h, int d, double maxDistance) {
        this.pointsList1 = pointsList1;
        this.pointsList2 = pointsList2;

        this.w = w;
        this.h = h;
        this.d = d;
        this.maxDistance = maxDistance;
        map = new HashMap<>();
        close = new ArrayList<>();

    }


    public List<Long> getClose() {
        return close;
    }

//    public HashMap<Long, SortedSet<Map.Entry<Long, Double>>> makeMap() {
    public HashMap<Long, HashMap<Long, Double>> makeMap() {
        for (long i1 : pointsList1) {
            int[] e1 = indexToXYZ(i1);
//            map.put(i1, makeSortedSet());

            HashMap<Long, Double> tmpMap;
            HashMap<Long, Double> tmpMap0 = new LinkedHashMap<>();
            //map.put(i1, tmpMap);
            boolean bk = false;
            for (long i2 : pointsList2) {

                int[] e2 = indexToXYZ(i2);

                double di = distance(e1, e2);
                if (pointsList2.contains(i1)) {
                    di = -di;
                }
                tmpMap0.put(i2, di);
                if (!bk) {
                    if (di <= maxDistance) {
                        close.add(i1);
                        bk = true;
                    }
                }
            }

            tmpMap = tmpMap0.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (s1, s2) -> s1,
                            LinkedHashMap::new));

//            map.get(i1).addAll(tmpMap.entrySet());
            map.put(i1, tmpMap);
//            SortedSet<Map.Entry<Long, Double>>  ts = map.get(i1);
            int j = 34;
        }

        return map;
    }

    private SortedSet<Map.Entry<Long, Double>> makeSortedSet() {

        SortedSet<Map.Entry<Long, Double>> pset = new TreeSet<>(
                new Comparator<Map.Entry<Long, Double>>() {
                    @Override
                    public int compare(Map.Entry<Long, Double> o1,
                                       Map.Entry<Long, Double> o2) {
                        return o1.getValue().compareTo(o2.getValue());
                    }
                }
        );

        return pset;
    }


    public int[] indexToXYZ(long index) {

        int z = (int)(index / (w*h));
        int x = (int)((index % (w*h)) % w);
        int y = (int)((index % (w*h)) / w);

        return new int[] {x, y, z};
    }

    public double distance(int[] p1, int[] p2) {

        double dx = p1[0] - p2[0];
        double dy = p1[1] - p2[1];
        double dz = p1[2] - p2[2];

        double dx2 = dx*dx;
        double dy2 = dy*dy;
        double dz2 = dz*dz;

        double di = Math.sqrt(dx2 + dy2 + dz2);
        return di;
    }
}
