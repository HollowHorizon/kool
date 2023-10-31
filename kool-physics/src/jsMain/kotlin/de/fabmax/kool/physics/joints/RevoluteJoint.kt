package de.fabmax.kool.physics.joints

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.physics.*
import de.fabmax.kool.physics.joints.RevoluteJointHelper.computeFrame
import physx.PxRevoluteJoint
import physx.PxRevoluteJointFlagEnum
import physx.driveForceLimit
import physx.driveVelocity

actual class RevoluteJoint actual constructor(actual val bodyA: RigidActor, actual val bodyB: RigidActor,
                                              frameA: Mat4f, frameB: Mat4f) : Joint() {

    actual val frameA = Mat4f(frameA)
    actual val frameB = Mat4f(frameB)

    actual constructor(bodyA: RigidActor, bodyB: RigidActor,
                       pivotA: Vec3f, pivotB: Vec3f,
                       axisA: Vec3f, axisB: Vec3f)
            : this(bodyA, bodyB, computeFrame(pivotA, axisA), computeFrame(pivotB, axisB))

    override val pxJoint: PxRevoluteJoint

    init {
        Physics.checkIsLoaded()
        MemoryStack.stackPush().use { mem ->
            val frmA = frameA.toPxTransform(mem.createPxTransform())
            val frmB = frameB.toPxTransform(mem.createPxTransform())
            pxJoint = PxTopLevelFunctions.RevoluteJointCreate(Physics.physics, bodyA.pxRigidActor, frmA, bodyB.pxRigidActor, frmB)
        }
    }

    actual fun disableAngularMotor() {
        pxJoint.driveVelocity = 0f
        pxJoint.driveForceLimit = 0f
        pxJoint.setRevoluteJointFlag(PxRevoluteJointFlagEnum.eDRIVE_ENABLED, false)
    }

    actual fun enableAngularMotor(angularVelocity: Float, forceLimit: Float) {
        pxJoint.driveVelocity = angularVelocity
        pxJoint.driveForceLimit = forceLimit
        pxJoint.setRevoluteJointFlag(PxRevoluteJointFlagEnum.eDRIVE_ENABLED, true)
    }
}