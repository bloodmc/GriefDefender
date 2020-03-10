/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) Dockter
 * Copyright (c) contributors
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
package com.griefdefender.provider;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GDBootstrap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimManager;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.api.event.CreateClaimEvent;
import com.griefdefender.api.event.RemoveClaimEvent;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.category.DynmapOwnerStyleCategory;
import com.griefdefender.configuration.category.DynmapCategory;
import com.griefdefender.util.PlayerUtil;

import net.kyori.event.method.annotation.Subscribe;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class DynmapProvider {

    private final Logger logger;
    private DynmapCommonAPI dynmap;
    private MarkerAPI markerapi;
    private DynmapCategory cfg;
    private DynmapOwnerStyleCategory defaultStyle = new DynmapOwnerStyleCategory();
    private MarkerSet set;
    private boolean disabled = false;
    private boolean reload = false;

    public DynmapProvider() {
        this.logger = GriefDefenderPlugin.getInstance().getLogger();
        logger.info("Initializing GriefDefender Dynmap provider...");
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI api) {
                dynmap = api;
                cfg = GriefDefenderPlugin.getGlobalConfig().getConfig().dynmap;
                activate();
            }
        });
    }

    private Map<String, AreaMarker> areaMarkers = new HashMap<String, AreaMarker>();

    private String getWindowInfo(Claim claim, AreaMarker marker) {
        String info;
        if (claim.isAdminClaim()) {
            info = "<div class=\"regioninfo\">" + this.cfg.infoWindowAdmin + "</div>";
        } else {
            info = "<div class=\"regioninfo\">" + this.cfg.infoWindowBasic + "</div>";
        }
        info = info.replace("%owner%", ((GDClaim) claim).getOwnerFriendlyName());
        info = info.replace("%area%", Integer.toString(claim.getArea()));
        info = info.replace("%claimname%",
                claim.getData().getName().isPresent()
                        ? PlainComponentSerializer.INSTANCE.serialize(claim.getName().get())
                        : "none");
        info = info.replace("%lastseen%", claim.getData().getDateLastActive().toString());
        info = info.replace("%gdtype%", claim.getType().toString());

        final List<UUID> builderList = claim.getUserTrusts(TrustTypes.BUILDER);
        final List<UUID> containerList = claim.getUserTrusts(TrustTypes.CONTAINER);
        final List<UUID> accessorList = claim.getUserTrusts(TrustTypes.ACCESSOR);
        final List<UUID> managerList = claim.getUserTrusts(TrustTypes.MANAGER);

        String trusted = "";
        for (int i = 0; i < builderList.size(); i++) {
            if (i > 0) {
                trusted += ", ";
            }
            final UUID uuid = builderList.get(i);
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName.equalsIgnoreCase("unknown")) {
                trusted += uuid.toString();
            } else {
                trusted += userName;
            }
        }
        info = info.replace("%builders%", trusted);

        trusted = "";
        for (int i = 0; i < containerList.size(); i++) {
            if (i > 0) {
                trusted += ", ";
            }
            final UUID uuid = containerList.get(i);
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName.equalsIgnoreCase("unknown")) {
                trusted += uuid.toString();
            } else {
                trusted += userName;
            }
        }
        info = info.replace("%containers%", trusted);

        trusted = "";
        for (int i = 0; i < accessorList.size(); i++) {
            if (i > 0) {
                trusted += ", ";
            }
            final UUID uuid = accessorList.get(i);
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName.equalsIgnoreCase("unknown")) {
                trusted += uuid.toString();
            } else {
                trusted += userName;
            }
        }
        info = info.replace("%accessors%", trusted);

        trusted = "";
        for (int i = 0; i < managerList.size(); i++) {
            if (i > 0) {
                trusted += ", ";
            }
            final UUID uuid = managerList.get(i);
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName.equalsIgnoreCase("unknown")) {
                trusted += uuid.toString();
            } else {
                trusted += userName;
            }
        }
        info = info.replace("%managers%", trusted);

        return info;
    }

    private boolean isVisible(GDClaim claim, String owner, String worldname) {
        if (!this.cfg.hiddenRegions.isEmpty()) {
            if (this.cfg.hiddenRegions.contains(claim.getUniqueId().toString()) || this.cfg.hiddenRegions.contains(owner) || 
                    this.cfg.hiddenRegions.contains("world:" + worldname) || this.cfg.hiddenRegions.contains(worldname + "/" + owner))
                return false;
        }
        return true;
    }

    private void addOwnerStyle(String owner, String worldid, AreaMarker marker, Claim claim) {
        DynmapOwnerStyleCategory ownerStyle = null;

        if (!this.cfg.ownerStyles.isEmpty()) {
            ownerStyle = this.cfg.ownerStyles.get(owner.toLowerCase());
        }

        if (ownerStyle == null) {
            ownerStyle = this.defaultStyle;
        }

        int sc = 0xFF0000;
        int fc = 0xFF0000;

        if (claim.getType().equals(ClaimTypes.ADMIN)) {
            sc = 0xFF0000;
            fc = 0xFF0000;
        } else if (claim.getType().equals(ClaimTypes.BASIC)) {
            sc = 0xFFFF00;
            fc = 0xFFFF00;
        } else if (claim.getType().equals(ClaimTypes.TOWN)) {
            sc = 0x00FF00;
            fc = 0x00FF00;
        } else if (claim.getType().equals(ClaimTypes.SUBDIVISION)) {
            sc = 0xFF9C00;
            fc = 0xFF9C00;
        }

        marker.setLineStyle(ownerStyle.strokeWeight, ownerStyle.strokeOpacity, sc);
        marker.setFillStyle(ownerStyle.fillOpacity, fc);
        if (ownerStyle.label != null) {
            marker.setLabel(ownerStyle.label);
        }
    }

    private void updateClaimMarker(Claim claim, Map<String, AreaMarker> markerMap) {
        final World world = Bukkit.getWorld(claim.getWorldUniqueId());
        if (world == null) {
            return;
        }
        final String worldName = world.getName();
        final String owner = ((GDClaim) claim).getOwnerFriendlyName();
        if (isVisible((GDClaim) claim, owner, worldName)) {
            final Vector3i lesserPos = claim.getLesserBoundaryCorner();
            final Vector3i greaterPos = claim.getGreaterBoundaryCorner();
            final double[] x = new double[4];
            final double[] z = new double[4];
            x[0] = lesserPos.getX();
            z[0] = lesserPos.getZ();
            x[1] = lesserPos.getX();
            z[1] = greaterPos.getZ() + 1.0;
            x[2] = greaterPos.getX() + 1.0;
            z[2] = greaterPos.getZ() + 1.0;
            x[3] = greaterPos.getX() + 1.0;
            z[3] = lesserPos.getZ();
            final UUID id = claim.getUniqueId();
            final String markerid = "GD_" + id;
            AreaMarker marker = this.areaMarkers.remove(markerid);
            if (marker == null) {
                marker = this.set.createAreaMarker(markerid, owner, false, worldName, x, z, false);
                if (marker == null) {
                    return;
                }
            } else {
                marker.setCornerLocations(x, z);
                marker.setLabel(owner);
            }
            if (this.cfg.use3dRegions) {
                marker.setRangeY(greaterPos.getY() + 1.0, lesserPos.getY());
            }

            addOwnerStyle(owner, worldName, marker, claim);
            String desc = getWindowInfo(claim, marker);
            marker.setDescription(desc);
            markerMap.put(markerid, marker);
        }
    }

    private void updateClaims() {
        Map<String, AreaMarker> newmap = new HashMap<String, AreaMarker>();
        Bukkit.getServer().getWorlds().stream().map(w -> GriefDefender.getCore().getClaimManager(w.getUID()))
                .map(ClaimManager::getWorldClaims).forEach(claims -> {
                    claims.forEach(claim -> updateClaimMarker(claim, newmap));
                });

        for (AreaMarker oldm : this.areaMarkers.values()) {
            oldm.deleteMarker();
        }

        this.areaMarkers = newmap;
    }

    private void activate() {
        this.markerapi = this.dynmap.getMarkerAPI();
        if (this.markerapi == null) {
            this.logger.severe("Error loading Dynmap Provider! Could not locate Marker API.");
            return;
        }
        if (this.reload) {
            GriefDefenderPlugin.getInstance().loadConfig();
            if (this.set != null) {
                this.set.deleteMarkerSet();
                this.set = null;
            }
            this.areaMarkers.clear();
        } else {
            this.reload = true;
        }

        this.set = this.markerapi.getMarkerSet("griefdefender.markerset");
        if (this.set == null) {
            this.set = this.markerapi.createMarkerSet("griefdefender.markerset", GriefDefenderPlugin.MOD_ID, null, false);
        } else {
            this.set.setMarkerSetLabel(GriefDefenderPlugin.MOD_ID);
        }
        if (this.set == null) {
            this.logger.severe("Error creating marker set");
            return;
        }

        int minzoom = this.cfg.minzoom;
        if (minzoom > 0) {
            this.set.setMinZoom(minzoom);
        }

        this.set.setLayerPriority(this.cfg.layerPriority);
        this.set.setHideByDefault(this.cfg.layerHideByDefault);

        new GriefDefenderUpdate(40L);
        GriefDefender.getEventManager().register(this);
        this.logger.info("Dynmap provider is activated");
    }

    public void onDisable() {
        if (this.set != null) {
            this.set.deleteMarkerSet();
            this.set = null;
        }
        this.areaMarkers.clear();
        this.disabled = true;
    }

    private class GriefDefenderUpdate extends BukkitRunnable {

        public GriefDefenderUpdate(long delay) {
            this.runTaskLater(GDBootstrap.getInstance(), delay);
        }

        @Override
        public void run() {
            if (!disabled) {
                updateClaims();
            } else {
                this.cancel();
            }
        }
    }

    @Subscribe
    public void onClaimCreate(CreateClaimEvent event) {
        new GriefDefenderUpdate(20L);
    }

    @Subscribe
    public void onClaimDelete(RemoveClaimEvent event) {
        new GriefDefenderUpdate(20L);
    }

    @Subscribe
    public void onClaimChange(ChangeClaimEvent event) {
        new GriefDefenderUpdate(20L);
    }
}