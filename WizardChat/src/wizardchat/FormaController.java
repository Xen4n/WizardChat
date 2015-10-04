/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardchat;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import com.sun.javafx.application.HostServicesDelegate;
import java.io.File;
import static java.lang.Thread.sleep;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;
import org.controlsfx.control.PopOver;

/**
 * FXML Controller class
 *
 * @author Xenan
 */
public class FormaController implements Initializable {

    private final String SERVER_WIZARD = "192.168.0.47";

    private double y = -1;
    public ConcurrentHashMap<String, ChatSocket> connections = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, ExampleScrollPane> panes = new ConcurrentHashMap<>();
    private final HashMap<String, String> users = new HashMap<>();
    private final HashMap<ExampleScrollPane, Hyperlink> links = new HashMap<>();
    private final HashMap<ExampleScrollPane, Label> dropped = new HashMap<>();
    private final HashMap<String, ProgressBar> bars = new HashMap<>();
    volatile private ArrayList<String[]> onlineUsers = new ArrayList<>();
    private final List<ImageView> images = new ArrayList<>();
    private String tabStyle;
    @FXML
    private TextArea editText;
    @FXML
    public VBox scrollVBox;
    @FXML
    private SplitMenuButton btnSend;
    @FXML
    private TabPane tabPane;
    @FXML
    private Separator resizer;
    @FXML
    private Tab mainTab;
    @FXML
    private MenuButton userSelector;

