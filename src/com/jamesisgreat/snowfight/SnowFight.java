/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import java.util.*;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class SnowFight extends JavaPlugin {
    
    public final int TIMER_INTERVAL = 1000;
    public final byte MINIMUM_PLAYERS = 1; //TODO: Should this should be 2 before release?
    public final byte MAX_DISPLAY_SCORES = 5;
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
    public List<Block> DecayBlocks = new ArrayList<Block>();
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
        
        //MessageAllPlayers(String.format("YEAH! %s! It's on! :)", PluginName));
        MessageAllPlayers("Mod up!");
    }
    
    @Override
    public void onDisable(){
        FightTimer.cancel(); //Clear any/all timers
        //MessageAllPlayers(String.format("Booooo... %s is going offline :(", PluginName));
        MessageAllPlayers("Mod down...");
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
            ShowUsage(player, 1); //The other pages are at the bottom to avoid excess try/catching
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
            else if(!FightWorld.equals(player.getWorld())){
                MessagePlayer(player, "You're not in the same world as the host! Go find them!");
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
        else if(args[0].equalsIgnoreCase("r")){
            if(FightPlayers.containsKey(player) && PluginState == PluginStates.Fighting){
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
            if(PluginState.getValue() >= PluginStates.Fighting.getValue()){
                StringBuilder sb = new StringBuilder("Players:\n");
                int count = 0;
                for(Player p : FightPlayers.keySet()){
                    if(p == player)
                        continue;
                    sb.append(p.getDisplayName());
                    sb.append(", ");
                    count++;
                }
                if(count == 0){
                    sb.append("No other players in game!  "); //Intentional whitespace for trimming commas
                }
                MessagePlayer(player, sb.substring(0, sb.length() - 2));
            }
            else{
                MessagePlayer(player, "There is no game in progress.");
            }
        }
        else if(args[0].equalsIgnoreCase("score")){
            PrintScores(player);
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
        else if(args[0].equalsIgnoreCase("decay")){
            DecayBlocks(player);
        }
        else if(args[0].equalsIgnoreCase("version")){
            MessagePlayer(player, this.PluginVersion);
        }
        else if(TryParseInt(args[0])){ //Help for page #x
            ShowUsage(player, Integer.parseInt(args[0]));
        }
        else{
            MessagePlayer(player, String.format("Unknown command! Use `/%s` to get help.", COMMAND));
        }
        return true;
    }
    
    public boolean TryParseInt(String value){
        try {
            Integer.parseInt(value);
            return true;
        }
        catch(NumberFormatException ex) {
            return false;
        }
    }
    
    public void ShowUsage(Player player, int page){
        MessagePlayer(player, String.format("How to use %s (pg %d):", this.PluginName, page));
        if(page == 1){
            MessagePlayer(player, String.format("/%s 2 - More help", COMMAND));
            MessagePlayer(player, String.format("/%s start - Start a snow fight", COMMAND));
            MessagePlayer(player, String.format("/%s join - Join a snow fight during voting", COMMAND));
            MessagePlayer(player, String.format("/%s r - Get more snow (reload) during a fight", COMMAND));
            MessagePlayer(player, String.format("/%s quit - Chicken out during a fight", COMMAND));
        }
        else if(page == 2){
            MessagePlayer(player, String.format("/%s players - Get a list of players during a fight", COMMAND));
            MessagePlayer(player, String.format("/%s score - View scores during a fight", COMMAND));
            MessagePlayer(player, String.format("/%s decay - Clean up snow after a fight", COMMAND));
            MessagePlayer(player, String.format("/%s version - Get the current version (it's %s)", COMMAND, this.PluginVersion));
        }
        else{
            MessagePlayer(player, String.format("No help available for page %d", COMMAND, page));
        }
    }
    
    public void MessagePlayer(Player player, String message){
        //Private messages go light purple
        String output = this.ChatPrefix + ChatColor.LIGHT_PURPLE + message;
        LogInfo(String.format("Message player [%s]: %s", player.getDisplayName(), output));
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

    public void PrintScores(Player privateMessagePlayer) {
        if(FightPlayers.isEmpty()){
            if(privateMessagePlayer != null){
                MessagePlayer(privateMessagePlayer, "There are no scores to show!");
            }
            return;
        }
        
        //Order players by score
        List<FightRecord> fightRecords = new ArrayList<FightRecord>(FightPlayers.values());
        Collections.sort(fightRecords, new Comparator<FightRecord>(){
            @Override
            public int compare(FightRecord s1, FightRecord s2) {
                return s2.ScoreCount.intValue() - s1.ScoreCount.intValue();
            }
        });
        
        //Tell each player their score
        for (Player player : FightPlayers.keySet()){
            if(FightPlayers.containsKey(player)
                    && (privateMessagePlayer == null || privateMessagePlayer == player))
            {
                FightRecord record = FightPlayers.get(player);
                MessagePlayer(player, String.format("Your score: %d", record.ScoreCount));
                MessagePlayer(player, String.format("Times you were hit: %d", record.HitCount));
                
                int index = 0;
                for(int i = 0; i < fightRecords.size(); i++){
                    if(fightRecords.get(i).ThisPlayer.getEntityId() == player.getEntityId()){
                        index = i + 1;
                        break;
                    }
                }
                MessagePlayer(player, String.format("Your place: %d / %d", index, fightRecords.size()));
            }
        }
        
        //Check for ties
        int maxScore = 0;
        int maxScoreCount = 0;
        boolean ties = false;
        for(FightRecord record : fightRecords){
            if(record.ScoreCount.intValue() > maxScore){
                maxScore = record.ScoreCount.intValue();
                maxScoreCount = 1;
            }
            else if(record.ScoreCount.intValue() == maxScore){
                maxScoreCount++;
            }
        }
        
        //Print scores
        String winner;
        if(maxScoreCount > 1){
            winner = String.format("There was a %d-way tie for best score!", maxScoreCount);
        }
        else{
            winner = String.format("%s has won the match!", fightRecords.get(0).ThisPlayer.getDisplayName());
        }
        if(privateMessagePlayer != null){
            MessagePlayer(privateMessagePlayer, winner);
        }
        else{
            MessageGamePlayers(winner);
        }
        
        int displayScoreCount = Math.min(MAX_DISPLAY_SCORES, fightRecords.size());
        String topScores = String.format("Top %d scorers:", displayScoreCount);
        if(privateMessagePlayer != null){
            MessagePlayer(privateMessagePlayer, topScores);
        }
        else{
            MessageGamePlayers(topScores);
        }
        
        for(int i = 0; i < displayScoreCount; i++){
            FightRecord record = fightRecords.get(i);
            String scoreLine = String.format("%s - Score: %d | Times Hit: %d", record.ThisPlayer.getDisplayName(), record.ScoreCount, record.HitCount);
            if(privateMessagePlayer != null){
                MessagePlayer(privateMessagePlayer, scoreLine);
            }
            else{
                MessageGamePlayers(scoreLine);
            }
        }
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
            SpawnReloadKit(player);
            SpawnItemsAtPlayer(player, new ItemStack(Material.STONE_SPADE));
            
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
    
    public void SpawnItemsAtPlayer(Player player, ItemStack item){
        FightWorld.dropItemNaturally(player.getEyeLocation(), item);
    }
    
    public void DecayBlocks(Player requestingPlayer){
        if(PluginState.getValue() > PluginStates.Joining.getValue()){
            if(requestingPlayer != null){
                MessagePlayer(requestingPlayer, "You can't decay blocks in the middle of a fight!");
                return;
            }
        }
        else if(DecayBlocks.isEmpty()){
            if(requestingPlayer != null){
                MessagePlayer(requestingPlayer, "There are no blocks to decay!");
            }
            return;
        }
        
        MessageAllPlayers("Snow blocks are decaying now! Hope you're not up high!");
//        for (Player player : this.getServer().getOnlinePlayers()){
//            Location l = player.getLocation();
//            System.out.println(String.format("%s - X: %d Y: %d Z: %d",
//                                                player.getName(),
//                                                l.getBlockX(),
//                                                l.getBlockY(),
//                                                l.getBlockZ()));
//        }
        
        for(Block block : DecayBlocks){
            if(block.getType() == Material.SNOW_BLOCK){
//                System.out.println(String.format("Snow Block - X: %d Y: %d Z: %d",
//                                                block.getX(),
//                                                block.getY(),
//                                                block.getZ()));
                
                //Make sure no one is standing on this block
                boolean standingPlayer = false;
                for (Player player : this.getServer().getOnlinePlayers()){
                    Location pl = player.getLocation();
                    if(block.getX() >= (pl.getBlockX() - 2) // x/z = left/right
                        && block.getX() <= (pl.getBlockX() + 2)
                        && block.getZ() >= (pl.getBlockZ() - 2)
                        && block.getZ() <= (pl.getBlockZ() + 2)
                        && block.getY() >= (pl.getBlockY() - 1) // y = vertical
                        && block.getY() < pl.getBlockY())
                    {
                        standingPlayer = true;
                    }
                }
                
                if(!standingPlayer){
                    //block.breakNaturally(); //This makes more snowballs - e.g. clutter!
                    block.breakNaturally(null); //This breaks but produces nothing
                    //block.setType(Material.AIR); //Set the block to be air. (just disappears without animation)
                }
            }
        }
        DecayBlocks.clear(); //Empty the list
    }
}
