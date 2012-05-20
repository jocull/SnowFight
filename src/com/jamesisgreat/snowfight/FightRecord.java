/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import java.util.Date;
import org.bukkit.entity.Player;

public class FightRecord {
    public Player ThisPlayer;
    public Integer ScoreCount = 0;
    public Integer HitCount = 0;
    private long _LastKitRequest = 0;
    private final int KIT_REQUEST_INTERVAL = 30 * 1000; //Milliseconds!!!
    
    public FightRecord(Player player){
        ThisPlayer = player;
    }
    
    public void setLastKitRequest(){
        _LastKitRequest = new Date().getTime();
    }
    
    public boolean canRequestKit(){
        return new Date().getTime() - _LastKitRequest > KIT_REQUEST_INTERVAL;
    }
    
    public int secondsTilRequestKit(){
        int remaining = (int)(KIT_REQUEST_INTERVAL - (new Date().getTime() - _LastKitRequest)) / 1000;
        if(remaining <= 0)
            return 0;
        else
            return remaining;
    }
}
