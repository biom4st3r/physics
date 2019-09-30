package gliby.minecraft.physics.common.physics;

import com.bulletphysicsx.linearmath.Clock;
import com.bulletphysicsx.linearmath.Transform;
import com.google.gson.annotations.SerializedName;
import gliby.minecraft.gman.WorldUtility;
import gliby.minecraft.physics.common.physics.engine.*;
import gliby.minecraft.physics.common.physics.mechanics.PhysicsMechanic;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import javax.vecmath.Vector3f;
import java.util.*;

/**
 *
 */

public abstract class PhysicsWorld {

    private final IPhysicsWorldConfiguration physicsConfiguration;
    private final Vector3f gravityDirection;
    protected HashMap<String, PhysicsMechanic> physicsMechanics;
    protected Clock clock = new Clock();

    public PhysicsWorld(final IPhysicsWorldConfiguration physicsConfiguration) {
        this.physicsConfiguration = physicsConfiguration;
        physicsMechanics = new HashMap<String, PhysicsMechanic>();
        gravityDirection = new Vector3f(physicsConfiguration.getRegularGravity());
        gravityDirection.normalize();
    }

    public Vector3f getGravityDirection() {
        return gravityDirection;
    }

    public IPhysicsWorldConfiguration getPhysicsConfiguration() {
        return physicsConfiguration;
    }

    public Map<String, PhysicsMechanic> getMechanics() {
        return physicsMechanics;
    }

    public void create() {
        final Iterator it = physicsMechanics.entrySet().iterator();
        while (it.hasNext()) {
            PhysicsMechanic mechanic = ((Map.Entry<String, PhysicsMechanic>) it.next()).getValue();
            mechanic.init();
        }
    }

    public ICollisionShape createBlockShape(final World worldObj, final BlockPos blockPos, final IBlockState blockState) {
        if (!blockState.getBlock().isNormalCube()) {
            final List<AxisAlignedBB> collisionBBs = new ArrayList<AxisAlignedBB>();
            blockState.getBlock().addCollisionBoxesToList(worldObj, blockPos, blockState, WorldUtility.MAX_BB,
                    collisionBBs, null);
            return buildCollisionShape(collisionBBs, new Vector3f(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        }
        final Vector3f blockPosition = new Vector3f((float) blockState.getBlock().getBlockBoundsMaxX(),
                (float) blockState.getBlock().getBlockBoundsMaxY(), (float) blockState.getBlock().getBlockBoundsMaxZ());
        blockPosition.scale(0.5f);
        return createBoxShape(blockPosition);
    }

    public void dispose() {
        final Iterator it = physicsMechanics.entrySet().iterator();
        while (it.hasNext()) {
            final PhysicsMechanic mechanic = ((Map.Entry<String, PhysicsMechanic>) it.next()).getValue();
            mechanic.dispose();
            mechanic.setEnabled(false);
            mechanic.physicsWorld = null;
        }
        physicsMechanics.clear();
    }

    public float getDeltaTimeMicroseconds() {
        float dt = clock.getTimeMicroseconds();
        clock.reset();
        return dt;
    }


    protected void update(final int maxSubSteps) {
        final Iterator it = physicsMechanics.entrySet().iterator();
        while (it.hasNext()) {
            final PhysicsMechanic mechanic = ((Map.Entry<String, PhysicsMechanic>) it.next()).getValue();
            mechanic.update();
        }
    }

    public abstract ICollisionShape buildCollisionShape(List<AxisAlignedBB> bbs, Vector3f offset);

    public abstract IRigidBody createRigidBody(Entity owner, Transform transform, float mass, ICollisionShape shape);

    public abstract IRigidBody createInertialessRigidbody(Entity owner, Transform transform, float mass,
                                                          ICollisionShape shape);

    public abstract ICollisionShape createBoxShape(Vector3f extents);

    public abstract IRayResult createClosestRayResultCallback(Vector3f rayFromWorld, Vector3f rayToWorld);

    public abstract void addRope(IRope object);

    public abstract void addRigidBody(final IRigidBody body);

    public abstract void addRigidBody(final IRigidBody body, short collisionFilterGroup, short collisionFilterMask);

    public abstract void removeRigidBody(final IRigidBody body);

    public abstract void awakenArea(final Vector3f min, final Vector3f max);

    public abstract void rayTest(final Vector3f rayFromWorld, final Vector3f rayToWorld, IRayResult resultCallback);

    public abstract void clearRayTest(final IRayResult resultCallback);

    public abstract void removeCollisionObject(final ICollisionObject collisionObject);

    public abstract void setGravity(final Vector3f newGravity);

    public abstract void addCollisionObject(ICollisionObject object);

    public abstract void addCollisionObject(ICollisionObject collisionObject, short collisionFilterGroup,
                                            short collisionFilterMask);


    public abstract List<IRigidBody> getRigidBodies();

    public abstract List<IRope> getRopes();

    /**
     * @param startPos
     * @param endPos
     * @param detail
     * @return
     */
    public abstract IRope createRope(Vector3f startPos, Vector3f endPos, int detail);

    /**
     * @return
     */
    public abstract IGhostObject createPairCachingGhostObject();

    /**
     * @param collisionObject
     * @return
     */
    public abstract IRigidBody upcastRigidBody(Object collisionObject);

    /**
     * @param rigidBody
     * @param pivot
     * @return
     */
    public abstract IConstraintPoint2Point createPoint2PointConstraint(IRigidBody rigidBody, Vector3f pivot);

    /**
     * @param constraint
     */
    public abstract void addConstraint(IConstraint constraint);

    /**
     * @param constraint
     */
    public abstract void removeConstraint(IConstraint constraint);

    public abstract void removeRope(IRope rope);

    /**
     * @param string
     * @return
     */
    public abstract ICollisionShape readBlockCollisionShape(String string);

    /**
     * @param shape
     * @return
     */
    public abstract String writeBlockCollisionShape(ICollisionShape shape);

    /**
     * @return
     */
    public abstract List<IConstraint> getConstraints();

    public abstract IConstraintSlider createSliderConstraint(IRigidBody rbA, IRigidBody rbB, Transform frameInA,
                                                             Transform frameInB, boolean useLinearReferenceFrameA);

    /**
     * @return
     */
    public abstract IConstraintGeneric6Dof createGeneric6DofConstraint(IRigidBody rbA, IRigidBody rbB,
                                                                       Transform frameInA, Transform frameInB, boolean useLinearReferenceFrameA);

    public abstract ICollisionShape createSphereShape(float radius);

    protected class CollisionPart {

        @SerializedName("isCompoundShape")
        public boolean compoundShape;
        @SerializedName("Transform")
        public Transform transform;
        @SerializedName("Extent")
        public Vector3f extent;

        /**
         * @param transform
         * @param extent
         */
        public CollisionPart(final boolean compoundShape, final Transform transform, final Vector3f extent) {
            this.compoundShape = compoundShape;
            this.transform = transform;
            this.extent = extent;
        }
    }

}
