package Worker;

import Models.Client;
import Models.Event;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * SharedObject que irá partilhar informações e recursos entre as diferentes
 * Threads
 */
public class WorkerSharedObject {

    private final ArrayList<Client> clients = new ArrayList<>();
    private final ArrayList<Socket> clientsListeners = new ArrayList<>();
    private final ArrayList<PrintWriter> messageToClients = new ArrayList<>();
    private final ArrayList<Event> events = new ArrayList<>();
    private String multicastIP = "undefined";
    private int clientMulticastPortNumber = 0;
    private String centralServerReportsString = "<html></html>";
    private String clientsReportsString = "<html></html>";
    private int nClientsNotified = 0;
    private double timeTakenToNotify = 0;
    private double time1 = 0;
    private double time2 = 0;

    public WorkerSharedObject(String multicastIP, int clientPort) {
        this.multicastIP = multicastIP;
        this.clientMulticastPortNumber = clientPort;
    }

    public synchronized boolean clientLogin(Client client) {
        if (!this.clients.isEmpty()) {
            for (int i = 0; i < this.clients.size(); i++) {
                if (client.getUsername().equalsIgnoreCase(this.clients.get(i).getUsername())) {
                    return false;
                }
            }
        }

        this.clients.add(client);

        return true;
    }

    public synchronized boolean clientLogout(String clientUsername) throws IOException {
        for (int i = 0; i < this.clients.size(); i++) {
            if (clientUsername.equalsIgnoreCase(this.clients.get(i).getUsername())) {
                this.clients.remove(i);
                this.closeListener(i);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean newListener(Socket clientSocket, String username) throws IOException {
        for (int i = 0; i < this.clients.size(); i++) {
            if (username.equalsIgnoreCase(this.clients.get(i).getUsername())) {
                this.clientsListeners.add(clientSocket);
                this.messageToClients.add(new PrintWriter(clientSocket.getOutputStream(), true));
                return true;
            }
        }
        return false;
    }

    public synchronized void closeListener(int position) throws IOException {
        if (!this.clientsListeners.isEmpty()) {
            this.clientsListeners.remove(position);
            PrintWriter messageToClient = this.messageToClients.remove(position);
            messageToClient.println("[WORKER] Logout realizado com sucesso!");
            messageToClient.close();
        }
    }

    public synchronized void newEvent(String event, int dangerlevel, String local) {
        Event newEvent = new Event(event, dangerlevel, local);
        this.events.add(newEvent);
    }

    public synchronized void changeEvent(String event, int dangerlevel, String local) {
        for (int i = 0; i < this.events.size(); i++) {
            if (event.equals(this.events.get(i).getEvent())
                    && dangerlevel == this.events.get(i).getDangerLevel()
                    && local.equalsIgnoreCase(this.events.get(i).getLocal())) {
                this.events.get(i).setDangerLevel(dangerlevel);
            }
        }
    }

    public synchronized boolean closeEvent(String event, int dangerlevel, String local) {
        for (int i = 0; i < this.events.size(); i++) {
            if (event.equalsIgnoreCase(this.events.get(i).getEvent())
                    && local.equalsIgnoreCase(this.events.get(i).getLocal())) {
                this.events.remove(i);
                return true;
            }
        }

        return false;
    }

    public ArrayList<Client> getClients() {
        return this.clients;
    }

    public ArrayList<Event> getEvents() {
        return this.events;
    }

    public synchronized void notifyClient(String username, String notification) throws IOException {
        for (int i = 0; i < this.clients.size(); i++) {
            if (username.equalsIgnoreCase(this.clients.get(i).getUsername())) {

                this.time1 = LocalTime.now().toSecondOfDay();
                this.messageToClients.get(i).println(notification);
                this.time2 = LocalTime.now().toSecondOfDay();

                this.timeTakenToNotify = this.time2 - this.time1;
                this.nClientsNotified++;
            }
        }
    }

    public synchronized void notifyAllClientsTCP(String notification) throws IOException {
        for (int i = 0; i < this.clients.size(); i++) {

            this.time1 = LocalTime.now().toSecondOfDay();
            this.messageToClients.get(i).println(notification);
            this.time2 = LocalTime.now().toSecondOfDay();

            this.timeTakenToNotify = this.time2 - this.time1;
            this.nClientsNotified++;
        }
    }

    public synchronized void notifyAllClientsUDP(String notification) throws SocketException, UnknownHostException, IOException {
        DatagramSocket datagramSocket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(this.multicastIP);
        byte[] messageToBytes = notification.getBytes();

        this.time1 = LocalTime.now().toSecondOfDay();
        DatagramPacket packet = new DatagramPacket(messageToBytes, messageToBytes.length, group, this.clientMulticastPortNumber);
        datagramSocket.send(packet);
        datagramSocket.close();
        this.time2 = LocalTime.now().toSecondOfDay();
        this.timeTakenToNotify = this.time2 - this.time1;
        this.nClientsNotified = this.clients.size();
    }

    public synchronized void setCentralServerReportsString(String centralServerReportsString) {
        this.centralServerReportsString = centralServerReportsString;
    }

    public String getCentralServerReportsString() {
        return centralServerReportsString;
    }

    public synchronized void setClientsReportsString(String clientsReportsString) {
        this.clientsReportsString = clientsReportsString;
    }

    public String getClientsReportsString() {
        return this.clientsReportsString;
    }

    public void resetTimeTakenToNotify() {
        this.timeTakenToNotify = 0;
    }

    public double getTimeTakenToNotify() {
        return this.timeTakenToNotify;
    }

    public void resetNClientsNotified() {
        this.nClientsNotified = 0;
    }

    public int getNClientsNotified() {
        return this.nClientsNotified;
    }
}
