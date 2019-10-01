package gliby.minecraft.physics.common.entity;

import com.bulletphysicsx.collision.broadphase.CollisionFilterGroups;
import com.bulletphysicsx.linearmath.QuaternionUtil;
import com.bulletphysicsx.linearmath.Transform;
import gliby.minecraft.gman.BlockUtility;
import gliby.minecraft.gman.DataWatchableQuat4f;
import gliby.minecraft.gman.DataWatchableVector3f;
import gliby.minecraft.physics.Physics;
import gliby.minecraft.physics.client.render.RenderHandler;
import gliby.minecraft.physics.common.blocks.PhysicsBlockMetadata;
import gliby.minecraft.physics.common.entity.mechanics.RigidBodyMechanic;
import gliby.minecraft.physics.common.physics.PhysicsWorld;
import gliby.minecraft.physics.common.physics.engine.ICollisionShape;
import gliby.minecraft.physics.common.physics.engine.IQuaternion;
import gliby.minecraft.physics.common.physics.engine.IRigidBody;
import gliby.minecraft.physics.common.physics.engine.IVector3;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

/**
 *
 */

// TODO improvement: Remove ability to spawn tile-entites.
public class EntityPhysicsBlock extends EntityPhysicsBase implements IEntityAdditionalSpawnData {

    /**
     * Client-side render position, basically a smoothed position.
     */
    @SideOnly(Side.CLIENT)
    public Vector3f renderPosition;
    /**
     * Client-side render rotation, basically a smoothed rotation.
     */
    @SideOnly(Side.CLIENT)
    public Quat4f renderRotation;
    /**
     * Optional drop item.
     */
    private ItemStack dropItem;
    /**
     * Block.
     */
    private IBlockState blockState;
    /**
     * Common variable, physics object's absolute position.
     */
    private Vector3f position = new Vector3f();
    /**
     * Common variable, physics object's absolute rotation.
     */
    private Quat4f rotation = new Quat4f();
    /**
     * Reference to block's built rigid body.
     */
    private IRigidBody rigidBody;
    /**
     * Reference to rigid bodies -> collision shape.
     */
    private ICollisionShape collisionShape;
    /**
     * If true, physics block will not use generated collision shape, but rather a
     * simple box shape.
     */
    private boolean defaultCollisionShape;
    /**
     * Collision with entites(including players).
     */
    private boolean collisionEnabled = true;
    /**
     * Rigid body mass.
     */
    private float mass;
    /**
     * Friction.
     */
    private float friction;
    private Vector3f linearVelocity, angularVelocity;
    private DataWatchableVector3f watchablePosition;
    private DataWatchableQuat4f watchableRotation;
    @SideOnly(Side.CLIENT)
    private BlockPos blockPosition;
    private @SideOnly(Side.CLIENT)
    boolean hasLight;
    private int tintIndex;

    public EntityPhysicsBlock(World world) {
        super(world);
        noClip = true;
        setSize(0.85f, 1.05f);
    }

    public EntityPhysicsBlock(World world, PhysicsWorld physicsWorld, IBlockState blockState, float x, float y,
                              float z) {
        super(world, physicsWorld);

        this.blockState = blockState;
        this.position.set(x, y, z);
        QuaternionUtil.setEuler(rotation, 0, 0, 0);
        this.rotation.set(rotation);

        if (world.isRemote) {
            this.renderPosition = new Vector3f(position);
            this.renderRotation = new Quat4f(rotation);
        }

        Physics physics = Physics.getInstance();

        PhysicsBlockMetadata metadata = physics.getBlockManager().getPhysicsBlockMetadata()
                .get(physics.getBlockManager().getBlockIdentity(blockState.getBlock()));
        boolean metadataExists = metadata != null;

        this.mass = metadataExists ? metadata.mass : 10;
        this.friction = metadataExists ? metadata.friction : 0.5f;
        this.defaultCollisionShape = metadataExists && metadata.defaultCollisionShape;
        this.collisionEnabled = !metadataExists || metadata.collisionEnabled;

        if (this.defaultCollisionShape)
            this.collisionShape = physicsWorld.getDefaultShape();
        else
            this.collisionShape = physicsWorld.getBlockCache().getShape(world, new BlockPos(x, y, z), blockState);


//        Physics.getLogger().error("Block doesn't exist, couldn't create collision shape");


        createPhysicsObject(physicsWorld);
        if (metadata != null) {
            if (metadata.mechanics != null)
                this.mechanics.addAll(metadata.mechanics);
        }
        setLocationAndAngles(position.x, position.y, position.z, 0, 0);
    }

