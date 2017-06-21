package org.stowers.microscopy.ijplugins.utils;

import ij.process.ImageProcessor;
import org.stowers.microscopy.pipelines.PunctaFret;

import java.security.Key;
import java.util.*;

/**
 * Created by cjw on 6/19/17.
 */
public class PunctaPatch extends AbstractPatch {


    int cellId;
    String filename;

    HashMap<Integer, PunctaMeasurement>  map;

    float cellsum;
    float cellarea;
    float allpunctasum;
    float allpunctasize;

    public PunctaPatch(int cellId, ImageProcessor oip, int x, int y, int width, int height) {
        super(oip, x, y, width, height);
        ip = makePatch();
        this.cellId = cellId;
        System.out.println("---------");
    }

    public class PunctaMeasurement {
        int regionId;

        float sum;
        float mean;
        float xc;
        float yc;
        float size;
        HashMap<Integer, Float> ivMap;

        public PunctaMeasurement(int regionId) {
            this.regionId = regionId;
            ivMap = new HashMap<>();
        }

        public void addToIVMap(int index, float value) {

            ivMap.put(index, value);
        }

        public void calcStats() {

            size  = ivMap.size();
            sum = 0.f;
            xc =0;
            yc = 0;
            for (Map.Entry<Integer, Float> kv : ivMap.entrySet()) {
                sum += kv.getValue();
                int kx = kv.getKey() % width;
                int ky = kv.getKey() / width;
                xc += kx;
                yc += ky;
            }

            xc = (1.f*xc) /size;
            yc = (1.f*yc) /size;

            mean = sum/size;
//            System.out.println(regionId + " "  +xc + " " + yc + " " + sum + " " + mean + " " + size);
        }

    }

    @Override
    public ImageProcessor  makePatch() {

        ImageProcessor zip = null;
        oip.setRoi(x, y, width, height);
        zip = oip.crop().convertToFloatProcessor();
        zip = zip.convertToFloatProcessor();

        return zip;
    }

    public void measureFromLabeledRegions(ImageProcessor regionsIp) {

        int nregions = (int)(regionsIp.getStatistics().max + 0.001);
        short[] pixels = (short[])regionsIp.getPixels();

        float[] ipPixels = (float[])ip.getPixels();


        map = new HashMap<>();
        for (int i = 1; i <= nregions; i++) {

            PunctaMeasurement pm = new PunctaMeasurement(i);
            map.put(i, pm);
        }

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] > 0) {
                int m = pixels[i];
                PunctaMeasurement pm = map.get(m);
                pm.addToIVMap(i, ipPixels[i]);

            }
        }

        allpunctasum = 0;
        allpunctasize = 0;
        for (int k : map.keySet()) {
            PunctaMeasurement pm = map.get(k);
            pm.calcStats();
            allpunctasum += pm.sum;
            allpunctasize += pm.size;
        }

//        System.out.println(allpunctasum + " " + allpunctasize+ " " + allpunctasum/allpunctasize);

    }

    public void measureFromCellMask(ImageProcessor maskIp) {

        byte[] maskPixels = (byte[]) maskIp.getPixels();
        float[] ipPixels = (float[])ip.getPixels();

        cellsum = 0;
        cellarea = 0;
        for (int i = 0; i < maskPixels.length; i++) {
            if (maskPixels[i] != 0) {
                cellsum += ipPixels[i];
                cellarea++;
            }
        }

//        System.out.println(cellarea + " " + cellsum + " " + cellsum/cellarea);
    }

    public ArrayList<String> makePunctaOutput() {

        ArrayList<String> res = new ArrayList<>();

        for (Map.Entry<Integer, PunctaMeasurement> entry : map.entrySet()) {
            int id = entry.getKey();
            PunctaMeasurement p = entry.getValue();
            //id, sum, mean, xc, yc, size
            String line = String.format("%12d, %16.3f, %12.3f, %12.3f, %12.3f, %12.3f",
                    p.regionId, p.sum, p.mean, p.size, p.xc, p.yc);
            System.out.println(line);
        }

        return res;
    }

    public String makeCellOutput() {
        //id, sum, mean, area, allpunctasum, allpuncatmean, allpunctasize
        String line = String.format("%12d, %16.3f, %12.3f, %12.3f, %12.3f, %12.3f, %12.3f, %12d",
                cellId, cellsum, cellsum/cellarea, cellarea,
                allpunctasum, allpunctasum/allpunctasize, allpunctasize, map.size());

        System.out.println(line);
        return line;
    }
}
