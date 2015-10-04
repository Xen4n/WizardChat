/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Xenan
 */
public class WizardServer extends Thread {

    volatile private Socket s;
    private final String ip;
    volatile private InputStream is = null;
    volatile private OutputStream os = null;
    volatile public static ConcurrentHashMap<String, WizardServer> threadTray = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, WizardServer> threadChat = new ConcurrentHashMap<>();
    volatile public static ConcurrentHashMap<String, Socket> connectionsLogin = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<Integer, String[]> tempMessages = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> chatUsers = new ConcurrentHashMap<>();
    private static ActionsDB database;

    public static void main(String args[]) {
        try {
            database = new ActionsDB("root", "29101986");
            // привинтить сокет на локалхост, порт 3128
            ServerSocket server = new ServerSocket(3129, 0,
                    InetAddress.getByName("localhost"));
            server.setReuseAddress(true);

            System.out.println("server is started");

            tempMsgToSQLStart();

            while (true) {
                // ждём нового подключения
                sleep(200);
                Socket connection = server.accept();
                String ip = getIP(connection.getRemoteSocketAddress());
                boolean trayChecked = threadTray.containsKey(ip);
                boolean loginChecked = connectionsLogin.containsKey(ip);
                System.out.println("Tray + Login | " + trayChecked + " + " + loginChecked);
                if (trayChecked && loginChecked) {
                    connectionsLogin.remove(ip);
                    threadTray.get(ip).setStreams(connection);
                    System.out.println("Восстановили соединение");
                } else {
                    WizardServer threadServer = new WizardServer(ip);
                    threadServer.setStreams(connection);
                    threadServer.start();
                    System.out.println("Начали новый поток");
                }
            }
        } catch (IOException | InterruptedException | SQLException e) {
            System.out.println("Connect init error: " + e);
        }
    }

    public WizardServer(String ip) {
        this.ip = ip;
        setDaemon(true);
        setPriority(NORM_PRIORITY);
    }

