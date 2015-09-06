/**
 * Copyright (c) 2015, Mine Fortress.
 */
package net.gliby.physics.common.event;

import javax.vecmath.Vector3f;

import net.gliby.physics.Physics;
import net.gliby.physics.common.EntityUtility;
import net.gliby.physics.common.entity.EntityPhysicsBlock;
import net.gliby.physics.common.physics.IRigidBody;
import net.gliby.physics.common.physics.PhysicsWorld;
import net.gliby.physics.common.physics.block.PhysicsBlockMetadata;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 *
 */
public class ExplosionHandler {

	Physics physics;

	public ExplosionHandler(Physics physics) {
		this.physics = physics;
	}

	@SubscribeEvent
	public void handleEvent(ExplosionEvent.Detonate event) {
		PhysicsWorld physicsWorld = physics.getCommonProxy().getPhysicsOverworld().getPhysicsByWorld(event.world);
		Vector3f explosion = new Vector3f((float) event.explosion.getPosition().xCoord,
				(float) event.explosion.getPosition().yCoord, (float) event.explosion.getPosition().zCoord);

		for (int i = 0; i < event.getAffectedBlocks().size(); i++) {
			// TODO Probability that TNT will spawn?
			BlockPos pos = event.getAffectedBlocks().get(i);
			IBlockState blockState = event.world.getBlockState(pos);
			PhysicsBlockMetadata metadata = Physics.getInstance().getCommonProxy().getPhysicsOverworld()
					.getPhysicsBlockMetadata().get(blockState.getBlock().getUnlocalizedName());
			boolean shouldSpawnInExplosions = metadata != null ? metadata.shouldSpawnInExplosion : true;
			if (blockState.getBlock().getMaterial() != Material.air && shouldSpawnInExplosions) {
				blockState = blockState.getBlock().getActualState(blockState, event.world, pos);
				EntityPhysicsBlock analog = new EntityPhysicsBlock(event.world, physicsWorld, blockState, pos.getX(),
						pos.getY(), pos.getZ());
				EntityUtility.spawnEntitySynchronized(event.world, analog);
			}
		}

		float force = Physics.getInstance().getSettings().getFloatSetting("Game.ExplosionImpulseForce").getFloatValue();
		for (int i = 0; i < physicsWorld.getRigidBodies().size(); i++) {
			IRigidBody body = physicsWorld.getRigidBodies().get(i);
			Vector3f centerOfMass = body.getCenterOfMassPosition(new Vector3f());
			Vector3f direction = new Vector3f();
			direction.sub(centerOfMass, explosion);
			float distance = direction.length();
			if (distance <= physics.getSettings().getFloatSetting("Game.ExplosionImpulseRadius").getFloatValue()) {
				direction.normalize();
				direction.scale(force * (distance
						/ physics.getSettings().getFloatSetting("Game.ExplosionImpulseRadius").getFloatValue()));
				body.applyCentralImpulse(direction);
			}
		}
	}
}
