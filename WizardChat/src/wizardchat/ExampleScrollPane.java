/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardchat;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 *
 * @author Xenan
 */
public class ExampleScrollPane extends ScrollPane {

    private boolean scrollToBottom  = false;
    
    public ExampleScrollPane() {
       this.setPrefHeight(324);
       this.setPrefWidth(491);
       this.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
       this.setFocusTraversable(false);
       this.getStyleClass().add("scrollClass");
       VBox vbox = new VBox();
       vbox.setFocusTraversable(false);
       vbox.setPrefWidth(424);
       vbox.setPrefHeight(39);
       vbox.setPadding(new Insets(0, 0, 15, 0));
       this.setContent(vbox);
       this.vvalueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
            if (scrollToBottom) {
                this.setVvalue(this.getVmax());
                scrollToBottom = false;
            }
        });
    }
    
    public void setScroll(boolean bool){
        scrollToBottom = bool;
    }
    
    public boolean isScroll(){
        return scrollToBottom;
    }
}
