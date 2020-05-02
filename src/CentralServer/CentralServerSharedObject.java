package CentralServer;

import Models.Event;
import Models.Port;
import Models.Worker;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * SharedObject que irá partilhar informações e recursos entre as diferentes
 * Threads
 */
public class CentralServerSharedObject {

    /*ArrayList que irá conter o username dos clientes que estão conectados*/
    private final ArrayList<String> clientsUsernames = new ArrayList<>();

    private final ArrayList<Port> ports = new ArrayList<>();

    /*ArrayList que irá associar um worker a um cliente
    A ideia é que o cliente na posição 0 no ArrayList clientsUsernames, está
    conectado através do Worker no ArrayList clientsAssociatedToWorker na posição 0*/
    private final ArrayList<String> clientsAssociatedToWorker = new ArrayList<>();

    /*ArrayList que irá conter os Workers que estão conectados*/
    private final ArrayList<Worker> workers = new ArrayList<>();

    /*ArrayList que irá conter as Sockets dos listeners dos Workers, responsáveis
    por receber notificações do servidor central*/
    private final ArrayList<Socket> workersListeners = new ArrayList<>();
    private final ArrayList<PrintWriter> messageToWorkers = new ArrayList<>();

    /*ArrayList que irá conter todos os eventos que decorreram e estão a ocorrer*/
    private final ArrayList<Event> events = new ArrayList<>();

    /*Número que representa a porta que será atribuída a um worker.
    Quando um Worker realiza o login, esta é incrementada e devolvida a esse Worker.*/
    private int portNumber = 2050;

    /*Números que irão constituir o IP multicast atribuido a um Worker*/
    private int multicastIPFirstOct = 230;
    private int multicastIPSecondOct = 0;
    private int multicastIPThirdOct = 0;
    private int multicastIPFourthOct = 0;

    /**
     * Método responsável por adicionar um cliente (realizar a autenticação
     * deste). É verificado se o cliente já não se encontra autenticado, para
     * impedir a autenticação do mesmo cliente várias vezes.
     *
     * @param clientUsername - Username do cliente
     * @param workerUsername - Username do Worker
     * @return - Retorna se a autenticação foi um sucesso ou não
     */
    public synchronized boolean clientLogin(String clientUsername, String workerUsername) {
        if (!this.clientsUsernames.isEmpty()) {
            for (int i = 0; i < this.clientsUsernames.size(); i++) {
                if (clientUsername.equalsIgnoreCase(this.clientsUsernames.get(i))) {
                    return false;
                }
            }
        }

        this.clientsUsernames.add(clientUsername);
        this.clientsAssociatedToWorker.add(workerUsername);

        return true;
    }

