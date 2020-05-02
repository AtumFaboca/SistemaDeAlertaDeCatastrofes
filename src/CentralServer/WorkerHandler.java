package CentralServer;

import Models.Event;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import Protocols.*;
import java.util.ArrayList;

/**
 * Esta classe é responsável por tratar dos pedidos realizados pela proteção
 * civil
 */
public class WorkerHandler implements Runnable {

    private CentralServerSharedObject sharedObject = null;
    private Socket workerSocket = null;
    private PrintWriter messageToWorker = null;
    private BufferedReader messageFromWorker = null;
    private String workerMessage = "undefined";

    private String workerUsername = "undefined";
    private String workerLocalization = "undefined";

    public WorkerHandler(CentralServerSharedObject sharedObject, Socket workerSocket) throws IOException {
        this.sharedObject = sharedObject;
        this.workerSocket = workerSocket;
        this.messageToWorker = new PrintWriter(this.workerSocket.getOutputStream(), true);
        this.messageFromWorker = new BufferedReader(new InputStreamReader(this.workerSocket.getInputStream()));
    }

    /**
     * Método run que irá ler a mensagem da proteção civil, e que dependendo
     * desta, irá reencaminhar para os diferentes métodos desta classe.
     */
    @Override
    public void run() {
        try {
            this.workerMessage = this.messageFromWorker.readLine();

            if (this.workerMessage.equalsIgnoreCase("newWorker")) {
                this.newWorker();
            } else if (this.workerMessage.startsWith("Listener of")) {
                this.newListener();
            } else if (this.workerMessage.equalsIgnoreCase("Client Login")) {
                this.clientLogin();
            } else if (this.workerMessage.equalsIgnoreCase("Client Logout")) {
                this.clientLogout();
            }

        } catch (IOException e) {
            System.err.println("IOException at WorkerHandler!");
        }
    }

    /**
     * Método que representa o menu inicial da proteção civil, onde é
     * questionado se o utilizador pretende realizar o login ou o registo.
     *
     * @throws IOException
     */
    private void newWorker() throws IOException {
        boolean goNext = false;
        while (!goNext) {
            this.messageToWorker.println("[SERVER] Pretende realizar o login, o registo ou sair: ");
            this.workerMessage = this.messageFromWorker.readLine();

            if (this.workerMessage != null) {
                if (this.workerMessage.equalsIgnoreCase("login") || this.workerMessage.equalsIgnoreCase("registo") || this.workerMessage.equalsIgnoreCase("sair")) {
                    goNext = true;

                } else {
                    this.messageToWorker.println("[SERVER] Inseriu uma resposta inválida!");
                }
            }
        }

        if (this.workerMessage.equalsIgnoreCase("login")) {
            this.workerLogin();
        } else {
            if (this.workerMessage.equalsIgnoreCase("registo")) {
                this.workerRegistration();
            } else {
                this.messageToWorker.println("[SERVER] Saiu da aplicação!");
            }
        }

    }

    /**
     * Método responsável pelo login de um worker, utilizando o protocolo de
     * WorkerLoginProtocol.
     *
     * @throws IOException
     */
    private void workerLogin() throws IOException {
        WorkerLoginProtocol loginHandler = new WorkerLoginProtocol();

        String protocolAnswer = "undefined";
        this.messageToWorker.println(loginHandler.welcomeMessage());
        while (!protocolAnswer.equalsIgnoreCase("[SERVER] Sucesso na autenticação!")) {
            protocolAnswer = loginHandler.processInput(this.workerMessage);
            this.messageToWorker.println(protocolAnswer);

            if (protocolAnswer.equalsIgnoreCase("[SERVER] Erro na autenticação!")) {
                this.newWorker();
            }

            if (protocolAnswer.equalsIgnoreCase("[SERVER] Sucesso na autenticação!")) {
                int port = this.sharedObject.workerLogin(loginHandler.getWorker());
                if (port != -1) {
                    this.messageToWorker.println("[SERVER] Done!");
                    this.messageToWorker.println("[SERVER] portNumber: " + port);
                    this.messageToWorker.println("[SERVER] multicastIP: " + this.sharedObject.getMulticastIP());
                    this.messageToWorker.println("[SERVER] clientPortNumber: " + this.sharedObject.getPortNumber());
                    this.workerLocalization = this.sharedObject.getWorkerLocalization(this.workerUsername);
                    this.loggedWorker();
                } else {
                    this.messageToWorker.println("[SERVER] Esta conta já se encontra autenticada!");
                    this.newWorker();
                }

            } else {
                this.workerMessage = this.messageFromWorker.readLine();
                if (protocolAnswer.equalsIgnoreCase("[SERVER] Indique o seu username: ")) {
                    this.workerUsername = this.workerMessage;
                }
            }
        }
    }

