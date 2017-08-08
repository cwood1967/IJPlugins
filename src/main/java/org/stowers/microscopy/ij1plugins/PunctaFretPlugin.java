package org.stowers.microscopy.ij1plugins;

import org.scijava.plugin.Parameter;
import org.stowers.microscopy.pipelines.PunctaFret;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Created by cjw on 6/13/17.
 */

/*
    This plugin will use the pipelines/PunctaFret class to run one file
 */

@Plugin(type = Command.class, menuPath="Plugins>Chris>PunctaFret")
public class PunctaFretPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter
    boolean show;

    @Override
    public void run() {
        PunctaFret p = new PunctaFret(imp);
        p.setShow(show);
        p.run();
        System.out.print(p.getPunctaOutput());
        System.out.print(p.getCellOutput());
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    public static void main(String[] args) {
        System.out.println("Running");
        //final net.imagej.ImageJ imagej = net.imagej.Main.launch(args);
        org.scijava.AbstractGateway g = new org.scijava.SciJava();
        g.launch(args);
    }
}

