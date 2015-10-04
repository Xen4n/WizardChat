/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardchat;

import java.awt.Toolkit;
import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Xenan
 */
public class WizardChat extends Application {

    private static Stage stage;
    private static Scene scene;
    private static double x = -1, y = -1, previousHeight = -1;
    private static String connectName = "--";
    public static FormaController controller;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("forma.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage = primaryStage;
        scene = new Scene(root);
        stage.getIcons().add(new Image("/images/ico.png"));
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Wizard Chat");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(true);
        stage.show();

        scene.setOnMouseReleased((MouseEvent event) -> {
            x = -1;
            y = -1;
            scene.setCursor(Cursor.DEFAULT);
        });
        scene.setOnMouseDragged((MouseEvent event) -> {
            scene.setCursor(Cursor.CLOSED_HAND);
            if (x >= 0 && y >= 0) {
                stage.setX(stage.getX() + event.getScreenX() - x);
                stage.setY(stage.getY() + event.getScreenY() - y);
                if (previousHeight != -1) {
                    controller.resizePosition(previousHeight - stage.getHeight());
                    stage.setHeight(previousHeight);                  
                    previousHeight = -1;
 
                }
            }
            if (y == 0 && previousHeight == -1) {
                previousHeight = stage.getHeight();
                stage.setY(0);
                stage.setHeight(Screen.getPrimary().getVisualBounds().getHeight());
                controller.resizePosition(stage.getHeight()-previousHeight);
            }   
            x = event.getScreenX();
            y = event.getScreenY();

        });
        controller.scrollVBox.setOnMouseReleased(scene.getOnMouseReleased());
        controller.scrollVBox.setOnMouseDragged(scene.getOnMouseDragged());
        Platform.setImplicitExit(false);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        connectName = args[0];
        launch(args);
    }

    public static boolean setVerticalResize(double height) {
        double temp = stage.getHeight();
        stage.setHeight(temp + height);
        if (stage.getHeight() < 300) {
            stage.setHeight(temp);
            return false;
        }
        return true;
    }

    public static Stage getStage() {
        return stage;
    }

    public static String getName() {
        return connectName;
    }
    
}
