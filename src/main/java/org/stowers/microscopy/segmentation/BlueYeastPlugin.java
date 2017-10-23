package org.stowers.microscopy.segmentation;

/**
 * Created by cjw on 9/20/17.
 */

import ij.process.ImageProcessor;
import org.scijava.plugin.Parameter;
import org.stowers.microscopy.pipelines.PunctaFret;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;


@Plugin(type = Command.class, menuPath="Plugins>Chris>Segment Yeast Blue")
public class BlueYeastPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter(label="Method", choices={"Blue", "Cytoplasm"})
    String method;

    @Override
    public void run() {
        int c = imp.getChannel();
        imp.setSlice(c);
        ImageProcessor mask = null;

        if (method.equals("Blue")) {
            mask = SegmentYeast.segmentBlue(imp.getProcessor());
        } else if (method.equals("Cytoplasm")) {
            mask = SegmentYeast.segmentCyto(imp.getProcessor());
        } else return;

        ImagePlus maskImp = ij.IJ.createImage("Mask", imp.getWidth(), imp.getHeight(), 1, 8);
        maskImp.setProcessor(mask);
        maskImp.setTitle("Mask");
        maskImp.show();

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }


}

