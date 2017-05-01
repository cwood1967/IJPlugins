package org.stowers.microscopy.org.stowers.microscopy.utils;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

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
    HashMap<String, Double> statsMap;
    LinkedHashMap<Integer, Double> sortedMap;

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
    int width;
    int height;
    public NucleusPixels(ImageProcessor ip, List<Integer> indexList) {
        this.ip = ip; //ip.convertToFloatProcessor();
        this.pixels = (float[])ip.getPixels();
        this.indexList = indexList;
        this.statsMap = new HashMap<>();
        calcStats();
        makePatch();
//        getTop();
    }

    public void calcStats() {

        height =ip.getHeight();
        width = ip.getWidth();

        sum = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;

        xmin = width;
        ymin = height;
        xmax = 0;
        ymax = 0;
        HashMap<Integer, Double> pixelMap = new HashMap<>();
        for (int index : indexList) {
            double v = pixels[index];
            int y = index/width;
            int x = index % width;
            if (x < xmin) xmin = x;
            if (y < ymin) ymin = y;
            if (x > xmax) xmax = x;
            if (y > ymax) ymax = y;
            pixelMap.put(index, v);
            sum += v;
            if (v < min) min = v;
            if (v > max) max = v;
        }

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

        System.out.print(indexList.size() + " " );
        System.out.print(sum + ", ");
        System.out.print(min + ", ");
        System.out.print(max + ", ");
        System.out.print(mean + ", ");
        System.out.print(stdDev + ", ");
        System.out.println(median + ", ");

        System.out.print(xmin + ", ");
        System.out.print(xmax + ", ");
        System.out.print(ymin + ", ");
        System.out.println(ymax + ", ");

        System.out.println(sortedMap.get(sortedMap.keySet().iterator().next()));
    }


    protected void makePatch() {

        int w = xmax - xmin + 1;
        int h = ymax - ymin + 1;

        float[] p = new float[w*h];

        for (Map.Entry<Integer, Double> entry : sortedMap.entrySet()) {
            int index0 = entry.getKey();
            int x0 = index0 % width;
            int y0 = index0 / width;

            int x = x0 - xmin;
            int y = y0 - ymin;
            int index = y*w + x;

            float v = entry.getValue().floatValue();
//            System.out.println(x0 + " " + y0 + " " + index0 + " " + x + " " + y + " " + index + " " + w + " " + h);
            p[index] = v;
        }
        ImageProcessor pip = new FloatProcessor(w, h, p);
        String z = Long.toString(System.currentTimeMillis());
        ImagePlus pimp = new ImagePlus(z, pip);
        pimp.show();

    }

    public void getTop() {

        int i = 0;
        int index0 = 0;
        for (Map.Entry<Integer, Double> entry : sortedMap.entrySet()) {
            int index = entry.getKey();
            if (i == 0) {
                index0 = index;
            }
            int x = indexToX(index);
            int y = indexToY(index);
            double d = distance(index0, index);
            double v = entry.getValue();
            System.out.println(x + " " + y + " " + d + " " + v);
            i++;
            if (i > 25) break;
        }
    }

    protected int indexToX(int index) {
        int x = index % width;
        return x;
    }

    protected int indexToY(int index) {
        int y = index / width;
        return y;
    }

    protected double distance(int index1, int index2) {

        int x1 = indexToX(index1);
        int y1 = indexToY(index1);
        int x2 = indexToX(index2);
        int y2 = indexToY(index2);

        double dxsq = (x2 -x1)*(x2 - x1);
        double dysq = (y2 -y1)*(y2 - y1);

        double d = Math.sqrt(dxsq + dysq);
        return d;

    }
}