    /**
     * Método responsável pelo logout de um worker.
     *
     * @throws IOException
     */
    private void workerLogout() throws IOException {
        String username = this.messageFromWorker.readLine();
        this.sharedObject.workerLogout(username);
        this.messageToWorker.println("[SERVER] Logout realizado com sucesso!");
        this.messageToWorker.close();
        this.messageFromWorker.close();
        this.workerSocket.close();
    }

    /**
     * Método responsável pelo registo de um worker, utilizando o protocolo
     * WorkerRegistrationProtocol.
     *
     * @throws IOException
     */
    private void workerRegistration() throws IOException {
        WorkerRegistrationProtocol registrationHandler = new WorkerRegistrationProtocol();

        String protocolAnswer = "undefined";
        this.messageToWorker.println(registrationHandler.welcomeMessage());
        while (!protocolAnswer.equalsIgnoreCase("[SERVER] Sucesso ao realizar o registo!")) {
            protocolAnswer = registrationHandler.processInput(this.workerMessage);
            this.messageToWorker.println(protocolAnswer);

            if (protocolAnswer.equalsIgnoreCase("[SERVER] Erro no registo!")
                    || protocolAnswer.equalsIgnoreCase("[SERVER] Sucesso ao realizar o registo!")) {
                this.newWorker();
            } else {
                if (!protocolAnswer.equalsIgnoreCase("[SERVER] Esse username já se encontra a ser utilizado!")) {
                    this.workerMessage = this.messageFromWorker.readLine();
                }
            }
        }
        this.newWorker();
    }

    /**
     * Após a realização do Login, é criada uma Thread de escuta, responsável
     * por receber notificações do servidor central. Este método trata de
     * "reconhecer" essa escuta do Worker, e também notifiá-la se algum evento
     * encontra-se ativo e a afetar a zona na qual o Worker encontra-se
     * responsável de monitorizar.
     *
     * @throws IOException
     */
    private void newListener() throws IOException {
        String workerUsername = this.workerMessage.replace("Listener of ", "");
        this.sharedObject.newListener(this.workerSocket, workerUsername);
        ArrayList<Event> events = this.sharedObject.getActiveEventsForWorker(workerUsername);

        if (!events.isEmpty()) {
            for (int i = 0; i < events.size(); i++) {
                this.messageToWorker.println("[SERVER] Nova ocorrência que afeta a sua zona! Notifique os seus clientes!");
                this.messageToWorker.println("[SERVER] [EVENT] " + events.get(i).getEvent());
                this.messageToWorker.println("[SERVER] [DANGERLEVEL] " + events.get(i).getDangerLevel());
                this.messageToWorker.println("[SERVER] [LOCAL] " + events.get(i).getLocal());
            }
        }
    }

    /**
     * Após realizado o login do Worker, é então chamado este método que irá
     * servir como um menu de comunicação com o Worker.
     *
     * @throws IOException
     */
    private void loggedWorker() throws IOException {
        while (!this.workerMessage.equalsIgnoreCase("Logout")) {
            if ((this.workerMessage = this.messageFromWorker.readLine()) != null) {
                if (this.workerMessage.equalsIgnoreCase("Eventos ativos")) {
                    this.getActiveEvents();
                } else if (this.workerMessage.equalsIgnoreCase("Reportar")) {
                    this.reportNewEvent();
                } else if (this.workerMessage.equalsIgnoreCase("Alterar estado do evento")) {
                    this.changeEventState();
                } else if (this.workerMessage.equalsIgnoreCase("Concluir evento")) {
                    this.closeEvent();
                } else if (this.workerMessage.equalsIgnoreCase("Logout")) {
                    this.workerLogout();
                }
            }
        }
    }

    /**
     * Método responsável pela notificação so servidor central de quando um novo
     * cliente se conecta a um Worker.
     *
     * @throws IOException
     */
    private void clientLogin() throws IOException {
        String clientUsername = this.messageFromWorker.readLine();
        String workerUsername = this.messageFromWorker.readLine();
        if (this.sharedObject.clientLogin(clientUsername, workerUsername)) {
            this.messageToWorker.println("[SERVER] Cliente válido!");
        } else {
            this.messageToWorker.println("[SERVER] Cliente inválido!");
        }
    }

