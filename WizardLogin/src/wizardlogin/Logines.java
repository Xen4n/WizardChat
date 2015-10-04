/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardlogin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Xenan
 */
public class Logines implements Serializable {

    //private ArrayList<String> logines;
    private HashMap<String,Boolean> logines;

    public Logines(String login,Boolean check) {
        logines = new HashMap<>();
        logines.put(login, check);
    }

    public void removeIfContains(String login){
        if(logines.containsKey(login)){
            logines.remove(login);
        }
    }

    public Set<String> getLogines(){
        return logines.keySet();
    }
    public boolean getSelected(String login){
        return logines.get(login);
    }
    
    public void addLogin(String login, Boolean check) {
        logines.put(login,check);
    }

    public void save() {
        FileOutputStream fos = null;
        try {

            fos = new FileOutputStream(System.getProperty("user.dir") + "/logines.wzrd");
            try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(this);
                oos.flush();
            }
        } catch (FileNotFoundException ex) {
        } catch (IOException ex) {
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
            }
        }
    }

    public static Logines open() {
        File file = new File(System.getProperty("user.dir") + "/logines.wzrd");
        if (file.exists()) {
            FileInputStream fis = null;
            Logines logs = null;
            try {
                fis = new FileInputStream(file.getAbsolutePath());
                ObjectInputStream oin = new ObjectInputStream(fis);
                logs = (Logines) oin.readObject();
            } catch (FileNotFoundException ex) {
                System.out.println("notfound");
            } catch (IOException | ClassNotFoundException ex) {
                System.out.println("no class");
            } finally {
                try {
                    fis.close();
                } catch (IOException ex) {

                }
            }
            return logs;
        } else {
            return null;
        }
    }
}
