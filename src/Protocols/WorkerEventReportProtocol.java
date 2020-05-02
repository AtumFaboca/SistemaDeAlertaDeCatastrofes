package Protocols;

import Models.Event;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

public class WorkerEventReportProtocol {

    private final ArrayList<Event> events = new ArrayList<>();
    private String workerLocalization = null;
    private String event = null;
    private int dangerlevel = 0;
    private String local = null;

    private final String[] validEvents = {"Terramoto", "Tsunami", "Incendio", "Acidente nuclear"};
    private String[] validLocals = null;

    private final int sentEventRequest = 0;
    private final int sentDangerLevelRequest = 1;
    private final int sentLocalRequest = 2;
    private final int stillGoing = 3;
    private final int confirm = 4;
    private int state = sentEventRequest;

    private final String welcomeMessage = "[SERVER] Será iniciado o processo do report dum evento!";
    private final String answerEvent = "[SERVER] Indique o evento: ";
    private final String errorEvent = "[SERVER] Os eventos válidos são: Terramoto, Tsunami, Incendio, Acidente nuclear.";
    private final String answerDangerLevel = "[SERVER] Indique o nivel de perigo (entre 1 e 3): ";
    private final String errorDangerLevel = "[SERVER] Inseriu um valor inválido para o nível de perigo! Indique o nivel de perigo novamente (entre 1 e 3): ";
    private final String moreLocals = "[SERVER] Existem mais locais, para além do local que supervisiona, a serem afetados? (Sim/Nao) ";
    private final String moreLocalsAfter = "[SERVER] Existem ainda mais locais a serem afetados? (Sim/Nao) ";
    private final String errorMoreLocals = "[SERVER] Resposta inválida. Existem mais locais a serem afetados? (Sim/Nao) ";
    private final String moreLocalsLocal = "[SERVER] Indique um desses locais: ";
    private String invalidLocal = null;
    private final String sucess = "[SERVER] Sucesso no reporte!";
    private final String erro = "[SERVER] Erro no reporte!";

