package Client;

import Models.Port;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RunClient {

    private static final String HOST = "localhost";
    private static String username = null;
    private static int portNumber = 0;
    private static boolean isConnected = false;
    private static boolean areWorkersOnline = false;

    private static String IP = "undefined";
    private static int listenPort = 0;

    private static Object obj = null;
    private static String json = null;
    private static Port[] ports = null;

    public static void main(String[] args) throws IOException {

        BufferedReader inputFromClient = new BufferedReader(new InputStreamReader(System.in));
        /*Vamos primeiro buscar a lista de servidores da Proteção Civil online*/
        getListOfOnlineServers();

        /*Se houverem servidores da Proteção Civil online*/
        if (areWorkersOnline) {
            /*É pedido ao cliente qual o servidor da proteção civil que este quer se conectar*/
            String clientInput = "undefined";
            System.out.print("Introduza o portNumber do servidor da proteção civil ao qual se pretende conectar: ");
            boolean validPort = false;
            while (!validPort) {

                clientInput = inputFromClient.readLine();

                for (Port port : ports) {
                    if (port.getPort().equalsIgnoreCase(clientInput)) {
                        portNumber = Integer.valueOf(clientInput);
                        validPort = true;
                    }
                }

                if (!validPort) {
                    System.out.print("Introduza um valor que conste na lista: ");
                }
            }

            /*Início da tentativa de estabelecer comunicação com a proteção civil*/
            Socket clientSocket = null;
            PrintWriter messageToWorker = null;
            BufferedReader messageFromWorker = null;

            try {
                clientSocket = new Socket(HOST, portNumber);
                messageToWorker = new PrintWriter(clientSocket.getOutputStream(), true);
                messageFromWorker = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.out.println("Problema ao conectar-se ao servidor da proteção civil!");
                System.exit(1);
            }


            /*Inicio de comunicação com o Worker para a realização do login, do registo
            ou para simplesmente sair da aplicação*/
            String workerMessage = "undefined";
            messageToWorker.println("newClient");
            while (!workerMessage.equalsIgnoreCase("[WORKER] Saiu da aplicação!") && !isConnected) {
                workerMessage = messageFromWorker.readLine();

                if (workerMessage.equalsIgnoreCase("[WORKER] Done!")) {
                    workerMessage = messageFromWorker.readLine();
                    IP = workerMessage.replace("[WORKER] IP: ", "");
                    workerMessage = messageFromWorker.readLine();
                    listenPort = Integer.valueOf(workerMessage.replace("[WORKER] ClientPortNumber: ", ""));
                    isConnected = true;

                } else {

                    if (workerMessage.equalsIgnoreCase("[WORKER] Inseriu uma resposta inválida!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Será iniciado o processo do Login!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Erro na autenticação!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Esta conta já se encontra autenticada!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Será iniciado o processo de registo!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Esse username já se encontra a ser utilizado!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Sucesso ao realizar o registo!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Erro no registo!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Saiu da aplicação!")
                            || workerMessage.equalsIgnoreCase("[WORKER] Sucesso na autenticação!")) {
                        System.out.println(workerMessage);

                    } else {
                        System.out.print(workerMessage);
                        clientInput = inputFromClient.readLine();
                        messageToWorker.println(clientInput);

                        if (workerMessage.equalsIgnoreCase("[WORKER] Indique o seu username: ")) {
                            username = clientInput;
                        }

                    }
                }

            }

            messageToWorker.close();
            messageFromWorker.close();
            clientSocket.close();

            /*Se o cliente se conectou com sucesso*/
            if (isConnected) {
                boolean isTCP = false;
                NotificationsHandler notificationsHandler = null;

                if (isTCP) {
                    /*Caso queiramos usar o "método TCP"*/
                    notificationsHandler = new NotificationsHandler(username, HOST, portNumber);
                } else {
                    /*Caso queiramos usar o "método UDP"*/
                    notificationsHandler = new NotificationsHandler(IP, listenPort);
                }

                /*Thread responsável por receber notificações da proteção civil*/
                Thread notifications = new Thread(notificationsHandler);
                notifications.start();

                /*Ciclo responsável por receber input do cliente enquanto este se encontra online*/
                System.out.println("[CLIENT] Comandos disponíveis:\n-> Eventos ativos\n-> Reportar\n-> Logout");
                while (isConnected) {
                    workerMessage = "undefined";
                    clientInput = inputFromClient.readLine();

                    /*É verificada se a Thread notifications ainda se encontra operacional, porque
                    caso esta não esteja, é porque o servidor da proteção civil ao qual este cliente 
                    estava conectado, foi encerrado*/
                    if (notifications.isAlive()) {

                        System.out.println("[CLIENT] Comando inserido: " + clientInput);

                        if (!clientInput.equalsIgnoreCase("Eventos ativos")
                                && !clientInput.equalsIgnoreCase("Reportar")
                                && !clientInput.equalsIgnoreCase("Logout")) {
                            System.out.println("[CLIENT] Inseriu um comando inválido!");
                            System.out.println("[CLIENT] Comandos disponíveis:\n-> Eventos ativos\n-> Reportar\n-> Logout");
                        } else {
                            /*Caso seja introduzido um comando válido, é criada uma nova Socket
                            para comunicar com o servidor da Proteção Civil*/
                            clientSocket = new Socket(HOST, portNumber);
                            messageToWorker = new PrintWriter(clientSocket.getOutputStream(), true);
                            messageFromWorker = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                            if (clientInput.equalsIgnoreCase("Eventos ativos")) {
                                messageToWorker.println(clientInput);

                                while (!workerMessage.equalsIgnoreCase("[WORKER] Done!") && (workerMessage = messageFromWorker.readLine()) != null) {
                                    if (!workerMessage.equalsIgnoreCase("[WORKER] Done!")) {
                                        System.out.println(workerMessage);
                                    }
                                }

                            } else if (clientInput.equalsIgnoreCase("Reportar")) {
                                messageToWorker.println(clientInput);
                                messageToWorker.println(username);

                                while (!workerMessage.equalsIgnoreCase("[WORKER] Sucesso no reporte!") && (workerMessage = messageFromWorker.readLine()) != null) {
                                    if (!workerMessage.equalsIgnoreCase("[WORKER] Sucesso no reporte!")
                                            && !workerMessage.equalsIgnoreCase("[WORKER] Será iniciado o processo do report dum evento!")) {
                                        System.out.print(workerMessage);
                                        clientInput = inputFromClient.readLine();
                                        messageToWorker.println(clientInput);
                                    } else {
                                        System.out.println(workerMessage);
                                    }
                                }

                            } else if (clientInput.equalsIgnoreCase("Logout")) {
                                messageToWorker.println(clientInput);
                                messageToWorker.println(username);
                                workerMessage = messageFromWorker.readLine();
                                System.out.println(workerMessage);
                                notificationsHandler.closeUDP();
                                isConnected = false;
                            }

                            messageToWorker.close();
                            messageFromWorker.close();
                            clientSocket.close();
                        }

                    } else {
                        System.out.println("[CLIENT] O servidor da proteção ao qual "
                                + "estava conectado, encontra-se encerrado!\n"
                                + "[CLIENT] Se quiser continuar a usufruir do "
                                + "serviço, realize novamente o login noutro "
                                + "servidor da proteção civil disponível.");
                        isConnected = false;
                    }
                }

                messageToWorker.close();
                messageFromWorker.close();
                clientSocket.close();
            }
        }
        inputFromClient.close();
    }

    /**
     * Método responsável por obter quais os servidores da proteção civil se
     * encontram online e apresentar essa lista ao cliente.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void getListOfOnlineServers() throws FileNotFoundException, IOException {
        JsonParser parser = new JsonParser();
        obj = parser.parse(new FileReader("Database/AvailablePorts.json"));
        json = obj.toString();
        ports = new Gson().fromJson(json, Port[].class);

        System.out.println("------- Servidores da proteção civil disponíveis -------");
        if (ports.length == 0) {
            System.out.println("[Não existem servidores da proteção civil disponíveis!]");
        } else {
            areWorkersOnline = true;
            for (Port port : ports) {
                System.out.println(port.toString());
            }
        }
        System.out.println("--------------------------------------------------------");
    }
}
