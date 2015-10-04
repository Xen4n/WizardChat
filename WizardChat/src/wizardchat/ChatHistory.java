/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardchat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import javafx.scene.layout.VBox;

/**
 *
 * @author Xenan
 */
public class ChatHistory implements Serializable {

    private ArrayList<String[]> history = null;
    private final String name;

    public ChatHistory(String ip) {
        this.name = WizardChat.controller.getNameByIp(ip);
    }

    public void add(String[] msg) {
        if (!open()) {
            history = new ArrayList<>();
        }
        history.add(msg);
        checkAndClean();
        save();
    }

    public ArrayList<VBox> getVBoxes() {
        ArrayList<VBox> vboxes = new ArrayList<>();
        for (String[] massiv : history) {
            vboxes.add(WizardChat.controller.takeVBox(massiv));
        }
        return vboxes;
    }

    private ArrayList<String[]> getHistory() {
        return history;
    }

    public boolean open() {
        File file = new File(System.getProperty("user.dir") + "/history/" + name.replace(" ", "") + ".hstr");
        if (file.exists()) {
            FileInputStream fis;
            ChatHistory hstr;
            try {
                fis = new FileInputStream(file.getAbsolutePath());
                try (ObjectInputStream ois = new ObjectInputStream(fis)) {
                    hstr = (ChatHistory) ois.readObject();
                    history = hstr.getHistory();
                }
                fis.close();
            } catch (FileNotFoundException ex) {
                System.out.println("notfound" + ex);
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("no class" + ex);
            }
            return history != null;

        } else {
            return false;
        }
    }

    public void save() {
        File fileDir = new File(System.getProperty("user.dir") + "/history");
        fileDir.mkdirs();
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(fileDir.getAbsolutePath() + "\\" + name.replace(" ", "") + ".hstr");
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this);
                oos.flush();
                oos.close();
                fos.close();
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        }
    }

    private void checkAndClean() {
            while(history.size()>150){
                history.remove(0);
            }
    }

}
