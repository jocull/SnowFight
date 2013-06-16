/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

import java.util.TimerTask;
import org.bukkit.block.Block;

/**
 *
 * @author James
 */
class DecayTimerTask extends TimerTask {
    
    private SnowFight _Plugin;

    public DecayTimerTask(SnowFight plugin) {
        _Plugin = plugin;
    }

    @Override
    public void run() {
        synchronized(_Plugin.DecayPendingBlocks){
            for(int i = (_Plugin.DecayPendingBlocks.size() - 1); i > 0; i--){
                Block b = _Plugin.DecayPendingBlocks.get(i);
                
                //See if the block is eligible for breaking and if so, break it
                if(!_Plugin.PlayerStandingOnBlock(b)){
                    //b.breakNaturally(); //This makes more snowballs - e.g. clutter!
                    b.breakNaturally(null); //This breaks but produces nothing (no animation)
                    //b.setType(Material.AIR); //Set the block to be air. (no animation)

                    //Remove it from the list
                    _Plugin.DecayBlocks.remove(b);
                }
            }
        }
    }
}
