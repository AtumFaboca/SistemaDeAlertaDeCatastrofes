package Models;

import com.google.gson.annotations.Expose;

public class Event {
    
    @Expose
    private String event;
    @Expose
    private int dangerLevel;
    @Expose
    private String local;
    private boolean isStillGoing;
    @Expose
    private String startDate;
    @Expose
    private String endDate;
    
    public Event(String event, int dangerLevel, String local){
        this.event = event;
        this.dangerLevel = dangerLevel;
        this.local = local;
        this.isStillGoing = true;
        this.endDate = "none";
    }

    public void closeEvent(){
        this.isStillGoing = false;
    }
    
    public void setDangerLevel(int dangerLevel){
        this.dangerLevel = dangerLevel;
    }
    
    public String getEvent() {
        return event;
    }

    public int getDangerLevel() {
        return dangerLevel;
    }

    public String getLocal() {
        return local;
    }

    public boolean isIsStillGoing() {
        return isStillGoing;
    }
    
    public void setStartDate(String date){
        this.startDate = date;
    }
    
    public String getStartDate() {
        return this.startDate;
    }
    
    public void setEndDate(String date){
        this.endDate = date;
    }
    
    public String getEndDate() {
        return this.endDate;
    }
}
