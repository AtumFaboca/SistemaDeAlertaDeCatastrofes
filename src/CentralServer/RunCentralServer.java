package CentralServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RunCentralServer {

    private static boolean running = true;
    private static final int centralServerPort = 2050;

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        /*Criação do servidor central*/
        ServerSocket serverSocket = new ServerSocket(centralServerPort);
        
        /*Criação do SharedObject*/
        CentralServerSharedObject sharedObject = new CentralServerSharedObject();

        /*Aqui serão tratados os pedidos dos workers*/
        while (running) {
            System.out.println("[SERVER] Waiting for connection...");
            Socket workerSocket = serverSocket.accept();
            System.out.println("[SERVER] Connected!");
            Thread workerHandler = new Thread(new WorkerHandler(sharedObject, workerSocket), "WorkerHandler");
            workerHandler.start();
        }

        serverSocket.close();

    }

}
