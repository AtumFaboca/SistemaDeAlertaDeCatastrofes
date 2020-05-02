package Worker;

import Models.Client;
import Models.Event;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Classe responsável por processar o input inserido pelo Worker.
 *
 */
public class InputHandler implements Runnable {

    private WorkerSharedObject sharedObject = null;
    private ServerSocket workerServer = null;
    private String username = null;
    private Socket workerSocket = null;
    private PrintWriter messageToServer = null;
    private BufferedReader messageFromServer = null;
    private BufferedReader workerInputReader = null;
    private String workerInput = "undefined";
    private String serverMessage = "undefined";
    private boolean connected = false;
    private final String commands = "[WORKER] Comandos disponíveis:\n-> Eventos ativos\n-> Eventos ativos na minha zona"
            + "\n-> Reportar\n-> Alterar estado do evento\n-> Concluir evento\n-> Notificar cliente\n-> Notificar todos os clientes\n-> Logout";

    private boolean token = true;

    public InputHandler(WorkerSharedObject sharedObject, ServerSocket workerServer, Socket workerSocket, String username, boolean token) throws IOException {
        this.sharedObject = sharedObject;
        this.workerServer = workerServer;
        this.workerSocket = workerSocket;
        this.username = username;
        this.messageToServer = new PrintWriter(this.workerSocket.getOutputStream(), true);
        this.messageFromServer = new BufferedReader(new InputStreamReader(this.workerSocket.getInputStream()));
        this.workerInputReader = new BufferedReader(new InputStreamReader(System.in));
        this.connected = true;
        this.token = token;
    }

    /**
     * Método run responsável por validar o input do Worker e consoante esse
     * input, encaminhar o pedido para os diferentes métodos desta classe.
     */
    @Override
    public void run() {
        try {
            System.out.println(commands);
            while (this.connected && (this.workerInput = this.workerInputReader.readLine()) != null) {
                this.serverMessage = "undefined";
                System.out.println("[WORKER] Comando inserido: " + this.workerInput);

                if (this.workerInput.equalsIgnoreCase("Eventos ativos")) {
                    this.messageToServer.println(this.workerInput);
                    this.getCurrentEvents();

                } else if (this.workerInput.equalsIgnoreCase("Eventos ativos na minha zona")) {
                    this.getCurrentEventsInMyLocal();

                } else if (this.workerInput.equalsIgnoreCase("Reportar")) {
                    this.messageToServer.println(this.workerInput);
                    this.reportNewEvent();

                } else if (this.workerInput.equalsIgnoreCase("Alterar estado do evento")) {
                    this.messageToServer.println(this.workerInput);
                    this.changeEventState();

                } else if (this.workerInput.equalsIgnoreCase("Concluir evento")) {
                    this.messageToServer.println(this.workerInput);
                    this.closeEvent();

                } else if (this.workerInput.equalsIgnoreCase("Notificar cliente")) {
                    if (this.token) {
                        this.notifyClient();
                    } else {
                        System.out.println("Funcionalidade não disponível.");
                    }

                } else if (this.workerInput.equalsIgnoreCase("Notificar todos os clientes")) {
                    if (this.token) {
                        this.notifyClientsTCP();
                    } else {
                        this.notifyClientsUDP();
                    }

                } else if (this.workerInput.equalsIgnoreCase("Logout")) {
                    this.messageToServer.println(this.workerInput);
                    this.Logout();
                    this.connected = false;

                } else {
                    System.out.println("[WORKER] Inseriu um comando inválido!");
                    System.out.println(commands);
                }

            }

        } catch (IOException e) {
            System.err.println("IOException at WorkerInputHandler");
        }
    }

    /**
     * Método responsável por processar o pedido "Logout" do Worker. Após a
     * realização do logout no servidor central, é então notificado a todos os
     * clientes conectados a este Worker que este servidor fechou.
     *
     * @throws IOException
     */
    private void Logout() throws IOException {
        this.messageToServer.println(this.username);
        this.serverMessage = this.messageFromServer.readLine();
        System.out.println(this.serverMessage);

        /*É preciso notificar os clientes de que o servidor fechou*/
        if (this.token) {
            this.sharedObject.notifyAllClientsTCP("[WORKER] O servidor fechou!");
        } else {
            this.sharedObject.notifyAllClientsUDP("[WORKER] O servidor fechou!");
        }

        this.workerInputReader.close();
        this.messageToServer.close();
        this.messageFromServer.close();
        this.workerSocket.close();
        this.workerServer.close();
    }

