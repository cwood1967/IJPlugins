package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 10/11/16.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.ImageStack;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageStatistics;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>ColorChange")
public class ColorChangePlugin implements Previewable, Command {

    @Parameter
    ImagePlus imp;

    @Override
    public void run() {

        Roi roi = imp.getRoi();
        if (roi == null) {
            IJ.log("Select a rectangular ROI");
            return;
        }

        ImagePlus rimp = roi.getImage();
        double mean = rimp.getProcessor().getStatistics().mean;

        replaceColor((int)mean);
    }


    public void replaceColor(int mean) {

        ImageStack stack = imp.getStack();

        int n = stack.getSize();

        for (int i = 0; i < n; i++) {

        }
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
