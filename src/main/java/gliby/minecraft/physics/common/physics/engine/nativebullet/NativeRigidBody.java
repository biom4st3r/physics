package gliby.minecraft.physics.common.physics.engine.nativebullet;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.bulletphysicsx.linearmath.Transform;
import gliby.minecraft.physics.client.render.VecUtility;
import gliby.minecraft.physics.common.physics.PhysicsWorld;
import gliby.minecraft.physics.common.physics.engine.ICollisionShape;
import gliby.minecraft.physics.common.physics.engine.IRigidBody;
import net.minecraft.entity.Entity;

import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
class NativeRigidBody extends NativeCollisionObject implements IRigidBody {

    private btRigidBody rigidBody;
    private ICollisionShape collisionShape;
    private Map<String, Object> properties;

    public NativeRigidBody(PhysicsWorld physicsWorld, btRigidBody rigidBody, Entity entity) {
        super(physicsWorld, entity, rigidBody);
        this.physicsWorld = (NativePhysicsWorld) physicsWorld;
        this.rigidBody = rigidBody;
        this.collisionShape = new NativeCollisionShape(physicsWorld, rigidBody.getCollisionShape());
        this.properties = new HashMap<String, Object>();
    }

    @Override
    public Object getBody() {
        return rigidBody;
    }

    @Override
    public ICollisionShape getCollisionShape() {
        return collisionShape;
    }

    @Override
    public boolean isActive() {
        return rigidBody.isActive();

    }

    @Override
    public Vector3f getAngularVelocity() {
        return VecUtility.toVector3f(rigidBody.getAngularVelocity());
    }

    @Override
    public Vector3f getLinearVelocity() {
        return VecUtility.toVector3f(rigidBody.getLinearVelocity());

    }

    /*
     * (non-Javadoc)
     *
     * @see net.gliby.physics.common.physics.IRigidBody#getWorldTransform(com.
     * bulletphysics.linearmath.Transform)
     */
    @Override
    public Transform getWorldTransform() {
        return VecUtility.toTransform(rigidBody.getWorldTransform());
    }

    public void setWorldTransform(final Transform transform) {
        rigidBody.setWorldTransform(VecUtility.toMatrix4(transform));

    }

    @Override
    public boolean isValid() {
        return !rigidBody.isDisposed();
    }

    @Override
    public void setGravity(final Vector3f acceleration) {
        rigidBody.setGravity(VecUtility.toVector3(acceleration));

    }

    @Override
    public void setFriction(final float friction) {
        rigidBody.setFriction(friction);

    }

    @Override
    public void setLinearVelocity(final Vector3f linearVelocity) {
        rigidBody.setLinearVelocity(VecUtility.toVector3(linearVelocity));

    }

    @Override
    public void setAngularVelocity(Vector3f angularVelocity) {
        rigidBody.setAngularVelocity(VecUtility.toVector3(angularVelocity));

    }

    @Override
    public void applyCentralImpulse(final Vector3f direction) {
        rigidBody.applyCentralImpulse(VecUtility.toVector3(direction));

    }

    @Override
    public boolean hasContactResponse() {
        return rigidBody.hasContactResponse();

    }

    @Override
    public float getInvMass() {
        return rigidBody.getInvMass();
    }

    @Override
    public void activate() {
        rigidBody.activate();

    }

    @Override
    public void getAabb(Vector3f aabbMin, Vector3f aabbMax) {
        Vector3 min = new Vector3(), max = new Vector3();
        rigidBody.getAabb(min, max);
        aabbMin.set(VecUtility.toVector3f(min));
        aabbMin.set(VecUtility.toVector3f(max));

    }

    @Override
    public Transform getCenterOfMassTransform() {
        return VecUtility.toTransform(rigidBody.getCenterOfMassTransform());
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public void applyCentralForce(final Vector3f force) {
        rigidBody.applyCentralForce(VecUtility.toVector3(force));

    }

    @Override
    public Vector3f getGravity(Vector3f vector3f) {
        vector3f.set(VecUtility.toVector3f(rigidBody.getGravity()));
        return vector3f;
    }

    @Override
    public Vector3f getCenterOfMassPosition() {
        return VecUtility.toVector3f(rigidBody.getCenterOfMassPosition());
    }

    @Override
    public Quat4f getRotation() {
        return VecUtility.toQuat4f(rigidBody.getOrientation());
    }

    @Override
    public Vector3f getPosition() {
        return VecUtility.toVector3f(rigidBody.getWorldTransform().getTranslation(new Vector3()));
    }

    @Override
    public void applyTorque(final Vector3f vector) {
        rigidBody.applyTorque(VecUtility.toVector3(vector));

    }

    @Override
    public void applyTorqueImpulse(final Vector3f vector) {
        rigidBody.applyTorqueImpulse(VecUtility.toVector3(vector));
    }

    @Override
    public void dispose() {
        if (!rigidBody.isDisposed()) {
            rigidBody.dispose();
        }
    }
}