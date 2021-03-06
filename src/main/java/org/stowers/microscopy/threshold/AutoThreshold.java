package org.stowers.microscopy.threshold;

/**
 * Created by cjw on 4/25/17.
 */

import ij.IJ;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import ij.plugin.filter.MaximumFinder;
import ij.process.*;
import ij.ImagePlus;

import inra.ijpb.binary.BinaryImages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AutoThreshold {

    public static float[] threshold(ImageProcessor ip, String method) {

        int w = ip.getWidth();
        int h = ip.getHeight();

        float[] mask = new float[w*h];
        AutoThresholder auto = new ij.process.AutoThresholder();
        int thresh = auto.getThreshold(method, ip.getStatistics().histogram);

        int nh = ip.getStatistics().histogram.length;

        ip.setHistogramRange(ip.getStatistics().min, ip.getStatistics().max);
        double a1 = ip.getHistogramMin();
        double a2 = ip.getHistogramMax();
        double d = (a2 - a1)/(1.*nh);
        double tval = a1 + d*(thresh + 1);

        float[] pixels = (float[])ip.convertToFloatProcessor().getPixels();

        for (int i = 0; i < w*h; i++) {
            if (pixels[i] >= tval) {
                mask[i] = 255.f;
            }
        }
        System.out.println(a1 + " " + a2 + " " + d + " " + thresh + " " + tval + " " + nh);
        return mask;
    }

    public static ImageProcessor thresholdIp(ImageProcessor ip, String method, boolean doWatershed) {

        return thresholdIp(ip, method, true, doWatershed);
    }

    public static ImageProcessor thresholdIp(ImageProcessor ip, String method, boolean fill,
                                             boolean doWatershed) {

        int w = ip.getWidth();
        int h = ip.getHeight();

        float[] maskPixels = threshold(ip, method);
        ImageProcessor floatmask = new FloatProcessor(w, h, maskPixels);
        ImageProcessor mask = floatmask.convertToByte(false);
        mask.setBackgroundValue(0);
        MaximumFinder maxFinder = new MaximumFinder();
        EDM edm = new EDM();
//        FloatProcessor floatEdm = edm.makeFloatEDM(mask, 0, false);
//        ByteProcessor maxIp = maxFinder.findMaxima(floatEdm, 0.5,
//                ImageProcessor.NO_THRESHOLD, MaximumFinder.SEGMENTED, false, true);
//
        ImagePlus mm = new ImagePlus("wm", mask);
//        mm.show();

        if (fill) {
            IJ.run(mm, "Fill Holes", "");
        }
        if (doWatershed) {
            edm.toWatershed(mask);
        }

        return mask;
    }


    public static ImageProcessor labelRegions(ImageProcessor ip) {

//        ImageProcessor mask = thresholdIp(ip, method);
        ImageProcessor regions =  BinaryImages.componentsLabeling(ip, 4, 16);
        return regions;
    }

    public static HashMap<Integer, List<Integer>>  labelsToMap(ImageProcessor ip) {
        HashMap<Integer, List<Integer>> map = new HashMap<>();

        short[] pixels = (short[])ip.getPixels();

        for (int i = 0; i < pixels.length; i++) {
            int label = pixels[i];
            if (label == 0) {
                continue;
            }
            if (map.keySet().contains(label)) {
                map.get(label).add(i);
            }
            else {
                List a = new ArrayList<Integer>();
                map.put(label, a);
                a.add(i);
            }
        }
        return map;
    }
}
