package Protocols;

import Models.Worker;
import java.io.IOException;

public class WorkerLoginProtocol {

    private final Worker worker = new Worker();
    private String username = null;
    private String password = null;

    private final int sentUsernameRequest = 0;
    private final int sentPasswordRequest = 1;
    private final int checkIfValid = 2;
    private int state = sentUsernameRequest;

    private final String welcomeMessage = "[SERVER] Será iniciado o processo do Login!";
    private final String answerUsername = "[SERVER] Indique o seu username: ";
    private final String answerPassword = "[SERVER] Indique a sua password: ";
    private final String sucessAuthen = "[SERVER] Sucesso na autenticação!";
    private final String erroAuthen = "[SERVER] Erro na autenticação!";

    /*Método responsável por retornar a welcomeMessage*/
    public String welcomeMessage() {
        return this.welcomeMessage;
    }

    /**
     * Método responsável por processar o input do utilizador e atribuir uma
     * resposta.
     * 
     * @param input - input do utilizador
     * @return - resposta consoante o input do utilizador
     * @throws IOException
     */
    public String processInput(String input) throws IOException {

        /*Caso o estado seja sentUsernameRequest, é pedido ao utilizador para introduzir o seu username*/
        if (state == sentUsernameRequest) {
            state = sentPasswordRequest;
            return answerUsername;
        } else {
            /*Caso o estado seja sentPasswordRequest, é atribuido à variável username o input do utilizador
            e é pedido ao utilizador que insira a password*/
            if (state == sentPasswordRequest) {
                this.username = input;
                state = checkIfValid;
                return answerPassword;
            } else {
                /*Caso o estado seja checkIfValid, é atribuido à variavel password o input do utilizador
                e é realizado o login. Caso o login seja realizado com sucesso, é retornado uma mensagem de sucesso,
                caso o login falhe, é retornado uma mensagem de erro na autenticação*/
                if (state == checkIfValid) {
                    this.password = input;
                    
                    if (!this.worker.login(this.username, this.password)) {
                        state = sentUsernameRequest;
                        return erroAuthen;
                    } else {
                        return sucessAuthen;
                    }
                }
            }
        }

        return null;
    }

    public Worker getWorker() {
        return this.worker;
    }
}
