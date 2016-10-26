package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import net.imagej.ImageJ;

import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Properties;


/**
 * Created by cjw on 5/2/16.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>NotReady>ClickForRoi")
public class ClickForRoi
        implements MouseListener, Previewable, Command {

//
    @Parameter
    ImagePlus imp;

    @Parameter(label="Roi Size")
    int roiSize;

    int roiWidth = 20;
    int roiHeight = 20;

    ImageWindow window;
    ImageCanvas canvas;

    RoiManager manager;
    public void run() {
        
        window = imp.getWindow();
        canvas = window.getCanvas();
        canvas.addMouseListener(this);

        manager = RoiManager.getInstance();
        if (manager == null) {
            manager = new RoiManager();
        }
        manager.setVisible(true);
    }

    private void drawRoi(double x, double y) {

        int x0 = (int)x - roiSize/2;
        int y0 = (int)y - roiSize/2;
        Roi roi = new Roi(x0, y0, roiSize, roiSize);

        manager.add(imp, roi, 1);
        manager.runCommand(imp, "Show All");

    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void mouseClicked(MouseEvent e) {

        double x = canvas.getCursorLoc().getX();
        double y = canvas.getCursorLoc().getY();
        drawRoi(x, y);
    }

    @Override
    public void mousePressed(MouseEvent e) {


    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public static void main(String[] args) {
        System.out.println("Running");
        final ImageJ imagej = net.imagej.Main.launch(args);
    }


}
