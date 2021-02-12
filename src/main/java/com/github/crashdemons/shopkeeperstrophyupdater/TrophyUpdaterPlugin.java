/*
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/ .
 */
package com.github.crashdemons.shopkeeperstrophyupdater;

import com.github.crashdemons.miningtrophies.MiningTrophies;
import com.github.crashdemons.miningtrophies.TrophyType;
import com.github.crashdemons.playerheads.api.HeadType;
import com.github.crashdemons.playerheads.api.PlayerHeads;
import com.github.crashdemons.playerheads.api.PlayerHeadsAPI;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.regular.RegularAdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradingOffer;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main plugin class for TrophyUpdaterPlugin
 *
 * @author crash
 */
public class TrophyUpdaterPlugin extends JavaPlugin implements Listener {
    public TrophyUpdaterPlugin instance = null;
    public boolean hasSK = false, hasPH = false, hasMT = false;
    
    PlayerHeadsAPI ph_api;
    HeadType ph_player;
    
    MiningTrophies mt;
    
    
    @Override
    public void onLoad() {
        instance = this;
        //Do Stuff here
    }

    @Override
    public void onEnable() {
        //saveDefaultConfig();
        //reloadConfig();
        
        hasSK = this.getServer().getPluginManager().getPlugin("Shopkeepers") != null;
        hasPH = this.getServer().getPluginManager().getPlugin("PlayerHeads") != null;
        hasMT = this.getServer().getPluginManager().getPlugin("MiningTrophies") != null;
        
        if(!hasSK){
            getLogger().severe("Cannot run without Shopkeepers support - disabling");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        if(!(hasPH || hasMT)){
            getLogger().severe("Cannot run without at least PlayerHeads or MiningTrophies - disabling");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if(hasPH) getLogger().info("PlayerHeads detected");
        if(hasMT) getLogger().info("MiningTrophies detected");
        
        //ready
        /*
        if(!ShopkeepersAPI.isEnabled()){
            getLogger().severe("Shopkeepers API not enabled - disabling");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }*/
        
        if(hasPH){
            ph_api = PlayerHeads.getApiInstance();
            ph_player = ph_api.getHeadOf(EntityType.PLAYER);
        }
        if(hasMT){
            mt = (MiningTrophies) this.getServer().getPluginManager().getPlugin("MiningTrophies");
        }
        
        getLogger().info("Updating loaded shops...");
        ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().forEach((shopkeeper)->{
            onShopkeeper(shopkeeper);
        });
        
        
        
        
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Enabled.");

    }

    @Override
    public void onDisable() {
        //saveConfig();

        //Cleanup here
        getLogger().info("Disabled.");
    }
    
    private ItemStack updateHead(HeadType headType, ItemStack stack){
        if(headType.equals(ph_player)){
            getLogger().info(" - player's head: "+headType);
            return stack;
        }else{
            getLogger().info(" - not a player's head: "+headType);
            ItemStack newStack = ph_api.getHeadItem(headType, stack.getAmount());
            return newStack;
        }
    }
    
    private ItemStack updateTrophy(TrophyType trophyType, ItemStack stack){
        getLogger().info(" - trophy detected "+trophyType);
        ItemStack newStack  = mt.createTrophyDrop(trophyType);
        newStack.setAmount(stack.getAmount());
        return newStack;
    }
    
    private ItemStack updateItem(ItemStack stack){
        if(stack==null || stack.getType().isAir()) return stack;
        if(hasPH){
            HeadType head = ph_api.getHeadFrom(stack);
            if(head!=null) return updateHead(head, stack);
        }
        if(hasMT){
            TrophyType trophyType = TrophyType.identifyTrophyItem(stack);
            if(trophyType!=null) return updateTrophy(trophyType,stack);
        }
        getLogger().info("   unidentified item, skipping "+stack.getType());
        return stack;
    }
    
    private List<TradingOffer> updateOffers(List<TradingOffer> offers){
        List<TradingOffer> newOffers = new ArrayList<TradingOffer>();
        for(TradingOffer offer : offers){
            ItemStack stack1 = updateItem(offer.getItem1());
            ItemStack stack2 = updateItem(offer.getItem2());
            ItemStack stackResult = updateItem(offer.getResultItem());
            TradingOffer newOffer = ShopkeepersAPI.createTradingOffer(stackResult, stack1, stack2);
            newOffers.add(newOffer);
        }
        return newOffers;
    }

    private void onShopkeeper(Shopkeeper shopkeeper){
        getLogger().info(" Shop detected "+shopkeeper);
        if(shopkeeper instanceof RegularAdminShopkeeper){
            getLogger().info("   was a regular admin shopkeeper");
            RegularAdminShopkeeper adminShopkeeper = (RegularAdminShopkeeper) shopkeeper;
            List<TradingOffer> offers = adminShopkeeper.getOffers();
            offers = updateOffers(offers);
            adminShopkeeper.setOffers(offers);
        }else{
            getLogger().info("   was NOT a regular admin shopkeeper");
        }
    }
    
    @EventHandler(ignoreCancelled=true)
    public void onShopkeeperAdd(ShopkeeperAddedEvent event){
        getLogger().info("Shop Added");
        onShopkeeper(event.getShopkeeper());
    }
    @EventHandler(ignoreCancelled=true)
    public void onShopkeeperAdd(ShopkeeperEditedEvent event){
        getLogger().info("Shop Edited");
        onShopkeeper(event.getShopkeeper());
    }
    
    /*
    //Example command handler
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("YourCommand")) {
            return false;
        }
        if (!sender.hasPermission("yourplugin.yourpermission")) {
            sender.sendMessage("You do not have permission for this command.");
            return true;
        }

        //Do stuff
        return false;
    }*/

    /*
    //Example event handler
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {

    }*/

}