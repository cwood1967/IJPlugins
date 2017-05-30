package org.stowers.microscopy.ijplugins.utils;

import ij.ImagePlus;
import ij.plugin.filter.MaximumFinder;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import net.imglib2.ops.parse.token.Int;
import org.stowers.microscopy.ij1plugins.FastFileSaver;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by cjw on 4/27/17.
 */
public class NucleusPixels {

    ImageProcessor ip;
    List<Integer> indexList;
    float[] pixels;
    float[] patchPixels;
    HashMap<String, Double> statsMap;
    LinkedHashMap<Integer, Double> sortedMap;
    LinkedHashMap<Integer, Double> maxMap = null;

    ImageProcessor patchIp = null;

    double sum;
    double mean;
    double min;
    double max;
    double stdDev;
    double median;
    int xmin;
    int ymin;
    int xmax;
    int ymax;
    int ipWidth;
    int ipHeight;
    int patchWidth;
    int patchHeight;
    int peakX;
    int peakY;


    boolean goodSpot;

    public NucleusPixels(ImageProcessor ip, List<Integer> indexList) {
        this.ip = ip; //ip.convertToFloatProcessor();
        this.pixels = (float[])ip.getPixels();
        this.indexList = indexList;
        this.statsMap = new HashMap<>();
        try {
            calcStats();
        }
        catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("A bad thing!!");
            return;
        }
        int w = xmax - xmin + 1;
        int h = ymax - ymin + 1;
        System.out.println("Patch Dims " + w + " X " + h);
        if (w < 100 || h < 100) {
            System.out.println("Too small " + w + " X " + h);
            return;
        } else {
            makePatch();
            findMaxima();
            System.out.println("######## " + decide() + " " + mean + " " + stdDev + " " + max);
        }
//        getTop();
    }

    public void calcStats() {

        ipHeight =ip.getHeight();
        ipWidth = ip.getWidth();

        sum = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;

        xmin = ipWidth;
        ymin = ipHeight;
        xmax = 0;
        ymax = 0;
        HashMap<Integer, Double> pixelMap = new HashMap<>();
        for (int index : indexList) {
            double v = pixels[index];
            int y = index/ipWidth;
            int x = index % ipWidth;
            if (x < xmin) xmin = x;
            if (y < ymin) ymin = y;
            if (x > xmax) xmax = x;
            if (y > ymax) ymax = y;
            pixelMap.put(index, v);
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        patchWidth = xmax - xmin + 1;
        patchHeight = ymax - ymin + 1;
        mean = sum/indexList.size();

        double sdsum = 0;
        for (int index : indexList) {
            double v = pixels[index];
            sdsum +=  (v - mean)*(v - mean);
        }
        stdDev = Math.sqrt(sdsum/(indexList.size() - 1));

        Comparator<Map.Entry<Integer, Double>> byValue = (entry1, entry2) -> entry1.getValue()
                .compareTo(entry2.getValue());

        List<Double> sortedPixelList = pixelMap.entrySet().stream()
                .sorted(byValue)
                .map(x -> x.getValue())
                .collect(Collectors.toList());

        sortedMap = pixelMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> {throw new AssertionError();},
                        LinkedHashMap::new
                ));

        int mid = sortedPixelList.size()/2;

        if (mid % 2 == 0) {
            median = (sortedPixelList.get(mid) + sortedPixelList.get(mid - 1))/2.;
        }
        else {
            median = sortedPixelList.get(mid);
        }

    }

    public int savePatch(String dir, String parentName) {

        if (patchIp == null) return -1;
        String posString = String.format("-%04d-%04d", xmin, ymin);
        String[] dotSplit = parentName.split("\\.");
        String basename = dotSplit[0];
        String name = basename + posString;
        String outName = dir + "/" + name + ".tif";
        ImagePlus p = new ImagePlus(name, patchIp);
        FastFileSaver saver = new FastFileSaver(p);
        saver.saveAsTiff(outName);
        return 0;
    }

    public int getPeakX() {
        return peakX;
    }

    public int getPeakY() {
        return peakY;
    }
    /* Make an image out of the pixels from the segmented regions
     Use the class variables to so this
     */
    protected void makePatch() {

        int w = xmax - xmin + 1;
        int h = ymax - ymin + 1;

        patchPixels= new float[w*h];

        for (int i = 0; i < patchPixels.length; i++) {
            patchPixels[i] = (float)mean;
        }
        for (Map.Entry<Integer, Double> entry : sortedMap.entrySet()) {
            int index0 = entry.getKey();
            int x0 = index0 %  ipWidth;
            int y0 = index0 / ipWidth;

            int x = x0 - xmin;
            int y = y0 - ymin;
            int index = y*w + x;

            float v = entry.getValue().floatValue();
//            System.out.println(x0 + " " + y0 + " " + index0 + " " + x + " " + y + " " + index + " " + w + " " + h);
            patchPixels[index] = v;
        }
        patchIp = new FloatProcessor(w, h, patchPixels);
        String z = Long.toString(System.currentTimeMillis());

    }

    /*
    Find the bright  spots in the image using Find Maxima - if more than one maxima is present, need to decide what
    happens.
     */
    public void findMaxima() {

//        ImagePlus t = new ImagePlus("test", patchIp);
//        t.show();
        MaximumFinder maxFinder = new MaximumFinder();
        double tol = stdDev;
        ByteProcessor mfip = maxFinder.findMaxima(patchIp, tol, 0, false);

        byte[] p = (byte[])mfip.getPixels();

        HashMap<Integer, Double>  map = new HashMap<>();
        for (int i = 0; i < p.length; i++) {
            //need to used != 0 because the max pixel value 255 is a negative byte
            if (p[i] != 0) {
                //System.out.println("GT0 " + " "  + i + " " + patchPixels[i]);
                map.put(i, (double)patchPixels[i]);
            }
        }

        maxMap = map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> {throw new AssertionError();},
                        LinkedHashMap::new
                ));

    }


    public boolean getIsGoodSpot() {
        return goodSpot;
    }

    public int getMeasureRegionIndex() {

        //the Integer key in this is the index in the patch
        Map.Entry<Integer, Double> entry = maxMap.entrySet().iterator().next();
        int index = entry.getKey();

        //need to transform back into the original image coordinates
        int xp = index % patchWidth;
        int yp = index / patchWidth;

        peakX = xp;
        peakY = yp;

        int x0 = xp + xmin;
        int y0 = yp + ymin;

        int index0 = y0*ipWidth + x0;
        System.out.println(x0 + " " + y0 + " " + xp + " " + yp + " " + xmin + " " + ymin);
        return index0;
    }

    public boolean decide() {
        boolean res = false;

        double minMaxVal = mean + 2.0*stdDev;
        double minSep = 2.0*stdDev;

        double maxVal = Double.MIN_VALUE;
        int mx;
        int my;
        int mindex = 0;
        if (maxMap.size() == 0) {
            res = false;
        } else  {

            int i = 0;
            for (Map.Entry<Integer, Double> ev : maxMap.entrySet()) {
                int index = ev.getKey();
                int x = index % patchWidth;
                int y = index / patchWidth;
                double v = ev.getValue();
                //System.out.print("<----> " + x + " " + y + " " + v);
                if (i == 0) {
                    maxVal = v;
                    //System.out.println();
                    if (maxVal < (mean + 2*stdDev)) {
                        res = false;

                        break;
                    } else {
                        res = true;
                    }
                    mindex = index;
                } else {
                    double dv = maxVal - v;
                    double d = distance(mindex, index, patchWidth);

                    //System.out.println(" " + d + " " + dv);
                    if ((d > 15) & (dv < (2*stdDev))) {
                        res = false;
                        break;
                    }
                }

                i++;
            }
        }

        goodSpot = res;
        return res;
    }

    public void getTop() {

        int i = 0;
        int index0 = 0;
        for (Map.Entry<Integer, Double> entry : sortedMap.entrySet()) {
            int index = entry.getKey();
            if (i == 0) {
                index0 = index;
            }
            int x = index % patchWidth;
            int y = index / patchWidth;
            double d = distance(index0, index, patchWidth);
            double v = entry.getValue();
            System.out.println(x + " " + y + " " + d + " " + v);
            i++;
            if (i > 25) break;
        }
    }


    protected double distance(int index1, int index2, int width) {

        int x1 = index1 % width;
        int y1 = index1 / width;
        int x2 = index2 % width;
        int y2 = index2 / width;

        double dxsq = (x2 -x1)*(x2 - x1);
        double dysq = (y2 -y1)*(y2 - y1);

        double d = Math.sqrt(dxsq + dysq);
        return d;

    }
}
