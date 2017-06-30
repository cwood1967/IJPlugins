package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 6/27/17.
 */

import ij.io.Opener;
import loci.common.DebugTools;
import org.scijava.plugin.Parameter;
import org.stowers.microscopy.pipelines.PunctaFret;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

import ij.ImagePlus;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Plugin(type = Command.class, menuPath="Plugins>Chris>Puncta Fret Batch")
public class PunctaFretBatch implements Command, Previewable {

    @Parameter(label="Pick a folder for images", style="directory")
    File inputDir;

    @Parameter(label="Pick an output", style="directory")
    File outputDir;


    @Override
    public void run() {

        DebugTools.setRootLevel("WARN");
        String[] files = inputDir.list();

        String punctFilename = outputDir + "/" + "punctaData.csv";
        String cellFilename = outputDir + "/" + "cell Data.csv";
        Path punctaPath = Paths.get(punctFilename);
        Path cellPath = Paths.get(cellFilename);

        try {
            BufferedWriter punctaWriter = Files.newBufferedWriter(punctaPath);
            BufferedWriter cellWriter = Files.newBufferedWriter(cellPath);

            for (int i = 0; i < files.length; i++) {
                if (files[i].endsWith("tiff")) {
                    String filename = inputDir + "/" + files[i];
                    Opener opener = new Opener();
                    ImagePlus imp = opener.openImage(filename);
                    PunctaFret p = new PunctaFret(imp);
                    p.setShow(false);
                    p.run();
                    if (i == 0) {
                        punctaWriter.write(p.getPunctaHeader());
                        cellWriter.write(p.getCellHeader());
                    }
                    if (p.getGood()) {
                        punctaWriter.write(p.getPunctaOutput());
                        cellWriter.write(p.getCellOutput());
                    }
                }
            }
            punctaWriter.close();
            cellWriter.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
        }
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
