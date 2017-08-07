package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 8/1/17.
 */

import ij.ImagePlus;
import ij.ImageStack;

import java.util.*;
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
    List<FindPeaks3DLocalMax> peaksList;

    public FindPeaks3D(ImagePlus imp, float tolerance, float threshold) {

        this.imp = imp;
        this.tolerance = tolerance;
        this.threshold = threshold;
        w = imp.getWidth();
        h = imp.getHeight();
        stack = imp.getStack().convertToFloat();
        d = stack.getSize();
        //findpeaks();
    }


    protected List<FindPeaks3DLocalMax> findpeaks() {

        List<FindPeaks3DThread> threadList = Collections.synchronizedList(new ArrayList<>());
        for (int k = 1; k < d -1; k++) {
            for (int j = 1; j < h - 1; j++) {
                FindPeaks3DThread thread = new FindPeaks3DThread(stack, 1, j, k, w - 1, 1, 1, tolerance, threshold);
                threadList.add(thread);
            }
        }

        peaksList = threadList.parallelStream()
                .map(e -> e.process())
                .flatMap(List::stream)
                .sorted((e1, e2) -> Long.compare(e1.getVoxelIndex(),
                        e2.getVoxelIndex()))
                .collect(Collectors.toList());
        System.out.println(peaksList.size());
        peaksList = peaksList.stream()
                .filter(e -> e.checkY())
                .sorted(
                        comparing(FindPeaks3DLocalMax::getValue).reversed())
//                .sorted((e1, e2) -> Float.compare(e1.getValue(),
//                        e2.getValue()))
                .collect(Collectors.toList());

        System.out.println(peaksList.size());
        return peaksList;


//        threadList.parallelStream()
//                .forEach(e -> e.process());
//
//        System.out.println(threadList.size());
//        threadList = threadList.parallelStream()
//                .filter(e -> e.getNumMaxima() > 0)
//                .collect(Collectors.toList());
//
//        threadList.parallelStream()
//                .forEach(e -> e.checkY());
//
//        System.out.println(threadList.size());
//        threadList = threadList.parallelStream()
//                .filter(e -> e.getNumMaxima() > 0)
//                .collect(Collectors.toList());
//
//        System.out.println("------- Y to Z ------");
//        threadList.parallelStream()
//                .forEach(e -> e.checkZ());
//
//        System.out.println(threadList.size());
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
