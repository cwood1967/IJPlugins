package org.stowers.microscopy.ij1plugins;

import ij.ImagePlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.ImagePlusAdapter;

/**
 * Created by cjw on 9/28/16.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>XZXZX")
public class CjwImgLib implements Previewable, Command{

    @Parameter
    ImagePlus imp;

    @Override
    public void run() {

        imglibstuff();
    }

    public <T extends NumericType< T > & NativeType< T >>  void imglibstuff() {
        Img< T > image = ImagePlusAdapter.wrap(imp);

        RandomAccessibleInterval<T> view =
                Views.interval(image, new long[] {100, 100, 0}, new long[] {300, 300, 4});

        ImageJFunctions.show(view);
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}


