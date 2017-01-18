package com.github.jodama.imageshrink;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DialogPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Reduces the size of JPEG, PNG and BMP image files. Command line parameters to be the full file paths (can be one or
 * multiple). Does not handle transparency in PNG files.
 *
 * In Windows, add this to the 'Send To' context menu:
 *
 * In Windows Explorer, in the address bar enter ‘shell:sendto’ which will open a special folder of shortcuts
 * In this folder create a shortcut to the .jar file e.g. javaw.exe -jar "\your\path\to\imageShrink.jar"
 * To customise icon, right click on the shortcut, select Properties, Change Icon, then browse to icon (in .ico format)
 * ... suggest using the icon: "src/assets/icon for shortcut.ico"
 *
 * Version history
 * 0.1 June 2016
 * 1.0 January 2017 - tidy up of code, revised package name
 *
 * @author  John Mallinson
 * @version 1.0
 * @since   January 2017
 *
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        int countOfFails=0;
        int countOfSuccesses=0;
        List<String> imageList = new ArrayList<>();

        // these are the strings to show the user in the dialogue
        String alertText = "";
        String failsText = "";

        // go through all parameters and see what is a file (for shrinking)
        if (getParameters().getRaw().size()>0) {
            for (String p: getParameters().getRaw()) {
                if(new File(p).isFile()) imageList.add(p);
            }
        }

        // if no files were found then show dialogue and exit
        if(imageList.size()==0) {
            showDialogue("No files found");
            System.exit(0);
        }

        // ask the user to select an amount to shrink the files by
        double reduceBy= getReduceByViaDialogue();

        // if we failed to get the amount, then exit
        if(reduceBy==0) {
            System.out.println("Failed to get amount to shrink the image(s) by ... exited");
            System.exit(1);
        }

        // sort the files in alpha order
        imageList.sort(Comparator.comparing(String::toLowerCase));

        // go through each file and try to shrink it, also add to the dialogue text while we're at it
        for (Object obj:imageList) {
            Object returnObject[]=createShrunkenImage(obj.toString(), reduceBy); // this actually does (or tries to do) the shrinking
            if((boolean)returnObject[0]) { // if the shrinking worked
                countOfSuccesses++;
                alertText+=(String)returnObject[1];
            } else { // if the shrinking didn't work
                countOfFails++;
                failsText+=(String)returnObject[1];
            }
        }

        // finalise the alert text for any failed files
        if(countOfFails>0) {
            failsText=":"+failsText;
            if(countOfFails>1) failsText="s"+failsText;
            failsText="\n\nUnable to shrink "+countOfFails+" file"+failsText;
        }

        // finalise the alert text for the successful shrunken files (including add the fails text)
        if(countOfSuccesses>0) {
            alertText=" (by a factor of "+(int)reduceBy+"):"+alertText;
            if(countOfSuccesses>1) alertText="s"+alertText;
            alertText="Successfully shrunk "+countOfSuccesses+" file"+alertText+failsText;
        } else {
            alertText="Could not shrink any images - imageShrink works on JPEG (.jpg and .jpeg), PNG (.png) and bitmap (.bmp) files only";
        }

        // show the alert dialogue
        showDialogue(alertText);
    }


    /**
     * Show the outcome 'alert' dialogue
     *
     * @param str the string to show in the dialogue
     */
    private void showDialogue(String str) {
        // prepare the dialogue
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ImageShrink");
        alert.setHeaderText("ImageShrink results");
        alert.setContentText(str);

        // get/set the icon
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        try {
            stage.getIcons().add(new Image("/assets/icon.png"));
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to load icon in showDialogue");
        }

        // get/set the graphic
        try {
            ImageView imv = new ImageView(new Image("/assets/icon.png"));
            imv.setFitHeight(30);
            imv.setFitWidth(30);
            alert.setGraphic(imv);
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to load graphic in showDialogue");
        }

        // apply styling
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogPane.getStyleClass().add("style");

        // show the dialogue
        System.out.println(str);
        alert.showAndWait();
    }


    /**
     * Show the choice dialogue for user to select the amount to shrink the images by
     *
     * @return value to reduce image by or 0 means that we couldn't get the response value
     */
    private int getReduceByViaDialogue() {

        // options to show in the dialogue
        List<Integer> choices = new ArrayList<>(Arrays.asList(2,4,8,16));

        // prepare the dialogue
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(2, choices);
        dialog.setTitle("ImageShrink");
        dialog.setHeaderText("ImageShrink");
        dialog.setContentText("Choose size to shrink by:");

        // get/set the icon
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        try {
            stage.getIcons().add(new Image("/assets/icon.png"));
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to load icon in getReduceByViaDialogue");
        }

        // get/set the graphic
        try {
            ImageView imv = new ImageView(new Image("/assets/icon.png"));
            imv.setFitHeight(30);
            imv.setFitWidth(30);
            dialog.setGraphic(imv);
        } catch (IllegalArgumentException e) {
            System.out.println("Failed to load graphic in getReduceByViaDialogue");
        }

        // apply styling
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialogPane.getStyleClass().add("style");

        // get and return the response value
        Optional<Integer> result = dialog.showAndWait();

        // returning 0 means that we couldn't get the response value
        return result.orElse(0);
    }


    /**
     * Actually does the shrinking of the image for allowed file types (JPEGs with extension .jpeg or .jpg, PNG with
     * extension .png and bitmaps with extension .bmp), creates a new file with '_smaller' appended to the filename and
     * returns details on the success (or otherwise) of the operation
     *
     * @param fileName a string of the filename of the file to shrink
     * @param reductionFactor the amount to reduce by (on each side)
     * @return returns an object made up of two parts:
     *      0 = a boolean where true is process was successful, else false if it failed
     *      1 = a String to add to the alert text (if successful) or to add to fails text (if failed)
     */
    private Object[] createShrunkenImage(String fileName, double reductionFactor) {

        // check it is a known file type
        if (fileName.lastIndexOf('.') > 0 && (fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase().equals("jpg") ||
                fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase().equals("jpeg") ||
                fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase().equals("png") ||
                fileName.substring(fileName.lastIndexOf('.')+1).toLowerCase().equals("bmp"))) {

            // file objects for the existing file and the new shrunken file
            File fromFile = new File(fileName);
            File toFile = new File(fileName.substring(0, fileName.lastIndexOf('.')) + "_smaller." +
                    fileName.substring(fileName.lastIndexOf('.') + 1));
            try {
                BufferedImage inputImage = ImageIO.read(fromFile);
                int newWidth = (int) Math.ceil(inputImage.getWidth() / reductionFactor);
                int newHeight = (int) Math.ceil(inputImage.getHeight() / reductionFactor);
                BufferedImage outputImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                int outputX = 0;
                int outputY = 0;
                for (int inputX = 0; inputX < inputImage.getWidth(); inputX += reductionFactor) {
                    for (int inputY = 0; inputY < inputImage.getHeight(); inputY += reductionFactor) {
                        outputImage.setRGB(outputX, outputY, inputImage.getRGB(inputX, inputY));
                        outputY++;
                    }
                    outputY = 0;
                    outputX++;
                }
                ImageIO.write(outputImage, "jpg", toFile);
                return new Object[]{true, "\n" + new File(fileName).getName()};
            } catch (IOException e) {
                System.out.println("IO Exception while processing file: "+fileName);
            }
        }
        // if not a known file type or if it is known but failed to shrink
        return new Object[]{false, "\n" + new File(fileName).getName()};
    }

    public static void main(String[] args) {
        launch(args);
    }
}
