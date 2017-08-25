package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.io.Opener;
import ij.plugin.frame.RoiManager;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Created by cjw on 8/11/17.
 */
@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>Little on Big")
public class LittleOnBigPlugin implements Command, Previewable {

    @Parameter(label="Pick a folder for images", style="directory")
    File inputDir;

    // Parameters for the cells with a few big spots

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

    RoiManager manager;

    Color[] colors;

    ImagePlus bigImp;
    ImagePlus smallImp;

    LittleOnBig x;
    @Override
    public void run() {

        String[] files = inputDir.list();

        for (int i = 0; i < files.length; i++) {
            if (files[i].toLowerCase().contains("coilin")) {
                String filename = inputDir + "/" + files[i];
                Opener opener = new Opener();
                bigImp = opener.openImage(filename);
            } else if (files[i].toLowerCase().contains("htr")) {
                String filename = inputDir + "/" + files[i];
                Opener opener = new Opener();
                smallImp = opener.openImage(filename);

            }
        }

        x = new LittleOnBig(howClose);
        x.setBigPeaks(bigImp, bigTol, bigThresh, bigMinSep, bigNpeaks, bigZscale);
        x.setSmallPeaks(smallImp, smallTol, smallThresh, smallMinSep, smallNpeaks, smallZscale);
        x.go();
        smallImp.show();
        bigImp.show();

        makeColors();
        manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }

        processToRoi();

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }


    private void processToRoi() {

        Map<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>> map = x.getDistMap();

        for (Map.Entry<FindPeaks3DLocalMax, SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>>> e:
                map.entrySet())
            {
                SortedSet<Map.Entry<FindPeaks3DLocalMax, Float>> v = e.getValue();

                Iterator<Map.Entry<FindPeaks3DLocalMax, Float>> iter = v.iterator();
                List<Float> xpoints = new ArrayList<>();
                List<Float> ypoints = new ArrayList<>();
                xpoints.add(e.getKey().getCOM()[0]);
                ypoints.add(e.getKey().getCOM()[1]);
                while (iter.hasNext()) {
                    Map.Entry<FindPeaks3DLocalMax, Float> p = iter.next();
                    float[] com= p.getKey().getCOM();
                    xpoints.add(com[0] + .5f);
                    ypoints.add(com[1] + .5f);
                }
                drawRoi(xpoints, ypoints, e.getKey().getZ(), true);
        }

    }

    private void drawRoi(List<Float> xpoints, List<Float> ypoints, int wz, boolean markZ) {
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
        manager.add(bigImp, proi, wz + 1);
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
