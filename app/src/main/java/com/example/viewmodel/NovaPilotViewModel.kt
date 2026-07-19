package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val name: String,
    val email: String,
    val credits: Int,
    val creditLimit: Int,
    val plan: String
)

class NovaPilotViewModel(application: Application) : AndroidViewModel(application) {

    private val db: NovaPilotDatabase by lazy {
        Room.databaseBuilder(
            application,
            NovaPilotDatabase::class.java, "novapilot_db"
        ).fallbackToDestructiveMigration().build()
    }

    private val dao by lazy { db.dao() }

    // Navigation and UI States
    val currentView = MutableStateFlow("landing") // landing, auth, dashboard, admin
    val authMode = MutableStateFlow("login") // login, signup, reset
    val activeDashboardTab = MutableStateFlow("overview") // overview, tools, api, billing, settings
    val activeAdminTab = MutableStateFlow("analytics") // analytics, providers, users

    // Selected Tool and Input
    val selectedTool = MutableStateFlow<AiTool?>(null)
    val toolInputs = MutableStateFlow<Map<String, String>>(emptyMap())
    val generationOutput = MutableStateFlow("")
    val isGenerating = MutableStateFlow(false)

    // User Profile state
    val userProfile = MutableStateFlow(
        UserProfile(
            name = "Siddharth Raj",
            email = "sid.raj@novapilot.ai",
            credits = 82450,
            creditLimit = 100000,
            plan = "Professional Pro"
        )
    )

    // Gemini API Configurations
    val userGeminiKey = MutableStateFlow("")
    val showKey = MutableStateFlow(false)

    // Toast notifications
    private val _toastFlow = MutableSharedFlow<Pair<String, String>>() // Message, Type ("success", "error")
    val toastFlow = _toastFlow.asSharedFlow()

    // Db Flow lists
    val savedProjects = MutableStateFlow<List<SavedProject>>(emptyList())
    val developerKeys = MutableStateFlow<List<DeveloperKey>>(emptyList())
    val invoiceLogs = MutableStateFlow<List<InvoiceLog>>(emptyList())
    val providerConfigs = MutableStateFlow<List<ProviderConfig>>(emptyList())

    init {
        // Collect flows from Room
        viewModelScope.launch {
            // Check & populate default data if empty
            prepopulateDatabaseIfNeeded()

            // Observe DB changes
            launch {
                dao.getAllSavedProjects().collect { savedProjects.value = it }
            }
            launch {
                dao.getAllDeveloperKeys().collect { developerKeys.value = it }
            }
            launch {
                dao.getAllInvoiceLogs().collect { invoiceLogs.value = it }
            }
            launch {
                dao.getAllProviderConfigs().collect { providerConfigs.value = it }
            }
        }
    }

    private suspend fun prepopulateDatabaseIfNeeded() {
        // Populating Default Provider Configs
        dao.getAllProviderConfigs().first().let { currentList ->
            if (currentList.isEmpty()) {
                val defaults = listOf(
                    ProviderConfig("Google Gemini API", true, 2489, "₹124.50"),
                    ProviderConfig("OpenAI API", true, 5120, "₹512.00"),
                    ProviderConfig("Anthropic Claude API", false, 420, "₹84.00"),
                    ProviderConfig("OpenRouter API", true, 945, "₹18.90"),
                    ProviderConfig("Stability AI API", true, 120, "₹120.00"),
                    ProviderConfig("Replicate API", true, 88, "₹88.00")
                )
                defaults.forEach { dao.insertProviderConfig(it) }
            }
        }

        // Populating Default Invoice Logs
        dao.getAllInvoiceLogs().first().let { currentList ->
            if (currentList.isEmpty()) {
                val defaults = listOf(
                    InvoiceLog("INV-2026-004", "Jul 10, 2026", "₹1,599.00", "Paid"),
                    InvoiceLog("INV-2026-003", "Jun 10, 2026", "₹1,599.00", "Paid")
                )
                defaults.forEach { dao.insertInvoiceLog(it) }
            }
        }

        // Populating Default Saved Projects
        dao.getAllSavedProjects().first().let { currentList ->
            if (currentList.isEmpty()) {
                val defaults = listOf(
                    SavedProject(title = "SaaS Launch Script", tool = "YouTube Script Generator", content = "Here is your SaaS launch script framework...", date = "Yesterday"),
                    SavedProject(title = "SEO Title Optimizer", tool = "YouTube SEO Generator", content = "Focus keywords: SaaS Masterclass, deploy...", date = "3 days ago"),
                    SavedProject(title = "Email Marketing Flow", tool = "Email Writer", content = "Subject: Elevate your business automation...", date = "1 week ago")
                )
                defaults.forEach { dao.insertProject(it) }
            }
        }

        // Populating Default Developer Keys
        dao.getAllDeveloperKeys().first().let { currentList ->
            if (currentList.isEmpty()) {
                val defaults = listOf(
                    DeveloperKey("1", "Production Chatbot", "np_live_8F3a2", "2026-03-10"),
                    DeveloperKey("2", "SEO Pipeline Tool", "np_live_Kj28a", "2026-05-18")
                )
                defaults.forEach { dao.insertDeveloperKey(it) }
            }
        }
    }