    /**
     * Método responsável pela notificação ao servidor central de quando um
     * cliente se desconecta de um Worker.
     *
     * @throws IOException
     */
    private void clientLogout() throws IOException {
        String clientUsername = this.messageFromWorker.readLine();
        if (this.sharedObject.clientLogout(clientUsername)) {
            this.messageToWorker.println("[SERVER] Realizado o logout do cliente!");
        } else {
            this.messageToWorker.println("[SERVER] Erro no logout do cliente!");
        }
    }

    /**
     * Método responsável por obter os eventos ativos e informar o Worker
     * destes.
     *
     */
    private void getActiveEvents() {
        ArrayList<Event> events = this.sharedObject.getActiveEvents();
        if (events.isEmpty()) {
            this.messageToWorker.println("[SERVER] Não existem eventos ativos!");
        } else {
            this.messageToWorker.println("--------------------- [EVENTOS ATIVOS] ---------------------");
            for (int i = 0; i < events.size(); i++) {
                this.messageToWorker.println("[EVENTO] " + events.get(i).getEvent());
                this.messageToWorker.println("[DANGERLEVEL] " + events.get(i).getDangerLevel());
                this.messageToWorker.println("[LOCAL] " + events.get(i).getLocal());
                this.messageToWorker.println("------------------------------------------------------------");
            }
        }
        this.messageToWorker.println("[SERVER] Done!");
    }

    /**
     * Método responsável por tratar do reporte de um novo evento vindo do
     * Worker, utilizando o WorkerEventReportProtocol.
     *
     * @throws IOException
     */
    private void reportNewEvent() throws IOException {
        WorkerEventReportProtocol reportHandler = new WorkerEventReportProtocol(this.workerLocalization);

        String protocolAnswer = "undefined";
        this.messageToWorker.println(reportHandler.welcomeMessage());
        while (!protocolAnswer.equalsIgnoreCase("[SERVER] Sucesso no reporte!")) {
            protocolAnswer = reportHandler.processInput(this.workerMessage);
            this.messageToWorker.println(protocolAnswer);

            if (!protocolAnswer.equalsIgnoreCase("[SERVER] Sucesso no reporte!")
                    && !protocolAnswer.equalsIgnoreCase("[SERVER] Os eventos válidos são: Terramoto, Tsunami, Incendio, Acidente nuclear.")) {
                this.workerMessage = this.messageFromWorker.readLine();
            }
        }

        this.sharedObject.newEvents(reportHandler.getEvents());
    }

    /**
     * Método responsável por tratar da alteração do "estado do evento".
     * O estado do evento que pode-se mudar é relativo ao nível de perigo do
     * evento, que ao longo do tempo pode aumentar ou diminuir.
     * 
     * @throws IOException 
     */
    private void changeEventState() throws IOException {
        String username = this.messageFromWorker.readLine();
        this.messageToWorker.println("[SERVER] Início do pedido de alteração de estado do evento.");
        this.messageToWorker.println("[SERVER] Indique o evento: ");
        String event = this.messageFromWorker.readLine();
        this.messageToWorker.println("[SERVER] Indique o novo nível de perigo (entre 1 e 3): ");
        String dangerlevel = this.messageFromWorker.readLine();

        if (!dangerlevel.equalsIgnoreCase("1")
                && !dangerlevel.equalsIgnoreCase("2")
                && !dangerlevel.equalsIgnoreCase("3")) {
            this.messageToWorker.println("[SERVER] Inseriu um valor inválido. O seu pedido de alteração de estado de evento foi cancelado.");
        } else {
            this.messageToWorker.println("[SERVER] Sucesso na alteração do estado do evento!");
            this.sharedObject.changeEventState(username, event, Integer.valueOf(dangerlevel));
        }
    }

    /**
     * Método responsável por dar como resolvida uma ocorrência.
     *
     * @throws IOException
     */
    private void closeEvent() throws IOException {
        String username = this.messageFromWorker.readLine();
        this.messageToWorker.println("[SERVER] Início do pedido de conclusão do evento.");
        this.messageToWorker.println("[SERVER] Indique o evento: ");
        String event = this.messageFromWorker.readLine();
        if (this.sharedObject.closeEvent(event, username)) {
            this.messageToWorker.println("[SERVER] Sucesso na alteração do estado evento!");
        } else {
            this.messageToWorker.println("[SERVER] Erro na alteração do estado do evento.");
        }
    }
}
