package Worker;

import Models.Event;
import Protocols.ClientEventReportProtocol;
import Protocols.ClientLoginProtocol;
import Protocols.ClientRegistrationProtocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.JLabel;

public class ClientHandler implements Runnable {

    private JLabel label = null;
    private String HOST = "localhost";
    private int centralServerPort = 2050;
    private String workerUsername = null;
    private WorkerSharedObject sharedObject = null;
    private Socket clientSocket = null;
    private String serverMessage = null;
    private PrintWriter messageToClient = null;
    private BufferedReader messageFromClient = null;
    private String clientMessage = null;

    private String MulticastIP = "undefined";
    private int clientPortNumber = 0;
    
    public ClientHandler(JLabel label, WorkerSharedObject sharedObject, String HOST, int centralServerPort, String MulticastIP, int clientPortNumber, String workerUsername, Socket clientSocket) throws IOException {
        this.label = label;
        this.sharedObject = sharedObject;
        this.HOST = HOST;
        this.centralServerPort = centralServerPort;
        this.MulticastIP = MulticastIP;
        this.clientPortNumber = clientPortNumber;
        this.workerUsername = workerUsername;
        this.clientSocket = clientSocket;
        this.messageToClient = new PrintWriter(this.clientSocket.getOutputStream(), true);
        this.messageFromClient = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
    }

    /**
     * Método run que irá ler a mensagem do cliente, e que dependendo desta, irá
     * reencaminhar para os diferentes métodos desta classe.
     */
    @Override
    public void run() {
        try {
            this.clientMessage = this.messageFromClient.readLine();

            if (this.clientMessage.startsWith("Listener of")) {
                this.sharedObject.newListener(this.clientSocket, this.clientMessage.replace("Listener of ", ""));
            } else if (this.clientMessage.equalsIgnoreCase("newClient")) {
                this.newClient();
            } else if (this.clientMessage.equalsIgnoreCase("Eventos ativos")) {
                this.getEvents();
            } else if (this.clientMessage.equalsIgnoreCase("Reportar")) {
                this.reportNewEvent();
            } else if (this.clientMessage.equalsIgnoreCase("Logout")) {
                this.clientLogout();
            }

        } catch (IOException e) {
            System.err.println("IOException at ClientHandler!");
        }
    }

    /**
     * Método que representa o menu inicial do cliente, onde é questionado se o
     * utilizador pretende realizar o login ou o registo.
     *
     * @throws IOException
     */
    private void newClient() throws IOException {
        boolean goNext = false;
        while (!goNext) {
            this.messageToClient.println("[WORKER] Pretende realizar o login, o registo ou sair: ");
            this.clientMessage = this.messageFromClient.readLine();

            if (this.clientMessage != null) {
                if (this.clientMessage.equalsIgnoreCase("login") || this.clientMessage.equalsIgnoreCase("registo") || this.clientMessage.equalsIgnoreCase("sair")) {
                    goNext = true;

                } else {
                    this.messageToClient.println("[WORKER] Inseriu uma resposta inválida!");
                }
            }

        }

        if (this.clientMessage.equalsIgnoreCase("login")) {
            this.clientLogin();
        } else {
            if (this.clientMessage.equalsIgnoreCase("registo")) {
                this.clientRegistration();
            } else {
                this.messageToClient.println("[WORKER] Saiu da aplicação!");
            }
        }
    }

    /**
     * Método responsável pelo login de um cliente, utilizando o protocolo de
     * ClientLoginProtocol.
     *
     * @throws IOException
     */
    private void clientLogin() throws IOException {
        ClientLoginProtocol loginHandler = new ClientLoginProtocol();

        String clientUsername = "undefined";
        String protocolAnswer = "undefined";
        this.messageToClient.println(loginHandler.welcomeMessage());
        while (!protocolAnswer.equalsIgnoreCase("[WORKER] Sucesso na autenticação!")) {
            protocolAnswer = loginHandler.processInput(this.clientMessage);
            this.messageToClient.println(protocolAnswer);

            if (protocolAnswer.equalsIgnoreCase("[WORKER] Erro na autenticação!")) {
                this.newClient();
            }

            if (protocolAnswer.equalsIgnoreCase("[WORKER] Sucesso na autenticação!")) {
                if (this.notifyClientLoginToServer(clientUsername, this.workerUsername) && this.sharedObject.clientLogin(loginHandler.getClient())) {
                    this.messageToClient.println("[WORKER] Done!");
                    this.messageToClient.println("[WORKER] IP: " + this.MulticastIP);
                    this.messageToClient.println("[WORKER] ClientPortNumber: " + this.clientPortNumber);
                } else {
                    this.messageToClient.println("[WORKER] Esta conta já se encontra autenticada!");
                    this.newClient();
                }

            } else {
                this.clientMessage = this.messageFromClient.readLine();
                if (protocolAnswer.equalsIgnoreCase("[WORKER] Indique o seu username: ")) {
                    clientUsername = this.clientMessage;
                }
            }

        }
    }

    /**
     * Método responsável pelo logout de um cliente.
     *
     * @throws IOException
     */
    private void clientLogout() throws IOException {
        String clientUsername = this.messageFromClient.readLine();
        if (this.sharedObject.clientLogout(clientUsername) && this.notifyClientLogoutToServer(clientUsername)) {
            this.messageToClient.println("[WORKER] Logout realizado com sucesso!");
        } else {
            this.messageToClient.println("[WORKER] Erro no logout!");
        }
    }

