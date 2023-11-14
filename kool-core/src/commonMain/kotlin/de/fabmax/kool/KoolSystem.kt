package de.fabmax.kool

object KoolSystem {
    private var initConfig: KoolConfig? = null
    private var defaultContext: KoolContext? = null

    var isInitialized = false
        private set

    val isContextCreated: Boolean
        get() = defaultContext != null

    val config: KoolConfig
        get() = initConfig ?: throw IllegalStateException("KoolSetup is not yet initialized. Call initialize(config) before accessing KoolSetup.config")

    val isJavascript: Boolean
        get() = requireContext().isJavascript
    val isJvm: Boolean
        get() = requireContext().isJvm

    fun initialize(config: KoolConfig) {
        if (isInitialized && config != initConfig) {
            throw IllegalStateException("KoolSetup is already initialized")
        }
        initConfig = config
        isInitialized = true
    }

    internal fun onContextCreated(ctx: KoolContext) {
        defaultContext = ctx
    }

    fun requireContext(): KoolContext {
        return defaultContext ?: throw IllegalStateException("KoolContext was not yet created")
    }

    fun getContextOrNull(): KoolContext? {
        return defaultContext
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class KoolConfig {
    /**
     * Base path used by [Assets] to look for assets to be loaded (textures, models, etc.).
     */
    val assetPath: String
}