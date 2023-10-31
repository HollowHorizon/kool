package de.fabmax.kool.physics.character

import de.fabmax.kool.math.MutableMat4f
import de.fabmax.kool.math.MutableVec3d
import de.fabmax.kool.math.Vec3d
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.physics.*
import physx.character.PxController
import physx.character.PxControllerCollisionFlagEnum
import physx.character.PxControllerFilters
import physx.character.PxExtendedVec3
import physx.common.PxVec3

class JvmCharacterController(
    private val pxController: PxController,
    private val hitListener: ControllerHitListener,
    private val behaviorCallback: ControllerBahaviorCallback,
    manager: CharacterControllerManager,
    world: PhysicsWorld
) : CharacterController(manager, world) {

    private val bufPosition = MutableVec3d()
    private val bufPxPosition = PxExtendedVec3()
    private val bufPxVec3 = PxVec3()
    private val pxControllerFilters = PxControllerFilters()

    init {
        hitListener.controller = this
        behaviorCallback.controller = this
    }

    override var position: Vec3d
        get() = pxController.position.toVec3d(bufPosition)
        set(value) {
            pxController.position = value.toPxExtendedVec3(bufPxPosition)
            prevPosition.set(value)
        }

    override val actor: RigidDynamic = RigidDynamic(1f, MutableMat4f(), false, pxController.actor)

    override fun move(displacement: Vec3f, timeStep: Float) {
        val flags = pxController.move(displacement.toPxVec3(bufPxVec3), 0.001f, timeStep, pxControllerFilters)

        isDownCollision = flags.isSet(PxControllerCollisionFlagEnum.eCOLLISION_DOWN)
        isUpCollision = flags.isSet(PxControllerCollisionFlagEnum.eCOLLISION_UP)
        isSideCollision = flags.isSet(PxControllerCollisionFlagEnum.eCOLLISION_SIDES)
    }

    override fun release() {
        super.release()
        pxController.release()
        bufPxPosition.destroy()
        bufPxVec3.destroy()
        pxControllerFilters.destroy()
        hitListener.destroy()
        behaviorCallback.destroy()
    }
}