    fun showToast(message: String, type: String = "success") {
        viewModelScope.launch {
            _toastFlow.emit(Pair(message, type))
        }
    }

    fun triggerAiGeneration() {
        val tool = selectedTool.value ?: return
        val inputs = toolInputs.value
        viewModelScope.launch {
            isGenerating.value = true
            generationOutput.value = ""

            // Decide API Key to use:
            // 1. Check if user configured local key in Portal Config (Settings)
            // 2. Check if BuildConfig.GEMINI_API_KEY is available (from secrets panel)
            val configuredKey = userGeminiKey.value.trim()
            val apiToken = if (configuredKey.isNotEmpty()) {
                configuredKey
            } else {
                BuildConfig.GEMINI_API_KEY
            }

            val topicKey = inputs["Topic"] ?: inputs["Video Topic"] ?: inputs["Video Subject"] ?: inputs["Seed Term"] ?: inputs["Main Tweet Idea"] ?: inputs["Blog Title"] ?: inputs["Goal of Email"] ?: inputs["Product Name"] ?: inputs["Your Message"] ?: "NovaPilot Venture"

            if (apiToken.isNotEmpty() && apiToken != "MY_GEMINI_API_KEY") {
                // Call real Gemini API
                val promptString = inputs.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                val systemPrompt = "You are NovaPilot AI - ${tool.title}. Generate high-quality, professional, production-ready outputs formatted beautifully."
                
                val result = GeminiApiClient.generateContent(
                    prompt = "Client Input Data:\n$promptString",
                    systemInstruction = systemPrompt,
                    apiKey = apiToken
                )

                generationOutput.value = result
                isGenerating.value = false

                if (!result.startsWith("Error:") && !result.startsWith("API Error:")) {
                    // Deduct credit
                    userProfile.value = userProfile.value.copy(
                        credits = (userProfile.value.credits - 150).coerceAtLeast(0)
                    )
                    // Add project
                    val newProj = SavedProject(
                        title = topicKey,
                        tool = tool.title,
                        content = result,
                        date = "Just now"
                    )
                    dao.insertProject(newProj)
                    showToast("Generation complete! Real-time API used.", "success")
                } else {
                    showToast("API generation failed. Simulating offline fallback.", "error")
                    runSimulation(tool, topicKey)
                }
            } else {
                // Run Simulation
                runSimulation(tool, topicKey)
            }
        }
    }

    private suspend fun runSimulation(tool: AiTool, topicKey: String) {
        val promptText = when (tool.id) {
            "yt-script" -> """
                ## Title Concept: $topicKey - Ultimate Guide
                
                ### [INTRO] - 0:00 to 0:30
                **Visual:** High-speed dynamic cinematic sequence showing charts and code.
                **Host (Voiceover):** "Have you ever wondered how simple ideas turn into multi-million dollar automation systems? Today, we are breaking down $topicKey in real-time..."
                
                ### [BODY PANEL 1] - 0:30 to 2:00
                **Visual:** Direct screenshare showing user interfaces and local storage options.
                **Host:** "First, let's analyze the foundational core architecture. Modern applications require incredibly clean state patterns and fast background services..."
            """.trimIndent()

            "yt-seo" -> """
                ### Optimized SEO Metadata Suite for "$topicKey"
                
                **1. Primary Search Keywords:**
                - $topicKey Tutorial
                - Best $topicKey workflows 2026
                - Deploying $topicKey guide
                
                **2. Multi-channel tags:**
                - #SaaS #AI #Developer #JetpackCompose #Kotlin #Tech
            """.trimIndent()

            "chat-assist" -> """
                ### AI Chat Assistant Response
                
                Hello pilot! I am your NovaPilot Assistant. How can I assist you with your project today?
                
                I am fully optimized to discuss SaaS architectures, cloud functions, Gemini APIs, Kotlin databases, and modern UI design. Let me know what you need me to write or explain!
            """.trimIndent()

            else -> """
                ### Generated Output for: ${tool.title}
                
                **Status:** Success (Simulated engine process)
                
                **Output Concept:**
                Here is your production-ready generated content focusing on **$topicKey**. We have successfully refined, proofread, and styled this block to align perfectly with enterprise standards.
                
                - High-retention hooks applied.
                - Strategic CTAs integrated.
                - Designed for instant copypasting.
            """.trimIndent()
        }

        kotlinx.coroutines.delay(1200)
        generationOutput.value = promptText
        isGenerating.value = false

        // Deduct credit
        userProfile.value = userProfile.value.copy(
            credits = (userProfile.value.credits - 120).coerceAtLeast(0)
        )

        // Add project
        val newProj = SavedProject(
            title = topicKey,
            tool = tool.title,
            content = promptText,
            date = "Just now"
        )
        dao.insertProject(newProj)
        showToast("Document generated successfully!", "success")
    }