    /**
     * Método responsável por processar o pedido "Eventos ativos" do Worker. O
     * Worker irá receber mensagens do servidor central enquanto a "mensagem
     * secreta" não for recebidas - neste caso, enquanto o servidor não enviar
     * "Done!".
     *
     * @throws IOException
     */
    private void getCurrentEvents() throws IOException {
        while (!serverMessage.equalsIgnoreCase("[SERVER] Done!")) {
            this.serverMessage = this.messageFromServer.readLine();
            if (!this.serverMessage.equalsIgnoreCase("[SERVER] Done!")) {
                System.out.println(this.serverMessage);
            }
        }
    }

    /**
     * Método responsável por processar o pedido "Eventos ativos na minha zona".
     * São obtidos os eventos ativos na zona no Worker, indo buscar o ArrayList
     * que contém esses eventos ao WorkerSharedObject.
     *
     */
    private void getCurrentEventsInMyLocal() {
        ArrayList<Event> events = this.sharedObject.getEvents();

        if (events.isEmpty()) {
            System.out.println("[SERVER] Não existem eventos ativos!");
        } else {
            System.out.println("--------------------- [EVENTOS ATIVOS] ---------------------");
            for (int i = 0; i < events.size(); i++) {
                System.out.println("[EVENTO] " + events.get(i).getEvent());
                System.out.println("[DANGERLEVEL] " + events.get(i).getDangerLevel());
                System.out.println("[LOCAL] " + events.get(i).getLocal());
                System.out.println("------------------------------------------------------------");
            }
        }
    }

    /**
     * Método responsável processar o pedido "Reportar". É iniciada uma
     * comunicação com o servidor central para que o Worker consiga então
     * reportar um evento. O servidor irá usar um protocolo já definido para
     * validar o input inserido pelo Worker.
     *
     * @throws IOException
     */
    private void reportNewEvent() throws IOException {
        while (!this.serverMessage.equalsIgnoreCase("[SERVER] Sucesso no reporte!")
                && (this.serverMessage = this.messageFromServer.readLine()) != null) {

            if (!this.serverMessage.equalsIgnoreCase("[SERVER] Sucesso no reporte!")
                    && !this.serverMessage.equalsIgnoreCase("[SERVER] Os eventos válidos são: Terramoto, Tsunami, Incendio, Acidente nuclear.")
                    && !this.serverMessage.equalsIgnoreCase("[SERVER] Será iniciado o processo do report dum evento!")) {
                System.out.print(this.serverMessage);
                this.workerInput = this.workerInputReader.readLine();
                this.messageToServer.println(this.workerInput);
            } else {
                System.out.println(this.serverMessage);
            }
        }
    }

    /**
     * Método responsável por processar o pedido "Alterar estado do evento". É
     * iniciada uma comunicação com o servidor central, em que o Worker terá de
     * indicar qual o evento que irá sofrer a alteração e qual o novo nivel de
     * perigo do evento.
     *
     * @throws IOException
     */
    private void changeEventState() throws IOException {
        this.messageToServer.println(this.username);
        while (!this.serverMessage.equalsIgnoreCase("[SERVER] Sucesso na alteração do estado do evento!")
                && !this.serverMessage.equalsIgnoreCase("[SERVER] Inseriu um valor inválido. O seu pedido de alteração de estado de evento foi cancelado.")
                && (this.serverMessage = this.messageFromServer.readLine()) != null) {

            if (this.serverMessage.equalsIgnoreCase("[SERVER] Indique o evento: ")
                    || this.serverMessage.equalsIgnoreCase("[SERVER] Indique o novo nível de perigo (entre 1 e 3): ")) {
                System.out.print(this.serverMessage);
                this.workerInput = this.workerInputReader.readLine();
                this.messageToServer.println(this.workerInput);
            } else {
                System.out.println(this.serverMessage);
            }
        }
    }