    /**
     * Initializes the controller class.
     *
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        panes.put(SERVER_WIZARD, new ExampleScrollPane());
        mainTab.setContent(panes.get(SERVER_WIZARD));
        mainTab.setId(SERVER_WIZARD);
        mainTab.setOnSelectionChanged((Event t) -> {
            mainTab.setStyle(tabStyle);
        });
        tabStyle = mainTab.getStyle();

        users.put("Основной", SERVER_WIZARD);

        resizer.setOnMouseReleased((MouseEvent event) -> {
            y = -1;
        });
        resizer.setOnMouseDragged((MouseEvent event) -> {
            if (y == -1) {
                y = event.getSceneY();
            }

            double newY = event.getSceneY();

            if (WizardChat.setVerticalResize(newY - y)) {
                resizePosition(newY - y);
                y = event.getSceneY();
            }

        });
        editText.setMinHeight(8 + 18 * (2));
        editText.setWrapText(true);
        editText.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            switch (event.getCode()) {
                case ENTER:
                    if (event.isControlDown()) {
                        editText.appendText(System.lineSeparator());
                    } else {
                        System.out.println(editText.getText());
                        onClickDone(null);
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        });
        new Thread() {
            @Override
            public void run() {
                selectorClick();
            }
        }.start();

        ((VBox) panes.get(SERVER_WIZARD).getContent()).getChildren().clear();
        ((VBox) panes.get(SERVER_WIZARD).getContent()).getChildren().addAll(connectAndGetHistory());
        connections.get(SERVER_WIZARD).startTakeMessage(false);
        panes.get(SERVER_WIZARD).setVvalue(1);
        LocalServer.startGetConnections();

        loadSmiles();
    }

    @FXML
    private void onClickDone(ActionEvent event) {
        editText.requestFocus();
        String currentUser = "";
        for (Tab tab : tabPane.getTabs()) {
            if (tab.isSelected()) {
                currentUser = tab.getText();
            }
        }
        if (connections.containsKey(users.get(currentUser))) {
            if (currentUser.equals("Основной")) {
                sendMessage(users.get(currentUser), WizardChat.getName(), editText.getText());
                editText.clear();
            } else {
                for (String[] array : onlineUsers) {
//                    ProgressBar bar = bars.get(users.get(currentUser));
                    boolean check = true;
//                    if(bar!=null && bar.getProgress() < 1){
//                        check = false;
//                    }
                    if (array[0].equals(currentUser) && connections.containsKey(array[1]) && check) {
                        sendMessage(users.get(currentUser), WizardChat.getName(), editText.getText());
                        editText.clear();
                        break;
                    }
                }
            }
        }

    }

    @FXML
    private void sendFile(ActionEvent event) {
        editText.requestFocus();
        String currentUser = "";
        for (Tab tab : tabPane.getTabs()) {
            if (tab.isSelected()) {
                currentUser = tab.getText();
            }
        }
        if (connections.containsKey(users.get(currentUser))) {
            if (!currentUser.equals("Основной")) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Выберите файл");
                connections.get(users.get(currentUser)).sendFile(fileChooser.showOpenDialog(WizardChat.getStage()).getAbsolutePath());
                editText.clear();
            }
        }
    }

    @FXML
    private void onSmile(ActionEvent event) {
        PopOver pop = new PopOver();
        pop.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        pop.setDetachedTitle("Смайлы");

        ScrollPane scroll = new ScrollPane();
        scroll.setPrefViewportHeight(250);
        scroll.setPrefViewportWidth(255);
        scroll.setPadding(new Insets(20, 5, 5, 5));
        GridPane grid = new GridPane();
        int vgap = images.size() / 7 + 1;
        grid.setHgap(7);
        grid.setVgap(vgap);
        int iterator = 0;
        for (int i = 0; i < vgap && iterator < images.size(); i++) {
            for (int j = 0; j < 7 && iterator < images.size(); j++, iterator++) {
                grid.add(images.get(iterator), j, i);
            }
        }
        scroll.setContent(grid);
        pop.setContentNode(scroll);
        double X = WizardChat.getStage().getX() + WizardChat.getStage().getWidth() - 225;
        double Y = WizardChat.getStage().getY() + WizardChat.getStage().getHeight() - 115 - 255;
        pop.setX(X);
        pop.setY(Y);
        pop.show(WizardChat.getStage());
    }

    @FXML
    private void onClickCancel(ActionEvent event) {
        editText.requestFocus();
        WizardChat.getStage().setIconified(true);
    }

    @FXML
    private void onClickOnTop(ActionEvent event) {
        WizardChat.getStage().setAlwaysOnTop(!WizardChat.getStage().isAlwaysOnTop());
        ToggleButton btn = (ToggleButton) event.getSource();
        editText.requestFocus();
    }

    @FXML
    private void onClickExit(ActionEvent event) {
        System.exit(0);
    }

    private void selectorClick() {
        Connect connect = new Connect(SERVER_WIZARD);
        connect.tryToConnect();
        connect.sendName("getUsers");
        try {
            while (true) {
                onlineUsers = (ArrayList<String[]>) connect.getInput();
                userSelector.getItems().clear();
                for (String[] item : onlineUsers) {
                    MenuItem menuitem = new MenuItem(item[0]);
                    String name = item[0];
                    String ip = item[1];
                    for (Tab tab : tabPane.getTabs()) {
                        if (tab.getText().equals(name) && dropped.containsKey(panes.get(ip))) {
                            dropped.remove(panes.get(ip));
                        }
                    }
                    menuitem.setOnAction((ActionEvent t) -> {
                        boolean check = true;
                        for (Tab tab : tabPane.getTabs()) {
                            if (tab.getText().equals(name) && dropped.containsKey(panes.get(ip))) {
                                check = false;
                            }
                        }
                        if (check) {
                            new Thread() {
                                @Override
                                public void run() {
                                    Platform.runLater(() -> {
                                        createNewTab(name, ip);
                                    });
                                    connections.put(ip, new Connect(ip));
                                    connections.get(ip).tryToConnect();
                                    connections.get(ip).sendName(WizardChat.getName());
                                    connections.get(ip).startTakeMessage(false);
                                }
                            }.start();
                        }
                    });
                    userSelector.getItems().add(menuitem);
                }

                for (Tab tab : tabPane.getTabs()) {
                    boolean checkContains = false;
                    for (String[] item : onlineUsers) {
                        if (tab.getId().equals(item[1])) {
                            checkContains |= true;
                        }
                    }
                    if (!checkContains) {
                        Platform.runLater(() -> {
                            ((VBox) panes.get(tab.getId()).getContent()).getChildren().remove(links.remove(panes.get(tab.getId())));
                        });
                    }
                }

            }
        } catch (Exception ex) {
            System.out.println("Сервер отключен, init error: " + ex);
            try {
                Connect con = new Connect(SERVER_WIZARD);
                while (true) {
                    sleep(1000);
                    if (con.tryToConnect()) {
                        con.closeConnection();
                        break;
                    }
                }
            } catch (InterruptedException ex1) {
                System.out.println("init error: " + ex);
            }
            Platform.runLater(() -> {
                initialize(null, null);
            });
        }
    }

    public void userdropped(String ip) {
        Label drop = new Label("Пользователь отключился");
        drop.setPadding(new Insets(10));
        ((VBox) panes.get(ip).getContent()).getChildren().add(drop);
        dropped.put(panes.get(ip), drop);
        panes.get(ip).setScroll(true);
    }

    public void resizePosition(double different) {
        resizer.setLayoutY(resizer.getLayoutY() + different);
        btnSend.setLayoutY(btnSend.getLayoutY() + different);
        editText.setLayoutY(editText.getLayoutY() + different);
        tabPane.setPrefHeight(tabPane.getPrefHeight() + different);

    }

    private void sendMessage(String ip, String name, String text) {
        if (!text.trim().replaceAll(System.lineSeparator(), "").equals("")) {
            Calendar calendar = new GregorianCalendar();
            String time = new SimpleDateFormat("yyyy.MM.dd HH:mm").format(calendar.getTime());
            String[] msg = {name, cleanN(text), time};
            connections.get(ip).sendObject(msg);
        }
    }

    private List<VBox> connectAndGetHistory() {
        while (true) {
            connections.put(SERVER_WIZARD, new Connect(SERVER_WIZARD));
            connections.get(SERVER_WIZARD).tryToConnect();
            connections.get(SERVER_WIZARD).sendName(WizardChat.getName());
            List<VBox> boxes = new ArrayList<>();
            String[][] data = (String[][]) connections.get(SERVER_WIZARD).getInput();
            for (String[] massiv : data) {
                boxes.add(takeVBox(massiv));
            }
            return boxes;
        }
    }

    public VBox takeVBox(String[] massiv) {
        HBox box = new HBox();
        VBox vbox = new VBox();
        box.setPadding(new Insets(10, 10, 1, 10));
        Label name = new Label(massiv[0]);
        name.setPrefWidth(300);
        name.getStyleClass().add("nameLabel");
        box.getChildren().add(name);
        Label time = new Label(massiv[2]);
        time.setPrefWidth(100);
        box.getChildren().add(time);
        vbox.getChildren().add(box);
        if (massiv[1].startsWith("http") && !massiv[1].contains(" ")) {
            Hyperlink link = new Hyperlink(massiv[1]);
            link.setOnAction((ActionEvent t) -> {
                try {
                    HostServicesDelegate hostServices = HostServicesFactory.getInstance(WizardChat.class
                            .newInstance());
                    hostServices.showDocument(link.getText());
                } catch (InstantiationException | IllegalAccessException ex) {
                    Logger.getLogger(FormaController.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
            });
            vbox.getChildren().add(link);
        } else {
            if (massiv[1].startsWith("/smiles/")) {
                File smile = new File(System.getProperty("user.dir") + massiv[1]);
                ImageView img = new ImageView(new Image(smile.toURI().toString()));
                img.setTranslateX(20);
                vbox.getChildren().add(img);
            } else {
                TextArea textarea = new TextArea(massiv[1]);
                textarea.setWrapText(true);
                textarea.setEditable(false);
                textarea.setFocusTraversable(false);
                textarea.setPadding(new Insets(0, 0, 0, 5));
                textarea.setStyle("-fx-background-color:#f3f3f3");
                int longcount = 0;
                String[] array = massiv[1].split("\n");
                for (String str : array) {
                    longcount += str.length() / 66;
                }
                textarea.setMinHeight(4 + 17 * (array.length + longcount) + 4);
                textarea.setPrefWidth(350);
                vbox.getChildren().add(textarea);
            }
        }
        return vbox;
    }

    private String cleanN(String str) {
        str = str.trim();
        while (str.startsWith("\n")) {
            str = str.replaceFirst("\n", "");
        }
        while (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private void loadSmiles() {
        if (images.isEmpty()) {
            for (File smile : new File(System.getProperty("user.dir") + "/smiles").listFiles()) {
                String tempstr = smile.toURI().toString();
                ImageView img = new ImageView(new Image(tempstr));
                img.setId(tempstr.substring(tempstr.indexOf("/smiles")));
                img.setPreserveRatio(true);
                img.setSmooth(true);
                img.setFitWidth(30);
                img.setOnMouseClicked((MouseEvent ev) -> {
                    String currentUser = "";
                    for (Tab tab : tabPane.getTabs()) {
                        if (tab.isSelected()) {
                            currentUser = tab.getText();
                        }
                    }
                    sendMessage(users.get(currentUser), WizardChat.getName(), ((ImageView) ev.getTarget()).getId());
                });
                images.add(img);
            }
        }
    }

    //Для работы с Connect и LocalServer
    public void addNewBox(String ip, String[] array) {
        ((VBox) panes.get(ip).getContent()).getChildren().add(takeVBox(array));
        panes.get(ip).setScroll(true);
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getId().equals(ip)) {

                if (!tab.isSelected() || !WizardChat.getStage().isFocused()) {
                    if (!tab.isSelected()) {
                        tab.setStyle("-fx-background-color:#FF6A6A;");
                    }
                    Notifications.create()
                            .title(array[0])
                            .text(array[1])
                            .hideAfter(Duration.seconds(10))
                            .onAction((ActionEvent t) -> {
                                if (WizardChat.getStage().isIconified()) {
                                    WizardChat.getStage().setIconified(false);
                                }
                                WizardChat.getStage().setAlwaysOnTop(true);
                                WizardChat.getStage().setAlwaysOnTop(false);
                            })
                            .show();
                }
            }
        }
        if (!ip.equals(SERVER_WIZARD)) {
            ChatHistory hstr = new ChatHistory(ip);
            hstr.add(array);
        }
    }

    public void addReconnectButton(String ip) {
        Hyperlink link = new Hyperlink("Переподключиться...");
        link.setPadding(new Insets(10));
        link.setOnAction((event) -> {
            connections.put(ip, new Connect(ip));
            connections.get(ip).tryToConnect();
            connections.get(ip).sendName(WizardChat.getName());
            connections.get(ip).startTakeMessage(true);
            link.setOnAction(null);

        });
        ((VBox) panes.get(ip).getContent()).getChildren().add(link);
        panes.get(ip).setScroll(true);
        links.put(panes.get(ip), link);
    }

    public void addSendFileProgress(String filename, String ip) {
        ProgressBar bar = new ProgressBar();
        bar.setPadding(new Insets(10));
        bar.setProgress(0);
        bar.setPrefWidth(200);
        Label label = new Label(filename);
        label.setPadding(new Insets(10));
        HBox box = new HBox();
        box.getChildren().add(bar);
        box.getChildren().add(label);
        ((VBox) panes.get(ip).getContent()).getChildren().add(box);
        panes.get(ip).setScroll(true);
        bars.put(ip, bar);
    }

    public void updateFileProgress(String ip, Double value) {
        bars.get(ip).setProgress(bars.get(ip).getProgress() + value);
    }

    public void createNewTab(String name, String ip) {
        if (!isTabExsist(name)) {
            panes.put(ip, new ExampleScrollPane());
            users.put(name, ip);
            Tab tab = new Tab(name);
            tab.setContent(panes.get(ip));
            tab.setId(ip);
            tab.setOnSelectionChanged((Event t) -> {
                tab.setStyle(tabStyle);
            });
            tab.setOnClosed((Event t) -> {
                connections.get(ip).closeConnection();
            });
            Platform.runLater(() -> {
                tabPane.getTabs().add(tab);
                tabPane.getSelectionModel().selectLast();
            });
            ChatHistory hstr = new ChatHistory(ip);
            if (hstr.open()) {

                ((VBox) panes.get(ip).getContent()).getChildren().addAll(hstr.getVBoxes());
                panes.get(ip).setScroll(true);
                panes.get(ip).setVvalue(1);
            }
        } else {
            Platform.runLater(() -> {
                ((VBox) panes.get(ip).getContent()).getChildren().remove(links.remove(panes.get(ip)));
                ((VBox) panes.get(ip).getContent()).getChildren().remove(dropped.remove(panes.get(ip)));
            });
        }
        editText.requestFocus();
    }

    public void closeTab(String ip) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getId().equals(ip)) {
                Platform.runLater(() -> {
                    tabPane.getTabs().remove(tab);
                });
            }
        }
    }

    public boolean isTabExsist(String nameOrIp) {
        for (Tab tab : tabPane.getTabs()) {
            if (tab.getText().equals(nameOrIp) || tab.getId().equals(nameOrIp)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUserOnline(String ip) {
        try {
            sleep(800);
            if (!isOnlineEmpty()) {
                for (String[] array : onlineUsers) {
                    if (array[1].equals(ip)) {
                        return true;

                    }
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(FormaController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public boolean isOnlineEmpty() {
        return onlineUsers != null ? onlineUsers.isEmpty() : true;
    }

    public String getNameByIp(String ip) {
        for (String[] array : onlineUsers) {
            if (array[1].equals(ip)) {
                return array[0];
            }
        }
        return "";
    }
}