    public WorkerEventReportProtocol(String localization) throws FileNotFoundException {
        this.workerLocalization = localization;
        this.getValidLocals();
    }

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
            /*Caso o estado seja sentDangerLevelRequest*/
            if (state == sentDangerLevelRequest) {

                /*É verificado se o utilizador inseriu um evento válido*/
                if (this.checkEvent(input)) {
                    this.event = input;

                    /*Se for um Acidente nuclear, o dangerlevel é logo atribuido 3
                    e o local de incidência a localização do Worker e finaliza o reporte*/
                    if (this.event.equalsIgnoreCase("Acidente nuclear")) {
                        this.events.add(new Event(this.event, 3, this.workerLocalization));
                        return sucess;
                    } else {
                        /*Se for um outro evento, é então pedido ao utilizador que 
                        agora insira o dangerlevel do evento*/
                        state = sentLocalRequest;
                        return answerDangerLevel;
                    }

                } else {
                    state = sentEventRequest;
                    return errorEvent;
                }

            } else {
                /*Caso o estado seja sentLocalRequest*/
                if (state == sentLocalRequest) {

                    /*É verificado se o utilizador atribuiu um dangerlevel válido*/
                    if (!input.equalsIgnoreCase("1")
                            && !input.equalsIgnoreCase("2")
                            && !input.equalsIgnoreCase("3")) {
                        return errorDangerLevel;
                    } else {
                        /*Se for válido, é imediatamente criado o evento na zona
                        do Worker*/
                        this.dangerlevel = Integer.valueOf(input);
                        this.events.add(new Event(this.event, this.dangerlevel, this.workerLocalization));
                        this.removePossibleLocal(this.workerLocalization);

                        /*Se existirem registadas proteções civis noutros locais
                        e o dangerlevel do evento for 2, é questionado ao utilizador
                        se existem mais locais a serem afetados por este evento*/
                        if (this.dangerlevel == 2 && this.validLocals.length != 0) {
                            state = confirm;
                            return moreLocals;
                        } else {
                            /*Caso contrário, o reporte é dado como concluído*/
                            return sucess;
                        }
                    }
                } else {
                    /*Caso o estado seja confirm, é validado o input do utilizador*/
                    if (state == confirm) {
                        /*Caso o input do utilizador seja sim, é dito ao utilizador
                        para indicar um desses locais afetados*/
                        if (input.equalsIgnoreCase("Sim")) {
                            state = stillGoing;
                            return moreLocalsLocal;
                        } else {
                            /*Caso o input do utilizador seja não, é dado como
                            concluído o processo de reporte*/
                            if (input.equalsIgnoreCase("Nao")) {
                                return sucess;
                            } else {
                                /*Se não for nem sim nem não, é dito ao utilizador
                                que a resposta é inválida e para que este insira
                                Sim ou Não*/
                                return errorMoreLocals;
                            }
                        }
                    } else {
                        /*Caso o estado seja stillGoing*/
                        if (state == stillGoing) {

                            /*É verificado se o utilizador indicou um local válido*/
                            if (this.checkLocal(input)) {
                                this.local = input;
                                this.events.add(new Event(this.event, this.dangerlevel, this.local));
                                this.removePossibleLocal(this.local);

                                /*Se não houverem mais locais possíveis de indicar
                                é terminado o processo de reporte*/
                                if (this.validLocals.length == 0) {
                                    return sucess;
                                } else {
                                    /*Se houverem mais locais possíveis de indicar,
                                    o define-se o estado como confirm, e é questionado
                                    novamente ao utilizador se existem mais locais a 
                                    serem afetados*/
                                    state = confirm;
                                    return moreLocalsAfter;
                                }
                            } else {
                                /*Se inseriu um local inválido,
                                é atualizada a String com os locais possíveis*/
                                this.invalidLocal = this.getPossibleLocals();
                                return invalidLocal;
                            }

                        }
                    }
                }
            }
        }
        return erro;
    }

    private boolean checkEvent(String event) {
        for (int i = 0; i < this.validEvents.length; i++) {
            if (event.equalsIgnoreCase(this.validEvents[i])) {
                return true;
            }
        }
        return false;
    }

    private boolean checkLocal(String local) {
        for (int i = 0; i < this.validLocals.length; i++) {
            if (local.equalsIgnoreCase(this.validLocals[i])) {
                return true;
            }
        }
        return false;
    }

    private String getPossibleLocals() {
        String possibleLocals = "[SERVER] Local inválido. Locais possiveis: ";
        int lastIndex = this.validLocals.length - 1;

        for (int i = 0; i < this.validLocals.length; i++) {
            if (i == lastIndex) {
                possibleLocals = possibleLocals + this.validLocals[i] + ". Indique novamente o local: ";
            } else {
                possibleLocals = possibleLocals + this.validLocals[i] + ", ";
            }
        }
        return possibleLocals;
    }

    private void removePossibleLocal(String local) {
        int nElements = this.validLocals.length - 1;

        if (nElements != 0) {
            boolean found = false;
            int position = 0;

            for (int i = 0; i < this.validLocals.length && !found; i++) {
                if (local.equalsIgnoreCase(this.validLocals[i])) {
                    position = i;
                    found = true;
                }
            }

            if (found) {
                String[] copy = new String[nElements];

                for (int i = 0; i < copy.length; i++) {
                    if (i >= position) {
                        copy[i] = this.validLocals[i + 1];
                    } else {
                        copy[i] = this.validLocals[i];
                    }
                }
                this.validLocals = copy;
            }

        } else {
            this.validLocals = new String[0];
        }
    }

    private void getValidLocals() throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        Object obj = null;
        JsonObject jsonObject = null;
        File[] files = new File("DataBase/WorkersDataBase").listFiles();
        String[] copy = new String[files.length];
        int count = 0;

        for (int i = 0; i < files.length; i++) {
            obj = parser.parse(new FileReader(files[i]));
            jsonObject = (JsonObject) obj;
            String localization = jsonObject.get("localization").getAsString();

            boolean found = false;
            for (int j = 0; j < copy.length && !found; j++) {
                if (localization.equalsIgnoreCase(copy[j])) {
                    found = true;
                }
            }

            if (!found) {
                copy[count] = localization;
                count++;
            }
        }

        this.validLocals = new String[count];

        for (int i = 0; i < count; i++) {
            this.validLocals[i] = copy[i];
        }
    }

    public ArrayList<Event> getEvents() {
        return events;
    }

}
