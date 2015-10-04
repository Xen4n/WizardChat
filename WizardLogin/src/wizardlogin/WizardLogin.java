/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardlogin;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Xenan
 */
public class WizardLogin extends Application {
    
    private static Stage stage;
    private static Scene scene;
    private static double x = 0, y = 0;
    
    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("forma.fxml"));
        stage = primaryStage;
        scene = new Scene(root);
        stage.getIcons().add(new Image("/images/ico.png"));
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Wizard Login");
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.show();
        
        scene.setOnMouseReleased((MouseEvent event) -> {
            x = 0;
            y = 0;
        });
        scene.setOnMouseDragged((MouseEvent event) -> {
            if (x != 0 && y != 0) {
                stage.setX(stage.getX() + event.getScreenX() - x);
                stage.setY(stage.getY() + event.getScreenY() - y);
            }
            x = event.getScreenX();
            y = event.getScreenY();
        });
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void stop(){
 
     //Здесь Вы можете прописать все действия при закрытии Вашего приложения.
 
    }
    
    public static Stage getStage(){
        return stage;
    }
    
}
