package org.stowers.microscopy.ij1plugins.findpeaks;

/**
 * Created by cjw on 8/1/17.
 */

import ij.ImagePlus;
import ij.ImageStack;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;

public class FindPeaks3D {

    ImagePlus imp = null;
    ImageStack stack;

    float[] voxels;
    int w;
    int h;
    int d;

    float tolerance;
    float threshold;
    float minsep;
    int npeaks;
    float zscale;
    String name = "None";
    List<FindPeaks3DLocalMax> peaksFoundList;
    List<FindPeaks3DLocalMax> peaksKeptList;
    List<Long> removeList;

    public FindPeaks3D(ImagePlus imp, float tolerance, float threshold,
                       float minsep, int npeaks, float zscale) {

        this.imp = imp;
        this.tolerance = tolerance;
        this.threshold = threshold;
        this.minsep = minsep;
        this.npeaks = npeaks;
        this.zscale = zscale;
        w = imp.getWidth();
        h = imp.getHeight();
        stack = imp.getStack().convertToFloat();
        d = stack.getSize();
        //findpeaks();
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<FindPeaks3DLocalMax> getPeaksKeptList() {
        return peaksKeptList;
    }

    protected void findpeaksNoRes() {
        findpeaks();
    }


    protected List<FindPeaks3DLocalMax> findpeaks() {

        List<FindPeaks3DThread> threadList = Collections.synchronizedList(new ArrayList<>());
        for (int k = 1; k < d -1; k++) {
            for (int j = 1; j < h - 1; j++) {
                FindPeaks3DThread thread = new FindPeaks3DThread(stack, 1, j, k, w - 1, 1, 1,
                        tolerance, threshold, minsep, zscale);
                thread.setName(name);
                threadList.add(thread);
            }
        }

        peaksFoundList = threadList.parallelStream()
                .map(e -> e.process())
                .flatMap(List::stream)
                .sorted((e1, e2) -> Long.compare(e1.getVoxelIndex(), //sort on pixel array index
                        e2.getVoxelIndex()))
                .collect(Collectors.toList());
        System.out.println(peaksFoundList.size());
        peaksFoundList = peaksFoundList.stream()
                .filter(e -> e.checkY())
                .sorted(
                        comparing(FindPeaks3DLocalMax::getValue).reversed())  //sort bright to dim
                .collect(Collectors.toList());

        System.out.println(peaksFoundList.size());

        int j = 0;

        removeList = new ArrayList<>();
        peaksKeptList = new ArrayList<>();

        for (FindPeaks3DLocalMax p : peaksFoundList) {
            /*
            keep the peak if it is not on the remove list
            if it is not close to a peek already on the keep list
            if its region is larger than 1 pixel
             */

            if (removeList.contains(p.getVoxelIndex())) {
                continue;
            }
            if (isClose(p)) {
                removeList.add(p.getVoxelIndex());
                continue;
            }
            List<Long> region = p.findRegion();

            if (p.getNeighborsList().size() > 1) {
                peaksKeptList.add(p);
                removeClose(region);
            } else {
                removeList.add(p.getVoxelIndex());
            }

            if (peaksKeptList.size() >= npeaks) {
                break;
            }
        }

        peaksKeptList = peaksKeptList.stream()
                .sorted(
                        comparing(FindPeaks3DLocalMax::getSumIntensity).reversed())  //sort bright to dim
//                        comparing(FindPeaks3DLocalMax::getZ)
                .collect(Collectors.toList());
        return peaksKeptList;

    }

    private boolean isClose(FindPeaks3DLocalMax peak) {

        for (FindPeaks3DLocalMax p : peaksKeptList) {
            if (checkDistance(peak, p)) return true;
        }
        return false;
    }

    private boolean checkDistance(FindPeaks3DLocalMax p1, FindPeaks3DLocalMax p2) {
        float dx = p1.getX() - p2.getX();
        float dy = p1.getY() - p2.getY();
        float dz = p1.getZ() - p2.getZ();

        float dd = dx*dx + dy*dy + zscale*zscale*dz*dz;
        boolean res;

        if (dd < minsep*minsep) {
            res = true;
            //System.out.println(res + " " + Math.sqrt(dd) + " " + dx + " " + dy + " " + dz);
        }
        else {
            res = false;
        }


        return res;
    }

    private void removeClose(List<Long> regions) {

        for (FindPeaks3DLocalMax p : peaksFoundList) {
            Long index = p.getVoxelIndex();
            //Object o = (Long)index;
            if (regions.contains(index)) {
                removeList.add(index);
            }
        }
    }

    private void stackToArray() {

        voxels = new float[w*h*d];

        for (int k = 0; k < d; k++) {

            float[] pixels = (float[])stack.getProcessor(k + 1)
                    .convertToFloatProcessor()
                    .getPixels();

            for (int i = 0; i < pixels.length; i++) {
                int index = k*w*h + i;
                voxels[index] = pixels[i];
            }
        }

        System.out.println(Integer.MAX_VALUE + " " + voxels.length);
    }


}
