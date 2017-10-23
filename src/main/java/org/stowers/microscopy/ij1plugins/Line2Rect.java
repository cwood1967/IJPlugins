package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 10/23/17.
 */

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import ij.gui.Roi;
import ij.gui.Line;
import ij.gui.RotatedRectRoi;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>Line To Rectangle")
public class Line2Rect implements Previewable, Command {

    @Parameter
    ImagePlus imp;

    @Parameter(label="Length of the roi")
    int size;

    @Override
    public void run() {

        Roi roi = imp.getRoi();
         // line roi type == 5
        if (roi.getType() != 5) {
            ij.IJ.log("Please use a line roi");
            return;
        }

        Line line = (Line)roi;

        double x1, x2, y1, y2;
        double xa = line.x1d;
        double ya = line.y1d;
        double xb = line.x2d;
        double yb = line.y2d;

        if (xa < xb) {
            x1 = xa;
            y1 = ya;
            x2 = xb;
            y2 = yb;
        } else {
            x1 = xb;
            y1 = yb;
            x2 = xa;
            y2 = ya;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;

        double d = Math.sqrt(dx*dx + dy*dy);
        double m = dy/dx;

        double xp = 0.;
        double yp = size/2.;
        double xpb = d;
        double ypb = size/2.;



        double theta = -Math.atan2(dy, dx);

        double xp1 = x1 + xp*Math.cos(theta) + yp*Math.sin(theta);
        double yp1 = y1 - xp*Math.sin(theta) + yp*Math.cos(theta);

        double xp2 = x2 + xp*Math.cos(theta) + yp*Math.sin(theta);
        double yp2 = y2 - xp*Math.sin(theta) + yp*Math.cos(theta);


        System.out.println(theta + " " + xp1 + "  " + yp1);
        System.out.println(theta + " " + xp2 + "  " + yp2);

        RotatedRectRoi rr = new RotatedRectRoi(xp1, yp1, xp2, yp2, size);
        imp.setRoi(rr);

        double mn = -1./m;


    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
