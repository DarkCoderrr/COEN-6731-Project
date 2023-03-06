package com.DistributedSystems.model;

public class Skiers {
	
    private int resortID;
    private String seasonID;
    private String dayID;
    private String skierID;
    private int time;
    private int liftID;


    public Skiers(int resortID, String seasonID, String dayID, String skierID, int time, int liftID ) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
        this.time = time;
        this.liftID = liftID;
       
    }

    public void setresortID(int resortID) {
        this.resortID = resortID;
    }

    public void setseasonID(String seasonID) {
        this.seasonID = seasonID;
    }

    public void setdayID(String dayID) {
        this.dayID = dayID;
    }
    
    public void setskierID(String skierID) {
        this.skierID = skierID;
    }
 
    public String getskierID() {
        return skierID;
    }

    public void settime(int time) {
        this.time = time;
    }
    
    public void setliftID(int liftID) {
        this.liftID = liftID;
    }

}
