package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImagePlus;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import ij.ImageStack;
import ij.process.ImageProcessor;
import org.stowers.microscopy.ijplugins.utils.DistanceMap;

/**
 * Created by cjw on 8/11/17.
 */

public class LittleOnBig {

    float howClose;
    FindPeaks3D bigPeaks;
    FindPeaks3D smallPeaks;
    List<FindPeaks3D> findPeaksList;
    Map<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>>  distMap;

    HashMap<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> closeMap;
    float smallZScale;
    float bigZScale;

    int w, h, d;
    public LittleOnBig(float howClose) {

        this.howClose = howClose;
    }

    public void setBigPeaks(ImagePlus imp, float tolerance, float threshold,
                            float minsep, int npeaks, float zscale, double smoothRadius) {

        w = imp.getWidth();
        h = imp.getHeight();
        d = imp.getImageStack().getSize();
        bigPeaks = new FindPeaks3D(imp, tolerance, threshold, minsep, npeaks, zscale, smoothRadius);
        bigPeaks.setName("big");
        bigZScale = zscale;


    }



    public void setSmallPeaks(ImagePlus imp, float tolerance, float threshold,
                            float minsep, int npeaks, float zscale, double smoothRadius) {

        smallPeaks = new FindPeaks3D(imp, tolerance, threshold, minsep, npeaks, zscale, smoothRadius);
        smallPeaks.setName("small");
        smallZScale = zscale;
    }

    public void go() {

        findPeaksList = new ArrayList<>();
        findPeaksList.add(bigPeaks);
        findPeaksList.add(smallPeaks);

        closeMap = new HashMap<>();

        Map<String, List<FindPeaks3DLocalMax>> map =
                findPeaksList.parallelStream()
                .collect(Collectors.toMap(FindPeaks3D::getName, FindPeaks3D::findpeaks));


//        distMap = new HashMap<>();

        List<Long> smallPts = new ArrayList<>();
        HashMap<Long, FindPeaks3DLocalMax> smap = new HashMap<>();

        for (FindPeaks3DLocalMax psmall: map.get("small")) {
            smallPts.add(psmall.getVoxelIndex());
//            System.out.println(psmall.getVoxelIndex() + " " + psmall.getX() + " " + psmall.getY() + " " + psmall.getZ());
            smap.put(psmall.getVoxelIndex(), psmall);
//            float d = distance(pbig, psmall);
//                if (d < howClose) {
//                    pmap.put(psmall, d);
//                    //System.out.println("Yes");
//                }
        }

        for (FindPeaks3DLocalMax pbig: map.get("big")) {
//            SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>> pset = new TreeSet<>(
//                    new Comparator<Map.Entry<FindPeaks3DLocalMax, Float>>() {
//                        @Override
//                        public int compare(Map.Entry<FindPeaks3DLocalMax, Float> o1,
//                                           Map.Entry<FindPeaks3DLocalMax, Float> o2) {
//                            return o1.getValue().compareTo(o2.getValue());
//                        }
//                    }
//            );

            List<Long> bigVox = pbig.getObjectVoxels();

            //Map<FindPeaks3DLocalMax, Float> pmap = new HashMap<>();
            // we now have everything to do a complete distance map on all voxels in pbig and psmall

            DistanceMap distanceMap = new DistanceMap(smallPts, bigVox, w, h, d, howClose);
            HashMap<Long, HashMap<Long, Double>> dmap = distanceMap.makeMap();

            List<FindPeaks3DLocalMax> tmpfp = new ArrayList<>();
            for (Long i : distanceMap.getClose()) {
                tmpfp.add(smap.get(i));
            }

            closeMap.put(pbig, tmpfp);
            //pset.addAll(pmap.entrySet());
            //distMap.put(pbig, pset);
        }
        int j = 4;
    }

    public Map<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> getCloseMap() {
        return closeMap;
    }

    public Map<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>> getDistMap(){
        return distMap;
    }

    protected float distance(FindPeaks3DLocalMax p1, FindPeaks3DLocalMax p2) {

        float fr = p1.foundRadius;

        float[] cm1 = p1.getCOM();
        float[] cm2 = p2.getCOM();
        float dx = cm1[0] - cm2[0];
        float dy = cm1[1] - cm2[1];
        float dz = bigZScale*cm1[2] - smallZScale*cm2[2];

        float dd = dx*dx + dy*dy + dz*dz;
        float d = (float)Math.sqrt(dd)- fr;
//        System.out.println("Radius " + fr + " " + (float)Math.sqrt(dd) + " " + d);
        return d;
    }
}
