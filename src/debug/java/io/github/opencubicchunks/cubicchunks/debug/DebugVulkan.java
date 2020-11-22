package io.github.opencubicchunks.cubicchunks.debug;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.system.MemoryUtil.memFree;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_DEBUG_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_ERROR_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_DEBUG_REPORT_WARNING_BIT_EXT;
import static org.lwjgl.vulkan.EXTDebugReport.VK_EXT_DEBUG_REPORT_EXTENSION_NAME;
import static org.lwjgl.vulkan.EXTDebugReport.VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_LINE_WIDTH;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_EXTENSION_NOT_PRESENT;
import static org.lwjgl.vulkan.VK10.VK_ERROR_INCOMPATIBLE_DRIVER;
import static org.lwjgl.vulkan.VK10.VK_FALSE;
import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT32;
import static org.lwjgl.vulkan.VK10.VK_LOGIC_OP_NO_OP;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindIndexBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdDrawIndexed;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdSetViewport;
import static org.lwjgl.vulkan.VK10.vkCreateBuffer;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkEnumerateDeviceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceExtensionProperties;
import static org.lwjgl.vulkan.VK10.vkEnumerateInstanceLayerProperties;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugReportCallbackCreateInfoEXT;
import org.lwjgl.vulkan.VkDebugReportCallbackEXT;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class DebugVulkan {

    private static final long UINT32_MAX = Integer.MIN_VALUE;
    private static final long UINT64_MAX = Long.MIN_VALUE;

    private static final int MAX_FRAMES_IN_FLIGHT = 2;

    private static final ByteBuffer EXT_DEBUG_REPORT = org.lwjgl.system.MemoryUtil.memASCII(VK_EXT_DEBUG_REPORT_EXTENSION_NAME);

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private long window;

    private VkInstance instance;
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;

    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    private final boolean enableValidationLayers;
    private final String[] validationLayers = { "VK_LAYER_KHRONOS_validation" };
    private final String[] deviceExtensions = { VK_KHR_SWAPCHAIN_EXTENSION_NAME };

    private VkLayerProperties.Buffer availableLayers;
    private VkExtensionProperties.Buffer availableExtensions;

    private final PointerBuffer extensions = memAllocPointer(64);

    private int enabledLayerCount;
    private int enabledExtensionCount;

    private long surface;

    private long swapChain;
    private long[] swapChainImages;
    private int swapChainImageFormat;
    private VkExtent2D swapChainExtent;

    private long[] swapChainImageViews;

    private LongBuffer swapChainFramebuffers;

    private long graphicsPipeline;
    private long renderPass;
    private long descriptorSetLayout;
    private long pipelineLayout;

    private int currentFrame = 0;
    private int timerFrameCounter = 0;

    private long timerNow;
    private long timerNext;

    private long commandPool;
    private ArrayList<VkCommandBuffer> commandBuffers;

    private long[] imageAvailableSemaphores;
    private long[] renderFinishedSemaphores;
    private long[] inFlightFences;
    private long[] imagesInFlight;

    private boolean framebufferResized = false;

    private long vertexBuffer;
    private long vertexBufferMemory;
    private long indexBuffer;
    private long indexBufferMemory;

    private long[] uniformBuffers;
    private long[] uniformBuffersMemory;

    private long descriptorPool;
    private LongBuffer descriptorSets;

    private VkViewport.Buffer viewport;

    //FACE VERTICES
//    private final Vertex[] vertices = {
//            new Vertex(new Vector3f(-0.5f, -0.5f, 0.0f), new Vector3f(.0f, 1.0f, 1.0f)),
//            new Vertex(new Vector3f(0.5f, -0.5f, 0.0f), new Vector3f(1.0f, 0.0f, 1.0f)),
//            new Vertex(new Vector3f(0.5f, 0.5f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f)),
//            new Vertex(new Vector3f(-0.5f, 0.5f, 0.0f), new Vector3f(1.0f, 1.0f, 1.0f))
//    };

    private final Vertex[] vertices = {
        new Vertex(new Vector3f(-1, -1, -1), 200, 50, 150, 128),
        new Vertex(new Vector3f(1, -1, -1), 200, 50, 150, 128),
        new Vertex(new Vector3f(1, 1, -1), 200, 50, 150, 128),
        new Vertex(new Vector3f(-1, 1, -1), 200, 50, 150, 128),
        new Vertex(new Vector3f(-1, -1, 1), 200, 50, 150, 128),
        new Vertex(new Vector3f(1, -1, 1), 200, 50, 150, 128),
        new Vertex(new Vector3f(1, 1, 1), 200, 50, 150, 128),
        new Vertex(new Vector3f(-1, 1, 1), 200, 50, 150, 128),

        new Vertex(new Vector3f(1, -1, -1), 50, 190, 60, 128),
        new Vertex(new Vector3f(3, -1, -1), 50, 190, 60, 128),
        new Vertex(new Vector3f(3, 1, -1), 50, 190, 60, 128),
        new Vertex(new Vector3f(1, 1, -1), 50, 190, 60, 128),
        new Vertex(new Vector3f(1, -1, 1), 50, 190, 60, 128),
        new Vertex(new Vector3f(3, -1, 1), 50, 190, 60, 128),
        new Vertex(new Vector3f(3, 1, 1), 50, 190, 60, 128),
        new Vertex(new Vector3f(1, 1, 1), 50, 190, 60, 128)
    };

    //FACE INDICES
    //private final int[] indices = { 0, 1, 2, 2, 3, 0 };
    //CUBE INDICES
    @SuppressWarnings("PointlessArithmeticExpression") private final int[] indices = {
        0, 1, 3, 3, 1, 2,
        1, 5, 2, 2, 5, 6,
        5, 4, 6, 6, 4, 7,
        4, 0, 7, 7, 0, 3,
        3, 2, 7, 7, 2, 6,
        4, 5, 0, 0, 5, 1,

        0 + 8, 1 + 8, 3 + 8, 3 + 8, 1 + 8, 2 + 8,
        1 + 8, 5 + 8, 2 + 8, 2 + 8, 5 + 8, 6 + 8,
        5 + 8, 4 + 8, 6 + 8, 6 + 8, 4 + 8, 7 + 8,
        4 + 8, 0 + 8, 7 + 8, 7 + 8, 0 + 8, 3 + 8,
        3 + 8, 2 + 8, 7 + 8, 7 + 8, 2 + 8, 6 + 8,
        4 + 8, 5 + 8, 0 + 8, 0 + 8, 5 + 8, 1 + 8
    };

    //buffers for handle output-params
    private final IntBuffer ip = memAllocInt(1);
    private final LongBuffer lp = memAllocLong(1);
    private final PointerBuffer pp = memAllocPointer(1);

    private final VkDebugReportCallbackEXT dbgFunc = VkDebugReportCallbackEXT.create(
        (flags, objectType, object, location, messageCode, pLayerPrefix, pMessage, pUserData) -> {
            new Exception().printStackTrace();
            String type;
            if ((flags & VK_DEBUG_REPORT_INFORMATION_BIT_EXT) != 0) {
                type = "INFORMATION";
            } else if ((flags & VK_DEBUG_REPORT_WARNING_BIT_EXT) != 0) {
                type = "WARNING";
            } else if ((flags & VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT) != 0) {
                type = "PERFORMANCE WARNING";
            } else if ((flags & VK_DEBUG_REPORT_ERROR_BIT_EXT) != 0) {
                type = "ERROR";
            } else if ((flags & VK_DEBUG_REPORT_DEBUG_BIT_EXT) != 0) {
                type = "DEBUG";
            } else {
                type = "UNKNOWN";
            }

            System.err.format(
                "%s: [%s] Code %d : %s\n",
                type, memASCII(pLayerPrefix), messageCode, VkDebugReportCallbackEXT.getString(pMessage)
            );

            /*
             * false indicates that layer should not bail-out of an
             * API call that had validation failures. This may mean that the
             * app dies inside the driver due to invalid parameter(s).
             * That's what would happen without validation layers, so we'll
             * keep that behavior here.
             */
            return VK_FALSE;
        }
    );

    private final Matrix4f inverseMatrix = new Matrix4f();

    public DebugVulkan() {
        this.enableValidationLayers = System.getProperty("cubicchunks.debug.validation", "false").equalsIgnoreCase("true");
    }

    public void run() {
        initWindow();
        initVulkan();
        mainLoop();
        cleanup();
    }

    void initWindow() {
        glfwInit();

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan Test", NULL, NULL);
        glfwSetFramebufferSizeCallback(window, (win, width, height) -> this.framebufferResized = true);
    }

    void initVulkan() {
        try (MemoryStack stack = stackPush()) {
            createInstance(stack);
            createSurface(stack);
            pickPhysicalDevice(stack);
            createLogicalDevice(stack);
            createSwapChain(stack);
            createImageViews(stack);
            createRenderPass(stack);
            createDescriptorSetLayout(stack);
            createGraphicsPipeline(stack);
            createFrameBuffers(stack);
            createCommandPool(stack);
            createVertexBuffer(stack);
            createIndexBuffer(stack);
            createUniformBuffers(stack);
            createDescriptorPool(stack);
            createDescriptorSets(stack);
            createCommandBuffers(stack);
            createSyncObjects(stack);
        }
    }

    private void createInstance(MemoryStack stack) {
        PointerBuffer requiredLayers = null;
        if (this.enableValidationLayers) {
            checkError(vkEnumerateInstanceLayerProperties(ip, null));

            if (ip.get(0) > 0) {
                availableLayers = VkLayerProperties.mallocStack(ip.get(0), stack);
                checkError(vkEnumerateInstanceLayerProperties(ip, availableLayers));

                requiredLayers = checkLayers(stack, availableLayers, validationLayers);

                if (requiredLayers == null) { // use alternative (deprecated) set of validation layers
                    requiredLayers = checkLayers(
                        stack, availableLayers,
                        "VK_LAYER_LUNARG_standard_validation"/*,
                        "VK_LAYER_LUNARG_assistant_layer"*/
                    );
                }
                if (requiredLayers == null) { // use alternative (deprecated) set of validation layers
                    requiredLayers = checkLayers(
                        stack, availableLayers,
                        "VK_LAYER_GOOGLE_threading",
                        "VK_LAYER_LUNARG_parameter_validation",
                        "VK_LAYER_LUNARG_object_tracker",
                        "VK_LAYER_LUNARG_core_validation",
                        "VK_LAYER_GOOGLE_unique_objects"/*,
                        "VK_LAYER_LUNARG_assistant_layer"*/
                    );
                }
            }

            if (requiredLayers == null) {
                throw new IllegalStateException("vkEnumerateInstanceLayerProperties failed to find required validation layer.");
            }
        }

        VkApplicationInfo appInfo = VkApplicationInfo.mallocStack();
        appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            .pApplicationName(stack.UTF8("Hello Triangle"))
            .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
            .pEngineName(stack.UTF8("No Engine"))
            .engineVersion(VK_MAKE_VERSION(1, 0, 0))
            .apiVersion(VK_API_VERSION_1_0)
            .pNext(NULL);

        PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions();
        VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.mallocStack(stack);
        createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            .pApplicationInfo(appInfo)
            .ppEnabledExtensionNames(requiredExtensions)
            .ppEnabledLayerNames(requiredLayers)
            .flags(0);
        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo;
        if (enableValidationLayers) {
            dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.mallocStack(stack)
                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(NULL)
                .flags(VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT)
                .pfnCallback(dbgFunc)
                .pUserData(NULL);

            createInfo.pNext(dbgCreateInfo.address());
        } else {
            createInfo.pNext(NULL);
        }

        int err = vkCreateInstance(createInfo, null, pp);
        instance = new VkInstance(pp.get(0), createInfo);

        if (err == VK_ERROR_INCOMPATIBLE_DRIVER) {
            throw new IllegalStateException("cannot find a compatible Vulkan icd");
        } else if (err == VK_ERROR_EXTENSION_NOT_PRESENT) {
            throw new IllegalStateException("Cannot find a specified extension library");
        } else if (err != VK_SUCCESS) {
            throw new IllegalStateException("failed to create vkinstance");
        }

        //        createInfo.free();
        //        appInfo.free();

        err = vkEnumerateInstanceExtensionProperties((String) null, ip, null);

        if (ip.get(0) != 0) {
            VkExtensionProperties.Buffer instanceExtensions = VkExtensionProperties.mallocStack(ip.get(0), stack);
            err = vkEnumerateInstanceExtensionProperties((String) null, ip, instanceExtensions);

            for (int i = 0; i < ip.get(0); i++) {
                instanceExtensions.position(i);
                if (VK_EXT_DEBUG_REPORT_EXTENSION_NAME.equals(instanceExtensions.extensionNameString())) {
                    if (enableValidationLayers) {
                        extensions.put(enabledExtensionCount++, EXT_DEBUG_REPORT);
                    }
                }
            }
            //instance_extensions.free();
        }
    }

    private void createSurface(MemoryStack stack) {
        if (glfwCreateWindowSurface(instance, window, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create window surface!");
        }
        surface = lp.get(0);
    }

    private void pickPhysicalDevice(MemoryStack stack) {
        checkError(vkEnumeratePhysicalDevices(instance, ip, null));
        int deviceCount = ip.get(0);

        if (deviceCount == 0) {
            throw new RuntimeException("failed to find GPU with Vulkan support!");
        }
        PointerBuffer physicalDevices = stack.mallocPointer(ip.get(0));
        checkError(vkEnumeratePhysicalDevices(instance, ip, physicalDevices));

        //TODO: get GPU** based on some GPU parameters
        physicalDevice = new VkPhysicalDevice(physicalDevices.get(0), instance);
        this.isDeviceSuitable(physicalDevice, stack);
    }

    private void createLogicalDevice(MemoryStack stack) {
        QueueFamilyIndices queueFamilyindices = findQueueFamilies(physicalDevice);


        //Create queue info
        int[] uniqueQueueFamilies;
        if (queueFamilyindices.graphicsFamily.equals(queueFamilyindices.presentFamily)) {
            uniqueQueueFamilies = new int[] { queueFamilyindices.graphicsFamily };
        } else {
            uniqueQueueFamilies = new int[] { queueFamilyindices.graphicsFamily, queueFamilyindices.presentFamily };
        }
        VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.mallocStack(uniqueQueueFamilies.length);

        for (int i = 0; i < uniqueQueueFamilies.length; i++) {
            queueCreateInfos.get(i).sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .pNext(NULL)
                .flags(0)
                .queueFamilyIndex(uniqueQueueFamilies[i])
                .pQueuePriorities(stack.floats(1.0f));
        }

        //This is left for later, when we use more device features
        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);

        VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.mallocStack(stack);
        createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            .pNext(NULL)
            .flags(0)
            .pQueueCreateInfos(queueCreateInfos)
            .ppEnabledExtensionNames(checkExtensions(stack, availableExtensions, deviceExtensions))
            .pEnabledFeatures(null); //TODO: replace with devicefeatures when they are used.

        if (this.enableValidationLayers) {
            createInfo.ppEnabledLayerNames(checkLayers(stack, availableLayers, validationLayers));
        }

        //Create logical device
        int err = vkCreateDevice(physicalDevice, createInfo, null, pp);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("failed to create logical device! With error " + err);
        }
        this.device = new VkDevice(pp.get(0), physicalDevice, createInfo);

        //Get graphics queue for device
        vkGetDeviceQueue(device, queueFamilyindices.graphicsFamily, 0, pp);
        this.graphicsQueue = new VkQueue(pp.get(0), device);

        vkGetDeviceQueue(device, queueFamilyindices.presentFamily, 0, pp);
        this.presentQueue = new VkQueue(pp.get(0), device);
    }

    void createSwapChain(MemoryStack stack) {
        SwapChainSupportDetails swapChainSupport = querySwapChainSupport(physicalDevice, stack);

        VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
        int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
        VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities, stack);

        //Request 1 more than min, so we dont have to wait for the driver to complete internal ops before we acquire another image
        int imageCount = swapChainSupport.capabilities.minImageCount() + 1;
        if (swapChainSupport.capabilities.maxImageCount() > 0 && imageCount > swapChainSupport.capabilities.maxImageCount()) {
            imageCount = swapChainSupport.capabilities.maxImageCount();
        }

        VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);
        createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
            .surface(surface)
            .minImageCount(imageCount)
            .imageFormat(surfaceFormat.format())
            .imageColorSpace(surfaceFormat.colorSpace())
            .imageExtent(extent)
            .imageArrayLayers(1) //Specifies the amount of layers each image consists of. This is always 1 unless doing stereoscopic 3d
            .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT); //Specifies what kind of operations the swapchain will be used for. If you want
        // to render to a seperate image first, for post processing, you could use VK_IMAGE_USAGE_TRANSFER_DST_BIT, then transfer data to this swapchain

        QueueFamilyIndices queueFamilyindices = findQueueFamilies(physicalDevice);
        IntBuffer queueFamilyIndices = stack.mallocInt(2);
        queueFamilyIndices.put(0, queueFamilyindices.graphicsFamily);
        queueFamilyIndices.put(0, queueFamilyindices.presentFamily);

        if (!queueFamilyindices.graphicsFamily.equals(queueFamilyindices.presentFamily)) {
            createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                .pQueueFamilyIndices(queueFamilyIndices);
        } else {
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null);
        }

        //
        createInfo.preTransform(swapChainSupport.capabilities.currentTransform()) //We want no pre-transform, so we specify this
            .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) //This is to allow for blending with other windows in the window system, we pick not to
            .presentMode(presentMode)
            .clipped(true) //This will not care about pixels that are obscured by other windows in the window system
            .oldSwapchain(VK_NULL_HANDLE); //For now we will not use this. It's for specifying a the old swapchain, eg: when the window is resized

        if (vkCreateSwapchainKHR(device, createInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create swap chain");
        }
        swapChain = lp.get(0);

        //Get Swap Chain images
        checkError(vkGetSwapchainImagesKHR(device, swapChain, ip, null));
        int swapChainImageCount = ip.get(0);

        LongBuffer swapChainImagesBuffer = stack.mallocLong(swapChainImageCount);
        vkGetSwapchainImagesKHR(device, swapChain, ip, swapChainImagesBuffer);
        swapChainImages = new long[swapChainImageCount];

        for (int i = 0; i < swapChainImageCount; i++) {
            swapChainImages[i] = swapChainImagesBuffer.get(i);
        }

        swapChainImageFormat = surfaceFormat.format();
        swapChainExtent = extent;
    }

    void createImageViews(MemoryStack stack) {
        swapChainImageViews = new long[swapChainImages.length];

        for (int i = 0; i < swapChainImages.length; i++) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(swapChainImages[i])
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(swapChainImageFormat)
                .components(it -> it
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY))
                .subresourceRange(it -> it
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1));

            if (vkCreateImageView(device, createInfo, null, lp) != VK_SUCCESS) {
                throw new RuntimeException("failed to create image views!");
            }
            swapChainImageViews[i] = lp.get(0);
        }
    }

    void createRenderPass(MemoryStack stack) {
        VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.callocStack(1, stack)
            .format(swapChainImageFormat)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR) //This will reset the framebuffer to black before drawing a new frame
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE) //This is relatedto the stencil buffer, which we dont use currently
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.callocStack(1, stack)
            .attachment(0) //Index to reference or the attachments
            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack)
            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            .colorAttachmentCount(1)
            .pColorAttachments(
                colorAttachmentRef); //The index of the attachment in this array is directly referenced from the fragment shader with the layout(location = 0) out vec4 outColor directive!

        VkSubpassDependency.Buffer dependency = VkSubpassDependency.callocStack(1, stack)
            .srcSubpass(VK_SUBPASS_EXTERNAL)
            .dstSubpass(0)
            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            .pAttachments(colorAttachment)
            .pSubpasses(subpass)
            .pDependencies(dependency);

        if (vkCreateRenderPass(device, renderPassInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create render pass");
        }
        renderPass = lp.get(0);
    }

    void createDescriptorSetLayout(MemoryStack stack) {
        VkDescriptorSetLayoutBinding.Buffer uboLayoutBinding = VkDescriptorSetLayoutBinding.callocStack(1, stack)
            .binding(0)
            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .descriptorCount(1) //This can be used to specify a single transformation for each thing
            .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
            .pImmutableSamplers(null);

        VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            .pBindings(uboLayoutBinding);

        if (vkCreateDescriptorSetLayout(device, layoutInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create descriptor set layout!");
        }
        descriptorSetLayout = lp.get(0);
    }

    void createGraphicsPipeline(MemoryStack stack) {
        byte[] vertShaderCode = readFile("shaders/vert.spv");
        byte[] fragShaderCode = readFile("shaders/frag.spv");

        long vertShaderModule = createShaderModule(vertShaderCode, stack);
        long fragShaderModule = createShaderModule(fragShaderCode, stack);

        ByteBuffer main = stack.UTF8("main");

        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.callocStack(2, stack);
        shaderStages.get(0)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .stage(VK_SHADER_STAGE_VERTEX_BIT)
            .module(vertShaderModule)
            .pName(main);

        shaderStages.get(1)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
            .module(fragShaderModule)
            .pName(main);

        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
            .pVertexBindingDescriptions(Vertex.getBindingDescription())
            .pVertexAttributeDescriptions(Vertex.getAttributeDescriptions());

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
            .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            .primitiveRestartEnable(false);

        viewport = VkViewport.callocStack(1, stack)
            .x(0.f)
            .y(0.f)
            .width((float) swapChainExtent.width()) //Using swapchain w/h because of course they may differ to window w/h
            .height((float) swapChainExtent.height())
            .minDepth(0.f)
            .maxDepth(1.f);

        VkRect2D.Buffer scissor = VkRect2D.callocStack(1, stack)
            .extent(it -> it
                .width(swapChainExtent.width())
                .height(swapChainExtent.height()))
            .offset(it -> it
                .x(0)
                .y(0));

        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
            .viewportCount(1)
            .pViewports(viewport)
            .scissorCount(1)
            .pScissors(scissor);

        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
            .depthClampEnable(false) //if this is true, fragments beyond the near and far planes are clamped to within them.
            .rasterizerDiscardEnable(false) //if this is true, geometry never passes through this stage, disabling output to framebuffer
            .polygonMode(VK_POLYGON_MODE_FILL)
            .lineWidth(1.f)
            .cullMode(VK_CULL_MODE_NONE) // <-- backface culling
            .frontFace(VK_FRONT_FACE_CLOCKWISE)
            .depthBiasEnable(false);

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
            .sampleShadingEnable(false)
            .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

        /* If using depth &/ stencil buffer then you need to configure the depth and stencil tests using VkPipelineDepthStencilStateCreateInfo*/

        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.callocStack(1, stack)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
            .blendEnable(true)
            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .colorBlendOp(VK_BLEND_OP_ADD)
            .srcAlphaBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .alphaBlendOp(VK_BLEND_OP_ADD);

        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
            .logicOpEnable(false)
            .logicOp(VK_LOGIC_OP_NO_OP) // find good operation to use here, needs some testing https://www.khronos.org/registry/vulkan/specs/1.2-extensions/man/html/VkLogicOp.html
            .pAttachments(colorBlendAttachment)
            .blendConstants(0, 0.0f) // Optional
            .blendConstants(1, 0.0f) // Optional
            .blendConstants(2, 0.0f) // Optional
            .blendConstants(3, 0.0f); // Optional

        IntBuffer dynamicStates = stack.mallocInt(2);
        dynamicStates.put(0, VK_DYNAMIC_STATE_VIEWPORT);
        dynamicStates.put(0, VK_DYNAMIC_STATE_LINE_WIDTH);


        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
            .pDynamicStates(dynamicStates);

        LongBuffer lp2 = stack.mallocLong(1);
        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
            .pSetLayouts(lp2.put(0, descriptorSetLayout))
            .pPushConstantRanges(null)
            .pNext(NULL);

        if (vkCreatePipelineLayout(device, pipelineLayoutInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create pipeline layout");
        }
        pipelineLayout = lp.get(0);

        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.callocStack(1, stack)
            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
            .pStages(shaderStages)
            .pVertexInputState(vertexInputInfo)
            .pInputAssemblyState(inputAssembly)
            .pViewportState(viewportState)
            .pRasterizationState(rasterizer)
            .pMultisampleState(multisampling)
            .pDepthStencilState(null)
            .pColorBlendState(colorBlending)
            .pDynamicState(dynamicState)
            .layout(pipelineLayout)
            .renderPass(renderPass)
            .subpass(0)
            .basePipelineHandle(VK_NULL_HANDLE) //This is for deriving a pipeline from an already existing, similar one
            .basePipelineIndex(-1);

        if (vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create graphics pipeline");
        }
        graphicsPipeline = lp.get(0);

        vkDestroyShaderModule(device, fragShaderModule, null);
        vkDestroyShaderModule(device, vertShaderModule, null);
    }

    void createFrameBuffers(MemoryStack stack) {
        swapChainFramebuffers = memAllocLong(swapChainImageViews.length);

        for (int i = 0; i < swapChainImageViews.length; i++) {
            LongBuffer attachments = stack.longs(swapChainImageViews[i]);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(renderPass)
                .pAttachments(attachments)
                .width(swapChainExtent.width())
                .height(swapChainExtent.height())
                .layers(1); // we only have single layer images atm, so we use 1 here

            if (vkCreateFramebuffer(device, framebufferInfo, null, lp) != VK_SUCCESS) {
                throw new RuntimeException("failed to create framebuffer!");
            }
            swapChainFramebuffers.put(i, lp.get(0));
        }
    }

    void createCommandPool(MemoryStack stack) {
        QueueFamilyIndices queueFamilyIndices = findQueueFamilies(physicalDevice);

        VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
            .queueFamilyIndex(queueFamilyIndices.graphicsFamily)
            .flags(0);

        if (vkCreateCommandPool(device, poolInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create command pool!");
        }
        commandPool = lp.get(0);
    }

    void createVertexBuffer(MemoryStack stack) {
        LongBuffer lp2 = stack.mallocLong(1);
        long bufferSize = Vertex.getsize() * vertices.length;

        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
            lp, lp2, stack);
        long stagingBuffer = lp.get(0);
        long stagingBufferMemory = lp2.get(0);

        vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, pp);
        IntBuffer data = pp.getIntBuffer(0, ((int) bufferSize) >> 2);
        for (Vertex vertex : vertices) {
            data.put(vertex.getData());
        }
        data.flip();
        vkUnmapMemory(device, stagingBufferMemory);

        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
            lp, lp2, stack);
        vertexBuffer = lp.get(0);
        vertexBufferMemory = lp2.get(0);

        copyBuffer(stagingBuffer, vertexBuffer, bufferSize, stack);

        vkDestroyBuffer(device, stagingBuffer, null);
        vkFreeMemory(device, stagingBufferMemory, null);
    }

    void createIndexBuffer(MemoryStack stack) {
        LongBuffer lp2 = stack.mallocLong(1);
        long bufferSize = Integer.BYTES * indices.length;

        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
            lp, lp2, stack);
        long stagingBuffer = lp.get(0);
        long stagingBufferMemory = lp2.get(0);

        vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, pp);
        IntBuffer data = pp.getIntBuffer(0, ((int) bufferSize) >> 2);
        data.put(indices).flip();
        vkUnmapMemory(device, stagingBufferMemory);

        createBuffer(bufferSize, VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
            lp, lp2, stack);
        indexBuffer = lp.get(0);
        indexBufferMemory = lp2.get(0);

        copyBuffer(stagingBuffer, indexBuffer, bufferSize, stack);

        vkDestroyBuffer(device, stagingBuffer, null);
        vkFreeMemory(device, stagingBufferMemory, null);
    }

    void createBuffer(long size, int usage, long properties, LongBuffer vertexBuf, LongBuffer vertexBufMem, MemoryStack stack) {
        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            .size(size)
            .usage(usage)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        if (vkCreateBuffer(device, bufferInfo, null, vertexBuf) != VK_SUCCESS) {
            throw new RuntimeException("failed to create vertex buffer!");
        }

        VkMemoryRequirements memoryRequirements = VkMemoryRequirements.callocStack(stack);
        vkGetBufferMemoryRequirements(device, vertexBuf.get(0), memoryRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
            .allocationSize(memoryRequirements.size())
            .memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(), properties, stack));

        if (vkAllocateMemory(device, allocInfo, null, vertexBufMem) != VK_SUCCESS) {
            throw new RuntimeException("failed to allocate vertex buffer memory");
        }

        vkBindBufferMemory(device, vertexBuf.get(0), vertexBufMem.get(0), 0);
    }

    void copyBuffer(long srcBuffer, long dstBuffer, long size, MemoryStack stack) {
        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            .commandPool(commandPool)
            .commandBufferCount(1);

        vkAllocateCommandBuffers(device, allocInfo, pp);
        VkCommandBuffer commandBuffer = new VkCommandBuffer(pp.get(0), device);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        vkBeginCommandBuffer(commandBuffer, beginInfo);

        VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack)
            .srcOffset(0)
            .dstOffset(0)
            .size(size);
        vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);

        vkEndCommandBuffer(commandBuffer);

        VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.callocStack(1, stack)
            .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            .pCommandBuffers(pp.put(0, commandBuffer));

        vkQueueSubmit(graphicsQueue, submitInfo,
            VK_NULL_HANDLE); //Using a fence would allow you to schedule multiple transfers simultaneously, and wait for them all, instead of one at a time.
        vkQueueWaitIdle(graphicsQueue);

        vkFreeCommandBuffers(device, commandPool, commandBuffer);
    }

    void createUniformBuffers(MemoryStack stack) {
        int bufferSize = Float.BYTES * 16;

        uniformBuffers = new long[swapChainImages.length];
        uniformBuffersMemory = new long[swapChainImages.length];

        LongBuffer lp2 = stack.mallocLong(1);

        for (int i = 0; i < swapChainImages.length; i++) {
            createBuffer(bufferSize, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                lp, lp2, stack);
            uniformBuffers[i] = lp.get(0);
            uniformBuffersMemory[i] = lp2.get(0);
        }
    }

    void createDescriptorPool(MemoryStack stack) {
        VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.mallocStack(1, stack)
            .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .descriptorCount(swapChainImages.length);

        VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
            .pPoolSizes(poolSize)
            .maxSets(swapChainImages.length);

        if (vkCreateDescriptorPool(device, poolInfo, null, lp) != VK_SUCCESS) {
            throw new RuntimeException("failed to create descriptor pool!");
        }
        descriptorPool = lp.get(0);

    }

    void createDescriptorSets(MemoryStack stack) {
        LongBuffer layoutsBuf = stack.mallocLong(swapChainImages.length); //We need a descriptor set for each swap chain image
        for (int i = 0; i < swapChainImages.length; i++) {
            layoutsBuf.put(i, descriptorSetLayout);
        }

        VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
            .descriptorPool(descriptorPool)
            .pSetLayouts(layoutsBuf);

        descriptorSets = memAllocLong(swapChainImages.length);
        if (vkAllocateDescriptorSets(device, allocInfo, descriptorSets) != VK_SUCCESS) {
            throw new RuntimeException("failed to allocate descriptor sets!");
        }

        for (int i = 0; i < swapChainImages.length; i++) {
            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.callocStack(1, stack)
                .buffer(uniformBuffers[i])
                .offset(0)
                .range(Float.BYTES * 16);

            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.callocStack(1, stack)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(descriptorSets.get(i))
                .dstBinding(0)
                .dstArrayElement(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .pBufferInfo(bufferInfo);

            vkUpdateDescriptorSets(device, descriptorWrite, null);
        }

    }

    void createCommandBuffers(MemoryStack stack) {
        PointerBuffer pCommandBuffers = stack.mallocPointer(swapChainImageViews.length);
        commandBuffers = new ArrayList<>(swapChainImageViews.length);

        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            .commandPool(commandPool)
            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY) //primary commandbuffers can be submitted to a queue for execution, but cannot be called  from other command buffers
            .commandBufferCount(swapChainImageViews.length);

        if (vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
            throw new RuntimeException("failed to allocate command buffers!");
        }
        for (int i = 0; i < pCommandBuffers.limit(); i++) {
            commandBuffers.add(i, new VkCommandBuffer(pCommandBuffers.get(i), device));
        }

        for (int i = 0; i < commandBuffers.size(); i++) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT) //Currently none of the flags are applicable, this will change
                .pInheritanceInfo(null); //only relevant for secondary command buffers

            if (vkBeginCommandBuffer(commandBuffers.get(i), beginInfo) != VK_SUCCESS) {
                throw new RuntimeException("failed to begin recording command buffer!");
            }

            VkClearValue.Buffer clearColor = VkClearValue.mallocStack(1, stack);
            clearColor.color()
                .float32(0, 0.5f)
                .float32(1, 0.7f)
                .float32(2, 0.5f)
                .float32(3, 1.f);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(renderPass)
                .framebuffer(swapChainFramebuffers.get(i))
                .renderArea(ra -> ra
                    .offset(it -> it
                        .x(0)
                        .y(0))
                    .extent(swapChainExtent))
                .pClearValues(clearColor);

            vkCmdBeginRenderPass(commandBuffers.get(i), renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdSetViewport(commandBuffers.get(i), 0, viewport);
            vkCmdBindPipeline(commandBuffers.get(i), VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);
            vkCmdBindVertexBuffers(commandBuffers.get(i), 0, lp.put(0, vertexBuffer), stack.longs(0));
            vkCmdBindIndexBuffer(commandBuffers.get(i), indexBuffer, 0, VK_INDEX_TYPE_UINT32);
            vkCmdBindDescriptorSets(commandBuffers.get(i), VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, lp.put(0, descriptorSets.get(i)), null);
            vkCmdDrawIndexed(commandBuffers.get(i), indices.length, 1, 0, 0, 0);

            vkCmdEndRenderPass(commandBuffers.get(i));

            if (vkEndCommandBuffer(commandBuffers.get(i)) != VK_SUCCESS) {
                throw new RuntimeException("failed to record command buffer!");
            }
        }
    }

    void createSyncObjects(MemoryStack stack) {
        VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
        VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack)
            .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
            .flags(VK_FENCE_CREATE_SIGNALED_BIT);
        inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];
        imagesInFlight = new long[swapChainImages.length];

        imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
        renderFinishedSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            if (vkCreateSemaphore(device, semaphoreInfo, null, lp) != VK_SUCCESS) {
                throw new RuntimeException("failed to create imageAvailable semaphore");
            }
            imageAvailableSemaphores[i] = lp.get(0);

            if (vkCreateSemaphore(device, semaphoreInfo, null, lp) != VK_SUCCESS) {
                throw new RuntimeException("failed to create renderFinished semaphore!");
            }
            renderFinishedSemaphores[i] = lp.get(0);

            if (vkCreateFence(device, fenceInfo, null, lp) != VK_SUCCESS) {
                throw new RuntimeException("failed to create fence!");
            }
            inFlightFences[i] = lp.get(0);

            imagesInFlight[i] = VK_NULL_HANDLE;
        }
    }

    long createShaderModule(byte[] code, MemoryStack stack) {
        ByteBuffer pCode = stack.malloc(code.length).put(code);
        pCode.flip();

        VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.mallocStack(stack)
            .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
            .pNext(NULL)
            .flags(0)
            .pCode(pCode);

        checkError(vkCreateShaderModule(device, moduleCreateInfo, null, lp));

        return lp.get(0);
    }

    static byte[] readFile(String filename) {
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            return Files.readAllBytes(Paths.get(Objects.requireNonNull(classloader.getResource(filename)).toURI()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Resource file " + filename + " not there when expected");
        }
    }

    boolean isDeviceSuitable(VkPhysicalDevice deviceIn, MemoryStack stack) {
        QueueFamilyIndices queueFamilyIndices = findQueueFamilies(deviceIn);

        boolean extensionsSupported = checkDeviceExtensionSupport(deviceIn, stack);

        boolean swapChainAdequate = false;
        if (extensionsSupported) {
            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(deviceIn, stack);
            swapChainAdequate = !(swapChainSupport.formats.limit() == 0) && !(swapChainSupport.presentModes.limit() == 0);
        }

        return queueFamilyIndices.isComplete() && extensionsSupported && swapChainAdequate;
    }

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice deviceIn, MemoryStack stack) {
        checkError(vkEnumerateDeviceExtensionProperties(deviceIn, (CharSequence) null, ip, null));

        this.availableExtensions = VkExtensionProperties.mallocStack(ip.get(0), stack);
        vkEnumerateDeviceExtensionProperties(deviceIn, (CharSequence) null, ip, availableExtensions);
        return true;
    }

    void checkError(int error) {
        if (error != VK_SUCCESS) {
            throw new IllegalStateException("Vulkan error: " + error);
        }
    }

    private static PointerBuffer checkLayers(MemoryStack stack, VkLayerProperties.Buffer available, String... layers) {
        PointerBuffer required = stack.mallocPointer(layers.length);
        for (int i = 0; i < layers.length; i++) {
            boolean found = false;

            for (int j = 0; j < available.capacity(); j++) {
                available.position(j);
                if (layers[i].equals(available.layerNameString())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.err.format("Cannot find layer: %s\n", layers[i]);
                return null;
            }

            required.put(i, stack.ASCII(layers[i]));
        }

        return required;
    }

    private static PointerBuffer checkExtensions(MemoryStack stack, VkExtensionProperties.Buffer available, String... layers) {
        PointerBuffer required = stack.mallocPointer(layers.length);
        for (int i = 0; i < layers.length; i++) {
            boolean found = false;

            for (int j = 0; j < available.capacity(); j++) {
                available.position(j);
                if (layers[i].equals(available.extensionNameString())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.err.format("Cannot find layer: %s\n", layers[i]);
                return null;
            }

            required.put(i, stack.ASCII(layers[i]));
        }

        return required;
    }

    private void mainLoop() {
        timerNow = System.nanoTime();
        timerNext = timerNow;

        while (!glfwWindowShouldClose(window)) {
            timerNow = System.nanoTime();
            if (timerNow >= timerNext) {
                timerNext = System.nanoTime() + 1000000000;
                System.out.print("FPS: " + timerFrameCounter + "\n");
                timerFrameCounter = 0;
            }

            glfwPollEvents();
            drawFrame();

            timerFrameCounter++;
            vkDeviceWaitIdle(device);
        }
    }

    void drawAndWait() {
        drawFrame();
        vkDeviceWaitIdle(device);
    }

    private void drawFrame() {
        try (MemoryStack stack = stackPush()) {
            vkWaitForFences(device, lp.put(0, inFlightFences[currentFrame]), true, UINT64_MAX);

            int result = vkAcquireNextImageKHR(device, swapChain, UINT64_MAX, imageAvailableSemaphores[currentFrame], VK_NULL_HANDLE, ip);
            int imageIndex = ip.get(0);

            if (result == VK_ERROR_OUT_OF_DATE_KHR) {
                recreateSwapChain(stack);
                return;
            } else if (result != VK_SUCCESS && result != VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("failed to acquire swap chain image!");
            }

            if (imagesInFlight[imageIndex] != VK_NULL_HANDLE) {
                vkWaitForFences(device, lp.put(0, imagesInFlight[imageIndex]), true, UINT64_MAX);
            }
            imagesInFlight[imageIndex] = inFlightFences[currentFrame];

            updateUniformBuffer(imageIndex);

            LongBuffer lp2 = stack.mallocLong(1);
            LongBuffer lp3 = stack.mallocLong(1);
            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(lp.put(0, imageAvailableSemaphores[currentFrame]))
                .pWaitDstStageMask(ip.put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                .pCommandBuffers(pp.put(0, commandBuffers.get(imageIndex)))
                .pSignalSemaphores(lp2.put(0, renderFinishedSemaphores[currentFrame]));

            vkResetFences(device, lp3.put(0, inFlightFences[currentFrame]));
            if (vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[currentFrame]) != VK_SUCCESS) {
                throw new RuntimeException("failed to submit draw command buffer!");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(lp.put(0, renderFinishedSemaphores[currentFrame]))
                .swapchainCount(1)
                .pSwapchains(lp2.put(0, swapChain))
                .pImageIndices(ip.put(0, imageIndex));

            result = vkQueuePresentKHR(presentQueue, presentInfo);

            if (result == VK_ERROR_OUT_OF_DATE_KHR || result == VK_SUBOPTIMAL_KHR || framebufferResized) {
                framebufferResized = false;
                recreateSwapChain(stack);
            } else if (result != VK_SUCCESS) {
                throw new RuntimeException("failed to present swap chain image!");
            }
        }
        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    void updateUniformBuffer(int currentImage) {

        Matrix4f mvp = new Matrix4f();
        mvp.setIdentity();

        //proj
        mvp.multiply(Matrix4f.perspective(60, swapChainExtent.width() / (float) swapChainExtent.height(), 0.1f, 10));
        Matrix4f modelView = inverseMatrix;
        modelView.setIdentity();
        //view
        modelView.multiply(Matrix4f.createTranslateMatrix(0, 0, -5));
        //model
        modelView.multiply(Vector3f.XP.rotationDegrees(50));
        modelView.multiply(Vector3f.ZP.rotationDegrees((float) ((System.currentTimeMillis() * 0.04) % 360)));

        mvp.multiply(modelView);
        inverseMatrix.invert();

        int bufferSize = Float.BYTES * 16;
        vkMapMemory(device, uniformBuffersMemory[currentImage], 0, bufferSize, 0, pp);
        FloatBuffer data = pp.getFloatBuffer(0, bufferSize >> 2);
        mvp.store(data);
        vkUnmapMemory(device, uniformBuffersMemory[currentImage]);
    }

    void cleanupSwapChain(MemoryStack stack) {
        for (int i = 0; i < swapChainFramebuffers.limit(); i++) {
            vkDestroyFramebuffer(device, swapChainFramebuffers.get(i), null);
        }

        PointerBuffer pp2 = stack.mallocPointer(commandBuffers.size());
        int i = 0;
        for (VkCommandBuffer buf : commandBuffers) {
            pp2.put(i, buf);
            i++;
        }
        vkFreeCommandBuffers(device, commandPool, pp2);

        vkDestroyPipeline(device, graphicsPipeline, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        vkDestroyRenderPass(device, renderPass, null);

        for (long imageView : swapChainImageViews) {
            vkDestroyImageView(device, imageView, null);
        }

        vkDestroySwapchainKHR(device, swapChain, null);

        for (i = 0; i < swapChainImages.length; i++) {
            vkDestroyBuffer(device, uniformBuffers[i], null);
            vkFreeMemory(device, uniformBuffersMemory[i], null);
        }

        vkDestroyDescriptorPool(device, descriptorPool, null);
    }

    void recreateSwapChain(MemoryStack stack) {
        vkDeviceWaitIdle(device);

        cleanupSwapChain(stack);

        createSwapChain(stack);
        createImageViews(stack);
        createRenderPass(stack);
        createGraphicsPipeline(stack); //Possible to avoid this by using dynamic state for viewport and scissor rectangles

        createFrameBuffers(stack);
        createUniformBuffers(stack);
        createDescriptorPool(stack);
        createDescriptorSets(stack);
        createCommandBuffers(stack);
    }

    void cleanup() {
        try (MemoryStack stack = stackPush()) {
            cleanupSwapChain(stack);
        }

        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);

        vkDestroyBuffer(device, indexBuffer, null);
        vkFreeMemory(device, indexBufferMemory, null);

        vkDestroyBuffer(device, vertexBuffer, null);
        vkFreeMemory(device, vertexBufferMemory, null);

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(device, renderFinishedSemaphores[i], null);
            vkDestroySemaphore(device, imageAvailableSemaphores[i], null);
            vkDestroyFence(device, inFlightFences[i], null);
        }

        vkDestroyCommandPool(device, commandPool, null);

        vkDestroyDevice(device, null);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);

        dbgFunc.free();

        glfwDestroyWindow(window);
        glfwTerminate();

        memFree(extensions);
        memFree(swapChainFramebuffers);
        memFree(descriptorSets);

        memFree(ip);
        memFree(lp);
        memFree(pp);
    }

    QueueFamilyIndices findQueueFamilies(VkPhysicalDevice deviceIn) {
        QueueFamilyIndices queueFamilyIndices = new QueueFamilyIndices();
        vkGetPhysicalDeviceQueueFamilyProperties(deviceIn, ip, null);

        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(ip.get(0));
        vkGetPhysicalDeviceQueueFamilyProperties(deviceIn, ip, queueFamilies);


        int i = 0;
        for (VkQueueFamilyProperties queueFamily : queueFamilies) {
            if ((queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT) == 1) {
                queueFamilyIndices.graphicsFamily = i;
            }

            vkGetPhysicalDeviceSurfaceSupportKHR(deviceIn, i, surface, ip);
            if (ip.get(0) != 0) {
                queueFamilyIndices.presentFamily = i;
            }


            if (queueFamilyIndices.isComplete()) break;

            i++;
        }

        return queueFamilyIndices;
    }

    VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        for (VkSurfaceFormatKHR availableFormat : availableFormats) {
            if (availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return availableFormat;
            }
        }
        return availableFormats.get(0);
    }

    int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        for (int i = 0; i < availablePresentModes.limit(); i++) {
            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return VK_PRESENT_MODE_FIFO_KHR;
    }

    VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, MemoryStack stack) {
        if (capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        } else {
            IntBuffer ip2 = stack.mallocInt(1);
            glfwGetFramebufferSize(window, ip, ip2);
            int width = ip.get(0);
            int height = ip2.get(0);

            VkExtent2D actualExtent = VkExtent2D.mallocStack(stack);
            actualExtent.width(width).height(height);

            //Max and min are used to clamp the value of width and height between the allowed min and max extents that are supported by the implementation
            actualExtent.width(Math.max(capabilities.minImageExtent().width(), Math.min(capabilities.maxImageExtent().width(),
                actualExtent.width())));
            actualExtent.height(Math.max(capabilities.minImageExtent().height(), Math.min(capabilities.maxImageExtent().height(),
                actualExtent.height())));
            return actualExtent;
        }
    }

    SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice deviceIn, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        //Capabilities
        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        checkError(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(deviceIn, surface, details.capabilities));

        //Surface Formats
        checkError(vkGetPhysicalDeviceSurfaceFormatsKHR(deviceIn, surface, ip, null));
        details.formats = VkSurfaceFormatKHR.mallocStack(ip.get(0), stack);
        checkError(vkGetPhysicalDeviceSurfaceFormatsKHR(deviceIn, surface, ip, details.formats));

        //Present Modes
        checkError(vkGetPhysicalDeviceSurfacePresentModesKHR(deviceIn, surface, ip, null));
        details.presentModes = stack.mallocInt(ip.get(0));
        checkError(vkGetPhysicalDeviceSurfacePresentModesKHR(deviceIn, surface, ip, details.presentModes));

        return details;
    }

    int findMemoryType(int typeFilter, long properties, MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.callocStack(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);

        for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("failed to find suitable memory type!");
    }

    public static void main(String[] args) {
        DebugVulkan app = new DebugVulkan();
        try {
            app.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class QueueFamilyIndices {
        public Integer graphicsFamily;
        public Integer presentFamily;

        boolean isComplete() {
            return graphicsFamily != null && presentFamily != null;
        }
    }

    private static class SwapChainSupportDetails {
        VkSurfaceCapabilitiesKHR capabilities;
        VkSurfaceFormatKHR.Buffer formats;
        IntBuffer presentModes;
    }

    private static class Vertex {
        Vector3f pos;
        int color;

        Vertex(Vector3f pos, int r, int g, int b, int a) {
            this.pos = pos;
            this.color = r << 24 | g << 16 | b << 8 | a;
        }

        static int getsize() {
            //noinspection PointlessArithmeticExpression
            return (Integer.BYTES * 3) + (Integer.BYTES * 1);
        }

        static VkVertexInputBindingDescription.Buffer getBindingDescription() {
            return VkVertexInputBindingDescription.calloc(1)
                .binding(0)
                .stride(Vertex.getsize())
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        }

        static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() { //ASDASDASD
            VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(2);

            //noinspection PointlessArithmeticExpression
            attributeDescriptions.get(0).binding(0)
                .location(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(Float.BYTES * 0);

            attributeDescriptions.get(1).binding(0)
                .location(1)
                .format(VK_FORMAT_R8G8B8A8_UNORM)
                .offset(Float.BYTES * 3);

            return attributeDescriptions;
        }

        public int[] getData() {
            return new int[] {
                Float.floatToIntBits(pos.x()),
                Float.floatToIntBits(pos.y()),
                Float.floatToIntBits(pos.z()),
                color
            };
        }
    }
}