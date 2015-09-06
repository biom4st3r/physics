/**
 * Copyright (c) 2015, Mine Fortress.
 */
package net.gliby.physics.common.physics.swig;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3f;

import net.gliby.physics.common.physics.ICollisionShape;
import net.gliby.physics.common.physics.ICollisionShapeChildren;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape;
import com.bulletphysics.linearmath.Transform;

/**
 *
 */
class BulletCollisionShape implements ICollisionShape {
	private static final int BOX_SHAPE = 0;

	private btCollisionShape shape;

	BulletCollisionShape(btCollisionShape shape) {
		this.shape = shape;
	}

	@Override
	public Object getCollisionShape() {
		return shape;
	}

	@Override
	public int getShapeType() {
		return shape.getShapeType();
	}

	@Override
	public boolean isBoxShape() {
		return shape.getShapeType() == BOX_SHAPE;
	}

	@Override
	public boolean isCompoundShape() {
		return shape.isCompound();
	}

	@Override
	public void calculateLocalInertia(float mass, Object localInertia) {
		shape.calculateLocalInertia(mass, (Vector3) localInertia);
	}

	@Override
	public void getHalfExtentsWithMargin(Vector3f halfExtent) {
		halfExtent.set(BulletPhysicsWorld.toVector3f(((btBoxShape) shape).getHalfExtentsWithMargin()));
	}

	@Override
	public List<ICollisionShapeChildren> getChildren() {
		ArrayList<ICollisionShapeChildren> shapeList = new ArrayList<ICollisionShapeChildren>();
		final btCompoundShape compoundShape = (btCompoundShape) shape;
		for (int i = 0; i < compoundShape.getNumChildShapes(); i++) {
			final int index = i;
			final Transform transform = new Transform();
			transform.setIdentity();
			transform.set(BulletPhysicsWorld.toMatrix4f(compoundShape.getChildTransform(index)));
			shapeList.add(new ICollisionShapeChildren() {
				@Override
				public Transform getTransform() {
					return transform;
				}

				@Override
				public ICollisionShape getCollisionShape() {
					return new BulletCollisionShape(compoundShape.getChildShape(index));
				}
			});
		}
		return shapeList;
	}

}