    @Override
    public void run() {
        String[] array;
        try {
            array = (String[]) new ObjectInputStream(is).readObject();

            switch (array.length) {
                case 2://Login
                    if (threadTray.containsKey(ip)) {
                        connectionsLogin.put(ip, s);
                    }
                    try {
                        String userName = database.getUserName(array[0], array[1]);
                        if (!userName.equals("")) {
                            ObjectOutputStream oos1 = new ObjectOutputStream(os);
                            oos1.writeObject(userName);
                            oos1.flush();
                        } else {
                            os.write(0);
                        }
                    } catch (SQLException ex) {
                    }
                    this.s.close();
                    break;
                case 3://Tray
                    threadTray.put(ip, this); //записываем себя в мапу потоков
                    try {
                        String[] cols = {"name", "date", "time_enter"};
                        String[] vals = {array[0], array[1], array[2]};
                        database.Insert("db_timesheet", cols, vals);
                        System.out.println("Добавил в базу время входа");
                        Calendar timeScape1 = new GregorianCalendar();
                        timeScape1.set(Calendar.HOUR_OF_DAY, 12);
                        timeScape1.set(Calendar.MINUTE, 0);
                        Calendar timeScape2 = new GregorianCalendar();
                        timeScape2.set(Calendar.HOUR_OF_DAY, 14);
                        timeScape2.set(Calendar.MINUTE, 0);
                        Calendar calendar = null;
                        String timeEnd = null;
                        int countErrors = 0;
                        int seconds = 1;
                        while (true) {
                            try {
                                System.out.println("Запускаю цикл обмена присутствия");
                                while (true) {
                                    os.write(1);
                                    is.read();
                                    countErrors = 0;
                                    TimeUnit.SECONDS.sleep(5);
                                }
                            } catch (InterruptedException | IOException e) {
                                System.out.println("User dropped: " + e);
                                if (countErrors == 0) {
                                    calendar = new GregorianCalendar();
                                    timeEnd = new SimpleDateFormat("HH:mm").format(calendar.getTime());
                                    seconds = (calendar.after(timeScape1) && calendar.before(timeScape2)) ? 3 : 1;
                                    System.out.println("Зарегестрировал время выхода");
                                }
                                
                                if (countErrors <= 900*seconds) {
                                    try {
                                        System.out.println("Ожидаю повторного подключения, errors:" + countErrors);       
                                        TimeUnit.SECONDS.sleep(1);
                                        countErrors++;
                                        continue;
                                    } catch (InterruptedException ex) {
                                        System.out.println("Прерывание во время ожидания. " + ex);
                                    }
                                }

                                database.Update(array[0], array[1], array[2], timeEnd);
                                System.out.println("User shutting down");
                                threadTray.remove(ip);
                                break;
                            }
                        }

                    } catch (SQLException ex) {
                        System.out.println("Error connecting \n" + ex);
                    }
                    //
                    break;
                case 1://Chat
                    if (array[0].equals("getUsers")) {
                        int iteration = 1;
                        while (true) {
                            if (iteration != chatUsers.size()) {
                                ArrayList<String[]> list = new ArrayList<>();
                                for (String key : chatUsers.keySet()) {
                                    if (!key.equals(ip)) {
                                        String[] ipname = {chatUsers.get(key), key};
                                        list.add(ipname);
                                    }
                                }
                                ObjectOutputStream oos2 = new ObjectOutputStream(os);
                                oos2.writeObject(list);
                                oos2.flush();
                                iteration = chatUsers.size();
                            }
                        }
                    } else {
                        threadChat.put(ip, this);
                        int iteration = getTempSize();
                        chatUsers.put(ip, array[0]);
                        //Отправка истории из базы данных
                        ObjectOutputStream oos3 = new ObjectOutputStream(os);
                        oos3.writeObject(database.givemeData("db_chat"));
                        oos3.flush();

                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    //Прием сообщения
                                    while (true) {
                                        String[] msg = (String[]) new ObjectInputStream(is).readObject();
                                        int position = getTempSize();
                                        tempMessages.put(position, msg);
                                    }
                                } catch (ClassNotFoundException | IOException ex) {
                                    threadChat.remove(ip);
                                    chatUsers.remove(ip);
                                    if (threadChat.isEmpty()) {
                                        tempMessages.clear();
                                        database.deleteOldMessages();
                                    }
                                    System.out.println(ex + "\n Пользователь чата отключился!");
                                }
                            }
                        }.start();
                        try {
                            while (true) {
                                //отправка нового сообщения
                                if (iteration < getTempSize() && !tempMessages.isEmpty()) {
                                    String[] msg = tempMessages.get(iteration);                                  
                                    new ObjectOutputStream(os).writeObject("msg");
                                    new ObjectOutputStream(os).writeObject(msg);      
                                    iteration++;
                                }
                            }
                        } catch (IOException ex) {
                            System.out.println(ex);
                        }
                        break;
                    }

            }

        } catch (ClassNotFoundException | IOException | SQLException ex) {
            System.out.println("Main try init error: " + ex);

        }
    }

    private static void tempMsgToSQLStart() {
        new Thread() {//tempMessagesSender
            @Override
            public void run() {
                int iterationSQL = 0;
                while (true) {

                    if (iterationSQL < getTempSize()) {
                        try {
                            String[] msg = tempMessages.get(iterationSQL);
                            String[] cols = {"name", "text", "time"};
                            String[] vals = {msg[0], msg[1], msg[2]};
                            database.InsertChat("db_chat", cols, vals);
                            iterationSQL++;
                        } catch (SQLException ex) {
                            System.out.println("init error: " + ex);
                        }
                    } else {
                        if (iterationSQL > getTempSize()) {
                            iterationSQL = 0;
                        }
                    }

                }
            }

        }.start();
    }

    private static int getTempSize() {
        return tempMessages.size();
    }

    private void setStreams(Socket s) {
        try {
            if (is != null && os != null) {
                os.flush();
                is.close();
                os.close();
                this.s.close();
            }
            this.s = s;
            is = s.getInputStream();
            os = s.getOutputStream();
        } catch (IOException ex) {
            System.out.println("Streams error:" + ex);
        }
    }

    public static String getIP(SocketAddress addrs) {
        String str = addrs.toString();
        return str.substring(1, str.indexOf(":"));
    }

}
