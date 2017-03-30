package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 3/13/17.
 */
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import ij.*;
import ij.gui.MessageDialog;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import net.imagej.*;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Pixels To Text")
public class PixelsToText implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter(label="Put in results table?", callback = "resultsChanged")
    boolean inResults;

    @Parameter(required=false, label="Select Output Directory", style="directory")
    File outputDir;

    String strDir;

    @Override
    public void run() {


        String name = imp.getTitle();
        Path outputFile = null;
        if (!outputDir.exists() || outputDir == null) {
            inResults = true;
        } else {

            outputFile = Paths.get(outputDir + "/" + name + ".txt");
        }

        float[] pixels = (float[])(imp.getProcessor().convertToFloatProcessor().getPixels());

        float[] outPixels;
        if (imp.getRoi() != null) {
            Point[] points = imp.getRoi().getContainedPoints();
            outPixels = new float[points.length];
            int i = 0;
            for (Point p : points) {
                int x = (int)p.getX();
                int y = (int)p.getY();
                int index = imp.getWidth()*y + x;
                outPixels[i] = pixels[index];
                i++;
            }
            System.out.println(points.length);
        } else {
            outPixels = pixels;
        }

        if (inResults) {
            writeResults(outPixels);
        } else {
            writePixels(outputFile, outPixels);
        }

    }

    protected void resultsChanged() {

        if (inResults == true) {
            //strDir = outputDir.getAbsolutePath();
            outputDir = null;
        }
        else {
            outputDir = new File(strDir);
        }
    }

    private void writeResults(float[] outPixels) {

        ResultsTable table = new ResultsTable();
        
        for (int i = 0; i < outPixels.length; i++) {
            table.incrementCounter();
            float pix = outPixels[i];
            String pout = String.format("%10.2f\n" , pix);
            table.addValue("Value", pix);
        }
        table.show("Pixel Values");
    }

    private void writePixels(Path outputFile, float[] outPixels) {

        Charset charset = Charset.forName("US-ASCII");
        try {
            BufferedWriter writer = Files.newBufferedWriter(outputFile, charset);

            String pout;
            for (int i = 0; i < outPixels.length; i++) {
                float pix = outPixels[i];
                pout = String.format("%10.2f\n" , pix);
                writer.write(pout);
            }

            writer.close();
        }
        catch (IOException e) {
            IJ.log("Can't create the output file");
        }
    }
    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}

