package com.griefdefender.internal.util;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.EntityRemovalListener;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.util.EntityUtils;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Container;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.entity.SpongeEntityType;
import org.spongepowered.common.item.inventory.custom.CustomInventory;
import org.spongepowered.common.util.SpongeUsernameCache;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class NMSUtil {

    public static final BiMap<String, EnumCreatureType> SPAWN_TYPES = HashBiMap.create();

    static {
        SPAWN_TYPES.put("ambient", EnumCreatureType.AMBIENT);
        SPAWN_TYPES.put("animal", EnumCreatureType.CREATURE);
        SPAWN_TYPES.put("aquatic", EnumCreatureType.WATER_CREATURE);
        SPAWN_TYPES.put("monster", EnumCreatureType.MONSTER);
    }

    private static NMSUtil instance;

    static {
        instance = new NMSUtil();
    }

    public static NMSUtil getInstance() {
        return instance;
    }

    public void addEntityRemovalListener(World world) {
        ((net.minecraft.world.WorldServer) world).addEventListener(new EntityRemovalListener());
    }

    public boolean isTransparent(BlockState state) {
        final IBlockState iblockstate = (IBlockState)(Object) state;
        return !iblockstate.isOpaqueCube();
    }

    public int getItemDamage(ItemStack stack) {
        return ((net.minecraft.item.ItemStack)(Object) stack).getItemDamage();
    }

    public String getEntityId(Entity targetEntity, boolean isSource) {
        net.minecraft.entity.Entity mcEntity = null;
        if (targetEntity instanceof net.minecraft.entity.Entity) {
            mcEntity = (net.minecraft.entity.Entity) targetEntity;
        }
        String id = "";
        if (mcEntity != null && mcEntity instanceof EntityItem) {
            EntityItem mcItem = (EntityItem) mcEntity;
            net.minecraft.item.ItemStack itemStack = mcItem.getItem();
            if (itemStack != null && itemStack.getItem() != null) {
                ItemType itemType = (ItemType) itemStack.getItem();
                id = itemType.getId() + "." + itemStack.getItemDamage();
            }
        } else {
            if (targetEntity.getType() != null) {
                id = targetEntity.getType().getId();
            }
            
        }

        if (mcEntity != null && id.contains("unknown") && SpongeImplHooks.isFakePlayer(mcEntity)) {
            final String modId = SpongeImplHooks.getModIdFromClass(mcEntity.getClass());
            id = modId + ":fakeplayer_" + EntityUtils.getFriendlyName(mcEntity).toLowerCase();
        } else if (id.equals("unknown:unknown") && targetEntity instanceof EntityPlayer) {
            id = "minecraft:player";
        }

        if (mcEntity != null && targetEntity instanceof Living) {
            String[] parts = id.split(":");
            if (parts.length > 1) {
                final String modId = parts[0];
                String name = parts[1];
                if (modId.equalsIgnoreCase("pixelmon") && modId.equalsIgnoreCase(name)) {
                    name = EntityUtils.getFriendlyName(mcEntity).toLowerCase();
                    GDPermissionManager.getInstance().populateEventSourceTarget(modId + ":" + name, isSource);
                    if (isSource) {
                        id = modId + ":" + name;
                        return id;
                    }
                }
                if (!isSource) {
                    for (EnumCreatureType type : EnumCreatureType.values()) {
                        if (SpongeImplHooks.isCreatureOfType(mcEntity, type)) {
                            id = modId + ":" + SPAWN_TYPES.inverse().get(type) + ":" + name;
                            break;
                        }
                    }
                }
            }
        }

        if (targetEntity instanceof Item) {
            id = ((Item) targetEntity).getItemType().getId();
        }

        return id;
    }
    
    public int getEntityMinecraftId(Entity entity) {
        return ((net.minecraft.entity.Entity) entity).getEntityId();
    }

    public String getEntityName(Entity entity) {
        return ((net.minecraft.entity.Entity) entity).getName();
    }

    // Fallback option for creating user's from usernamecache.json
    public User createUserFromCache(UUID uuid) {
        final String username = SpongeUsernameCache.getLastKnownUsername(uuid);
        if (username != null) {
            return Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(GameProfile.of(uuid, username)).orElse(null);
        }
        return null;
    }

    public User createUserFromCache(String username) {
        final UUID uuid = SpongeUsernameCache.getLastKnownUUID(username);
        if (uuid != null) {
            return Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(GameProfile.of(uuid, username)).orElse(null);
        }
        return null;
    }

    public String getItemStackId(ItemStack stack) {
        if (stack.getType() instanceof ItemBlock) {
            ItemBlock itemBlock = (ItemBlock) stack.getType();
            net.minecraft.item.ItemStack nmsStack = (net.minecraft.item.ItemStack)(Object) stack;
            BlockState blockState = ((BlockState) itemBlock.getBlock().getStateFromMeta(nmsStack.getItemDamage()));
            return blockState.getType().getId();
        }

        return stack.getType().getId();
    }

    public String getItemStackMeta(ItemStack stack) {
        return String.valueOf(((net.minecraft.item.ItemStack)(Object) stack).getItemDamage());
    }

    public int getPlayerBlockReachDistance(Player player) {
        return (int) SpongeImplHooks.getBlockReachDistance((EntityPlayerMP) player);
    }

    public boolean isBlockPressurePlate(BlockType type) {
        return type instanceof BlockBasePressurePlate;
    }

    public boolean isEntityMonster(Entity entity) {
        return SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) entity, EnumCreatureType.MONSTER);
    }

    public boolean isEntityAnimal(Entity entity) {
        return SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) entity, EnumCreatureType.CREATURE);
    }

    public boolean isFakePlayer(Object entity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        return SpongeImplHooks.isFakePlayer((EntityPlayer) entity);
    }

    public boolean isContainerCustomInventory(Container container) {
        if (container instanceof ContainerChest) {
            return ((ContainerChest) container).getLowerChestInventory() instanceof CustomInventory;
        }
        return false;
    }

    public boolean isItemFood(ItemType type) {
        return type instanceof ItemFood;
    }

    public boolean isItemBlock(ItemStack stack) {
        return stack instanceof ItemBlock;
    }

    public boolean containsInventory(Object object) {
        return object instanceof IInventory;
    }

    public boolean containsContainerPlayer(Cause cause) {
        return cause.containsType(ContainerPlayer.class);
    }

    public void closePlayerScreen(Player player) {
        ((EntityPlayerMP) player).closeScreen();
    }


    public RayTraceResult rayTracePlayerEyes(EntityPlayerMP player) {
        double distance = SpongeImplHooks.getBlockReachDistance(player) + 1;
        Vec3d startPos = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3d endPos = startPos.add(new Vec3d(player.getLookVec().x * distance, player.getLookVec().y * distance, player.getLookVec().z * distance));
        return player.world.rayTraceBlocks(startPos, endPos);
    }

    public Vec3d rayTracePlayerEyeHitVec(EntityPlayerMP player) {
        RayTraceResult result = rayTracePlayerEyes(player);
        return result == null ? null : result.hitVec;
    }

    public String getEntitySpawnFlag(Flag flag, String target) {
        if (flag.getName().contains("entity") || flag == Flags.ITEM_SPAWN || flag == Flags.ENTER_CLAIM || flag == Flags.EXIT_CLAIM) {
            String claimFlag = flag.getName();
            // first check for valid SpawnType
            String[] parts = target.split(":");
            EnumCreatureType type = SPAWN_TYPES.get(parts[1]);
            if (type != null) {
                claimFlag += "." + parts[0] + "." + SPAWN_TYPES.inverse().get(type);
                return claimFlag;
            } else {
                Optional<EntityType> entityType = Sponge.getRegistry().getType(EntityType.class, target);
                if (entityType.isPresent()) {
                    SpongeEntityType spongeEntityType = (SpongeEntityType) entityType.get();
                    if (spongeEntityType.getEnumCreatureType() != null) {
                        String creatureType = SPAWN_TYPES.inverse().get(spongeEntityType.getEnumCreatureType());
                        if (parts[1].equalsIgnoreCase("pixelmon")) {
                            claimFlag += "." + parts[0] + ".animal";
                        } else {
                            claimFlag += "." + parts[0] + "." + creatureType + "." + parts[1];
                        }
                        return claimFlag;
                    } else {
                        claimFlag += "." + parts[0] + "." + parts[1];
                        return claimFlag;
                    }
                }
                // Unfortunately this is required until Pixelmon registers their entities correctly in FML
                if (target.contains("pixelmon")) {
                    // If target was not found in registry, assume its a pixelmon animal
                    if (parts[1].equalsIgnoreCase("pixelmon")) {
                        claimFlag += "." + parts[0] + ".animal";
                    } else {
                        claimFlag += "." + parts[0] + ".animal." + parts[1];
                    }
                    return claimFlag;
                }
            }
        }

        return null;
    }

    public void populateTabComplete() {
        for (BlockType blockType : Sponge.getRegistry().getAllOf(BlockType.class)) {
            String modId = blockType.getId().split(":")[0].toLowerCase();
            if (modId.equals("none")) {
                continue;
            }
            final String blockTypeId = blockType.getId().toLowerCase();
            if (!GriefDefenderPlugin.ID_MAP.contains(modId + ":any")) {
                GriefDefenderPlugin.ID_MAP.add(modId + ":any");
            }
            GriefDefenderPlugin.ID_MAP.add(blockTypeId);
        }

        for (EntityType entityType : Sponge.getRegistry().getAllOf(EntityType.class)) {
            String modId = entityType.getId().split(":")[0].toLowerCase();
            if (modId.equals("none")) {
                continue;
            }
            final String entityTypeId = entityType.getId().toLowerCase();
            if (!GriefDefenderPlugin.ID_MAP.contains(modId + ":any")) {
                GriefDefenderPlugin.ID_MAP.add(modId + ":any");
            }
            if (!GriefDefenderPlugin.ID_MAP.contains(entityTypeId)) {
                GriefDefenderPlugin.ID_MAP.add(entityTypeId);
            }
            if (!GriefDefenderPlugin.ID_MAP.contains(modId + ":animal") && Living.class.isAssignableFrom(entityType.getEntityClass())) {
                GriefDefenderPlugin.ID_MAP.add(modId + ":ambient");
                GriefDefenderPlugin.ID_MAP.add(modId + ":animal");
                GriefDefenderPlugin.ID_MAP.add(modId + ":aquatic");
                GriefDefenderPlugin.ID_MAP.add(modId + ":monster");
            }
        }

        for (ItemType itemType : Sponge.getRegistry().getAllOf(ItemType.class)) {
            String modId = itemType.getId().split(":")[0].toLowerCase();
            if (modId.equals("none")) {
                continue;
            }
            final String itemTypeId = itemType.getId().toLowerCase();
            if (!GriefDefenderPlugin.ID_MAP.contains(modId + ":any")) {
                GriefDefenderPlugin.ID_MAP.add(modId + ":any");
            }
            if (!GriefDefenderPlugin.ID_MAP.contains(itemTypeId)) {
                GriefDefenderPlugin.ID_MAP.add(itemTypeId);
            }
            GriefDefenderPlugin.ITEM_IDS.add(itemTypeId);
        }

        for (DamageType damageType : Sponge.getRegistry().getAllOf(DamageType.class)) {
            String damageTypeId = damageType.getId().toLowerCase();
            if (!damageType.getId().contains(":")) {
                damageTypeId = "minecraft:" + damageTypeId;
            }
            if (!GriefDefenderPlugin.ID_MAP.contains(damageType.getId())) {
                GriefDefenderPlugin.ID_MAP.add(damageTypeId);
            }
        }
        // commands
        Set<? extends CommandMapping> commandList = Sponge.getCommandManager().getCommands();
        for (CommandMapping command : commandList) {
            PluginContainer pluginContainer = Sponge.getCommandManager().getOwner(command).orElse(null);
            if (pluginContainer != null) {
                for (String alias : command.getAllAliases()) {
                    String[] parts = alias.split(":");
                    if (parts.length > 1) {
                        GriefDefenderPlugin.ID_MAP.add(alias);
                    }
                }
            }
        }
    }

    public GDClaim createClaimFromCenter(Location<World> center, float radius) {
        AxisAlignedBB aabb = new AxisAlignedBB(VecHelper.toBlockPos(center));
        aabb = aabb.grow(radius);
        final Vector3i lesser = new Vector3i(aabb.minX, aabb.minY, aabb.minZ);
        final Vector3i greater = new Vector3i(aabb.maxX, aabb.maxY, aabb.maxZ);
        return new GDClaim(center.getExtent(), lesser, greater, ClaimTypes.BASIC, false);
    }

    public ItemStack getActiveItem(Player player) {
        return this.getActiveItem(player, null);
    }

    public ItemStack getActiveItem(Player player, Event currentEvent) {
        final ItemStackSnapshot snapshot = player.get(Keys.ACTIVE_ITEM).orElse(null);
        if (snapshot != null) {
            return snapshot.createStack();
        }
        return null;
    }
}