    public EntityPhysicsBlock setDropItem(ItemStack dropItem) {
        this.dropItem = dropItem;
        return this;
    }

    /**
     * Enables entity->entity collision.
     */
    @Override
    public AxisAlignedBB getBoundingBox() {
        return collisionEnabled ? AxisAlignedBB.fromBounds(0.20f, 0, 0.20f, 0.80f, 1.05f, 0.80f).offset(
                Math.round(position.x * 100.0f) / 100.0f, Math.round(position.y * 100.0f) / 100.0f,
                Math.round(position.z * 100.0f) / 100.0f) : null;
    }

    @Override
    protected void createPhysicsObject(PhysicsWorld physicsWorld) {

        this.watchablePosition = new DataWatchableVector3f(this, position);
        this.watchableRotation = new DataWatchableQuat4f(this, rotation);

        Transform transform = new Transform();
        transform.setIdentity();
        transform.origin.set(this.position);
        transform.setRotation(this.rotation);
        rigidBody = physicsWorld.createRigidBody(this, transform, Math.abs(mass), collisionShape);
        rigidBody.getProperties().put(EnumRigidBodyProperty.BLOCKSTATE.getName(), blockState);

        for (int i = 0; i < mechanics.size(); i++) {
            RigidBodyMechanic mechanic = mechanics.get(i);
            mechanic.onCreatePhysics(rigidBody);
        }

        if (collisionEnabled)
            physicsWorld.addRigidBody(rigidBody);
        else
            physicsWorld.addRigidBody(rigidBody, CollisionFilterGroups.CHARACTER_FILTER,
                    CollisionFilterGroups.ALL_FILTER);

        if (mass < 0)
            rigidBody.setGravity(new Vector3f());
        rigidBody.setFriction(friction);
        if (linearVelocity != null)
            rigidBody.setLinearVelocity(linearVelocity);
        if (angularVelocity != null)
            rigidBody.setAngularVelocity(angularVelocity);
    }

