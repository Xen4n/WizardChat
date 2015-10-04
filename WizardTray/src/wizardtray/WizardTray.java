/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardtray;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import static java.lang.Thread.sleep;
import java.net.Socket;
import javax.imageio.ImageIO;

/**
 *
 * @author Xenan
 */
public class WizardTray {

    private final SystemTray systemTray = SystemTray.getSystemTray();

    public WizardTray(String name) throws IOException, AWTException {
        WizardTray.class.getResourceAsStream("/ico/favicon.png");
        TrayIcon trayIcon = new TrayIcon(ImageIO.read(getClass().getResource("/ico/favicon.png")), "Wizard session keeper");

        PopupMenu popupMenu = new PopupMenu();
        MenuItem item = new MenuItem("Открыть Wizard-Чат");
        item.addActionListener((ActionEvent e) -> {
            startChat(name);
        });
        popupMenu.add(item);
        trayIcon.setPopupMenu(popupMenu);
        systemTray.add(trayIcon);
    }

    public static void main(String[] args) throws IOException, AWTException, InterruptedException {
        WizardTray wizardTray = new WizardTray(args[0]);
        while (true) {
            try {
                Socket s = new Socket("192.168.0.47", 3129);
                InputStream input = s.getInputStream();
                OutputStream output = s.getOutputStream();
                new ObjectOutputStream(output).writeObject(args);
                while (true) {
                    int signal = input.read();
                    output.write(signal);
                }
            } catch (Exception e) {
                System.out.println("init error: " + e);
                sleep(10000);
            } // вывод 
        }
    }

    public static void startChat(String name) {
        new Thread() {
            @Override
            public void run() {
                Runtime runtime = Runtime.getRuntime();
                Process proc;
                String path = System.getProperty("user.dir");
                String cmd = "javaw -jar \"" + path + "/WizardChat.jar\" \"" + name + "\"";
                try {
                    proc = runtime.exec(cmd);
                    proc.getInputStream().close();
                    proc.getOutputStream().close();
                    proc.getErrorStream().close();
                } catch (Exception ex) {
                }
            }
        }.start();
    }
}
