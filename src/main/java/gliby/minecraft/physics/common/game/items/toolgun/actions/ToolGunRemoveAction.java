package gliby.minecraft.physics.common.game.items.toolgun.actions;

import gliby.minecraft.gman.EntityUtility;
import gliby.minecraft.physics.common.entity.EnumRigidBodyProperty;
import gliby.minecraft.physics.common.physics.PhysicsWorld;
import gliby.minecraft.physics.common.physics.engine.IRayResult;
import gliby.minecraft.physics.common.physics.engine.IRigidBody;
import net.minecraft.entity.player.EntityPlayerMP;

import javax.vecmath.Vector3f;

/**
 *
 */
public class ToolGunRemoveAction implements IToolGunAction {

    @Override
    public String getName() {
        return "Remove";
    }

    @Override
    public boolean use(PhysicsWorld physicsWorld, EntityPlayerMP player, Vector3f otherLookAt) {
        Vector3f offset = new Vector3f(0.5f, 0.5f, 0.5f);
        Vector3f eyePos = EntityUtility.getPositionEyes(player);
        Vector3f eyeLook = EntityUtility.toVector3f(player.getLook(1));
        Vector3f lookAt = new Vector3f(eyePos);
        eyeLook.scale(64);
        lookAt.add(eyeLook);
        eyePos.sub(offset);
        lookAt.sub(offset);

        IRayResult ray = physicsWorld.createClosestRayResultCallback(eyePos, lookAt);
        physicsWorld.rayTest(eyePos, lookAt, ray);
        if (ray.hasHit()) {
            IRigidBody body = physicsWorld.upCastRigidBody(ray.getCollisionObject());
            if (body != null && body.isValid()) {
                body.getProperties().put(EnumRigidBodyProperty.DEAD.getName(), System.currentTimeMillis());
                physicsWorld.clearRayTest(ray);
                return true;
            }
        }
        physicsWorld.clearRayTest(ray);
        return false;
    }

    @Override
    public void stoppedUsing(PhysicsWorld world, EntityPlayerMP player) {
    }

}
