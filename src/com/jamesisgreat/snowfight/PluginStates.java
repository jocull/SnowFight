/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jamesisgreat.snowfight;

public enum PluginStates {
    Disabled(-1),
    Waiting(0),
    Joining(1),
    Building(2),
    Fighting(3);

    private int _Value;
    
    private PluginStates(int val){
        _Value = val;
    }

    public int getValue(){
        return _Value;
    }
}