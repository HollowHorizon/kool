package de.fabmax.kool.editor

import de.fabmax.kool.editor.api.EditorInfo
import de.fabmax.kool.math.*
import de.fabmax.kool.util.Color
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

object BehaviorReflection {

    fun getEditableProperties(behaviorClass: KClass<*>): List<BehaviorProperty> {
        return behaviorClass.declaredMemberProperties
            .filter {
                it is KMutableProperty<*>
                        && it.visibility == KVisibility.PUBLIC
                        && !it.returnType.isMarkedNullable
                        && it.annotations.none { anno -> anno is EditorInfo && anno.hideInEditor }
                        && editableTypes.contains(it.setter.parameters[1].type.classifier)
            }
            .map {
                val propertyType = (it as KMutableProperty<*>).setter.parameters[1].type

                val info = it.annotations.filterIsInstance<EditorInfo>().firstOrNull()
                val label = if (info != null && info.label.isNotBlank()) info.label else it.name
                val min = info?.min ?: Double.NEGATIVE_INFINITY
                val max = info?.max ?: Double.POSITIVE_INFINITY
                BehaviorProperty(it.name, propertyType, label, min, max)
            }
    }

    private val editableTypes = setOf<KClass<*>>(
        Int::class,
        Vec2i::class,
        Vec3i::class,
        Vec4i::class,

        Float::class,
        Vec2f::class,
        Vec3f::class,
        Vec4f::class,

        Double::class,
        Vec2d::class,
        Vec3d::class,
        Vec4d::class,

        Color::class,
        Mat4d::class,
        String::class
    )
}