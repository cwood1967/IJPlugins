package org.stowers.microscopy.ij1plugins;



import ij.IJ;

import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

import java.util.Random;

@Plugin(type = Command.class, menuPath="Plugins>Chris>Compliments>Give me a compliment")
public class Compliments implements Command, Previewable {
    @Override
    public void run() {
        IJ.showMessage(getCompliment());
    }

    public String getCompliment() {

        String[] compliments = {
                "You are smart",
                "That's a nice shirt",
                "That's better than Jeff would do",
                "Nice Image!"
        };

        int n = compliments.length;
        Random rand = new Random();
        String res = compliments[rand.nextInt(n)];
        return res;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
