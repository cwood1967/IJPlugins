package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 3/13/17.
 */
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
import ij.io.FileInfo;
import net.imagej.*;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.awt.Frame;

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Pixels To Text")
public class PixelsToText implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter(label="Select Ouput Directory", style="directory")
    File outputDir;



    @Override
    public void run() {

        Charset charset = Charset.forName("US-ASCII");
        String name = imp.getTitle();
        Path outputFile = Paths.get(outputDir + "/" + name + ".txt");
        float[] pixels = (float[])(imp.getProcessor().convertToFloatProcessor().getPixels());

        try {
            BufferedWriter writer = Files.newBufferedWriter(outputFile, charset);

            String pout;
            for (int i = 0; i < pixels.length; i++) {
                float pix = pixels[i];
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