    @Override
    public void onCommonUpdate() {
        this.prevDistanceWalkedModified = this.distanceWalkedModified;
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;

    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBrightnessForRender(float p_70070_1_) {
        AxisAlignedBB renderAABB = this.getRenderBoundingBox();
        float width = (float) (renderAABB.maxX - renderAABB.minX);
        float height = (float) (renderAABB.maxY - renderAABB.minY);
        float length = (float) (renderAABB.maxZ - renderAABB.minZ);
        float lightX = (float) (renderAABB.minX + (width / 2));
        float lightY = (float) (renderAABB.minY + (height / 2));
        float lightZ = (float) (renderAABB.minZ + (length / 2));
        blockPosition = new BlockPos(lightX, lightY, lightZ);
        if (tintIndex == 0)
            this.tintIndex = blockState.getBlock().colorMultiplier(worldObj, blockPosition, 0);
        if (this.worldObj.isBlockLoaded(blockPosition)) {
            int lightValue = this.worldObj.getCombinedLight(blockPosition, 0);
            return lightValue;
        } else {
            return 0;
        }
    }

    @Override
    public void onServerUpdate() {
        if (rigidBody != null && rigidBody.isValid()) {
            // Update position from given rigid body.
            final IVector3 newPosition = rigidBody.getPosition();

            position.set(newPosition.getX(), newPosition.getY(),
                    newPosition.getZ());
            // Update rotation from given rigid body.
            final IQuaternion newQuat = rigidBody.getRotation();
            rotation.set(newQuat.getX(), newQuat.getY(), newQuat.getZ(),
                    newQuat.getW());
            // Set location and angles, so client could have proper bounding
            // boxes.
            setLocationAndAngles(position.x + 0.5f, position.y, position.z + 0.5f, 0, 0);
            // Check if rigidBody is active, and if the last written postion
            // and rotation has changed.
            if (isDirty()) {
                if (watchablePosition != null && (!watchablePosition.lastWrote.equals(position)
                        || !watchableRotation.lastWrote.equals(rotation))) {
                    watchableRotation.write(rotation);
                    watchablePosition.write(position);
                }
            }
        }

    }

    @Override
    public void onClientUpdate() {
        // No-collision check.

        this.onGround = false;

        // Read dataWatcher objects, then set.
        watchablePosition.read(position);
        watchableRotation.read(rotation);

        // Force vanilla entity bounding box to follow custom physics render
        // bounding box.
        this.setEntityBoundingBox(getRenderBoundingBox());
        // setPosition(renderPosition.x + 0.5f, renderPosition.y,
        // renderPosition.z + 0.5f);

    }

    @Override
    public void writeEntityToNBT(final NBTTagCompound tagCompound) {
        ResourceLocation resourcelocation = (ResourceLocation) Block.blockRegistry
                .getNameForObject(blockState.getBlock());
        tagCompound.setString("Block", resourcelocation == null ? "" : resourcelocation.toString());
        tagCompound.setByte("Data", (byte) blockState.getBlock().getMetaFromState(blockState));

        tagCompound.setFloat("Mass", mass);
        tagCompound.setFloat("Friction", friction);

        if (defaultCollisionShape)
            tagCompound.setBoolean("DefaultCollisionShape", defaultCollisionShape);
        if (!collisionEnabled)
            tagCompound.setBoolean("CollisionEnabled", collisionEnabled);

        // Remove original tags.
        tagCompound.removeTag("Pos");
        tagCompound.removeTag("Rotation");

        // Add removed tags, but with additional values.
        tagCompound.setTag("Pos", newDoubleNBTList(position.x, position.y, position.z));
        // adjust actually position
        setPosition(position.x, position.y, position.z);

        tagCompound.setTag("Rotation", newFloatNBTList(rotation.x, rotation.y, rotation.z, rotation.w));

        Vector3f linearVelocity = rigidBody.getLinearVelocity(new Vector3f());
        tagCompound.setTag("LinearVelocity",
                newFloatNBTList(linearVelocity.x, linearVelocity.y, linearVelocity.z));
        Vector3f angularVelocity = rigidBody.getAngularVelocity(new Vector3f());
        tagCompound.setTag("AngularVelocity",
                newFloatNBTList(angularVelocity.x, angularVelocity.y, angularVelocity.z));

        if (dropItem != null) {
            NBTTagCompound compound = dropItem.writeToNBT(new NBTTagCompound());
            tagCompound.setTag("DropItem", compound);
        }
        super.writeEntityToNBT(tagCompound);

    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tagCompound) {
        super.readEntityFromNBT(tagCompound);

        this.physicsWorld = Physics.getInstance().getPhysicsOverworld().getPhysicsByWorld(worldObj);
        int data = tagCompound.getByte("Data") & 255;

        if (tagCompound.hasKey("Block", 8))
            this.blockState = Block.getBlockFromName(tagCompound.getString("Block")).getStateFromMeta(data);
        else if (tagCompound.hasKey("TileID", 99))
            this.blockState = Block.getBlockById(tagCompound.getInteger("TileID")).getStateFromMeta(data);
        else
            this.blockState = Block.getBlockById(tagCompound.getByte("Tile") & 255).getStateFromMeta(data);

        this.mass = (float) (tagCompound.hasKey("Mass") ? tagCompound.getFloat("Mass") : 1.0);
        this.friction = (float) (tagCompound.hasKey("Friction") ? tagCompound.getFloat("Friction") : 0.5);
        this.defaultCollisionShape = tagCompound.hasKey("DefaultCollisionShape") && tagCompound.getBoolean("DefaultCollisionShape");
        this.collisionEnabled = tagCompound.hasKey("CollisionEnabled") ? tagCompound.getBoolean("CollisionEnabled")
                : collisionEnabled;

        if (tagCompound.hasKey("CollisionShape")) {
            this.collisionShape = physicsWorld.getBlockCache().getShape(worldObj, new BlockPos((int) posX, (int) posY, (int) posZ), blockState);
        } else
            this.collisionShape = physicsWorld.getDefaultShape();

        this.position.set((float) posX, (float) posY, (float) posZ);
        if (tagCompound.hasKey("Rotation")) {
            NBTTagList list = tagCompound.getTagList("Rotation", 5);
            this.rotation.set(list.getFloat(0), list.getFloat(1), list.get(2) != null ? list.getFloat(2) : 0,
                    list.get(3) != null ? list.getFloat(3) : 0);
        }

        if (tagCompound.hasKey("LinearVelocity")) {
            NBTTagList linearVelocity = tagCompound.getTagList("LinearVelocity", 5);
            this.linearVelocity = new Vector3f(linearVelocity.getFloat(0), linearVelocity.getFloat(1),
                    linearVelocity.getFloat(2));
        }

        if (tagCompound.hasKey("AngularVelocity")) {
            NBTTagList angularVelocity = tagCompound.getTagList("AngularVelocity", 5);
            this.linearVelocity = new Vector3f(angularVelocity.getFloat(0), angularVelocity.getFloat(1),
                    angularVelocity.getFloat(2));
        }

        if (tagCompound.hasKey("DropItem")) {
            NBTTagCompound compound = tagCompound.getCompoundTag("DropItem");
            this.dropItem = ItemStack.loadItemStackFromNBT(compound);
        }

        // Read from NBT gets called multiple times.
        if (!doesPhysicsObjectExist()) {
            createPhysicsObject(physicsWorld);
        } else {
            updatePhysicsObject(physicsWorld);
        }

    }

    @Override
    protected void updatePhysicsObject(PhysicsWorld physicsWorld) {
        Transform transform = new Transform();
        transform.setIdentity();
        transform.origin.set(this.position);
        transform.setRotation(this.rotation);
        rigidBody.setWorldTransform(transform);
        // Used for specific block mechanics.
        rigidBody.getProperties().put(EnumRigidBodyProperty.BLOCKSTATE.getName(), blockState);
        if (mass < 0)
            rigidBody.setGravity(new Vector3f());
        rigidBody.setFriction(friction);
        if (linearVelocity != null)
            rigidBody.setLinearVelocity(linearVelocity);
        if (angularVelocity != null)
            rigidBody.setAngularVelocity(angularVelocity);

    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        super.writeSpawnData(buffer);

        BlockUtility.serializeBlockState(blockState, buffer);
        buffer.writeBoolean(collisionEnabled);
        buffer.writeFloat(position.x);
        buffer.writeFloat(position.y);
        buffer.writeFloat(position.z);

        buffer.writeFloat(rotation.x);
        buffer.writeFloat(rotation.y);
        buffer.writeFloat(rotation.z);
        buffer.writeFloat(rotation.w);
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        super.readSpawnData(buffer);
        if (watchablePosition == null) {
            watchablePosition = new DataWatchableVector3f(this, position);
            watchableRotation = new DataWatchableQuat4f(this, rotation);
        }
        this.blockState = BlockUtility.deserializeBlockState(buffer);
        this.collisionEnabled = buffer.readBoolean();
        this.position = new Vector3f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        this.rotation = new Quat4f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        this.renderPosition = new Vector3f(position);
        this.renderRotation = new Quat4f(rotation);
        // Create dynamic light source if we can!
        if (blockState.getBlock().getLightValue() > 0 && !hasLight) {
            RenderHandler.getLightHandler().create(this, blockState.getBlock().getLightValue());
            hasLight = true;
        }

    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean canRenderOnFire() {
        return isBurning();
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    /**
     * @return
     */
    public IBlockState getBlockState() {
        return blockState;
    }

    @Override
    public boolean isDirty() {
        return rigidBody.isActive();
    }

    @Override
    protected boolean doesPhysicsObjectExist() {
        return rigidBody != null;
    }

    /**
     * @return
     */
    public IRigidBody getRigidBody() {
        return rigidBody;
    }

    @Override
    protected void dispose() {
        // Drops item.
        if (dropItem != null && isNaturalDeath()) {
            entityDropItem(dropItem, 0);
            /*
             * Vector3f centerOfMass = rigidBody.getCenterOfMassPosition();
             * Block.spawnAsEntity(worldObj, new BlockPos(centerOfMass.x + 0.5f,
             * centerOfMass.y + 0.5F, centerOfMass.z + 0.5F), new ItemStack(dropItem));
             */
        }

        if (doesPhysicsObjectExist()) {
            physicsWorld.removeRigidBody(this.rigidBody);
        }
    }

    @Override
    public void interpolate() {
        this.renderPosition.interpolate(position, 0.15f);
        this.renderRotation.interpolate(rotation, 0.15f);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.fromBounds(-0.2f, -0.2f, -0.2f, 1.3f, 1.2f, 1.2f).offset(renderPosition.x,
                renderPosition.y, renderPosition.z);
    }

    @Override
    public void onCommonInit() {
    }

    @Override
    public void onClientInit() {
    }

    @Override
    public void onServerInit() {
    }

    public int getTintIndex() {
        return tintIndex;
    }

}