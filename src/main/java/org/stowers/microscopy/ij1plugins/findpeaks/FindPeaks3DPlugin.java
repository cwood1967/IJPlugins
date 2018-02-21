package org.stowers.microscopy.ij1plugins.findpeaks;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import net.imglib2.histogram.Integer1dBinMapper;
import net.imglib2.ops.parse.token.Int;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.stowers.microscopy.spotanalysis.Spot;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.Inflater;

import static java.util.Comparator.comparing;

/**
 * Created by cjw on 8/1/17.
 */

@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>Other>Find Peaks 3D")
public class FindPeaks3DPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter
    float tolerance;

    @Parameter
    float threshold;

    @Parameter(label="Minimum separation")
    float minsep;

    @Parameter(label="Z Sale")
    float zscale;

    @Parameter(label="Max number of peaks")
    int npeaks;

    @Parameter
    double smoothRadius;

    @Parameter(label="Measure spot intensity?")
    boolean doMeasure;

    RoiManager manager;
    Color[] colors;
    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public void run() {

        makeColors();
        if (npeaks < 1) {
            npeaks = Integer.MAX_VALUE;
        }

        if (minsep < 1) {
            minsep = 8;
        }

        FindPeaks3D peaks = new FindPeaks3D(imp, tolerance, threshold, minsep, npeaks, zscale, smoothRadius);
        List<FindPeaks3DLocalMax> peaksList = peaks.findpeaks();

        //put the peaks into rois
        manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
//        manager.reset();

        List<Float> xpoints = new ArrayList<>();
        List<Float> ypoints = new ArrayList<>();

        List<Float> allxpoints = new ArrayList<>();
        List<Float> allypoints = new ArrayList<>();

        ArrayList<PointRoi> rois = new ArrayList<>();
        int roiIndex = 0;
        int workingZ = peaksList.get(0).getZ();

        peaksList = peaksList.stream()
                .sorted(comparing(FindPeaks3DLocalMax::getMean3).reversed())
                .collect(Collectors.toList());

        for (FindPeaks3DLocalMax s : peaksList) {
            float x = s.getX() + 0.0f;
            float y = s.getY() + 0.0f;

//            float x = s.getCOM()[0];
//            float y = s.getCOM()[1];
            int z = s.getZ();
//            System.out.println(roiIndex + " " + z + " " + workingZ);
//            System.out.println("N: "+ s.getNeighborsList().size());

            if (z != workingZ) {
                drawRoi(xpoints, ypoints, workingZ, true);
                workingZ = z;
                xpoints.clear();
                ypoints.clear();
            }

            xpoints.add(x);
            ypoints.add(y);
            allxpoints.add(x);
            allypoints.add(y);
            roiIndex++;
        }

        writeTable(peaksList);

        drawRoi(xpoints, ypoints, workingZ, true);
        drawRoi(allxpoints, allypoints, 0, false);

        peaks.getRegionsStack().show();
        System.out.println(peaksList.size());
        System.out.println("Done");
        return;
    }

    private void writeTable(List<FindPeaks3DLocalMax> peaksList) {
        ResultsTable table = new ResultsTable();

        for (FindPeaks3DLocalMax s : peaksList) {
            table.incrementCounter();
            table.addValue("Image", imp.getTitle());
            table.addValue("X", s.getX());
            table.addValue("Y", s.getY());
            table.addValue("Z", s.getZ());
            table.addValue("NVoxels", s.getNeighborsList().size());
            table.addValue("Mean3", s.measureMean(3));
            table.addValue("Mean5", s.measureMean(5));
            table.addValue("VoxelValue", s.getValue());

        }
        table.show(imp.getTitle() + "_3D_Peaks");
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
            proi.setSize(4);
            proi.setPointType(3);
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
        manager.add(imp, proi, wz + 1);
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
