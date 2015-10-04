/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardlogin;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.input.KeyEvent;

/**
 * FXML Controller class
 *
 * @author Xenan
 */
public class FormaController implements Initializable {

    @FXML
    private ComboBox<String> editName;
    @FXML
    private PasswordField editPass;

    private String userName;
    private String dateToday;
    private String timeToday;
    @FXML
    private CheckBox selectStarter;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Logines logs = Logines.open();
        if (logs != null) {
            editName.getItems().addAll(logs.getLogines());
            editName.setValue(editName.getItems().get(editName.getItems().size() - 1));
            selectStarter.setSelected(logs.getSelected(editName.getValue()));
            Platform.runLater(editPass::requestFocus);
        }

        editPass.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            switch (event.getCode()) {
                case ENTER:
                    onClickDone(null);
                    break;
                default:
                    break;
            }
        });
    }

    @FXML
    private void onClickDone(ActionEvent event) {
        if (!sendDataComplete().equals("")) {
            saveLogin();
            if (selectStarter.isSelected()) {
                startChat();
            }
            startTray();
            System.exit(0);
        }
    }

    @FXML
    private void onClickCancel(ActionEvent event) {
        WizardLogin.getStage().setIconified(true);
    }

    private String sendDataComplete() {
        try {
            Socket s = new Socket("192.168.0.47", 3129);
            InputStream input = s.getInputStream();
            OutputStream output = s.getOutputStream();
            String[] args = {editName.getValue(), editPass.getText()};
            new ObjectOutputStream(output).writeObject(args);
            userName = (String) new ObjectInputStream(input).readObject();
            return userName;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("init error: " + e);
        }
        return "";
    }

    private void startTray() {
        Calendar calendar = new GregorianCalendar();
        dateToday = new SimpleDateFormat("yyyy.MM.dd").format(calendar.getTime());
        timeToday = new SimpleDateFormat("HH:mm").format(calendar.getTime());
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        String path = System.getProperty("user.dir");
        String cmd = "javaw -jar \"" + path + "/WizardTray.jar\" \"" + userName + "\" \"" + dateToday + "\" \"" + timeToday + "\"";
        try {
            proc = runtime.exec(cmd);
        } catch (Exception ex) {
        }
    }

    private void startChat() {
        Calendar calendar = new GregorianCalendar();
        dateToday = new SimpleDateFormat("yyyy.MM.dd").format(calendar.getTime());
        timeToday = new SimpleDateFormat("HH:mm").format(calendar.getTime());
        Runtime runtime = Runtime.getRuntime();
        Process proc = null;
        String path = System.getProperty("user.dir");
        String cmd = "javaw -jar \"" + path + "/WizardChat.jar\" \"" + userName + "\"";
        try {
            proc = runtime.exec(cmd);
        } catch (Exception ex) {
        }
    }

    private void saveLogin() {
        Logines logs = Logines.open();
        if (logs != null) {
            logs.removeIfContains(editName.getValue());
            logs.addLogin(editName.getValue(),selectStarter.isSelected());
        } else {
            logs = new Logines(editName.getValue(),selectStarter.isSelected());
        }
        logs.save();
    }

}
