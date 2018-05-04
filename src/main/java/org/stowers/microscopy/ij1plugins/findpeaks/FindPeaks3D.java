package org.stowers.microscopy.ij1plugins.findpeaks;


import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.StackStatistics;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;

/** Finds the peaks in a 3d image
 * @author Chris Wood
 * @version 0.01
 *
 * Stowers Institute for Medical Research
 * Microscopy Center
 *
 * 2017
 */
public class FindPeaks3D {

    ImagePlus imp = null;
    ImageStack stack;
    StackStatistics stats;

    float[] voxels;
    int w;
    int h;
    int d;

    float tolerance;
    float threshold;
    float minsep;
    int npeaks;
    float zscale;
    float maxSize;

    String name = "None";
    List<FindPeaks3DLocalMax> peaksFoundList;
    List<FindPeaks3DLocalMax> peaksKeptList;
    List<Long> removeList;

    List<Long> hotVoxels;
    /**
     * Created the FindPeaks object with the input parmeters
     * @param imp The ImagePlus from ImageJ
     * @param tolerance The noise tolerance (how high are peaks above baseline
     * @param threshold Consider nothing lower than this value
     * @param minsep Don't condsider spots closer than this
     * @param npeaks Maximum number of peaks to find
     * @param zscale How long is each z compared to x and y
     */
    public FindPeaks3D(ImagePlus imp, float tolerance, float threshold,
                       float minsep, int npeaks, float zscale, double smoothRadius,
                       float maxSize) {

        this.imp = imp;
        this.tolerance = tolerance;
        this.threshold = threshold;
        this.minsep = minsep;
        this.npeaks = npeaks;
        this.zscale = zscale;
        this.maxSize = maxSize;
        w = imp.getWidth();
        h = imp.getHeight();
        stack = imp.getStack().convertToFloat();
        stats = new StackStatistics(imp);
        d = stack.getSize();
        hotVoxels = new ArrayList<>();
        //findpeaks();

        if (smoothRadius > 0.f) {
            smoothStack(imp, smoothRadius);
        }
    }

    private void smoothStack(ImagePlus imp, double radius) {

        int n = stack.getSize();

        for (int i = 0; i < n; i++) {
            ImageProcessor ip = stack.getProcessor(i + 1);
            ip.blurGaussian(radius);
        }

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

    public List<Long> getHotVoxelsList() {
        return hotVoxels;
    }

    protected void findpeaksNoRes() {
        findpeaks();
    }

    /**
     *
     * @return List<FindPeaks3DLocalMax> Returns a List of FindPeaks3DLocalMax objects
     */
    protected List<FindPeaks3DLocalMax> findpeaks() {

        List<FindPeaks3DThread> threadList = Collections.synchronizedList(new ArrayList<>());
        for (int k = 1; k < d -1; k++) {
            for (int j = 1; j < h - 1; j++) {
                FindPeaks3DThread thread = new FindPeaks3DThread(stack, stats, 1, j, k, w - 1, 1, 1,
                        tolerance, threshold, minsep, zscale, maxSize);
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
            if (p.getVoxelIndex() == 359989) {
                int f = 5;
            }

            if (removeList.contains(p.getVoxelIndex())) {
                continue;
            }
            if (isClose(p)) {
                removeList.add(p.getVoxelIndex());
                continue;
            }
            // region is the same list as getNeighborsList
            List<Long> region = p.findRegion(hotVoxels);
            hotVoxels.addAll(region);
            if (p.getNeighborsList().size() >= 1) {
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

    public ImagePlus getRegionsStack() {


        ImageStack rStack = new ImageStack(w, h, d);
        for (int k = 0; k < d; k++) {
            byte[] bp = new byte[w * h];
            rStack.setProcessor(new ByteProcessor(w, h, bp), k + 1);
        }

        for (long v : hotVoxels) {

            int vz = (int)v/(w*h);
            int dex = (int)(v % (w*h));
            int vx = ((int)v % (w*h)) % w;
            int vy = ((int)v % (w*h)) / w;
            //System.out.println(v + " " + vx + " " + vy + " " + vz);
            ImageProcessor zp = rStack.getProcessor(vz + 1);
            zp.set(dex, 255);
        }
        ImagePlus rImp = new ImagePlus("3D mask", rStack);
        return rImp;
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

    protected void measurePeaks() {

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
