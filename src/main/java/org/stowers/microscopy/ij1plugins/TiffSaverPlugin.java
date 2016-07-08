package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 6/22/16.
 */


import java.nio.file.Paths;

import ij.*;
import ij.io.FileInfo;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>Save As Tiff")
public class TiffSaverPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    private String filename;

    FileInfo fi = null;

    String name;

    @Override
    public void run() {
        ij.io.SaveDialog sd = new ij.io.SaveDialog("Save file", name, "");
        String dirname = sd.getDirectory();
        filename =  Paths.get(sd.getDirectory(), sd.getFileName()).toString();

        FastFileSaver f = new FastFileSaver(imp);
        f.saveAsTiff(filename);
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }



}
