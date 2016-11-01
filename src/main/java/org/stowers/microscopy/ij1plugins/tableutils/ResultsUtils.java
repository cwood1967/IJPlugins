package org.stowers.microscopy.ij1plugins.tableutils;

/**
 * Created by cjw on 10/27/16.
 */

import ij.IJ;
import ij.WindowManager;
import ij.measure.ResultsTable;
import ij.text.TextPanel;
import ij.text.TextWindow;

import java.util.ArrayList;
import java.util.List;

public class ResultsUtils {

    public static String[] getHeaders(TextPanel panel) {

        String tabHeadings = panel.getColumnHeadings();
        String[] headings = tabHeadings.split("\t");

        return headings;
    }

    public static List<Double> getDoubleColumn(TextPanel panel, String column) {

        String[] headings = getHeaders(panel);
        List<Double> res = new ArrayList<>();

        int n = -1;
        for (int i = 0; i < headings.length; i++) {
            if (column.equals(headings[i])) {
                n = i;
                break;
            }
        }

        if (n == -1) {
            System.out.println("Didn't find column " + column);
            return null;
        }

//        while (panel.getCell(n, kr) != null) {
        return null;

    }

    public static List<Class<?>> typeFromFirstLine(TextPanel panel) {
        String line = panel.getLine(0);
        String[] tokens = line.split("\t");

        List<Class<?>> types = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            boolean anum = isNumeric(tokens[i]);
            if (tokens[i].equals("NaN")) {
                anum = true;
            }
            if (anum) {
                if (isFloat(tokens[i])) {
                    types.add(Float.class);
                }
                else {
                    types.add(Integer.class);
                }
            }
            else {
                types.add(String.class);
            }
        }
        System.out.println(tokens.length);

        return types;
    }

    public static boolean isNumeric(String str)
    {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static boolean isFloat(String test) {
        if (test.contains(".")) {
            return true;
        }
        else {
            return false;
        }
    }
}
