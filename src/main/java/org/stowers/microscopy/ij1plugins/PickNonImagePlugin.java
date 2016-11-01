package org.stowers.microscopy.ij1plugins;

/**
 * Created by cjw on 10/26/16.
 */

import ij.WindowManager;
import org.scijava.command.Command;
import org.scijava.command.Previewable;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@Plugin(type = Command.class, name = "Pick Non-Image Window",
        menuPath="Plugins>Chris>NotReady>Pick Non-Image Window")
public class PickNonImagePlugin implements Previewable, Command, ActionListener {

    JFrame frame;
    JLabel label;
    JComboBox<String> nonImage;
    JButton ok;
    JButton cancel;

    String[] frameTitles;
    String[] imageTitles;

    String pickedTitle;

    public PickNonImagePlugin() {
        frameTitles = WindowManager.getNonImageTitles();
    }

    @Override
    public void run() {

//        createDialog();
    }

//    private void createDialog() {
//        frame = new JFrame("Select Windows");
//        label = new JLabel("Pick a Window:");
//        nonImage = new JComboBox<>();
//        ok = new JButton("OK");
//        ok.addActionListener(this);
//        cancel = new JButton("Cancel");
//
//        for (int i = 0; i < frameTitles.length; i++) {
//            nonImage.addItem(frameTitles[i]);
//        }
//
//        frame.setLayout(new GridLayout(1, 4));
//        frame.add(label);
//        frame.add(nonImage);
//        frame.add(ok);
//        frame.add(cancel);
//        frame.pack();
//        frame.setVisible(true);
//    }


    public String getPickedTitle() {
        return pickedTitle;
    }

    @Override
    public void preview() {

    }

    @Override
    public void cancel() {

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == ok) {
            String title = (String) nonImage.getSelectedItem();
            pickedTitle = title;
        }
    }


    public static String getNonImageFrameTitle() {
        PickNonImagePlugin p = new PickNonImagePlugin();
//        p.createDialog();

        while (p.getPickedTitle() == null) {

        }
        String res = p.getPickedTitle();

        return res;

    }
}
