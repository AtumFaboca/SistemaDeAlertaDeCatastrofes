package Worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class NotificationsHandler implements Runnable {

    private JFrame centralServerNotificationsWindow = null;
    private JLabel centralServerLabel = null;
    private WorkerSharedObject sharedObject = null;
    private Socket workerSocket = null;
    private String username = null;
    private PrintWriter messageToServer = null;
    private BufferedReader messageFromServer = null;
    private String serverMessage = "undefined";

    public NotificationsHandler(WorkerSharedObject sharedObject, String username, String HOST, int portNumber) throws IOException {
        this.sharedObject = sharedObject;
        this.username = username;
        this.workerSocket = new Socket(HOST, portNumber);
        this.messageToServer = new PrintWriter(this.workerSocket.getOutputStream(), true);
        this.messageFromServer = new BufferedReader(new InputStreamReader(this.workerSocket.getInputStream()));
        this.createCentralServerNotificationsInterface();
    }

    @Override
    public void run() {
        try {
            this.messageToServer.println("Listener of " + this.username);
            while ((this.serverMessage = this.messageFromServer.readLine()) != null && !this.serverMessage.equalsIgnoreCase("[SERVER] Logout realizado com sucesso!")) {
                String indicator = this.serverMessage;
                //System.out.println(this.serverMessage);

                this.serverMessage = this.messageFromServer.readLine();
                String eventMessage = this.serverMessage;
                String event = this.serverMessage.replace("[SERVER] [EVENT] ", "");
                //System.out.println(this.serverMessage);

                this.serverMessage = this.messageFromServer.readLine();
                String dangerlevelMessage = this.serverMessage;
                int dangerlevel = Integer.valueOf(this.serverMessage.replace("[SERVER] [DANGERLEVEL] ", ""));
                //System.out.println(this.serverMessage);

                this.serverMessage = this.messageFromServer.readLine();
                String localMessage = this.serverMessage;
                String local = this.serverMessage.replace("[SERVER] [LOCAL] ", "");
                //System.out.println(this.serverMessage);

                String labelText = this.sharedObject.getCentralServerReportsString().replace("</html>", "")
                        + "<br>" + indicator
                        + "<br>" + eventMessage
                        + "<br>" + dangerlevelMessage
                        + "<br>" + localMessage + "<br></html>";

                this.sharedObject.setCentralServerReportsString(labelText);
                this.centralServerLabel.setText(this.sharedObject.getCentralServerReportsString());

                if (indicator.equalsIgnoreCase("[SERVER] Nova ocorrência que afeta a sua zona! Notifique os seus clientes!")) {
                    /*Nova ocorrência*/
                    this.sharedObject.newEvent(event, dangerlevel, local);
                } else if (indicator.equalsIgnoreCase("[SERVER] Houve uma alteração no nivel de perigo desta ocorrência! Notifique os seus clientes!")) {
                    /*Alteração da ocorrência*/
                    this.sharedObject.changeEvent(event, dangerlevel, local);
                } else if (indicator.equalsIgnoreCase("[SERVER] Esta ocorrência foi resolvida! Notifique os seus clientes!")) {
                    /*Fechar ocorrência*/
                    this.sharedObject.closeEvent(event, dangerlevel, local);
                }

            }

            this.centralServerNotificationsWindow.dispose();
            this.messageToServer.close();
            this.messageFromServer.close();
            this.workerSocket.close();

        } catch (IOException ex) {
            //System.err.println("IOException at WorkerNotificationHandler");
        }

    }

    private void createCentralServerNotificationsInterface() {
        this.centralServerNotificationsWindow = new JFrame("Notificações do Servidor Central");
        this.centralServerLabel = new JLabel();
        this.centralServerNotificationsWindow.add(this.centralServerLabel);
        this.centralServerNotificationsWindow.setSize(500, 250);
        this.centralServerNotificationsWindow.setLocation(100, 100);
        this.centralServerNotificationsWindow.setVisible(true);
    }

}
