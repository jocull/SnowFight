/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import java.util.TimerTask;

public class FightTimerTask extends TimerTask {

    private SnowFight _Plugin;
    private Integer _CurrentCountdown = 0;
    private Integer _CurrentCountdownMax = 0;
    
    public FightTimerTask(SnowFight plugin){
        _Plugin = plugin;
        _SetCountdown();
    }
    
    @Override
    public void run() {
        if(_Plugin.PluginState.getValue() >= PluginStates.Building.getValue()
                && _Plugin.FightPlayers.size() < _Plugin.MINIMUM_PLAYERS)
        {
            _Plugin.PluginState = PluginStates.Waiting;
            _Plugin.MessageAllPlayers("The game has ended because there are not enough players anymore.");
            this.cancel();
        }
        
        if(_CurrentCountdown.intValue() > 0){
            //Announce the ticking time
            if(_CurrentCountdown.intValue() == _CurrentCountdownMax.intValue()){
                _AnnounceCountdown();
            }
            else if(_CurrentCountdown.intValue() >= 60 && _CurrentCountdown.intValue() % 60 == 0){
                _AnnounceCountdown();
            }
            else if(_CurrentCountdown.intValue() < 60 && _CurrentCountdown.intValue() >= 30 && _CurrentCountdown.intValue() % 30 == 0){
                _AnnounceCountdown();
            }
            else if(_CurrentCountdown.intValue() < 30 && _CurrentCountdown.intValue() >= 15 && _CurrentCountdown.intValue() % 15 == 0){
                _AnnounceCountdown();
            }
            else if(_CurrentCountdown.intValue() <= 3){
                _AnnounceCountdown();
            }
            //Tick
            _CurrentCountdown--;
        }
        else{
            if(_Plugin.PluginState == PluginStates.Waiting){
                this.cancel(); //End the task
            }
            else if(_Plugin.PluginState == PluginStates.Joining){
                //Make sure there are at least two players
                if(_Plugin.FightPlayers.size() < _Plugin.MINIMUM_PLAYERS){
                    _Plugin.PluginState = PluginStates.Waiting;
                    _Plugin.MessageAllPlayers("Game cancelled!");
                    _Plugin.MessageAllPlayers("There were not enough players to start the game :(");
                    _SetCountdown();
                }
                else{
                    _Plugin.PluginState = PluginStates.Building;
                    _SetCountdown();
                    _Plugin.MessageGamePlayers(String.format("The build phase has started!", _CurrentCountdownMax));
                    
                    //Spawn snow block kits
                    _Plugin.SpawnBuildKits();
                }
            }
            else if(_Plugin.PluginState == PluginStates.Building){
                _Plugin.PluginState = PluginStates.Fighting;
                _SetCountdown();
                _Plugin.MessageGamePlayers(String.format("The fight phase has started! GO GET 'EM!", _CurrentCountdownMax));
                
                //Spawn snow fight kits
                _Plugin.SpawnFightKits();
            }
            else if(_Plugin.PluginState == PluginStates.Fighting){
                _EndGame();
            }
        }
    }
    
    private void _EndGame()
    {
        _Plugin.PluginState = PluginStates.Waiting;
        _SetCountdown();
        _Plugin.MessageGamePlayers("Game over! Good job everyone!");
        _Plugin.PrintScores(null);
        
        _Plugin.FightPlayers.clear(); //Empty the list for the next game.
        this.cancel();
    }
    
    private void _AnnounceCountdown(){
        String phaseName = "";
        String s = _CurrentCountdown == 1 ? "" : "s";
        if(_Plugin.PluginState == PluginStates.Joining){
            phaseName = "join";
        }
        else if(_Plugin.PluginState == PluginStates.Building){
            phaseName = "build";
        }
        else if(_Plugin.PluginState == PluginStates.Fighting){
            phaseName = "fight";
        }
        
        String message = String.format("%d second%s left in %s phase!", _CurrentCountdown, s, phaseName.toUpperCase());
        
        //Only message everyone during the join phase
        if(_Plugin.PluginState == PluginStates.Joining){
            _Plugin.MessageAllPlayers(message);
        }
        else{
            _Plugin.MessageGamePlayers(message);
        }
    }
    
    private void _SetCountdown(){
        if(_Plugin.STATE_SECONDS.containsKey(_Plugin.PluginState)){
            _CurrentCountdown = _Plugin.STATE_SECONDS.get(_Plugin.PluginState);
            _CurrentCountdownMax = _CurrentCountdown;
        }
        else{
            _CurrentCountdown = 0;
            _CurrentCountdownMax = 1;
        }
    }
    
}