    /**
     * Método responsável pelo registo de um cliente, utilizando o protocolo
     * ClientRegistrationProtocol.
     *
     * @throws IOException
     */
    private void clientRegistration() throws IOException {
        ClientRegistrationProtocol registrationHandler = new ClientRegistrationProtocol();

        String protocolAnswer = "undefined";
        this.messageToClient.println(registrationHandler.welcomeMessage());
        while (!protocolAnswer.equalsIgnoreCase("[WORKER] Sucesso ao realizar o registo!")) {
            protocolAnswer = registrationHandler.processInput(this.clientMessage);
            this.messageToClient.println(protocolAnswer);

            if (protocolAnswer.equalsIgnoreCase("[WORKER] Erro no registo!")
                    || protocolAnswer.equalsIgnoreCase("[WORKER] Sucesso ao realizar o registo!")) {
                this.newClient();
            } else {
                if (!protocolAnswer.equalsIgnoreCase("[WORKER] Esse username já se encontra a ser utilizado!")) {
                    this.clientMessage = this.messageFromClient.readLine();
                }
            }
        }
        this.newClient();
    }

    /**
     * Método responsável por notificar ao servidor central que um novo cliente
     * realizou o login.
     *
     * @param clientUsername - username do Cliente
     * @param workerUsername - username do Worker
     * @return - se foi notificado com sucesso
     * @throws IOException
     */
    private boolean notifyClientLoginToServer(String clientUsername, String workerUsername) throws IOException {
        Socket workerSocket = new Socket(HOST, centralServerPort);
        PrintWriter messageToServer = new PrintWriter(workerSocket.getOutputStream(), true);
        BufferedReader messageFromServer = new BufferedReader(new InputStreamReader(workerSocket.getInputStream()));

        messageToServer.println("Client Login");
        messageToServer.println(clientUsername);
        messageToServer.println(workerUsername);
        this.serverMessage = messageFromServer.readLine();

        messageFromServer.close();
        messageToServer.close();
        workerSocket.close();

        if (this.serverMessage.equalsIgnoreCase("[SERVER] Cliente válido!")) {
            return true;
        }
        return false;
    }

    /**
     * Método responsável por notificar ao servidor central que um cliente
     * realizou o logou.
     *
     * @param clientUsername - username do Cliente
     * @param workerUsername - username do Worker
     * @return - se foi notificado com sucesso
     * @throws IOException
     */
    private boolean notifyClientLogoutToServer(String clientUsername) throws IOException {
        Socket workerSocket = new Socket(HOST, centralServerPort);
        PrintWriter messageToServer = new PrintWriter(workerSocket.getOutputStream(), true);
        BufferedReader messageFromServer = new BufferedReader(new InputStreamReader(workerSocket.getInputStream()));

        messageToServer.println("Client Logout");
        messageToServer.println(clientUsername);
        this.serverMessage = messageFromServer.readLine();

        messageFromServer.close();
        messageToServer.close();
        workerSocket.close();

        if (this.serverMessage.equalsIgnoreCase("[SERVER] Realizado o logout do cliente!")) {
            return true;
        }
        return false;
    }

    /**
     * Método responsável por processar o pedido de "Eventos ativos" do cliente.
     * É enviado ao cliente os eventos que se encontram ativos na sua zona.
     */
    private void getEvents() {
        ArrayList<Event> events = this.sharedObject.getEvents();
        if (events.isEmpty()) {
            this.messageToClient.println("[WORKER] Não existem eventos ativos!");
        } else {
            this.messageToClient.println("------------------------------------------------------------");
            for (int i = 0; i < events.size(); i++) {
                this.messageToClient.println("[EVENTO] " + events.get(i).getEvent());
                this.messageToClient.println("[DANGERLEVEL] " + events.get(i).getDangerLevel());
                this.messageToClient.println("[LOCAL] " + events.get(i).getLocal());
                this.messageToClient.println("------------------------------------------------------------");
            }
        }
        this.messageToClient.println("[WORKER] Done!");
    }

    /**
     * Método responsável por processar o pedido de "Reportar" do cliente. É
     * utilizado o ClientEventReportProtocol para a realização do reporte.
     *
     * @throws IOException
     */
    private void reportNewEvent() throws IOException {
        String clientUsername = this.messageFromClient.readLine();
        ClientEventReportProtocol reportHandler = new ClientEventReportProtocol();

        String protocolAnswer = "undefined";
        this.messageToClient.println(reportHandler.welcomeMessage());
        while (!protocolAnswer.equalsIgnoreCase("[WORKER] Sucesso no reporte!")) {
            protocolAnswer = reportHandler.processInput(this.clientMessage);
            this.messageToClient.println(protocolAnswer);

            if (!protocolAnswer.equalsIgnoreCase("[WORKER] Sucesso no reporte!")) {
                this.clientMessage = this.messageFromClient.readLine();
            }
        }

        String labelText = this.sharedObject.getClientsReportsString().replace("</html>", "")
                + "<br>[CLIENT] Novo evento reportado pelo cliente: [" + clientUsername + "]"
                + "<br>[CLIENT][EVENT] " + reportHandler.getEvent()
                + "<br>[CLIENT][DANGERLEVEL] " + reportHandler.getDangerlevel()
                + "<br>[CLIENT][DESCRIPTION] " + reportHandler.getDescription() + "<br></html>";

        this.sharedObject.setClientsReportsString(labelText);
        this.label.setText(this.sharedObject.getClientsReportsString());

        /*System.out.println("[CLIENT] Novo evento reportado pelo cliente: [" + clientUsername + "]");
        System.out.println("[CLIENT][EVENT] " + reportHandler.getEvent());
        System.out.println("[CLIENT][DANGERLEVEL] " + reportHandler.getDangerlevel());
        System.out.println("[CLIENT][DESCRIPTION] " + reportHandler.getDescription());*/
    }
}
