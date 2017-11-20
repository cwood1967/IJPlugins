package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.io.Opener;
import ij.plugin.frame.RoiManager;
import org.apache.bcel.generic.FNEG;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by cjw on 8/11/17.
 */
@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>Little on Big")
public class LittleOnBigPlugin implements Command, Previewable {

    @Parameter(label="Pick a folder for images", style="directory")
    File inputDir;

    @Parameter(label="Run batch?")
    Boolean isRunBatch;

    // Parameters for the cells with a few big spots

    @Parameter(label="Pattern for big name")
    String bigNamePattern;

    @Parameter
    float howClose;

    @Parameter
    float bigTol;

    @Parameter
    float bigThresh;

    @Parameter
    float bigMinSep;

    @Parameter
    int bigNpeaks;

    @Parameter
    float bigZscale;

    @Parameter
    float bigSmoothRadius;

    //Parameter for a cell with lots of small spots
    @Parameter
    float smallTol;

    @Parameter
    float smallThresh;

    @Parameter
    float smallMinSep;

    @Parameter
    int smallNpeaks;

    @Parameter
    float smallZscale;

    @Parameter
    float smallSmoothRadius;

    RoiManager manager;

    Color[] colors;


    @Override
    public void run() {

        makeColors();

        if (isRunBatch) {
            runBatch();
        } else {
            runSingle(inputDir);
        }


    }

    private LittleOnBig runSingle(File dir) {

        ImagePlus bigImp = null;
        ImagePlus smallImp = null;

        LittleOnBig x =x = new LittleOnBig(howClose);
        String[] files = dir.list();
        System.out.println(dir.getAbsolutePath());
        for (int i = 0; i < files.length; i++) {
            if (files[i].toLowerCase().contains(bigNamePattern)) {
                String filename = dir + "/" + files[i];
                Opener opener = new Opener();
                bigImp = opener.openImage(filename);
            } else if (files[i].toLowerCase().contains("htr")) {
                String filename = dir + "/" + files[i];
                Opener opener = new Opener();
                smallImp = opener.openImage(filename);
            }
        }

        if (bigImp == null) return null;
        if (smallImp == null) return null;

        x.setBigPeaks(bigImp, bigTol, bigThresh, bigMinSep, bigNpeaks, bigZscale, bigSmoothRadius);
        x.setSmallPeaks(smallImp, smallTol, smallThresh, smallMinSep, smallNpeaks, smallZscale, smallSmoothRadius);
        x.go();


        if (!isRunBatch) {
            smallImp.show();
            bigImp.show();
            manager = RoiManager.getInstance();
            if (manager == null) {
                manager = new RoiManager();
            }
            processToRoi(bigImp, x);
        }
        return x;
    }

