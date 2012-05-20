/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class EventListener implements Listener {
    
    private SnowFight _Plugin;
    
    public EventListener(SnowFight plugin){
        _Plugin = plugin;
    }
    
    //Checking to see if players were hit by a snowball
    @EventHandler //(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if(_Plugin.PluginState.getValue() > PluginStates.Waiting.getValue())
        {
            Entity hitEntity = e.getEntity();
            Entity damageSource = e.getDamager();
            if(hitEntity instanceof Player){
                Player damagedPlayer = (Player)hitEntity;
                Player damagingPlayer = null;
                Snowball snowball = null;
                if (damageSource instanceof Snowball) {
                    snowball = (Snowball)damageSource;
                    damagingPlayer = (Player)snowball.getShooter();
                }
                else if(damageSource instanceof Projectile){
                    damagingPlayer = (Player)((Projectile)damageSource).getShooter();
                }
                else if (damageSource instanceof Player){
                    damagingPlayer = (Player)damageSource;
                }
                
                if(damagingPlayer == null
                        || !_Plugin.FightPlayers.containsKey(damagedPlayer)
                        || !_Plugin.FightPlayers.containsKey(damagingPlayer))
                {
                    return;
                }
                
                //Doesn't count if you hit yourself
                //Only counts during fighting
                if(snowball != null
                        && damagingPlayer != damagedPlayer
                        && _Plugin.PluginState == PluginStates.Fighting)
                {
                    _Plugin.FightPlayers.get(damagedPlayer).HitCount++;
                    _Plugin.FightPlayers.get(damagingPlayer).ScoreCount++;
                }
                else{
                    //Negate any non-snowball damage for game players
                    e.setCancelled(true);
                }
            }
        }
    }
    
    //Checking to see if players are trying to switch into creative
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent e) {
        Player player = e.getPlayer();
        GameMode gameMode = e.getNewGameMode();
        if(_Plugin.FightPlayers.containsKey(player) && gameMode == GameMode.CREATIVE){
            _Plugin.MessageGamePlayers(String.format("%s has been %sKICKED%s from game for creative mode.", player.getDisplayName(), ChatColor.RED, ChatColor.RESET));
            _Plugin.FightPlayers.remove(player);
        }
    }
    
    //Tracking disconnecting players
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if(_Plugin.FightPlayers.containsKey(player)){
            _Plugin.FightPlayers.remove(player);
            _Plugin.MessageGamePlayers(String.format("%s has been %sKICKED%s from game for disconnecting.", player.getDisplayName(), ChatColor.RED, ChatColor.RESET));
        }
    }
    
    //Tracking placed snow-blocks
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if(_Plugin.PluginState.getValue() >= PluginStates.Building.getValue()){
            if(_Plugin.FightPlayers.containsKey(e.getPlayer())){
                if(e.getBlock().getType() == Material.SNOW_BLOCK){
                    //Add this to the decay list
                    _Plugin.DecayBlocks.add(e.getBlock());
                }
            }
        }
    }
}