    fun generateDeveloperKey(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val keyId = UUID.randomUUID().toString()
            val randomSuffix = (1..5).map { ('A'..'Z').random() }.joinToString("")
            val key = DeveloperKey(
                id = keyId,
                name = name,
                prefix = "np_live_$randomSuffix",
                created = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            )
            dao.insertDeveloperKey(key)
            showToast("New developer API key initialized!", "success")
        }
    }

    fun revokeDeveloperKey(id: String) {
        viewModelScope.launch {
            dao.deleteDeveloperKey(id)
            showToast("Developer key successfully revoked.", "error")
        }
    }

    fun deleteSavedProject(project: SavedProject) {
        viewModelScope.launch {
            dao.deleteProject(project)
            showToast("Project history cleared.", "error")
        }
    }

    fun toggleProviderStatus(config: ProviderConfig) {
        viewModelScope.launch {
            val updated = config.copy(active = !config.active)
            dao.insertProviderConfig(updated)
            showToast("${config.name} configuration updated.", "success")
        }
    }

    fun addCreditsToUser(additional: Int) {
        userProfile.value = userProfile.value.copy(
            credits = (userProfile.value.credits + additional).coerceAtMost(userProfile.value.creditLimit)
        )
        showToast("Credits successfully injected!", "success")
    }

    fun updateProfileName(newName: String) {
        if (newName.isBlank()) return
        userProfile.value = userProfile.value.copy(name = newName)
        showToast("Profile details updated successfully.", "success")
    }
}

data class AiTool(
    val id: String,
    val title: String,
    val desc: String,
    val cat: String,
    val inputs: List<String>
)

val aiTools = listOf(
    AiTool("yt-script", "YouTube Script Generator", "Create full-length high-retention video scripts.", "video", listOf("Topic", "Tone (e.g. Informative, Hype)", "Duration (mins)")),
    AiTool("yt-seo", "YouTube SEO Generator", "Optimized video tags, keywords, and metadata.", "seo", listOf("Video Topic", "Target Keywords")),
    AiTool("yt-title", "YouTube Title Generator", "Highly click-worthy, viral potential titles.", "seo", listOf("Video Subject", "Audience Type")),
    AiTool("yt-desc", "Description Generator", "Engaging, links-optimized video descriptions.", "video", listOf("Video Concept", "Call to Action Links")),
    AiTool("yt-keyword", "Keyword Generator", "High traffic search term identification.", "seo", listOf("Seed Term", "Target Location")),
    AiTool("yt-thumb", "Thumbnail Prompt Gen", "Midjourney/DALL-E prompts for viral thumbs.", "prompts", listOf("Thumbnail Main Focus", "Color Vibe")),
    AiTool("ig-caption", "Instagram Caption Generator", "Persuasive, highly engaging social copy.", "social", listOf("Post Image/Video Idea", "Hashtag Vibe")),
    AiTool("li-post", "LinkedIn Post Generator", "Establish thought leadership with professional text.", "social", listOf("Insight/Topic", "Core Audience")),
    AiTool("x-post", "X (Twitter) Post Gen", "Viral hooks, threads, and high CTR tweets.", "social", listOf("Main Tweet Idea", "Style (Bold, Witty)")),
    AiTool("blog-writer", "Blog Writer", "SEO optimized long-form articles in seconds.", "writing", listOf("Blog Title", "Key Sections to Cover")),
    AiTool("email-writer", "Email Writer", "Cold pitches, followups, and newsletters.", "writing", listOf("Goal of Email", "Recipient Profile")),
    AiTool("prod-desc", "Product Description Gen", "Copywriting schemas optimized for e-commerce conversion.", "writing", listOf("Product Name", "Highlight Features")),
    AiTool("resume-builder", "Resume Builder", "ATS optimized structural descriptions.", "careers", listOf("Your Role & Experience", "Skills List")),
    AiTool("cover-letter", "Cover Letter Builder", "Stellar, highly customized job application letters.", "careers", listOf("Job Role & Company", "Your Core Background")),
    AiTool("prompt-gen", "General Prompt Generator", "Refines system instructions for better LLM outputs.", "prompts", listOf("Raw Task Idea", "Target Model")),
    AiTool("image-prompt", "AI Image Prompt Gen", "Detailed, lighting/render rich photorealistic prompts.", "prompts", listOf("Subject Description", "Art Style")),
    AiTool("video-prompt", "AI Video Prompt Gen", "Sora, Runway, Pika optimized camera movement prompts.", "prompts", listOf("Video Scenario", "Camera Direction")),
    AiTool("chat-assist", "AI Chat Assistant", "Talk directly with a highly intelligent assistant.", "chat", listOf("Your Message"))
)
