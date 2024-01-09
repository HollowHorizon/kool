package de.fabmax.kool.pipeline.backend.vk.pipeline

import de.fabmax.kool.pipeline.*
import de.fabmax.kool.pipeline.backend.vk.VkSystem
import de.fabmax.kool.pipeline.backend.vk.callocVkDescriptorSetAllocateInfo
import de.fabmax.kool.pipeline.backend.vk.callocVkWriteDescriptorSetN
import de.fabmax.kool.pipeline.drawqueue.DrawCommand
import de.fabmax.kool.util.memStack
import org.lwjgl.vulkan.VK10.*

class DescriptorSet(val graphicsPipeline: GraphicsPipeline) {
    private val descriptorSets = mutableListOf<Long>()
    private val objects = Array<MutableList<DescriptorObject>>(graphicsPipeline.nImages) { mutableListOf() }

    var allValid = true
        private set

    private val isDescriptorSetUpdateRequired = BooleanArray(graphicsPipeline.nImages) { false }

    init {
        createDescriptorSets()
    }

    fun getDescriptorSet(imageIdx: Int) = descriptorSets[imageIdx]

    private fun createDescriptorSets() {
        // check if graphicsPipeline has a valid descriptorPool, if not the pipeline does not use any descriptors
        // i.e. the shader does not have any uniform inputs
        if (graphicsPipeline.descriptorPool != 0L) {
            memStack {
                val layouts = mallocLong(graphicsPipeline.nImages)
                for (i in 0 until graphicsPipeline.nImages) {
                    layouts.put(i, graphicsPipeline.descriptorSetLayout)
                }
                val allocInfo = callocVkDescriptorSetAllocateInfo {
                    sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    descriptorPool(graphicsPipeline.descriptorPool)
                    pSetLayouts(layouts)
                }

                val sets = mallocLong(graphicsPipeline.nImages)
                check(vkAllocateDescriptorSets(graphicsPipeline.sys.device.vkDevice, allocInfo, sets) == VK_SUCCESS)
                for (i in 0 until graphicsPipeline.nImages) {
                    descriptorSets += sets[i]
                }
            }
        } else {
            for (i in 0 until graphicsPipeline.nImages) {
                descriptorSets += 0L
            }
        }
    }

    fun clearDescriptorObjects() {
        objects.forEach {
            it.forEach { desc -> desc.destroy(graphicsPipeline) }
            it.clear()
        }
    }

    fun createDescriptorObjects(pipeline: Pipeline) {
        pipeline.bindGroupLayout.bindings.forEachIndexed { idx, desc ->
            addDescriptor {
                when (desc.type) {
                    BindingType.UNIFORM_BUFFER -> UboDescriptor(idx, graphicsPipeline, desc as UniformBufferBinding)
                    BindingType.TEXTURE_1D -> SamplerDescriptor(idx, desc as Texture1dBinding)
                    BindingType.TEXTURE_2D -> SamplerDescriptor(idx, desc as Texture2dBinding)
                    BindingType.TEXTURE_3D -> SamplerDescriptor(idx, desc as Texture3dBinding)
                    BindingType.TEXTURE_CUBE -> SamplerDescriptor(idx, desc as TextureCubeBinding)
                    BindingType.STORAGE_TEXTURE_1D -> TODO()
                    BindingType.STORAGE_TEXTURE_2D -> TODO()
                    BindingType.STORAGE_TEXTURE_3D -> TODO()
                }
            }
        }
    }

    private fun addDescriptor(block: () -> DescriptorObject): Int {
        for (i in 0 until graphicsPipeline.nImages) {
            objects[i].add(block())
        }
        return objects[0].size - 1
    }

    fun updateDescriptorSets(imageIdx: Int) {
        if (isDescriptorSetUpdateRequired[imageIdx]) {
            clearUpdateRequired(imageIdx)

            val descriptors = graphicsPipeline.pipeline.bindGroupLayout.bindings
            memStack {
                val descriptorWrite = callocVkWriteDescriptorSetN(descriptors.size) {
                    for (descIdx in descriptors.indices) {
                        val descObj = objects[imageIdx][descIdx]
                        descObj.setDescriptorSet(this@memStack, this[descIdx], descriptorSets[imageIdx])
                    }
                }
                vkUpdateDescriptorSets(graphicsPipeline.sys.device.vkDevice, descriptorWrite, null)
            }
        }
    }

    private fun clearUpdateRequired(imageIdx: Int) {
        isDescriptorSetUpdateRequired[imageIdx] = false
        objects[imageIdx].forEach { it.isDescriptorSetUpdateRequired = false }
    }

    fun updateDescriptors(cmd: DrawCommand, imageIndex: Int, sys: VkSystem): Boolean {
        val descs = objects[imageIndex]
        allValid = true
        var updateRequired = false
        for (i in descs.indices) {
            val desc = descs[i]
            desc.update(cmd, sys)
            allValid = allValid && desc.isValid
            updateRequired = updateRequired || desc.isDescriptorSetUpdateRequired
        }
        isDescriptorSetUpdateRequired[imageIndex] = updateRequired
        return allValid
    }
}
