package Protocols;

import Models.Client;
import java.io.IOException;

public class ClientRegistrationProtocol {

    private final Client client = new Client();
    private String username = null;
    private String password = null;
    private String name = null;

    private final int sentUsernameRequest = 0;
    private final int sentPasswordRequest = 1;
    private final int sentNameRequest = 2;
    private final int almostDone = 3;
    private int state = sentUsernameRequest;

    private final String welcomeMessage = "[WORKER] Será iniciado o processo de registo!";
    private final String answerUsername = "[WORKER] Indique um username: ";
    private final String answerUsernameError = "[WORKER] Esse username já se encontra a ser utilizado!";
    private final String answerPassword = "[WORKER] Indique uma password: ";
    private final String answerName = "[WORKER] Indique o seu nome: ";
    private final String sucessAuthen = "[WORKER] Sucesso ao realizar o registo!";
    private final String erroAuthen = "[WORKER] Erro no registo!";

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
     */
    public String processInput(String input) throws IOException {
        
        /*Caso o estado seja sentUsernameRequest, é pedido ao utilizador para
        que insira o username*/
        if (state == sentUsernameRequest) {
            state = sentPasswordRequest;
            return answerUsername;
        } else {
            
            /*Caso o estado seja sentPasswordRequest, é verificado se o username (input)
            inserido pelo utilizador é válido. Caso seja válido, é atribuido à variável username
            o input do utilizador, e pedido a este que insira a password. Caso seja inválido,
            é pedido que insira novamente um outro username.*/
            if (state == sentPasswordRequest) {
                if (this.restrictWords(input) && this.client.isUsernameFree(input)) {
                    this.username = input;
                    state = sentNameRequest;
                    return answerPassword;
                } else {
                    state = sentUsernameRequest;
                    return answerUsernameError;
                }
            } else {
                
                /*Caso o estado seja sentNameRequest, é atribuido à variavel password
                o input do utilizador, e é lhe perguntado o seu nome*/
                if (state == sentNameRequest) {
                    this.password = input;
                    state = almostDone;
                    return answerName;
                } else {
                    
                    /*Caso o estado seja almostDone, é atribuido à variavel name o input
                    do utilizador, e é realizado o registo*/
                    if (state == almostDone) {
                        this.name = input;

                        if (!this.client.registration(this.username, this.password, this.name)) {
                            state = sentUsernameRequest;
                            return erroAuthen;
                        } else {
                            return sucessAuthen;
                        }
                    }
                }
            }
        }

        return erroAuthen;
    }

    private boolean restrictWords(String input) {
        if (input.contains("<html>")) {
            return false;
        }
        if (input.contains("</html>")) {
            return false;
        }
        if (input.contains("<br>")) {
            return false;
        }
        return true;
    }
}
