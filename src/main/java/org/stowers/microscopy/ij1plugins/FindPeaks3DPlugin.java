package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.*;
import java.util.List;

/**
 * Created by cjw on 8/1/17.
 */

@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>Find Peaks 3D")
public class FindPeaks3DPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter
    float tolerance;

    @Parameter
    float threshold;

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public void run() {

        FindPeaks3D peaks = new FindPeaks3D(imp, tolerance, threshold);
        List<FindPeaks3DLocalMax> peaksList = peaks.findpeaks();
        RoiManager manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
        for (FindPeaks3DLocalMax p : peaksList) {
            int x = p.getX();
            int y = p.getY();
            int z = p.getZ();
            System.out.println(p.toString());
            OvalRoi roi = new OvalRoi(x - 4, y - 4, 8, 8);
//            roi.setSize(4);
//            roi.setPointType(3);
            roi.setStrokeColor(new Color(255, 0, 0));
            roi.setPosition(z + 1);
            String strSlice = String.format("%04d-%04d-%04d", z + 1, x, y);
//            roi.setName(strSlice);
            manager.add(imp, roi, z + 1);

        }
        System.out.println("Done");
        return;
    }
}
