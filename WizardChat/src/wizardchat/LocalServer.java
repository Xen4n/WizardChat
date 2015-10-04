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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
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
public class LocalServer extends Thread implements ChatSocket {

    private Socket s;
    private final String ip;
    private InputStream is = null;
    private OutputStream os = null;
    private static File savefile;
    private Boolean isCanSendFile = null;

    public LocalServer(String ip) {
        this.ip = ip;
        setDaemon(true);
        setPriority(NORM_PRIORITY);
    }

    public static void startGetConnections() {
        new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(3130, 0,
                            InetAddress.getLocalHost());
                    server.setReuseAddress(true);
                    System.out.println("server is started ");

                    while (true) {
                        // ждём нового подключения
                        sleep(200);
                        Socket connection = server.accept();
                        String ip = getIP(connection.getRemoteSocketAddress());
                        LocalServer threadServer = new LocalServer(ip);
                        WizardChat.controller.connections.put(ip, threadServer);
                        threadServer.setStreams(connection);
                        threadServer.start();
                    }
                } catch (IOException | InterruptedException ex) {
                    System.out.println("init error: " + ex);
                }
            }
        }.start();
    }

    @Override
    public void run() {
        String[] array;
        try {
            array = (String[]) new ObjectInputStream(is).readObject();
            runAndWait(() -> {
                WizardChat.controller.createNewTab(array[0], ip);
            });
            try {
                //Прием сообщения
                while (true) {

                    switch ((String) new ObjectInputStream(is).readObject()) {
                        case "msg":
                            String[] msg = (String[]) new ObjectInputStream(is).readObject();
                            Platform.runLater(() -> {
                                WizardChat.controller.addNewBox(ip, msg);
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
                                                WizardChat.controller.addSendFileProgress(fileName, ip);
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
                                                            WizardChat.controller.updateFileProgress(ip, progressSize);
                                                        });
                                                    }
                                                    dis.readFully(buffer, 0, (int) tail);
                                                    fos.write(buffer, 0, (int) tail);
                                                    Platform.runLater(() -> {
                                                        WizardChat.controller.updateFileProgress(ip, progressTail);
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
                            isCanSendFile = (Boolean) new ObjectInputStream(is).readObject();
                            break;
                    }

                }
            } catch (ClassNotFoundException | IOException ex) {
                System.out.println(ex + "\n Пользователь отключился!");
                Platform.runLater(() -> {
                    WizardChat.controller.userdropped(ip);
                });
                closeConnection();
                if (WizardChat.controller.isUserOnline(ip)) {
                    Platform.runLater(() -> {
                        WizardChat.controller.addReconnectButton(ip);
                    });
                }
            }
        } catch (IOException | ClassNotFoundException | InterruptedException | ExecutionException ex) {
            Logger.getLogger(LocalServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void sendObject(Object obj) {
        try {
            new ObjectOutputStream(os).writeObject("msg");
            new ObjectOutputStream(os).writeObject(obj);
            FormaController.panes.get(ip).setVvalue(1);
            String[] array = (String[]) obj;
            Platform.runLater(() -> {
                WizardChat.controller.addNewBox(ip, array);
            });
        } catch (IOException ex) {
            Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void setStreams(Socket s) {
        try {
            if (is != null && os != null) {
                is.close();
                os.close();
                this.s.close();
            }
            this.s = s;
            is = s.getInputStream();
            os = s.getOutputStream();
        } catch (IOException ex) {
            System.out.println("1111" + ex);
        }
    }

    public static String getIP(SocketAddress addrs) {
        String str = addrs.toString();
        return str.substring(1, str.indexOf(":"));
    }

    @Override
    public void closeConnection() {
        try {
            is.close();
            os.close();
            s.close();
            WizardChat.controller.connections.remove(ip);
        } catch (IOException ex) {
            Logger.getLogger(Connect.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public boolean tryToConnect() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void startTakeMessage(boolean reconnect) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object getInput() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendFile(String fileName) {
        new Thread() {
            @Override
            public void run() {
                if (WizardChat.controller.isUserOnline(ip)) {
                    try {

                        String cutFileName = fileName.substring(fileName.lastIndexOf("\\") + 1, fileName.length());
                        new ObjectOutputStream(os).writeObject("file");
                        Socket newS = new Socket(ip, 3131);
                        newS.setKeepAlive(true);
                        InputStream inputS = newS.getInputStream();
                        OutputStream outputS = newS.getOutputStream();
                        new ObjectOutputStream(outputS).writeObject(cutFileName);
                        System.out.println("wait");
                        isCanSendFile = (Boolean) new ObjectInputStream(inputS).readObject();
                        if (isCanSendFile) {
                            runAndWait(() -> {
                                WizardChat.controller.addSendFileProgress(cutFileName, ip);
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
                                        WizardChat.controller.updateFileProgress(ip, progress);
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
            }
        }.start();
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
