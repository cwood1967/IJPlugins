package org.stowers.microscopy.ij1plugins;

import ij.measure.ResultsTable;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

/**
 * Created by cjw on 7/7/16.
 */

@Plugin(type = Command.class, name = "Click for Roi",  menuPath="Plugins>Chris>NotReady>Results Plot")
public class ResultsHist implements Command, Previewable {

    public void run() {

        ResultsTable table = ResultsTable.getResultsTable();
        String[] headings = table.getHeadings();

        for (int i = 0; i < headings.length; i++ ){
            System.out.println(headings[i]);
        }

        ColumnPlotter chooser = new ColumnPlotter(table);
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }
}
