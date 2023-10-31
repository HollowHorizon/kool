package de.fabmax.kool.physics.joints

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.physics.*
import physx.PxDistanceJoint
import physx.PxDistanceJointFlagEnum
import physx.maxDistance
import physx.minDistance

actual class DistanceJoint actual constructor(
    actual val bodyA: RigidActor,
    actual val bodyB: RigidActor,
    posA: Mat4f,
    posB: Mat4f
) : Joint() {
    actual val frameA = Mat4f(posA)
    actual val frameB = Mat4f(posB)

    override val pxJoint: PxDistanceJoint

    init {
        Physics.checkIsLoaded()
        MemoryStack.stackPush().use { mem ->
            val frmA = frameA.toPxTransform(mem.createPxTransform())
            val frmB = frameB.toPxTransform(mem.createPxTransform())
            pxJoint = PxTopLevelFunctions.DistanceJointCreate(Physics.physics, bodyA.pxRigidActor, frmA, bodyB.pxRigidActor, frmB)
        }
    }

    actual fun setMaxDistance(maxDistance: Float) {
        pxJoint.maxDistance = maxDistance
        pxJoint.setDistanceJointFlag(PxDistanceJointFlagEnum.eMAX_DISTANCE_ENABLED, maxDistance >= 0f)
    }
    actual fun setMinDistance(minDistance: Float) {
        pxJoint.minDistance = minDistance
        pxJoint.setDistanceJointFlag(PxDistanceJointFlagEnum.eMIN_DISTANCE_ENABLED, minDistance >= 0f)
    }

    actual fun removeMaxDistance() {
        pxJoint.setDistanceJointFlag(PxDistanceJointFlagEnum.eMAX_DISTANCE_ENABLED, false)
    }

    actual fun removeMinDistance() {
        pxJoint.setDistanceJointFlag(PxDistanceJointFlagEnum.eMIN_DISTANCE_ENABLED, false)
    }
}