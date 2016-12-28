package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 12/12/16.
 */

import ij.IJ;
import ij.WindowManager;
import ij.plugin.Thresholder;
import ij.plugin.filter.MaximumFinder;
import ij.process.AutoThresholder;
import ij.process.ByteProcessor;
import ij.text.TextPanel;
import net.imagej.patcher.LegacyInjector;
import net.imagej.ImageJ;
import net.imagej.ops.Ops;
import ij.measure.ResultsTable;
import net.imglib2.ops.parse.token.Int;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import org.stowers.microscopy.ij1plugins.tableutils.ResultsUtils;
import org.stowers.microscopy.org.stowers.microscopy.utils.PombeSpot;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

import static ij.WindowManager.getImageTitles;


@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>NotReady>PombeSpots")
public class PombeSpots implements  Previewable, Command  {

	static {
        LegacyInjector.preinit();
    }
	
    @Parameter
    ImagePlus sumImp = null;
    
    @Parameter
    ImagePlus maxImp = null;

    @Parameter(label = "Radius of large blur")
    double largeRadius;
    double blurredThreshold;

    @Parameter(label = "Dot number")
    int dotnum;

    int channel;
    int patchSize = 20;

    ImageProcessor blurredIp;

    ArrayList<PombeSpot> spots;
    @Override
    public void run() {
        //assume the channel is the active channel
//        System.out.println(imp.getCurrentSlice());
        String[] titles = WindowManager.getImageTitles();

        for (int i = 0; i < titles.length; i++) {
            String name = titles[i];
            if (name.startsWith("MAX")) {
                maxImp = WindowManager.getImage(name);
                String last = name.substring(5);
                for (int j = 0; j < titles.length; j++) {
                    if (j == i) continue;
                    String jname = titles[j];
                    if (jname.startsWith("SUM")) {
                        if (jname.endsWith(last)) {
                            sumImp = WindowManager.getImage(jname);
                            break;
                        }
                    }
                }
                if (sumImp != null) {
                    break;
                }
            }
        }

        sumImp.setSlice(2);
        maxImp.setSlice(2);
        channel = maxImp.getCurrentSlice();
        blurredIp = blur(largeRadius);

        blurredThreshold = calcThreshold("Otsu", blurredIp);

        ArrayList<PombeSpot> spots = findMaxima(blurredIp);

        int k = 0;
        HashMap<Integer, Integer> peakMap = new HashMap<>();

        ResultsTable table = new ResultsTable();

        table.setDefaultHeadings();
        for (PombeSpot spot : spots) {
//            System.out.print(k + " -- ");
            ArrayList<Integer> peakList = spot.findPeaks();
            Integer key  = spot.getNpeaks();
            int sx = spot.getX();
            int sy = spot.getY();
            float nucSum = spot.sumNucleus();

            if (nucSum < 1.0) {
                continue;
            }
            if (spot.getMaskArea() < spot.getNpeaks()*18) {
                continue;
            }
            String part1 = String.format("%8d, %8d, %8d,  %8d, %8.1f", k, key, sx, sy, nucSum);
            String part2 = "";

            table.incrementCounter();
            table.addValue("Id", k);
            table.addValue("NPeaks", key);
            table.addValue("X", sx);
            table.addValue("Y", sy);
            table.addValue("Nuclear Area", spot.getMaskArea());
            table.addValue("Peak Area", key*9.);
            table.addValue("Nuc - Peak Area", spot.getMaskArea()- key*9.);
            table.addValue("Nuc Sum", nucSum);
            table.addValue("(Nuc - Peaks) Avg", 0.);
            table.addValue("Minus Peaks" ,0);
            int pk = 0;
            float asum = 0;
            for (int p : peakList){
                part2 += String.format(", %10.2f" , spot.getPeakSum(p));
                String pnum = "Peak " + Integer.toString(pk);
                table.addValue(pnum, spot.getPeakSum(p));
                asum += spot.getPeakSum(p);
                pk++;
            }

            table.setValue("Minus Peaks", table.getCounter() - 1, nucSum - asum);
            table.setValue("(Nuc - Peaks) Avg", table.getCounter() - 1, (nucSum - asum)/(spot.getMaskArea()- key*9.));
            System.out.println(part1 + part2);
//            Integer key  = spot.getNpeaks();
            if (peakMap.containsKey(key)) {
                peakMap.put(key, peakMap.get(key) + 1);
            } else {
                peakMap.put(key, 1);
            }

            k++;
        }

        table.show("The Results");

        for (int key : peakMap.keySet()) {
            System.out.println(key + " " + peakMap.get(key));
        }
//        System.out.println(spots.size() + " " + blurredThreshold);
//        PombeSpot s1 = spots.get(dotnum);
//        int x = s1.getX();
//        int y = s1.getY();
//        System.out.println(x + " " + y);
//        ImageProcessor cip = s1.getMaxPatch();
//
//        ImagePlus cImp = new ImagePlus("Temp Patch", cip);
//        maxImp.setRoi(x -10,y -10, 20,20);
//        cImp.show();
//
//        ImagePlus mImp = new ImagePlus("Mask Patch", s1.getMaskPatch());
//        mImp.show();
//
        ImagePlus bImp = new ImagePlus("Blur Patch", blurredIp);
        bImp.show();
//
//        ImagePlus sImp = new ImagePlus("Blur Patch", s1.getSumPatch());
//        sImp.show();
//
//        s1.findPeaks();

    }

    protected ImageProcessor blur(double radius) {
        ImageProcessor resIp = maxImp.getProcessor().duplicate();
        GaussianBlur blur = new GaussianBlur();
        blur.blurGaussian(resIp, radius);
        return resIp;
    }

    protected double calcThreshold(String method, ImageProcessor ip) {

        AutoThresholder auto = new AutoThresholder();
        int thresh = auto.getThreshold(method, ip.getStatistics().histogram);
        double a1 = ip.getStatistics().histMin;
        double a2 = ip.getStatistics().histMax;
        double d = (a2 - a1)/256.;
        System.out.println(a1 + " " + a2 + " " + d + " " + thresh);
        return a1 + d*(1. +thresh);

    }

    protected ArrayList<PombeSpot> findMaxima(ImageProcessor ip) {

        MaximumFinder maxFinder = new MaximumFinder();
        ByteProcessor mfip = maxFinder.findMaxima(ip, 1., 0, false);

        byte[] pixels = (byte[])mfip.getPixels();
        int np = pixels.length;

        ArrayList<PombeSpot> spots = new ArrayList();
        for (int i = 0; i < np; i++) {
            int x = i % mfip.getWidth();
            int y = i / mfip.getWidth();
            if (pixels[i] != 0) {
                PombeSpot dot = new PombeSpot(x, y, blurredThreshold, maxImp.getProcessor(), ip, patchSize);
                dot.setSumPatch(sumImp.getProcessor());
                spots.add(dot);
            }
        }

        return spots;
    }



    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public static void main(String[] args) {

        final ImageJ imagej = net.imagej.Main.launch(args);

        String dir = "/Volumes/projects/jjl/public/lili pan/12082016/";
        String file = "MAX UNMIXED pp1735-6h-tile-_2016_12_08__16_49_40.lsm.tif";
        String image = dir + file;
//        ij.io.Opener opener = new ij.io.Opener();
//        opener.open(image);

//        imagej.command().run(PombeSpots.class, true);

    }
}
