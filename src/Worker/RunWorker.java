package Worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class RunWorker {

    private static final String HOST = "localhost";
    private static String username = null;
    private static int portNumber = 0;
    private static String MulticastIP = "0.0.0.0";
    private static int clientPortNumber = 0;
    private static boolean isConnected = false;
    private static final int centralServerPort = 2050;

    private static JFrame clientsNotificationsWindow = null;
    private static JLabel clientLabel = null;

    public static void main(String[] args) throws IOException {

        /*Início da tentativa de estabelecer comunicação com o servidor central*/
        Socket workerSocket = null;
        PrintWriter messageToServer = null;
        BufferedReader serverAnswer = null;

        boolean isCentralServerOnline = true;

        try {
            workerSocket = new Socket(HOST, centralServerPort);
            messageToServer = new PrintWriter(workerSocket.getOutputStream(), true);
            serverAnswer = new BufferedReader(new InputStreamReader(workerSocket.getInputStream()));
        } catch (IOException e) {
            isCentralServerOnline = false;
            System.out.println("O servidor central não se encontra operacional!");
        }

        if (isCentralServerOnline) {
            BufferedReader inputFromWorker = new BufferedReader(new InputStreamReader(System.in));
            String workerInput = "undefined";
            String serverOutput = "undefined";

            /*Início da comunicação com o servidor central para a realização do
            Login ou do Registo do worker.
            Nota: Esta conversa irá decorrer até o worker realizar o login 
            ou quando este decidir fechar a aplicação.*/
            messageToServer.println("newWorker");
            while (!serverOutput.equalsIgnoreCase("[SERVER] Saiu da aplicação!") && !isConnected) {
                serverOutput = serverAnswer.readLine();

                if (serverOutput.equalsIgnoreCase("[SERVER] Done!")) {
                    serverOutput = serverAnswer.readLine();
                    portNumber = Integer.valueOf(serverOutput.replace("[SERVER] portNumber: ", ""));
                    serverOutput = serverAnswer.readLine();
                    MulticastIP = serverOutput.replace("[SERVER] multicastIP: ", "");
                    serverOutput = serverAnswer.readLine();
                    clientPortNumber = Integer.valueOf(serverOutput.replace("[SERVER] clientPortNumber: ", ""));
                    isConnected = true;
                } else {

                    if (serverOutput.equalsIgnoreCase("[SERVER] Inseriu uma resposta inválida!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Será iniciado o processo do Login!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Sucesso na autenticação!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Erro na autenticação!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Esta conta já se encontra autenticada!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Será iniciado o processo de registo!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Esse username já se encontra a ser utilizado!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Sucesso ao realizar o registo!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Erro no registo!")
                            || serverOutput.equalsIgnoreCase("[SERVER] Saiu da aplicação!")) {
                        System.out.println(serverOutput);

                    } else {
                        System.out.print(serverOutput);
                        workerInput = inputFromWorker.readLine();
                        messageToServer.println(workerInput);

                        if (serverOutput.equalsIgnoreCase("[SERVER] Indique o seu username: ")) {
                            username = workerInput;
                        }

                    }
                }

            }

            /*Caso então este tenha desejado sair da aplicação, é uma boa prática
            fechar as conecções abertas.*/
            if (serverOutput.equalsIgnoreCase("[SERVER] Saiu da aplicação!")) {
                messageToServer.close();
                serverAnswer.close();
                workerSocket.close();
            }

            /*Se o Login foi realizado com sucesso*/
            if (isConnected) {

                /*Shared object que irá conter várias informações importantes
                para o funcionamento das diferentes Threads*/
                WorkerSharedObject sharedObject = new WorkerSharedObject(MulticastIP, clientPortNumber);

                /*ServerSocket que do worker que será criado na porta atribuída
                pelo servidor central*/
                ServerSocket serverSocket = new ServerSocket(portNumber);

                /*Thread responsável por receber notificações do servidor central*/
                Thread notificationsWorkerHandler = new Thread(new NotificationsHandler(sharedObject, username, HOST, centralServerPort));
                notificationsWorkerHandler.start();

                /*Thread responsável por reportar periodicamente o número de clientes
                notificados e o tempo que demorou para notificar esse número de clientes*/
                Thread infoStats = new Thread(new InfoStats(sharedObject));
                infoStats.start();

                /*Thread que será responsável por receber input do worker enquanto 
                este se encontra operacional
                O último parâmetro no construtor indica se é para usar o método
                UDP ou TCP*/
                boolean isTCP = false;
                Thread inputWorkerHandler = new Thread(new InputHandler(sharedObject, serverSocket, workerSocket, username, isTCP));
                inputWorkerHandler.start();

                /*Aqui o servidor do worker estará operacional e irá tratar dos 
                pedidos efetuados pelos clientes*/
                createClientsNotificationsInterface();
                while (inputWorkerHandler.isAlive() && !serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        Thread clientHandler = new Thread(new ClientHandler(clientLabel, sharedObject, HOST, centralServerPort, MulticastIP, clientPortNumber, username, clientSocket));
                        clientHandler.start();

                    } catch (IOException ex) {
                        infoStats.interrupt();
                        clientsNotificationsWindow.dispose();
                        /*A exception aqui não será tratada porque ela será só
                    lançada quando o worker realiza o logout. O motivo disso,
                    é porque forçamos o encerramento do método serverSocket.accept()
                    quando realizamos o logout, logo, o lançamento da exceção já
                    é esperado e propositado.*/
                    }

                }

                serverSocket.close();
            }
        }
    }

    /**
     * Método responsável pela criação da JFrame e do JLabel (interfaces)
     */
    private static void createClientsNotificationsInterface() {
        clientsNotificationsWindow = new JFrame("Notificações dos Clientes");
        clientLabel = new JLabel();
        clientsNotificationsWindow.add(clientLabel);
        clientsNotificationsWindow.setSize(500, 250);
        clientsNotificationsWindow.setLocation(700, 100);
        clientsNotificationsWindow.setVisible(true);
    }
}
