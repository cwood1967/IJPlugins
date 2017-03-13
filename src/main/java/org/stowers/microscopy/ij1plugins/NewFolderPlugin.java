package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 3/8/17.
 */


import java.io.File;
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

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Stowers>Chris>NewFolder")
public class NewFolderPlugin implements Command, Previewable {

    @Parameter
    File path;

    @Parameter
    String newDir;

    @Override
    public void run() {

        String slash = File.separator;
        if (!path.isDirectory()) {
            IJ.log(path.getAbsolutePath() + " is not a directory");
            return;
        }
        if (!path.exists()) {
            IJ.log(path.getAbsolutePath() + " does not exist");
            return;
        }

        File dir = new File(path.getAbsolutePath() + slash + newDir);

        if (dir.exists()) {
            IJ.log(dir.getAbsolutePath() + " already exists");
            return;
        }
        System.out.println(path.getAbsolutePath());
        System.out.println(dir);

        boolean res = dir.mkdir();

        if (res) {
            IJ.log(dir.getAbsolutePath() + " was created");
        } else {
            IJ.log("Something happened so creating " + dir.getAbsolutePath() + " failed");
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
        final net.imagej.ImageJ imagej = net.imagej.Main.launch(args);
    }
}
