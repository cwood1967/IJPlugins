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
 * Created by cjw on 11/9/17.
 */

public class LittleOnBigBatch {
    @Parameter(label="Pick a folder for images", style="directory")
    File inputDir;

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

    public void run() {

    }
}
