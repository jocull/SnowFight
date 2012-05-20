/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class EventListener implements Listener {
    
    private SnowFight _Plugin;
    
    public EventListener(SnowFight plugin){
        _Plugin = plugin;
    }
    
    //Checking to see if players were hit by a snowball
    @EventHandler (priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if(_Plugin.PluginState == PluginStates.Fighting)
        {
            Entity hitEntity = e.getEntity();
            Entity damageSource = e.getDamager();
            if (damageSource instanceof Snowball) {
                Snowball snowball = (Snowball)damageSource;
                if(hitEntity instanceof Player){
                    Player hitPlayer = (Player)hitEntity;
                    if(snowball.getShooter() instanceof Player){
                        Player shootPlayer = (Player)snowball.getShooter();
                        
                        //Log the hit/score for both players
                        if(hitPlayer != shootPlayer //Doesn't count if you hit yourself... //TODO: TEST THIS
                                && _Plugin.FightPlayers.containsKey(hitPlayer)
                                && _Plugin.FightPlayers.containsKey(shootPlayer))
                        {
                            _Plugin.FightPlayers.get(hitPlayer).HitCount++;
                            _Plugin.FightPlayers.get(shootPlayer).ScoreCount++;
                        }
                    }
                }
            }
        }
    }
    
    //Checking to see if players are trying to switch into creative
    @EventHandler (priority = EventPriority.HIGH)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        GameMode gameMode = e.getNewGameMode();
        if(_Plugin.FightPlayers.containsKey(player) && gameMode == GameMode.CREATIVE){
            _Plugin.MessageGamePlayers(String.format("%s has been %sKICKED%s from game for creative mode.", player.getDisplayName(), ChatColor.RED, ChatColor.RESET));
            _Plugin.FightPlayers.remove(player);
        }
    }
}
