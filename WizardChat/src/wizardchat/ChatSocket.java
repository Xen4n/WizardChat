/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wizardchat;

/**
 *
 * @author Xenan
 */
public interface ChatSocket {
    public void sendObject(Object msg);
    //public void sendObjectImage(Object msg);
    public boolean tryToConnect();
    public void startTakeMessage(boolean reconnect);
    public void sendName(String name);
    public void sendFile(String fileName);
    public Object getInput();
    public void closeConnection();
}
