package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import ij.ImageStack;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Created by cjw on 7/13/17.
 */
@Plugin(type = Command.class, menuPath="Plugins>Stowers>Chris>Log3D")
public class LoG3DPlugin implements Command, Previewable {

    @Parameter
    ImagePlus imp;

    @Parameter
    double sigmaX;

    @Parameter
    double sigmaY;

    @Parameter
    double sigmaZ;

    LoG3DFilter log3d;

    @Override
    public void run() {

        double time1 = System.currentTimeMillis();
        log3d = new LoG3DFilter(imp, sigmaX, sigmaY, sigmaZ);
        ImageStack logged = log3d.runFilter();

        double time2 = System.currentTimeMillis();
        System.out.println(.001*(time2 -time1));
//        ImagePlus nImp = new ImagePlus("LoG 3D");
        String title = "Log3D " + imp.getTitle();
        ImagePlus nImp = ij.IJ.createHyperStack(title, imp.getWidth(), imp.getHeight(),
                1, imp.getNSlices(), 1, 32);
//        nImp.setDimensions(1, logged.getSize(), 1);
        nImp.setStack(logged);
        nImp.setTitle("Log3D " + imp.getTitle());
        nImp.show();
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