    /**
     * Método responsável por remover um cliente (realizar o logout deste). É
     * verificado se o cliente que pretende realizar o logout encontra-se
     * autenticado.
     *
     * @param clientUsername - Username do cliente
     * @return - Retorna se o logout foi um sucesso ou não
     */
    public synchronized boolean clientLogout(String clientUsername) {
        for (int i = 0; i < this.clientsUsernames.size(); i++) {
            if (clientUsername.equalsIgnoreCase(this.clientsUsernames.get(i))) {
                this.clientsUsernames.remove(i);
                this.clientsAssociatedToWorker.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Método responsável pelo login dum Worker. É verificado se este já não se
     * encontra autenticado.
     *
     * @param worker - Instância do worker que acabou de realizar o Login
     * @return - Retorna se o login foi um sucesso ou não
     * @throws IOException
     */
    public synchronized int workerLogin(Worker worker) throws IOException {
        if (!this.workers.isEmpty()) {
            for (int i = 0; i < this.workers.size(); i++) {
                if (worker.getUsername().equalsIgnoreCase(this.workers.get(i).getUsername())) {
                    return -1;
                }
            }
        }

        this.workers.add(worker);
        int port = this.getPortNumber();
        this.ports.add(new Port(worker.getUsername(), String.valueOf(port), worker.getLocalization()));
        this.writeToFile("DataBase/AvailablePorts.json", this.ports);

        return port;
    }

    /**
     * Método responsável pelo logout dum Worker. É verificado se este se
     * encontra autenticado.
     *
     * @param workerUsername - Username do worker que pretende realizar o logout
     * @return - Retorna se o logout foi um sucesso ou não
     * @throws IOException
     */
    public synchronized boolean workerLogout(String workerUsername) throws IOException {
        for (int i = 0; i < this.workers.size(); i++) {
            if (workerUsername.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                this.workers.remove(i);
                this.ports.remove(i);
                this.writeToFile("DataBase/AvailablePorts.json", this.ports);
                this.closeListener(i);
                this.logoutWorkersClients(workerUsername);
                return true;
            }
        }
        return false;
    }

    /**
     * Método responsável pelo logout dos clientes conectados a um Worker quando
     * este realiza o logout.
     *
     * @param workerUsername - Username do worker que pretende realizar o logout
     */
    private synchronized void logoutWorkersClients(String workerUsername) {
        if (!this.clientsAssociatedToWorker.isEmpty()) {
            for (int i = 0; i < this.clientsUsernames.size(); i++) {
                if (workerUsername.equalsIgnoreCase(this.clientsAssociatedToWorker.get(i))) {
                    this.clientsUsernames.remove(i);
                }
            }

            while (this.clientsAssociatedToWorker.contains(workerUsername)) {
                this.clientsAssociatedToWorker.remove(workerUsername);
            }

        }
    }

    /**
     * Método responsável por adicionar um novo listener dum worker.
     *
     * @param workerSocket - Socket do listener
     * @param username - Username do worker ao qual este listener encontra-se
     * associado
     * @return - Retorna se a adição foi um sucesso ou não
     * @throws IOException
     */
    public synchronized boolean newListener(Socket workerSocket, String username) throws IOException {
        for (int i = 0; i < this.workers.size(); i++) {
            if (username.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                this.workersListeners.add(workerSocket);
                this.messageToWorkers.add(new PrintWriter(workerSocket.getOutputStream(), true));
                return true;
            }
        }
        return false;
    }

    /**
     * Método responsável por remover o Listener dum worker quando este realiza
     * o logout.
     *
     * @param position - posição no ArrayList no qual o listener que se pretende
     * fechar se encontra.
     * @throws IOException
     */
    private synchronized void closeListener(int position) throws IOException {
        this.workersListeners.remove(position);
        PrintWriter messageToWorker = this.messageToWorkers.remove(position);
        messageToWorker.println("[SERVER] Logout realizado com sucesso!");
        messageToWorker.close();
    }

    /**
     * Método que adiciona um evento ao ArrayList de eventos.
     *
     * @param events - eventos a adicionar
     * @throws IOException
     */
    public synchronized void newEvents(ArrayList<Event> events) throws IOException {
        for (int i = 0; i < events.size(); i++) {
            this.newEvent(events.get(i));
        }
    }

    /**
     * Método que complementa o método newEvents. Aqui são realizadas todas as
     * verificações para ver se o evento que vai ser adicionado é "válido", ou
     * seja, se o evento por exemplo já não se encontra reportado. Trata ainda
     * de notificar os outros workers, consoante o nível de perigo do evento.
     *
     * @param event - evento que se pretende adicionar
     * @return - se foi adicionado ou não
     * @throws IOException
     */
    private boolean newEvent(Event event) throws IOException {
        /*É verificado se ele está vazio ou não*/
        if (!this.events.isEmpty()) {
            /*É verificado se este evento que está prestes a ser adicionado já
            não se encontra reportado, ou seja, para impedir o reprote de dois
            eventos iguais*/
            for (int i = 0; i < this.events.size(); i++) {
                if (event.getEvent().equalsIgnoreCase(this.events.get(i).getEvent())
                        && event.getLocal().equalsIgnoreCase(this.events.get(i).getLocal())
                        && this.events.get(i).isIsStillGoing()) {
                    return false;
                }
            }
        }
        this.saveLogToFile(event);
        this.events.add(event);

        /*Se o nível de perigo for local, alerta-se a proteção civil daquele
        local. Se for nacional, avisam-se todos.*/
        if (event.getDangerLevel() < 3) {
            for (int j = 0; j < this.workers.size(); j++) {
                if (event.getLocal().equalsIgnoreCase(this.workers.get(j).getLocalization())) {
                    this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] Nova ocorrência que afeta a sua zona! Notifique os seus clientes!");
                    this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] [EVENT] " + event.getEvent());
                    this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] [DANGERLEVEL] " + event.getDangerLevel());
                    this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] [LOCAL] " + event.getLocal());
                }
            }
        } else {
            this.notifyAllWorkers("[SERVER] Nova ocorrência que afeta a sua zona! Notifique os seus clientes!");
            this.notifyAllWorkers("[SERVER] [EVENT] " + event.getEvent());
            this.notifyAllWorkers("[SERVER] [DANGERLEVEL] " + event.getDangerLevel());
            this.notifyAllWorkers("[SERVER] [LOCAL] " + event.getLocal());
        }
        return true;
    }

    public synchronized boolean changeEventState(String username, String event, int dangerlevel) throws IOException {
        if (this.events.isEmpty()) {
            return false;
        }

        Event reportedEvent = null;
        String local = null;
        int dangerlevelBefore = 0;

        /*Vamos buscar a localização do Worker que realizou o pedido*/
        for (int i = 0; i < this.workers.size(); i++) {
            if (username.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                local = this.workers.get(i).getLocalization();
            }
        }

        /*Verificamos se a alteração é válida*/
        for (int i = 0; i < this.events.size(); i++) {
            if (event.equalsIgnoreCase(this.events.get(i).getEvent())
                    && local.equalsIgnoreCase(this.events.get(i).getLocal())) {
                if (this.events.get(i).getDangerLevel() == dangerlevel) {
                    return false;
                } else {
                    dangerlevelBefore = this.events.get(i).getDangerLevel();
                    this.events.get(i).setDangerLevel(dangerlevel);
                    changeStateOnFileLog(this.events.get(i));
                    reportedEvent = this.events.get(i);
                }
            }
        }

        /*Vamos notificar os envolvidos*/
        if (dangerlevelBefore == 3) {
            /*Temos de notificar os outros workers que o evento acabou
                e os da zona afetada notificar a alteração do nivel de perigo*/
            Event previousEvent = new Event(event, dangerlevelBefore, local);
            for (int i = 0; i < this.workers.size(); i++) {
                if (!this.workers.get(i).getLocalization().equalsIgnoreCase(local)) {
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] Esta ocorrência foi resolvida! Notifique os seus clientes!");
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [EVENT] " + previousEvent.getEvent());
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [DANGERLEVEL] " + previousEvent.getDangerLevel());
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [LOCAL] " + previousEvent.getLocal());
                } else {
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] Houve uma alteração no nivel de perigo desta ocorrência! Notifique os seus clientes!");
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [EVENT] " + reportedEvent.getEvent());
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [DANGERLEVEL] " + reportedEvent.getDangerLevel());
                    this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [LOCAL] " + reportedEvent.getLocal());
                }
            }
        } else {
            if (dangerlevel == 3) {
                /*Temos de notificar os outros workers que existe um evento
                    de nivel 3, e notificar os da zona da alteração*/
                for (int i = 0; i < this.workers.size(); i++) {
                    if (!this.workers.get(i).getLocalization().equalsIgnoreCase(local)) {
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] Nova ocorrência que afeta a sua zona! Notifique os seus clientes!");
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [EVENT] " + reportedEvent.getEvent());
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [DANGERLEVEL] " + reportedEvent.getDangerLevel());
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [LOCAL] " + reportedEvent.getLocal());
                    } else {
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] Houve uma alteração no nivel de perigo desta ocorrência! Notifique os seus clientes!");
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [EVENT] " + reportedEvent.getEvent());
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [DANGERLEVEL] " + reportedEvent.getDangerLevel());
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [LOCAL] " + reportedEvent.getLocal());
                    }
                }
            } else {
                /*Temos de notificar os workers afetados da alteração*/
                for (int i = 0; i < this.workers.size(); i++) {
                    if (this.workers.get(i).getLocalization().equalsIgnoreCase(local)) {
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] Houve uma alteração no nivel de perigo desta ocorrência! Notifique os seus clientes!");
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [EVENT] " + reportedEvent.getEvent());
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [DANGERLEVEL] " + reportedEvent.getDangerLevel());
                        this.notifyWorker(this.workers.get(i).getUsername(), "[SERVER] [LOCAL] " + reportedEvent.getLocal());
                    }
                }
            }
        }
        return true;
    }

    /**
     * Método responsável por declarar o encerramento dum evento e por notificar
     * os outros workers, consoante o nível de perigo do evento.
     *
     * @param event - evento que se pretende encerrar
     * @param username - username do Worker que realizou o pedido
     * @return - se foi fechado com sucesso ou não
     * @throws IOException
     */
    public synchronized boolean closeEvent(String event, String username) throws IOException {
        String local = null;

        /*Vamos buscar a localização do Worker que realizou o pedido*/
        for (int i = 0; i < this.workers.size(); i++) {
            if (username.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                local = this.workers.get(i).getLocalization();
            }
        }

        /*É realizado uma procura nos eventos ativos na localização do Worker
        o evento que este pediu para que fosse encerrado*/
        for (int i = 0; i < this.events.size(); i++) {
            if (event.equalsIgnoreCase(this.events.get(i).getEvent())
                    && local.equalsIgnoreCase(this.events.get(i).getLocal())) {
                this.events.get(i).closeEvent();
                this.addEndDateToFileLog(this.events.get(i));

                /*É verificado se o nível de perigo do evento é local ou nacional
                Para decidir quais os outros workers se deve notificar*/
                if (this.events.get(i).getDangerLevel() < 3) {
                    for (int j = 0; j < this.workers.size(); j++) {
                        if (local.equalsIgnoreCase(this.workers.get(j).getLocalization())) {
                            this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] Esta ocorrência foi resolvida! Notifique os seus clientes!");
                            this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] [EVENT] " + this.events.get(i).getEvent());
                            this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] [DANGERLEVEL] " + this.events.get(i).getDangerLevel());
                            this.notifyWorker(this.workers.get(j).getUsername(), "[SERVER] [LOCAL] " + this.events.get(i).getLocal());
                        }
                    }
                } else {
                    this.notifyAllWorkers("[SERVER] Esta ocorrência foi resolvida! Notifique os seus clientes!");
                    this.notifyAllWorkers("[SERVER] [EVENT] " + this.events.get(i).getEvent());
                    this.notifyAllWorkers("[SERVER] [DANGERLEVEL] " + this.events.get(i).getDangerLevel());
                    this.notifyAllWorkers("[SERVER] [LOCAL] " + this.events.get(i).getLocal());
                }

                return true;
            }
        }

        return false;
    }

    /**
     * Método responsável por notificar um Worker específico.
     *
     * @param username - username do Worker
     * @param notification - mensagem
     * @throws IOException
     */
    private synchronized void notifyWorker(String username, String notification) throws IOException {
        for (int i = 0; i < this.workers.size(); i++) {
            if (username.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                this.messageToWorkers.get(i).println(notification);
            }
        }
    }

    /**
     * Método responsável por notificar todos os Workers conectados.
     *
     * @param notification - mensagem
     * @throws IOException
     */
    private synchronized void notifyAllWorkers(String notification) throws IOException {
        for (int i = 0; i < this.workers.size(); i++) {
            this.messageToWorkers.get(i).println(notification);
        }

    }

    public boolean isWorkerOnline(Worker worker) {
        return this.workers.contains(worker);
    }

    public ArrayList<Worker> getWorkers() {
        return this.workers;
    }

    public ArrayList<String> getClients() {
        return this.clientsUsernames;
    }

    /**
     * Método que retorna um ArrayList com apenas os eventos que se encontram
     * ativos.
     *
     * @return - ArrayList com os eventos que ainda se encontram ativos.
     */
    public ArrayList<Event> getActiveEvents() {
        ArrayList<Event> activeEvents = new ArrayList<>();

        if (!this.events.isEmpty()) {
            for (int i = 0; i < this.events.size(); i++) {
                if (this.events.get(i).isIsStillGoing()) {
                    activeEvents.add(this.events.get(i));
                }
            }
        }

        return activeEvents;
    }

    /**
     * Método que irá retornar os eventos ativos na zona do Worker que realizou
     * o pedido.
     *
     * @param workerUsername - Username do Worker que realizou o pedido
     * @return - ArrayList contendo os eventos ativos na zona do Worker
     */
    public ArrayList<Event> getActiveEventsForWorker(String workerUsername) {
        ArrayList<Event> activeEvents = new ArrayList<>();
        String local = "undefined";

        for (int i = 0; i < this.workers.size(); i++) {
            if (workerUsername.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                local = this.workers.get(i).getLocalization();
            }
        }

        if (!this.events.isEmpty()) {
            for (int i = 0; i < this.events.size(); i++) {
                if (this.events.get(i).isIsStillGoing()
                        && this.events.get(i).getLocal().equalsIgnoreCase(local)) {
                    activeEvents.add(this.events.get(i));
                } else {
                    if (this.events.get(i).isIsStillGoing()
                            && this.events.get(i).getDangerLevel() == 3) {
                        activeEvents.add(this.events.get(i));
                    }
                }
            }
        }

        return activeEvents;
    }

    public ArrayList<Event> getEvents() {
        return events;
    }

    /**
     * Método responsável por retornar uma porta de escuta disponível
     *
     * @return - Número da porta
     */
    public synchronized int getPortNumber() {
        this.portNumber++;
        return this.portNumber;
    }

    /**
     * Método responsável por retribuir um IP multicast disponível
     *
     * @return - IP multicast
     */
    public synchronized String getMulticastIP() {
        if (this.multicastIPFourthOct == 254) {
            this.multicastIPThirdOct++;
            this.multicastIPFourthOct = 1;
        } else if (this.multicastIPThirdOct == 255) {
            this.multicastIPSecondOct++;
            this.multicastIPThirdOct = 0;
            this.multicastIPFourthOct = 1;
        } else if (this.multicastIPSecondOct == 255) {
            this.multicastIPFirstOct++;
            this.multicastIPSecondOct = 0;
            this.multicastIPThirdOct = 0;
            this.multicastIPFourthOct = 1;
        } else {
            this.multicastIPFourthOct++;
        }

        return this.multicastIPFirstOct + "." + this.multicastIPSecondOct + "." + this.multicastIPThirdOct + "." + this.multicastIPFourthOct;
    }

    /**
     * Método responsável por obter a localização do Worker
     *
     * @param workerUsername - Username do Worker que queremos obter a
     * localização
     * @return - Localização do Worker
     */
    public String getWorkerLocalization(String workerUsername) {
        for (int i = 0; i < this.workers.size(); i++) {
            if (workerUsername.equalsIgnoreCase(this.workers.get(i).getUsername())) {
                return this.workers.get(i).getLocalization();
            }
        }
        return null;
    }

    /**
     * Método responsável por escrever no ficheiro json
     *
     * @param path - path do ficheiro
     * @param list - lista que contem o que vai ser guardado no ficheiro
     * @throws IOException
     */
    private <T> void writeToFile(String path, ArrayList<T> list){
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try (FileWriter file = new FileWriter(path)) {
            gson.toJson(list, file);
            file.flush();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    /**
     * Método responsável por por carregar dados do ficheiro json para um arrayList
     *
     * @param path - path do ficheiro
     * @throws FileNotFoundException
     */
    private String fromFileToList(String path) throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        Object obj = parser.parse(new FileReader(path));
        String json = obj.toString();
        
        return json;
    }
    
    /**
     * Método responsável guardar novo registo de evento no ficheiro json correspondente
     *
     * @param event - novo evento a ser guardado
     * @throws FileNotFoundException, IOException
     */
    private void saveLogToFile(Event ev) throws FileNotFoundException, IOException {
        //Start date
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        ev.setStartDate(formatter.format(date));
        
        ArrayList<Event> eventsList = new ArrayList<>();

        if (new File("Database/LogsDataBase/" + ev.getLocal() + ".json").exists()) {
            String json = this.fromFileToList("Database/LogsDataBase/" + ev.getLocal() + ".json");
            eventsList = new Gson().fromJson(json, new TypeToken<ArrayList<Event>>() {}.getType());
        }
        //ADD NEW ENTRY TO LIST
        eventsList.add(ev);
        //SAVE TO FILE
        this.writeToFile("Database/LogsDataBase/" + ev.getLocal() + ".json", eventsList);
    }
    
    /**
     * Método responsável por adicionar endDate a um evento do ficheiro json
     * quando é fechado
     *
     * @param event - evento que vai ser adicionado o endDate
     * @throws FileNotFoundException, IOException
     */
    private void addEndDateToFileLog(Event ev) throws FileNotFoundException, IOException {
        //End date
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String json = this.fromFileToList("Database/LogsDataBase/" + ev.getLocal() + ".json");
        ArrayList<Event> eventsList = new Gson().fromJson(json, new TypeToken<ArrayList<Event>>() {}.getType());
        
        for (Event e : eventsList) {
            if (e.getEvent().equalsIgnoreCase(ev.getEvent()) && e.getEndDate().equals("none")) {
                e.setEndDate(formatter.format(date));
            }
        }
        //SAVE TO FILE
        this.writeToFile("Database/LogsDataBase/" + ev.getLocal() + ".json", eventsList);
    }
    
    /**
     * Método responsável por alterar o estado de um evento no ficheiro
     *
     * @param event - evento que vai ser adicionado o endDate
     * @throws FileNotFoundException, IOException
     */
    private void changeStateOnFileLog(Event ev) throws FileNotFoundException, IOException {
        
        String json = this.fromFileToList("Database/LogsDataBase/" + ev.getLocal() + ".json");
        ArrayList<Event> eventsList = new Gson().fromJson(json, new TypeToken<ArrayList<Event>>() {}.getType());
        for (Event e : eventsList) {
            if (e.getEvent().equalsIgnoreCase(ev.getEvent()) && e.getEndDate().equals("none")) {
                e.setDangerLevel(ev.getDangerLevel());
            }
        }
        //SAVE TO FILE
        this.writeToFile("Database/LogsDataBase/" + ev.getLocal() + ".json", eventsList);
    }
}
