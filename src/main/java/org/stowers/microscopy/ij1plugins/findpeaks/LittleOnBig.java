package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImagePlus;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by cjw on 8/11/17.
 */

public class LittleOnBig {

    float howClose;
    FindPeaks3D bigPeaks;
    FindPeaks3D smallPeaks;
    List<FindPeaks3D> findPeaksList;
    Map<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>>  distMap;

    public LittleOnBig(float howClose) {

        this.howClose = howClose;
    }

    public void setBigPeaks(ImagePlus imp, float tolerance, float threshold,
                            float minsep, int npeaks, float zscale) {

        bigPeaks = new FindPeaks3D(imp, tolerance, threshold, minsep, npeaks, zscale);
        bigPeaks.setName("big");
    }

    public void setSmallPeaks(ImagePlus imp, float tolerance, float threshold,
                            float minsep, int npeaks, float zscale) {

        smallPeaks = new FindPeaks3D(imp, tolerance, threshold, minsep, npeaks, zscale);
        smallPeaks.setName("small");
    }

    public void go() {

        findPeaksList = new ArrayList<>();
        findPeaksList.add(bigPeaks);
        findPeaksList.add(smallPeaks);

        Map<String, List<FindPeaks3DLocalMax>> map =
                findPeaksList.parallelStream()
                .collect(Collectors.toMap(FindPeaks3D::getName, FindPeaks3D::findpeaks));

        distMap = new HashMap<>();

        for (FindPeaks3DLocalMax pbig: map.get("big")) {
            SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>> pset = new TreeSet<>(
                    new Comparator<Map.Entry<FindPeaks3DLocalMax, Float>>() {
                        @Override
                        public int compare(Map.Entry<FindPeaks3DLocalMax, Float> o1,
                                           Map.Entry<FindPeaks3DLocalMax, Float> o2) {
                            return o1.getValue().compareTo(o2.getValue());
                        }
                    }
            );

            Map<FindPeaks3DLocalMax, Float> pmap = new HashMap<>();
            for (FindPeaks3DLocalMax psmall: map.get("small")) {
                float d = distance(pbig, psmall);
                if (d < howClose) {
                    pmap.put(psmall, d);
                }
            }
            pset.addAll(pmap.entrySet());
            distMap.put(pbig, pset);
        }
        int j = 4;
    }

    public Map<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>> getDistMap(){
        return distMap;
    }

    protected float distance(FindPeaks3DLocalMax p1, FindPeaks3DLocalMax p2) {

        float[] cm1 = p1.getCOM();
        float[] cm2 = p2.getCOM();
        float dx = cm1[0] - cm2[0];
        float dy = cm1[1] - cm2[1];
        float dz = cm1[2] - cm2[2];

        float dd = dx*dx + dy*dy + dz*dz;
        float d = (float)Math.sqrt(dd);

        return d;
    }
}
