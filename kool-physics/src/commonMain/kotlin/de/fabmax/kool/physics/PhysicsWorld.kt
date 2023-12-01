package de.fabmax.kool.physics

import de.fabmax.kool.KoolContext
import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.RayF
import de.fabmax.kool.math.Vec3f
import de.fabmax.kool.math.deg
import de.fabmax.kool.physics.articulations.Articulation
import de.fabmax.kool.physics.geometry.CollisionGeometry
import de.fabmax.kool.physics.geometry.PlaneGeometry
import de.fabmax.kool.scene.Scene
import de.fabmax.kool.util.BaseReleasable
import de.fabmax.kool.util.logW

expect fun PhysicsWorld(scene: Scene?, isContinuousCollisionDetection: Boolean = false) : PhysicsWorld

abstract class PhysicsWorld : BaseReleasable() {
    var physicsTime = 0.0

    var simStepper: PhysicsStepper = ConstantPhysicsStepperSync()
    var isStepInProgress = false
    var prevStepTime = 0f

    val onAdvancePhysics = mutableListOf<(Float) -> Unit>()
    val onPhysicsUpdate = mutableListOf<(Float) -> Unit>()

    protected val mutActors = mutableListOf<RigidActor>()
    val actors: List<RigidActor>
        get() = mutActors
    protected val mutArticulations = mutableListOf<Articulation>()
    val articulations: List<Articulation>
        get() = mutArticulations

    protected val triggerListeners = mutableMapOf<RigidActor, TriggerListenerContext>()
    protected val contactListeners = mutableListOf<ContactListener>()

    private var registeredAtScene: Scene? = null
    private val onRenderSceneHook: (KoolContext) -> Unit = { ctx ->
        physicsTime += simStepper.stepSimulation(this, ctx)
    }

    abstract var gravity: Vec3f
    abstract val activeActors: Int

    open fun registerHandlers(scene: Scene) {
        unregisterHandlers()
        registeredAtScene = scene
        scene.onRenderScene += onRenderSceneHook
    }

    open fun unregisterHandlers() {
        registeredAtScene?.let { it.onRenderScene -= onRenderSceneHook }
    }

    fun registerTriggerListener(trigger: RigidActor, listener: TriggerListener) {
        if (!trigger.isTrigger) {
            logW { "Given trigger actor is not a trigger (isTrigger == false)" }
        }
        triggerListeners.getOrPut(trigger) { TriggerListenerContext() }.listeners += listener
    }

    fun unregisterTriggerListener(listener: TriggerListener) {
        triggerListeners.values.forEach { it.listeners -= listener }
        triggerListeners.keys.removeAll { k -> triggerListeners[k]?.listeners?.isEmpty() ?: false }
    }

    fun registerContactListener(listener: ContactListener) {
        contactListeners += listener
    }

    fun unregisterContactListener(listener: ContactListener) {
        contactListeners -= listener
    }

    protected fun fireOnTouchFound(a: RigidActor, b: RigidActor, contactPoints: List<ContactPoint>?) {
        for (i in contactListeners.indices) {
            contactListeners[i].onTouchFound(a, b, contactPoints)
        }
    }

    protected fun fireOnTouchLost(a: RigidActor, b: RigidActor) {
        for (i in contactListeners.indices) {
            contactListeners[i].onTouchLost(a, b)
        }
    }

    override fun release() {
        if (isStepInProgress) {
            fetchAsyncStepResults()
        }
        unregisterHandlers()
        clear(true)
        super.release()
    }

    open fun addActor(actor: RigidActor) {
        mutActors += actor
    }

    open fun removeActor(actor: RigidActor) {
        mutActors -= actor
    }

    open fun addArticulation(articulation: Articulation) {
        mutArticulations += articulation
    }

    open fun removeArticulation(articulation: Articulation) {
        mutArticulations -= articulation
    }

    abstract fun raycast(ray: RayF, maxDistance: Float, result: HitResult): Boolean
    abstract fun sweepTest(testGeometry: CollisionGeometry, geometryPose: Mat4f, testDirection: Vec3f, distance: Float, result: HitResult): Boolean

    fun wakeUpAll() {
        actors.forEach {
            if (it is RigidDynamic) {
                it.wakeUp()
            }
        }
        articulations.forEach {
            it.wakeUp()
        }
    }

    fun clear(releaseActors: Boolean = true) {
        val removeActors = mutableListOf<RigidActor>().apply { this += actors }
        for (i in removeActors.lastIndex downTo 0) {
            removeActor(removeActors[i])
            if (releaseActors) {
                removeActors[i].release()
            }
        }
        val removeArticulations = mutableListOf<Articulation>().apply { this += articulations }
        for (i in removeArticulations.lastIndex downTo 0) {
            removeArticulation(removeArticulations[i])
            if (releaseActors) {
                removeArticulations[i].release()
            }
        }
        triggerListeners.clear()
    }

    private fun isContinueStep(physicsTime: Double, physicsTimeDesired: Double, step: Float): Boolean {
        return physicsTimeDesired - physicsTime > step * 0.5
    }

    fun singleStepSync(timeStep: Float) {
        singleStepAsync(timeStep)
        fetchAsyncStepResults()
    }

    open fun singleStepAsync(timeStep: Float) {
        if (isStepInProgress) {
            throw IllegalStateException("Previous simulation step not yet finished")
        }
        onAdvancePhysics(timeStep)
        isStepInProgress = true
        prevStepTime = timeStep
    }

    open fun fetchAsyncStepResults() {
        isStepInProgress = false
        onPhysicsUpdate(prevStepTime)
    }

    protected open fun onAdvancePhysics(timeStep: Float) {
        for (i in onAdvancePhysics.indices) {
            onAdvancePhysics[i](timeStep)
        }
    }

    protected open fun onPhysicsUpdate(timeStep: Float) {
        for (i in mutActors.indices) {
            mutActors[i].onPhysicsUpdate(timeStep)
        }
        for (i in mutArticulations.indices) {
            mutArticulations[i].onPhysicsUpdate(timeStep)
        }
        for (i in onPhysicsUpdate.indices) {
            onPhysicsUpdate[i](timeStep)
        }
    }

    /**
     * Adds a static plane with y-axis as surface normal (i.e. xz-plane) at y = 0.
     */
    fun addDefaultGroundPlane(): RigidStatic {
        val groundPlane = RigidStatic()
        val shape = Shape(PlaneGeometry(), Material(0.5f, 0.5f, 0.2f))
        groundPlane.attachShape(shape)
        groundPlane.setRotation(0f.deg, 0f.deg, 90f.deg)
        addActor(groundPlane)
        return groundPlane
    }

    protected class TriggerListenerContext {
        val listeners = mutableListOf<TriggerListener>()
        val actorEnterCounts = mutableMapOf<RigidActor, Int>()
    }
}