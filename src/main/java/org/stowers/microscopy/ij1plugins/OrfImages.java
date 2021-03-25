package org.stowers.microscopy.ij1plugins;

 /* in a macro do this:
    a = call("org.stowers.microscopy.ij1plugins.OrfImages.getOrfImages", "YKL055C", "/Users/cjw/Jobs/jjl/FCS/path_orf.csv");
print(a);
     */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class OrfImages {

    public static String getOrfImages(String orfname, String datafile) {

        return ReadDataFile(orfname, datafile);
    }

    private static String ReadDataFile(String orfname, String datafile) {

        String[] res = null;
        StringBuffer sb = new StringBuffer();
        try (BufferedReader br = new BufferedReader(new FileReader(datafile))) {
            String line;
            String orf;
            String filepath;
            ArrayList<String> filepaths = new ArrayList();
            while ((line = br.readLine()) != null) {
                String[] s = line.split(",");
                orf = s[2].trim();
                //System.out.println(orf);
                if (orf.equalsIgnoreCase(orfname)) {
                    filepaths.add(s[1]);
                    sb.append(s[1].substring(2));
                    sb.append(',');
                }

            }



        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();

    }
    public static void main(String[] args) {

        String p = OrfImages.getOrfImages("YPL129W",
                    "/Users/cjw/Jobs/jjl/FCS/path_orf.csv");

        System.out.println(p);
    }


}
