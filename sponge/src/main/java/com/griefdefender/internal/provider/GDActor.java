/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.internal.provider;

import com.griefdefender.GriefDefenderPlugin;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.annotation.Nullable;

public class GDActor extends AbstractPlayerActor {

    private final EntityPlayerMP player;
    public static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");
    public static final String CUI_PLUGIN_CHANNEL = "WECUI";

    public GDActor(Player player) {
        this.player = (EntityPlayerMP) player;
    }

    @Override
    public UUID getUniqueId() {
        return this.player.getUniqueID();
    }

    @Override
    public int getItemInHand() {
        ItemStack is = this.player.getHeldItem(EnumHand.MAIN_HAND);
        return is == null ? 0 : Item.getIdFromItem(is.getItem());
    }

    @Override
    public String getName() {
        return this.player.getName();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public Location getLocation() {
        return new Location(
                this.getWorld(),
                new Vector(this.player.posX, this.player.posY, this.player.posZ),
                this.player.rotationYaw,
                this.player.rotationPitch);
    }

    @SuppressWarnings("deprecation")
    @Override
    public WorldVector getPosition() {
        return new WorldVector(LocalWorldAdapter.adapt(this.getWorld()), this.player.posX, this.player.posY, this.player.posZ);
    }

    @Override
    public com.sk89q.worldedit.world.World getWorld() {
        return GriefDefenderPlugin.getInstance().worldEditProvider.getWorld((World) this.player.getEntityWorld()) ;
    }

    @Override
    public double getPitch() {
        return this.player.rotationPitch;
    }

    @Override
    public double getYaw() {
        return this.player.rotationYaw;
    }

    @Override
    public void giveItem(int type, int amt) {
        this.player.inventory.addItemStackToInventory(new ItemStack(Item.getItemById(type), amt, 0));
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        String[] params = event.getParameters();
        String send = event.getTypeId();
        if (params.length > 0) {
            send = send + "|" + StringUtil.joinString(params, "|");
        }

        PacketBuffer buffer = new PacketBuffer(Unpooled.copiedBuffer(send.getBytes(UTF_8_CHARSET)));
        SPacketCustomPayload packet = new SPacketCustomPayload(CUI_PLUGIN_CHANNEL, buffer);
        this.player.connection.sendPacket(packet);
    }

    @Override
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            this.player.sendMessage(new TextComponentString(part));
        }
    }

    @Override
    public void printDebug(String msg) {
        sendColorized(msg, TextFormatting.GRAY);
    }

    @Override
    public void print(String msg) {
        sendColorized(msg, TextFormatting.LIGHT_PURPLE);
    }

    @Override
    public void printError(String msg) {
        sendColorized(msg, TextFormatting.RED);
    }

    private void sendColorized(String msg, TextFormatting formatting) {
        for (String part : msg.split("\n")) {
            TextComponentString component = new TextComponentString(part);
            component.getStyle().setColor(formatting);
            this.player.sendMessage(component);
        }
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        this.player.connection.setPlayerLocation(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
    }

    @Override
    public String[] getGroups() {
        return new String[]{};
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return null;
    }

    @Override
    public boolean hasPermission(String perm) {
        return ((Player) this.player).hasPermission(perm);
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(this.player.getUniqueID(), this.player.getName());
    }

    private static class SessionKeyImpl implements SessionKey {
        // If not static, this will leak a reference

        private final UUID uuid;
        private final String name;

        private SessionKeyImpl(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID getUniqueId() {
            return this.uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public boolean isActive() {
            return Sponge.getServer().getPlayer(this.uuid).isPresent();
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

}