package net.citizensnpcs.nms.v1_21_R2.entity.nonliving;

import net.citizensnpcs.trait.CustomEntityTrait;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R2.CraftServer;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_21_R2.entity.CraftMinecartRideable;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.nms.v1_21_R2.entity.MobEntityController;
import net.citizensnpcs.nms.v1_21_R2.util.ForwardingNPCHolder;
import net.citizensnpcs.nms.v1_21_R2.util.NMSBoundingBox;
import net.citizensnpcs.nms.v1_21_R2.util.NMSImpl;
import net.citizensnpcs.npc.CitizensNPC;
import net.citizensnpcs.npc.ai.NPCHolder;
import net.citizensnpcs.util.NMS;
import net.citizensnpcs.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MinecartRideableController extends MobEntityController {
    public MinecartRideableController() {
        super(EntityMinecartRideableNPC.class, EntityType.MINECART);
    }

    @Override
    public org.bukkit.entity.Minecart getBukkitEntity() {
        return (org.bukkit.entity.Minecart) super.getBukkitEntity();
    }

    public static class EntityMinecartRideableNPC extends Minecart implements NPCHolder {
        private final CitizensNPC npc;

        public EntityMinecartRideableNPC(EntityType<? extends Minecart> types, Level level) {
            this(types, level, null);
        }

        public EntityMinecartRideableNPC(EntityType<? extends Minecart> types, Level level, NPC npc) {
            super(types, level);
            this.npc = (CitizensNPC) npc;
        }

        @Override
        public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entitytrackerentry) {
            CustomEntityTrait customEntityTrait = npc.getTraitNullable(CustomEntityTrait.class);
            if(customEntityTrait != null && customEntityTrait.getCustomEntityName() != null) {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(ResourceLocation.parse(customEntityTrait.getCustomEntityName()));
                return new ClientboundAddEntityPacket(
                        this.getId(),
                        this.getUUID(),
                        entitytrackerentry.getPositionBase().x(),
                        entitytrackerentry.getPositionBase().y(),
                        entitytrackerentry.getPositionBase().z(),
                        entitytrackerentry.getLastSentXRot(),
                        entitytrackerentry.getLastSentYRot(),
                        entityType,
                        0,
                        entitytrackerentry.getLastSentMovement(),
                        entitytrackerentry.getLastSentYHeadRot()
                );
            }
            return super.getAddEntityPacket(entitytrackerentry);
        }

        @Override
        public boolean broadcastToPlayer(ServerPlayer player) {
            return NMS.shouldBroadcastToPlayer(npc, () -> super.broadcastToPlayer(player));
        }

        @Override
        public CraftEntity getBukkitEntity() {
            if (npc != null && !(super.getBukkitEntity() instanceof NPCHolder)) {
                NMSImpl.setBukkitEntity(this, new MinecartRideableNPC(this));
            }
            return super.getBukkitEntity();
        }

        @Override
        public NPC getNPC() {
            return npc;
        }

        @Override
        public PushReaction getPistonPushReaction() {
            return Util.callPistonPushEvent(npc) ? PushReaction.IGNORE : super.getPistonPushReaction();
        }

        @Override
        public boolean isPushable() {
            return npc == null ? super.isPushable()
                    : npc.data().<Boolean> get(NPC.Metadata.COLLIDABLE, !npc.isProtected());
        }

        @Override
        protected AABB makeBoundingBox() {
            return NMSBoundingBox.makeBB(npc, super.makeBoundingBox());
        }

        @Override
        public void push(Entity entity) {
            // this method is called by both the entities involved - cancelling
            // it will not stop the NPC from moving.
            super.push(entity);
            if (npc != null) {
                Util.callCollisionEvent(npc, entity.getBukkitEntity());
            }
        }

        @Override
        public boolean save(CompoundTag save) {
            return npc == null ? super.save(save) : false;
        }

        @Override
        public Entity teleport(TeleportTransition transition) {
            if (npc == null)
                return super.teleport(transition);
            return NMSImpl.teleportAcrossWorld(this, transition);
        }

        @Override
        public void tick() {
            super.tick();
            if (npc != null) {
                npc.update();
                NMSImpl.minecartItemLogic(this);
            }
        }

        @Override
        public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> tagkey, double d0) {
            if (npc == null)
                return super.updateFluidHeightAndDoFluidPushing(tagkey, d0);
            Vec3 old = getDeltaMovement().add(0, 0, 0);
            boolean res = super.updateFluidHeightAndDoFluidPushing(tagkey, d0);
            if (!npc.isPushableByFluids()) {
                setDeltaMovement(old);
            }
            return res;
        }
    }

    public static class MinecartRideableNPC extends CraftMinecartRideable implements ForwardingNPCHolder {
        public MinecartRideableNPC(EntityMinecartRideableNPC entity) {
            super((CraftServer) Bukkit.getServer(), entity);
        }
    }
}