    /**
     * Método responsável por processar o pedido "Concluir evento". É iniciada
     * uma comunicação com o servidor central, em que o Worker terá de indicar
     * qual o evento que este pretende dar como concluído.
     *
     * @throws IOException
     */
    private void closeEvent() throws IOException {
        this.messageToServer.println(this.username);
        while (!this.serverMessage.equalsIgnoreCase("[SERVER] Sucesso na alteração do estado evento!")
                && !this.serverMessage.equalsIgnoreCase("[SERVER] Erro na alteração do estado do evento.")
                && (this.serverMessage = this.messageFromServer.readLine()) != null) {

            if (this.serverMessage.equalsIgnoreCase("[SERVER] Indique o evento: ")) {
                System.out.print(this.serverMessage);
                this.workerInput = this.workerInputReader.readLine();
                this.messageToServer.println(this.workerInput);
            } else {
                System.out.println(this.serverMessage);
            }
        }
    }

    /**
     * Método responsável por processar o pedido "Notificar cliente". É
     * apresentado ao Worker a lista de clientes que se encontram conectados a
     * este, e depois é perguntado qual o cliente que este pretende conectar e
     * qual a mensagem que este quer-lhe enviar.
     *
     * @throws IOException
     */
    private void notifyClient() throws IOException {
        ArrayList<Client> clients = this.sharedObject.getClients();

        System.out.println("-------- Clientes Disponíveis --------");
        if (clients.isEmpty()) {
            System.out.println("[Não existem clientes conectados!]");
            System.out.println("--------------------------------------");
        } else {
            for (int i = 0; i < clients.size(); i++) {
                System.out.println("[Username] " + clients.get(i).getUsername());
                System.out.println("[Nome] " + clients.get(i).getName());
                System.out.println("--------------------------------------");
            }

            String clientUsername = "undefined";
            String notification = "undefined";
            this.workerInput = "undefined";
            boolean validClient = false;
            while (!validClient) {
                System.out.print("[WORKER] Indique o username do cliente que pretende contactar: ");
                this.workerInput = this.workerInputReader.readLine();

                for (int i = 0; i < clients.size(); i++) {
                    if (this.workerInput.equalsIgnoreCase(clients.get(i).getUsername())) {
                        clientUsername = this.workerInput;
                        validClient = true;
                    }
                }

                if (!validClient) {
                    System.out.println("[WORKER] Inseriu um username que não consta na lista!");
                }
            }

            System.out.print("[WORKER] Escreva a mensagem que quer mandar ao cliente: ");
            notification = "[WORKER] " + this.workerInputReader.readLine();

            this.sharedObject.notifyClient(clientUsername, notification);
            System.out.println("[WORKER] Cliente " + clientUsername + " notificado com sucesso!");
        }

    }

    /**
     * Método responsável por processar o pedido "Notificar todos os clientes".
     * É perguntada ao Worker qual a mensagem que este quer enviar aos seus
     * clientes, e depois usando o SharedObject onde terá todos os Listeners
     * relativos a cada cliente, será enviada a mensagem através desses
     * listeners.
     *
     * @throws IOException
     */
    private void notifyClientsTCP() throws IOException {
        System.out.print("[WORKER] Insira a mensagem que quer enviar aos clientes: ");
        this.workerInput = "[WORKER] " + this.workerInputReader.readLine();
        this.sharedObject.notifyAllClientsTCP(this.workerInput);
        System.out.println("[WORKER] Mensagem enviada aos seus clientes com sucesso!");
    }

    /**
     * Este método não se encontra operacional, no entanto, decidiu-se deixá-lo
     * aqui. Basicamente, este era a alternativa aos "listeners" que foi usada
     * para notificar os clientes - utilizando DatagramSockets.
     *
     * @throws IOException
     */
    private void notifyClientsUDP() throws IOException {
        System.out.print("[WORKER][UDP] Insira a mensagem que quer enviar aos clientes: ");
        String multicastMessage = "[WORKER] " + this.workerInputReader.readLine();
        this.sharedObject.notifyAllClientsUDP(multicastMessage);
        System.out.println("[WORKER] Mensagem enviada aos seus clientes com sucesso!");
    }
}