    private void runBatch() {

        File[] dirs = inputDir.listFiles();

        Map<String, LittleOnBig> resMap = new HashMap<>();
        for (File d : dirs) {
            if (d.isDirectory()) {
                LittleOnBig res = runSingle(d);
                if (res != null) {
                    resMap.put(d.getName(), res);
                    batchOutput(res, d);
                }
            }
        }

        for (Map.Entry<String, LittleOnBig> mb : resMap.entrySet()) {
            Map<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> c = mb.getValue().getCloseMap();
            for (Map.Entry<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> e : c.entrySet()) {
                System.out.println(mb.getKey() +  " " + e.getKey().getVoxelIndex() +" " + e.getValue().size());
            }
        }

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    private void batchOutput(LittleOnBig res, File dir) {

        Path outdir = Paths.get(dir.getAbsolutePath());
        String outfilename = outdir + "/" + bigNamePattern + ".csv";
        Path outfile = Paths.get(outfilename);
        try {
            BufferedWriter writer = Files.newBufferedWriter(outfile);
            Map<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> map = res.getCloseMap();
            System.out.println(outfile);
            for (Map.Entry<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> e : map.entrySet()) {

                FindPeaks3DLocalMax big = e.getKey();
                List<FindPeaks3DLocalMax> smallSpots = e.getValue();

                float cx = big.getCOM()[0];
                float cy = big.getCOM()[1];
                float cz = big.getCOM()[2];

                int rx = big.getX();
                int ry = big.getY();
                int rz = big.getZ();

                int nspots = smallSpots.size();

                int size = big.getObjectVoxels().size();
                float v = big.getSumIntensity();
                String out = String.format("%s,%4d, %4d, %4d, %8.2f, %8.2f, %8.2f, %8.1f, %4d, %4d\n",
                                            dir, rx, ry, rz, cx, cy, cz, v, size, nspots);

                System.out.print(out);
                writer.write(out);
            }

            writer.flush();
            writer.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void processToRoi(ImagePlus rImp, LittleOnBig x) {

        //change this for a map that has (FindPeaks3Dlocalmax, list(FindPeaks3DLocalMax)

        Map<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> map = x.getCloseMap();
        //Map<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>> map = x.getDistMap();

        for (Map.Entry<FindPeaks3DLocalMax, List<FindPeaks3DLocalMax>> e:
                map.entrySet())
            {
                // the list of small spots
                List<FindPeaks3DLocalMax> v = e.getValue();

//                Iterator<Map.Entry<FindPeaks3DLocalMax, Float>> iter = v.iterator();

                //get the com of the big spot
                List<Float> xpoints = new ArrayList<>();
                List<Float> ypoints = new ArrayList<>();
                xpoints.add(e.getKey().getCOM()[0]);
                ypoints.add(e.getKey().getCOM()[1]);
                xpoints.add((float)e.getKey().getX());
                ypoints.add((float)e.getKey().getY());
                System.out.println("***");
                System.out.println(e.getKey().getCOM()[0] +
                        " " + e.getKey().getCOM()[1] + " "  +e.getKey().getCOM()[2] + " " +
                        e.getKey().getFoundRadius() + " " + e.getKey().getDx() + " " +
                        e.getKey().getDy() + "  " + e.getKey().getDz() + " " +
                        e.getKey().getObjectVoxels().size());
                System.out.println(e.getKey().getX()  + " " + e.getKey().getY());
                for (FindPeaks3DLocalMax p : v) {
//                    Map.Entry<FindPeaks3DLocalMax, Float> p = iter.next();

                    float[] com= p.getCOM();
//                    xpoints.add(com[0] + .5f);
//                    ypoints.add(com[1] + .5f);
                    xpoints.add((float)p.getX());
                    ypoints.add((float)p.getY());
//                    System.out.println("--   " + com[0] + " " + com[1] + " " + com[2] + " " + p.getValue());
                    System.out.println("--   " + p.getX() + " " + p.getY() + " " + p.getZ() + " " +
                            p.getSumIntensity());
                }
                drawRoi(rImp, xpoints, ypoints, e.getKey().getZ(), true);
        }

    }

    private void drawRoi(ImagePlus rImp, List<Float> xpoints, List<Float> ypoints, int wz, boolean markZ) {
        float[] fx = new float[xpoints.size()];
        float[] fy = new float[ypoints.size()];
        for (int i = 0; i < fx.length; i++) {
            fx[i] = xpoints.get(i);
            fy[i] = ypoints.get(i);
        }
        PointRoi proi = new PointRoi(fx, fy);
        if (markZ) {
            proi.setSize(2);
            proi.setPointType(4);
            proi.setPosition(wz + 1);
        } else {
            proi.setSize(1);
            proi.setPointType(2);
        }
        //System.out.println(workingZ + " " + colors.length + " " + workingZ % colors.length);
//        proi.setStrokeColor(colors[wz % colors.length]);
        proi.setStrokeColor(colors[2]);
        proi.setStrokeWidth(1);
        String strSlice;
        if (markZ) {
            strSlice = String.format("%03d", wz + 1);
        }
        else {
            strSlice = "All points";
        }
        String name = strSlice;
        proi.setName(name);
        if (!markZ) {
            Overlay ov = new Overlay(proi);
        }
        manager.add(rImp, proi, wz + 1);
    }

    private void makeColors() {

        colors = new Color[6];
        colors[0] = new Color(255, 0, 0);
        colors[1] = new Color(0, 255, 0);
        colors[2] = new Color(100, 100, 255);
        colors[2] = new Color(255, 255, 0);
        colors[3] = new Color(255, 0, 255);
        colors[4] = new Color(255, 127, 0);
        colors[5] = new Color(0, 255, 255);
    }
}
