/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import java.util.HashMap;
import java.util.Timer;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SnowFight extends JavaPlugin {
    
    public final int TIMER_INTERVAL = 1000;
    public final int MINIMUM_PLAYERS = 1; //TODO: Should this should be 2 before release?
    public final String COMMAND = "sf";
    
    public String ChatPrefix;
    public String PluginName;
    public String PluginVersion;
    
    public HashMap<PluginStates, Integer> STATE_SECONDS = new HashMap<PluginStates, Integer>(){
        {
            put(PluginStates.Joining, 30);
            put(PluginStates.Building, 30);
            put(PluginStates.Fighting, 60); //TODO: Increase after debugging!
        }
    };
    
    public World FightWorld;
    public HashMap<Player, FightRecord> FightPlayers = new HashMap<Player, FightRecord>();
    public Timer FightTimer = new Timer();
    
    private EventListener _EventListener;
    
    public PluginStates PluginState = PluginStates.Disabled;
    
    public static void main(String[] args) {
        
    }
    
    public void LogInfo(String msg){
        this.getLogger().log(Level.INFO, msg);
    }
    
    @Override
    public void onEnable(){
        //Setup plugin vars
        PluginState = PluginStates.Waiting;
        PluginName = this.getName();
        PluginVersion = this.getDescription().getVersion();
        ChatPrefix = String.format("%s[%s]%s ", ChatColor.GOLD, PluginName, ChatColor.RESET);
        
        //Register event listeners
        _EventListener = new EventListener(this);
        this.getServer().getPluginManager().registerEvents(_EventListener, this);
        
        MessageAllPlayers(String.format("YEAH! %s! It's on! :)", PluginName));
    }
    
    @Override
    public void onDisable(){
        FightTimer.cancel(); //Clear any/all timers
        MessageAllPlayers(String.format("Booooo... %s is going offline :(", PluginName));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        Player player;
	if (sender instanceof Player) {
		player = (Player) sender;
	}
        else
            return false; //Only allows player commands
        
        if(player.getGameMode() == GameMode.CREATIVE){
            MessagePlayer(player, String.format("You cannot use %s while in creative mode.", PluginName));
        }
        else if(args.length == 0){
            ShowUsage(player);
        }
        else if(args[0].equalsIgnoreCase("start")){
            if(PluginState == PluginStates.Waiting){
                FightPlayers.put(player, new FightRecord(player));
                MessageAllPlayers(String.format("%s wants to start a snow fight!", player.getDisplayName()));
                MessageAllPlayers(String.format("Type `/%s join` now to play with them!", COMMAND));

                PluginState = PluginStates.Joining;
                FightWorld = player.getWorld();
                FightTimer.scheduleAtFixedRate(new FightTimerTask(this), 0, TIMER_INTERVAL); //Ticks every second!
            }
            else{
                MessagePlayer(player, "Something is going on. You can't start a game now.");
            }
        }
        else if(args[0].equalsIgnoreCase("join")){
            if(FightPlayers.containsKey(player)){
                MessagePlayer(player, "You already joined! Calm down!");
            }
            else if(PluginState == PluginStates.Joining){
                FightPlayers.put(player, new FightRecord(player));
                MessageAllPlayers(String.format("%s has joined the fight!", player.getDisplayName()));
            }
            else if(PluginState.getValue() > PluginStates.Joining.getValue()){
                MessagePlayer(player, "It's too late to join! Sorry!");
            }
            else {
                MessagePlayer(player, "There is no game in progress.");
            }
        }
        else if(args[0].equalsIgnoreCase("more")){
            if(FightPlayers.containsKey(player) && PluginState == PluginState.Fighting){
                FightRecord record = FightPlayers.get(player);
                if(record.canRequestKit()){
                    SpawnReloadKit(player);
                    MessagePlayer(player, "Here you go! YEAH STUFFS!");
                }
                else{
                    String s = record.secondsTilRequestKit() == 1 ? "" : "s";
                    MessagePlayer(player, String.format("You can request a reload kit in %d second%s.", record.secondsTilRequestKit(), s));
                }
            }
            else{
                MessagePlayer(player, "You're not even fighting yet!");
            }
        }
        else if(args[0].equalsIgnoreCase("players")){
            MessagePlayer(player, "incomplete plugin :(");
        }
        else if(args[0].equalsIgnoreCase("score")){
            MessagePlayer(player, "incomplete plugin :(");
        }
        else if(args[0].equalsIgnoreCase("quit")){
            if(PluginState.getValue() >= PluginStates.Joining.getValue()){
                if(FightPlayers.containsKey(player)){
                    FightPlayers.remove(player);
                    MessageAllPlayers(String.format("BAWK BAWK BAWK! %s is a giant chicken.", player.getDisplayName()));
                    MessageAllPlayers(String.format("%s has left the fight.", player.getDisplayName()));
                }
                else{
                    MessagePlayer(player, "You are not in the current game.");
                }
            }
            else{
                MessagePlayer(player, "There is no game in progress.");
            }
        }
        else if(args[0].equalsIgnoreCase("version")){
            MessagePlayer(player, this.PluginVersion);
        }
        else{
            MessagePlayer(player, String.format("Unknown command! Use `/%s` to get help.", COMMAND));
        }
        return true;
    }
    
    public void ShowUsage(Player player){
        MessagePlayer(player, String.format("How to use %s:", this.PluginName));
        MessagePlayer(player, String.format("/%s start - Start a snow fight", COMMAND));
        MessagePlayer(player, String.format("/%s join - Join a snow fight during voting", COMMAND));
        MessagePlayer(player, String.format("/%s more - Get more snow during a fight", COMMAND));
        MessagePlayer(player, String.format("/%s players - Get a list of players during a fight", COMMAND));
        MessagePlayer(player, String.format("/%s score - View scores during a fight", COMMAND));
        MessagePlayer(player, String.format("/%s quit - Chicken out during a fight", COMMAND));
        MessagePlayer(player, String.format("/%s version - Get the current version (it's %s)", COMMAND, this.PluginVersion));
    }
    
    public void MessagePlayer(Player player, String message){
        //Private messages go light purple
        String output = this.ChatPrefix + ChatColor.LIGHT_PURPLE + message;
        player.sendMessage(output);
    }
    
    public void MessageGamePlayers(String message){
        String output = this.ChatPrefix + message;
        LogInfo("Message game: " + output);
        
        for (Player player : FightPlayers.keySet()){
            player.sendMessage(output);
        }
    }
    
    public void MessageAllPlayers(String message){
        String output = this.ChatPrefix + message;
        LogInfo("Message all: " + output);
        
        for (Player player : this.getServer().getOnlinePlayers()){
            player.sendMessage(output);
        }
    }

    public void PrintScores() {
        for (Player player : FightPlayers.keySet()){
            if(FightPlayers.containsKey(player)){
                FightRecord record = FightPlayers.get(player);
                MessagePlayer(player, String.format("Your score: %d", record.ScoreCount));
            }
        }
        MessageGamePlayers("TODO: PRINT EVERYONE'S SCORES!");
    }
    
    public void SpawnBuildKits(){
        for(Player player : FightPlayers.keySet()){
            ItemStack blockStack = new ItemStack(Material.SNOW_BLOCK);
            blockStack.setAmount(blockStack.getMaxStackSize() / 2);
            SpawnItemsAtPlayer(player, blockStack);
            
            FightPlayers.get(player).setLastKitRequest();
        }
    }
    
    public void SpawnFightKits(){
        for(Player player : FightPlayers.keySet()){
            ItemStack blockStack = new ItemStack(Material.SNOW_BLOCK);
            blockStack.setAmount(blockStack.getMaxStackSize() / 4);
            SpawnItemsAtPlayer(player, blockStack);
            
            ItemStack ballStack = new ItemStack(Material.SNOW_BALL);
            ballStack.setAmount(ballStack.getMaxStackSize());
            SpawnItemsAtPlayer(player, ballStack);
            
            SpawnItemsAtPlayer(player, new ItemStack(Material.WOOD_SPADE));
            
            FightPlayers.get(player).setLastKitRequest();
        }
    }
    
    public void SpawnReloadKit(Player player){
        ItemStack blockStack = new ItemStack(Material.SNOW_BLOCK);
        blockStack.setAmount(blockStack.getMaxStackSize() / 4);
        SpawnItemsAtPlayer(player, blockStack);
        
        ItemStack ballStack = new ItemStack(Material.SNOW_BALL);
        ballStack.setAmount(ballStack.getMaxStackSize());
        SpawnItemsAtPlayer(player, ballStack);
        
        FightPlayers.get(player).setLastKitRequest();
    }
    
    public void SpawnSnowKitAtPlayer(Player player, int snowBlockStacks, int snowballStacks){
        ItemStack blockStack = new ItemStack(Material.SNOW_BLOCK);
        ItemStack ballStack = new ItemStack(Material.SNOW_BALL);
        
        blockStack.setAmount(blockStack.getMaxStackSize());
        ballStack.setAmount(ballStack.getMaxStackSize());
        
        for(int i = 0; i < snowballStacks; i++){
            SpawnItemsAtPlayer(player, ballStack);
        }
        for(int i = 0; i < snowBlockStacks; i++){
            SpawnItemsAtPlayer(player, blockStack);
        }
    }
    
    public void SpawnItemsAtPlayer(Player player, ItemStack item){
        FightWorld.dropItemNaturally(player.getEyeLocation(), item);
    }
}
