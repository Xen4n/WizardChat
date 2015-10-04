/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardserver;

import java.sql.*;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Pavilion
 */
public class ActionsDB {

    ActionsDB(String lgn, String pswrd) throws SQLException {
        p = new Properties();
        p.setProperty("user", lgn);
        p.setProperty("password", pswrd);
        p.setProperty("useUnicode", "true");
        p.setProperty("characterEncoding", "cp1251");
    }

    public void Update(String name, String date, String time, String leavetime) {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            Connection c = DriverManager.getConnection(url, p);
            String query = "UPDATE db_timesheet set time_leave=\"" + leavetime + "\" WHERE name=\"" + name + "\" AND date=\"" + date + "\" AND time_enter=\"" + time + "\"";
            System.out.println(query);
            Statement st = c.createStatement();
            st.executeUpdate(query);
            c.close();
        } catch (SQLException ex) {
            System.out.println(ex);
        }
    }

    public String getUserName(String login, String pass) throws SQLException {
        //создаем соединение
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        Connection c = DriverManager.getConnection(url, p);
        //создаем запрос
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT full_name FROM ru_users WHERE username=\"" + login
                + "\" AND password=md5(\"" + pass + "\")");
        //данные по запросу получены
        rs.next();
        String data = "";
        try {
            data = rs.getString(1);
        } catch (SQLException ex) {
        }
        rs.close();
        c.close();
        return data;
    }

    public void Insert(String tab, String[] col, String[] val) throws SQLException {
        //создаем соединение
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        Connection c = DriverManager.getConnection(url, p);
        String query = "INSERT INTO " + tab + " (";
        for (int i = 0; i < col.length; i++) {
            query += "`" + col[i] + "`";
            if (i != col.length - 1) {
                query += ",";
            }
        }
        query += ") SELECT * FROM (SELECT ";
        for (int i = 0; i < val.length; i++) {
            if (!val[i].equals("")) {
                val[i] = val[i].replace("'", "`");//заменяем апострофы, чтобы MySql не ругался
                query += "'" + val[i] + "'";//добавляем значения
            } else {
                query += "NULL";
            }
            if (i != val.length - 1) {
                query += ",";
            }
        }
        query += ") AS tmp WHERE NOT EXISTS (SELECT * FROM " + tab + " WHERE ";
        for (int i = 0; i < col.length; i++) {
            query += "`" + col[i] + "`=\"" + val[i] + "\"";
            if (i != col.length - 1) {
                query += " AND ";
            }
        }
        query += ")";

        Statement st = c.createStatement();
        st.executeUpdate(query);
        c.close();

    }

    public void InsertChat(String tab, String[] col, String[] val) throws SQLException {
        //создаем соединение
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        Connection c = DriverManager.getConnection(url, p);
        String query = "INSERT INTO " + tab + " (";
        for (int i = 0; i < col.length; i++) {
            query += "`" + col[i] + "`";
            if (i != col.length - 1) {
                query += ",";
            }
        }
        query += ") VALUES (";
        for (int i = 0; i < val.length; i++) {
            if (!val[i].equals("")) {
                val[i] = val[i].replace("'", "`");//заменяем апострофы, чтобы MySql не ругался
                query += "'" + val[i] + "'";//добавляем значения
            } else {
                query += "NULL";
            }
            if (i != val.length - 1) {
                query += ",";
            }
        }
        query += ")";

        Statement st = c.createStatement();
        st.executeUpdate(query);
        c.close();

    }

    public String[][] givemeData(String tab) throws SQLException {
        //создаем соединение
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        Connection c = DriverManager.getConnection(url, p);
        //создаем запрос
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT name,text,time FROM " + tab);
        //данные по запросу получены
        int ColCount = rs.getMetaData().getColumnCount();
        int RowCount = 0;
        while (rs.next()) {
            RowCount++;//подсчет количества строк
        }
        rs.absolute(0);//сброс курсора в нулевую позицию
        String[][] Data = new String[RowCount][ColCount];//данные
        //проход по данным
        int j = 0;
        while (rs.next()) {
            for (int i = 0; i < ColCount; i++) {
                Data[j][i] = (rs.getString(i + 1));
            }
            j++;
        }
        rs.close();
        c.close();
        return Data;
    }

    public void deleteOldMessages() {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            Connection c = DriverManager.getConnection(url, p);
            Statement s = c.createStatement();
            ResultSet rs = s.executeQuery("SELECT id FROM db_chat");
            LinkedList<Integer> ids = new LinkedList<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            int msgCount = (ids.getLast() - ids.getFirst()) - 150;
            int currentId = ids.getFirst() + msgCount;
            rs.close();
            if (currentId > 0) {
                String query = "DELETE FROM db_chat WHERE id<\"" + currentId + "\"";
                try (Statement st = c.createStatement()) {
                    st.executeUpdate(query);
                }
            }
            c.close();
        } catch (SQLException ex) {
            Logger.getLogger(ActionsDB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static final String schema = "db_local";
    private static final String server = "192.168.0.47";
    //private static final String url = "jdbc:mysql://" + server + "/" + schema;
    private static final String url = "jdbc:mysql://" + server + ":3306/" + schema + "?zeroDateTimeBehavior=convertToNull";
    static Properties p;
}
