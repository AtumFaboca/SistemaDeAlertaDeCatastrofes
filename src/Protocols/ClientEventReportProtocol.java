package Protocols;

public class ClientEventReportProtocol {

    private String event = null;
    private int dangerlevel = 0;
    private String description = null;

    private final int sentEventRequest = 0;
    private final int sentDangerLevelRequest = 1;
    private final int sentDescriptionRequest = 2;
    private final int confirm = 3;
    private int state = sentEventRequest;

    private final String welcomeMessage = "[WORKER] Será iniciado o processo do report dum evento!";
    private final String answerEvent = "[WORKER] Indique o evento: ";
    private final String answerDangerLevel = "[WORKER] Indique o nivel de perigo (entre 1 e 3): ";
    private final String errorDangerLevel = "[WORKER] Inseriu um valor inválido! Indique o nivel de perigo novamente (entre 1 e 3): ";
    private final String answerDescription = "[WORKER] Descreva resumidamente o evento: ";
    private final String sucess = "[WORKER] Sucesso no reporte!";
    private final String erro = "[WORKER] Erro no reporte!";

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
    public String processInput(String input) {
        /*Caso o estado seja sentEventRequest, é questionado ao utilizador qual o evento*/
        if (state == sentEventRequest) {
            state = sentDangerLevelRequest;
            return answerEvent;
        } else {
            /*Caso o estado seja sentDangerLevelRequest, o input do utilizador é atribuido
            à variável de classe evento, e é questionado ao utilizador qual o nivel de perigo
            do evento*/
            if (state == sentDangerLevelRequest) {
                /*É preciso retirar por causa de não bugar as interfaces*/
                input = this.restrictWords(input);
                this.event = input;
                state = sentDescriptionRequest;
                return answerDangerLevel;
            } else {
                /*Caso o estado seja sentDescriptionRequest, é verificado se o input do utilizador
                cumpre as normas para a atribuição de um nivel de perigo. Caso cumpra, é atribuido
                à váriàvel de classe dangerlevel o input do utilizador e é pedido ao
                utilizador para adicionar uma descrição, caso não cumpra, é pedido ao utilizador que 
                novamente o nivel de perigo do evento*/
                if (state == sentDescriptionRequest) {

                    if (!input.equalsIgnoreCase("1")
                            && !input.equalsIgnoreCase("2")
                            && !input.equalsIgnoreCase("3")) {
                        return errorDangerLevel;
                    } else {
                        this.dangerlevel = Integer.valueOf(input);
                        state = confirm;
                        return answerDescription;
                    }

                } else {
                    /*Caso o estado seja confirm, é atribuido à variável description
                    o input do utilizador*/
                    if (state == confirm) {
                        /*É preciso retirar por causa de não bugar as interfaces*/
                        input = this.restrictWords(input);
                        this.description = input;
                        return sucess;
                    }
                }
            }
        }
        return erro;
    }

    private String restrictWords(String input) {
        if (input.contains("<html>")) {
            input = input.replace("<html>", "");
        }
        if (input.contains("</html>")) {
            input = input.replace("</html>", "");
        }
        if (input.contains("<br>")) {
            input = input.replace("<br>", "");
        }

        return input;
    }

    public String getEvent() {
        return event;
    }

    public int getDangerlevel() {
        return dangerlevel;
    }

    public String getDescription() {
        return description;
    }

}
