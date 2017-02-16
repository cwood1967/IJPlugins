package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 2/16/17.
 */

import java.io.File;
import java.util.ArrayList;

public class LytroBatch {

    public static void main(String[] args) {

        String inputDir = args[0];
        String outPath = args[1];
        File dir = new File(inputDir);

        File[] files = dir.listFiles();

        ArrayList<LytroReader> readerList = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {

            String pathname = files[i].getAbsolutePath();
            LytroReader r = new LytroReader(pathname, 0);
            r.setOutPath(outPath);
            readerList.add(r);
        }

        readerList.parallelStream()
                .forEach(sp -> sp.run());


    }
}
