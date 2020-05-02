package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;

/**
 * Classe responsável por criar um listener de um cliente. Este listener
 * consiste numa Socket que será responsável por ficar à escuta de possíveis
 * notificações vindas do servidor da proteção civil.
 */
public class NotificationsHandler implements Runnable {

    private Socket clientSocket = null;
    private String username = null;
    private PrintWriter messageToWorker = null;
    private BufferedReader messageFromWorker = null;
    private String workerMessage = "undefined";

    private boolean isTCP = true;

    /*Variàveis que seriam usadas caso usa-se o método UDP*/
    private byte[] buffer = null;
    private MulticastSocket multicastSocket = null;
    private InetAddress group = null;
    private String ip = null;
    private int port = 0;

    public NotificationsHandler(String username, String HOST, int portNumber) throws IOException {
        this.username = username;
        this.clientSocket = new Socket(HOST, portNumber);
        this.messageToWorker = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.messageFromWorker = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
    }

    public NotificationsHandler(String ip, int port) throws IOException {
        this.isTCP = false;
        this.ip = ip;
        this.port = port;
        this.buffer = new byte[1024];
        this.multicastSocket = new MulticastSocket(this.port);
        this.group = InetAddress.getByName(this.ip);
        this.multicastSocket.joinGroup(this.group);

    }

    @Override
    public void run() {
        if (this.isTCP) {
            this.TCP();
        } else {
            this.UDP();
        }
    }

    /**
     * É notificado ao servidor da proteção civil que esta conecção é um
     * Listener de um cliente específico. Depois disso, ficará à espera de
     * mensagens vindas do servidor da proteção da civil, e irá mostrá-las ao
     * cliente. Ele irá fazer isto, até receber uma mensagem que indique que o
     * cliente realizou o pedido de logout, ou que o servidor ao qual se
     * encontrava conectado foi encerrado.
     */
    private void TCP() {
        try {
            this.messageToWorker.println("Listener of " + this.username);

            while (!this.workerMessage.equalsIgnoreCase("[WORKER] Logout realizado com sucesso!")
                    && !this.workerMessage.equalsIgnoreCase("[WORKER] O servidor fechou!")
                    && (this.workerMessage = this.messageFromWorker.readLine()) != null) {

                if (!this.workerMessage.equalsIgnoreCase("[WORKER] Logout realizado com sucesso!")) {
                    System.out.println(this.workerMessage);
                }

            }

            this.clientSocket.close();

        } catch (IOException e) {
            System.err.println("IOException at ClientNotificationHandlerTCP");
        }
    }

    /**
     * Ficará à espera de mensagens vindas da proteção civil, enquanto o servidor
     * da proteção civil se encontrar operacional.
     */
    private void UDP() {
        try {
            while (!this.workerMessage.equalsIgnoreCase("[WORKER] O servidor fechou!")) {
                DatagramPacket packet = new DatagramPacket(this.buffer, this.buffer.length);
                this.multicastSocket.receive(packet);
                this.workerMessage = new String(packet.getData(), packet.getOffset(), packet.getLength());
                System.out.println(this.workerMessage);
            }

            this.multicastSocket.leaveGroup(this.group);
            this.multicastSocket.close();
        } catch (IOException e) {
            //System.err.println("IOException at ClientNotificationHandlerTCP");
        }
    }

    /**
     * Método que força a paragem da multicastSocket, para que a Thread pare
     * de ficar à escuta quando o cliente realiza o logout.
     * 
     * @throws IOException 
     */
    public void closeUDP() throws IOException {
        this.multicastSocket.leaveGroup(this.group);
        this.multicastSocket.close();
    }

}
