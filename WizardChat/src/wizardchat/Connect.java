/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;

/**
 *
 * @author Xenan
 */
public class Connect implements ChatSocket {

    private Socket s;
    private InputStream input;
    private OutputStream output;
    private final String server_ip;
    private boolean isNotMainTab;
    private static File savefile;
    private Boolean isCanSendFile = null;

    public Connect(String ip) {
        this.server_ip = ip;
    }

    @Override
    public boolean tryToConnect() {
        try {
            if (server_ip.equals("192.168.0.47")) {
                s = new Socket(server_ip, 3129);
                isNotMainTab = false;
            } else {
                s = new Socket(server_ip, 3130);
                isNotMainTab = true;
            }
            s.setKeepAlive(true);
            input = s.getInputStream();
            output = s.getOutputStream();
            return true;
        } catch (IOException ex) {
            System.out.println(ex);
            return false;
        }
    }

    @Override
    public void startTakeMessage(boolean reconnect) {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        switch ((String) new ObjectInputStream(input).readObject()) {
                            case "msg":
                                FormaController.panes.get(server_ip).setVvalue(1);
                                String[] array = (String[]) new ObjectInputStream(input).readObject();
                                Platform.runLater(() -> {
                                    WizardChat.controller.addNewBox(server_ip, array);
                                });
                                break;
                            case "file":
                                new Thread() {
                                    @Override
                                    public void run() {
                                        try {
                                            ServerSocket server = new ServerSocket(3131, 0,
                                                    InetAddress.getLocalHost());
                                            server.setReuseAddress(true);
                                            Socket newS = server.accept();

                                            InputStream inputS = newS.getInputStream();
                                            OutputStream outputS = newS.getOutputStream();

                                            String fileName = (String) new ObjectInputStream(inputS).readObject();
                                            DirectoryChooser dirChoose = new DirectoryChooser();
                                            dirChoose.setTitle("Сохранить файл " + fileName + " в папку");
                                            runAndWait(() -> {
                                                savefile = dirChoose.showDialog(WizardChat.getStage());
                                                if (savefile != null) {
                                                    WizardChat.controller.addSendFileProgress(fileName, server_ip);
                                                }
                                            });
                                            new ObjectOutputStream(outputS).writeObject(savefile != null);
                                            if (savefile != null) {
                                                byte[] buffer = new byte[8192];
                                                DataInputStream dis = new DataInputStream(inputS);
                                                long fileSize = dis.readLong();
                                                long tail = fileSize % buffer.length;
                                                double progressTail = 1. / (tail / buffer.length);
                                                double progressSize = 1. / ((fileSize - tail) / buffer.length);
                                                try (FileOutputStream fos = new FileOutputStream(savefile.getAbsolutePath() + "\\" + fileName, false)) {
                                                    long rounds = fileSize / buffer.length;
                                                    try {
                                                        for (int i = 0; i < rounds; i++) {
                                                            dis.readFully(buffer);
                                                            fos.write(buffer);
                                                            Platform.runLater(() -> {
                                                                WizardChat.controller.updateFileProgress(server_ip, progressSize);
                                                            });
                                                        }
                                                        dis.readFully(buffer, 0, (int) tail);
                                                        fos.write(buffer, 0, (int) tail);
                                                        Platform.runLater(() -> {
                                                            WizardChat.controller.updateFileProgress(server_ip, progressTail);
                                                        });
                                                        fos.flush();
                                                    } catch (Exception ex) {

                                                    }
                                                }
                                            }
                                            inputS.close();
                                            outputS.close();
                                            newS.close();
                                        } catch (IOException | InterruptedException | ExecutionException | ClassNotFoundException ex) {
                                            Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                }.start();
                                break;
                            case "answerSendFile":
                                isCanSendFile = (Boolean) new ObjectInputStream(input).readObject();
                                break;
                        }
                    }
                } catch (ClassNotFoundException | IOException ex) {
                    System.out.println(ex + "\n Пользователь отключился!");
                    closeConnection();
                    Platform.runLater(() -> {
                        WizardChat.controller.userdropped(server_ip);
                    });
                    if (WizardChat.controller.isUserOnline(server_ip)) {
                        Platform.runLater(() -> {
                            WizardChat.controller.addReconnectButton(server_ip);
                        });
                    }
                }
            }
        }.start();
    }

    @Override
    public void sendName(String name) {
        try {
            String[] args = {name};
            new ObjectOutputStream(output).writeObject(args);
        } catch (IOException e) {
            System.out.println("init error: " + e);
            try {
                sleep(1000);
            } catch (InterruptedException ex) {
                System.out.println("init error: " + ex);
            }
        }
    }

    @Override
    public Object getInput() {
        try {
            return new ObjectInputStream(input).readObject();
        } catch (ClassNotFoundException | IOException ex) {
            System.out.println("init error: " + ex);
        }
        return null;
    }

    @Override
    public void sendObject(Object obj) {
        try {
            if (isNotMainTab) {
                new ObjectOutputStream(output).writeObject("msg");
            }
            new ObjectOutputStream(output).writeObject(obj);
            if (isNotMainTab) {
                FormaController.panes.get(server_ip).setVvalue(1);
                String[] array = (String[]) obj;
                Platform.runLater(() -> {
                    WizardChat.controller.addNewBox(server_ip, array);
                });

            }
        } catch (IOException ex) {
            Logger.getLogger(Connect.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void sendFile(String fileName) {
        new Thread() {
            @Override
            public void run() {
                try {

                    String cutFileName = fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length());
                    new ObjectOutputStream(output).writeObject("file");
                    Socket newS = new Socket(server_ip, 3131);
                    newS.setKeepAlive(true);
                    InputStream inputS = newS.getInputStream();
                    OutputStream outputS = newS.getOutputStream();
                    new ObjectOutputStream(outputS).writeObject(cutFileName);
                    System.out.println("wait");
                    isCanSendFile = (Boolean) new ObjectInputStream(inputS).readObject();
                    if (isCanSendFile) {
                        runAndWait(() -> {
                            WizardChat.controller.addSendFileProgress(cutFileName, server_ip);
                        });
                        DataOutputStream dos = new DataOutputStream(outputS);
                        byte[] buffer = new byte[8192];
                        File f = new File(fileName);
                        long fileSize = f.length();
                        dos.writeLong(fileSize);

                        double progress = 1. / (fileSize / buffer.length);
                        FileInputStream fis = new FileInputStream(f);
                        try {
                            int read;
                            while ((read = fis.read(buffer)) > 0) {
                                dos.write(buffer, 0, read);
                                Platform.runLater(() -> {
                                    WizardChat.controller.updateFileProgress(server_ip, progress);
                                });
                            }
                        } catch (Exception ex) {
                        }
                        dos.flush();
                        isCanSendFile = null;
                    }
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ExecutionException | ClassNotFoundException ex) {
                    Logger.getLogger(LocalServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }

    @Override
    public void closeConnection() {
        try {
            input.close();
            output.close();
            s.close();
            WizardChat.controller.connections.remove(server_ip);

        } catch (IOException ex) {
            Logger.getLogger(Connect.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void runAndWait(final Runnable run)
            throws InterruptedException, ExecutionException {
        if (Platform.isFxApplicationThread()) {
            try {
                run.run();
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        } else {
            final Lock lock = new ReentrantLock();
            final Condition condition = lock.newCondition();
            lock.lock();
            try {
                Platform.runLater(() -> {
                    lock.lock();
                    try {
                        run.run();
                    } catch (Throwable e) {
                    } finally {
                        try {
                            condition.signal();
                        } finally {
                            lock.unlock();
                        }
                    }
                });
                condition.await();
            } finally {
                lock.unlock();
            }
        }
    }

}
