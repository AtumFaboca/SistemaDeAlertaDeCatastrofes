package Protocols;

import Models.Worker;
import java.io.IOException;

public class WorkerRegistrationProtocol {

    private final Worker worker = new Worker();
    private String username = null;
    private String password = null;
    private String localization = null;

    private final int sentUsernameRequest = 0;
    private final int sentPasswordRequest = 1;
    private final int sentLocalizationRequest = 2;
    private final int almostDone = 3;
    private int state = sentUsernameRequest;

    private final String welcomeMessage = "[SERVER] Será iniciado o processo de registo!";
    private final String answerUsername = "[SERVER] Indique um username: ";
    private final String answerUsernameError = "[SERVER] Esse username já se encontra a ser utilizado!";
    private final String answerPassword = "[SERVER] Indique uma password: ";
    private final String answerLocalization = "[SERVER] Indique a localização do seu posto: ";
    private final String errorLocalization = "[SERVER] Inválido. Indique novamente a localização do seu posto: ";
    private final String sucessAuthen = "[SERVER] Sucesso ao realizar o registo!";
    private final String erroAuthen = "[SERVER] Erro no registo!";

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
                if (this.worker.isUsernameFree(input)) {
                    this.username = input;
                    state = sentLocalizationRequest;
                    return answerPassword;
                } else {
                    state = sentUsernameRequest;
                    return answerUsernameError;
                }
            } else {
                
                /*Caso o estado seja sentLocalizationRequest, é atribuido à variavel
                password o input do utilizador, e é pedido a este que insira a
                localização do seu posto*/
                if (state == sentLocalizationRequest) {
                    this.password = input;
                    state = almostDone;
                    return answerLocalization;
                } else {
                    
                    /*Caso o estado seja almostDone, é verificado se o input do utilizador
                    é válido. Caso seja válido, é atribuido à variavel localization o input
                    e é realizado o registo do utilizador.
                    Caso seja inválido, é dito ao utilizador que a localização do seu posto
                    é inválida, e para que este introduza novamente a localização do seu posto.*/
                    if (state == almostDone) {

                        if (!this.restrictWords(input)) {
                            return errorLocalization;
                        } else {
                            this.localization = input;
                            state = almostDone;

                            if (!this.worker.registration(this.username, this.password, this.localization)) {
                                state = sentUsernameRequest;
                                return erroAuthen;
                            } else {
                                return sucessAuthen;
                            }
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
