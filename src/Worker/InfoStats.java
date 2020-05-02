package Worker;

public class InfoStats implements Runnable {

    private boolean running = true;
    private WorkerSharedObject sharedObject = null;

    public InfoStats(WorkerSharedObject sharedObject) {
        this.sharedObject = sharedObject;
    }

    @Override
    public void run() {

        while (this.running) {
            this.sharedObject.resetNClientsNotified();
            this.sharedObject.resetTimeTakenToNotify();

            try {
                /*A thread irá realizar reports a cada 1 minuto*/
                Thread.sleep(1000 * 60);
                System.out.println("[SUMÁRIO DO QUE FOI REALIZADO NO ÚLTIMO MINUTO]\n"
                        + "[NÚMERO DE CLIENTES NOTIFICADOS: " + this.sharedObject.getNClientsNotified() + "]\n"
                        + "[TEMPO QUE DEMOROU A NOTIFICAR ESSE NÚMERO DE CLIENTES: " + this.sharedObject.getTimeTakenToNotify() + "]");
            } catch (InterruptedException e) {
                this.running = false;
            }
        }
    }

}
