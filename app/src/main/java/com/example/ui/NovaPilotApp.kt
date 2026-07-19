package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.launch

@Composable
fun NovaPilotApp(viewModel: NovaPilotViewModel) {
    val currentView by viewModel.currentView.collectAsStateWithLifecycle()
    val authMode by viewModel.authMode.collectAsStateWithLifecycle()
    val activeDashboardTab by viewModel.activeDashboardTab.collectAsStateWithLifecycle()
    val activeAdminTab by viewModel.activeAdminTab.collectAsStateWithLifecycle()

    val selectedTool by viewModel.selectedTool.collectAsStateWithLifecycle()
    val toolInputs by viewModel.toolInputs.collectAsStateWithLifecycle()
    val generationOutput by viewModel.generationOutput.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val userGeminiKey by viewModel.userGeminiKey.collectAsStateWithLifecycle()
    val showKey by viewModel.showKey.collectAsStateWithLifecycle()

    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val developerKeys by viewModel.developerKeys.collectAsStateWithLifecycle()
    val invoiceLogs by viewModel.invoiceLogs.collectAsStateWithLifecycle()
    val providerConfigs by viewModel.providerConfigs.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Observe Toast notification channel
    LaunchedEffect(Unit) {
        viewModel.toastFlow.collect { (msg, type) ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = {
            HeaderSection(
                currentView = currentView,
                userProfile = userProfile,
                onNavigate = { view -> 
                    viewModel.currentView.value = view
                    viewModel.selectedTool.value = null
                },
                onOpenAdmin = {
                    viewModel.currentView.value = "admin"
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(PrimaryIndigo.copy(alpha = 0.08f), Color.Transparent),
                        radius = 2000f
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentView,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "view_transition"
            ) { viewState ->
                when (viewState) {
                    "landing" -> LandingView(
                        onLaunchWorkspace = { viewModel.currentView.value = "auth" },
                        onShowToast = { msg, type -> viewModel.showToast(msg, type) }
                    )
                    "auth" -> AuthView(
                        authMode = authMode,
                        onSwitchMode = { mode -> viewModel.authMode.value = mode },
                        onAuthorize = {
                            viewModel.currentView.value = "dashboard"
                            viewModel.showToast("System authorization successful.", "success")
                        },
                        onSocialLogin = { provider ->
                            viewModel.currentView.value = "dashboard"
                            viewModel.showToast("$provider authorized.", "success")
                        }
                    )
                    "dashboard" -> DashboardView(
                        activeTab = activeDashboardTab,
                        selectedTool = selectedTool,
                        toolInputs = toolInputs,
                        generationOutput = generationOutput,
                        isGenerating = isGenerating,
                        userProfile = userProfile,
                        userGeminiKey = userGeminiKey,
                        showKey = showKey,
                        savedProjects = savedProjects,
                        developerKeys = developerKeys,
                        invoiceLogs = invoiceLogs,
                        onTabSelected = { tab ->
                            viewModel.activeDashboardTab.value = tab
                            viewModel.selectedTool.value = null
                        },
                        onToolSelected = { tool ->
                            viewModel.selectedTool.value = tool
                            viewModel.toolInputs.value = emptyMap()
                            viewModel.generationOutput.value = ""
                        },
                        onBackToTools = { viewModel.selectedTool.value = null },
                        onInputChange = { name, value ->
                            viewModel.toolInputs.value = viewModel.toolInputs.value + (name to value)
                        },
                        onGenerate = { viewModel.triggerAiGeneration() },
                        onCopyOutput = { content ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NovaPilot AI Output", content))
                            viewModel.showToast("Output copied to clipboard!", "success")
                        },
                        onGenerateKey = { name -> viewModel.generateDeveloperKey(name) },
                        onRevokeKey = { id -> viewModel.revokeDeveloperKey(id) },
                        onToggleShowKey = { viewModel.showKey.value = !showKey },
                        onUpdateGeminiKey = { key -> viewModel.userGeminiKey.value = key },
                        onUpdateProfileName = { name -> viewModel.updateProfileName(name) },
                        onDeleteProject = { project -> viewModel.deleteSavedProject(project) }
                    )
                    "admin" -> AdminView(
                        activeTab = activeAdminTab,
                        providerConfigs = providerConfigs,
                        userProfile = userProfile,
                        onTabSelected = { tab -> viewModel.activeAdminTab.value = tab },
                        onToggleProvider = { provider -> viewModel.toggleProviderStatus(provider) },
                        onAddCredits = { viewModel.addCreditsToUser(15000) },
                        onExitAdmin = { viewModel.currentView.value = "dashboard" }
                    )
                }
            }
        }
    }
}

// --- CORE UI SECTIONS & COMPONENTS ---

@Composable
fun HeaderSection(
    currentView: String,
    userProfile: UserProfile,
    onNavigate: (String) -> Unit,
    onOpenAdmin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = BgDark.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo & Icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onNavigate("landing") }
                    .testTag("brand_logo_header")
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryIndigo, AccentPurple)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "NovaPilot Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NovaPilot",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "AI",
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // Central Navigation Tabs and Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentView == "landing") {
                    Button(
                        onClick = { onNavigate("auth") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .testTag("launch_workspace_button")
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Launch Workspace",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Credits pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.15f))
                            .border(1.dp, PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Credits star",
                            tint = AccentPink,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${userProfile.credits.toLocaleString()} Cr",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    // Admin suite access
                    IconButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (currentView == "admin") PrimaryIndigo else BgCardSecondary)
                            .testTag("admin_suite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Admin suite",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Logout/Exit button
                    IconButton(
                        onClick = { onNavigate("landing") },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BgCardSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Log out",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- 1. LANDING PAGE SCREEN ---

@Composable
fun LandingView(
    onLaunchWorkspace: () -> Unit,
    onShowToast: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    var emailInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Headline Chip
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(PrimaryIndigo.copy(alpha = 0.15f))
                .border(1.dp, PrimaryIndigo.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Sparkle",
                tint = AccentPink,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Global Expansion: All-in-One AI Suite Live",
                color = AccentPink,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Headers
        Text(
            text = "Create Faster.",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 38.sp,
            textAlign = TextAlign.Center,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )
        Text(
            text = "Grow Smarter.",
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(AccentPink, AccentPurple, PrimaryIndigo)
                )
            ),
            fontWeight = FontWeight.Black,
            fontSize = 38.sp,
            textAlign = TextAlign.Center,
            letterSpacing = (-1).sp,
            lineHeight = 44.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Consolidate your content creation pipeline, video script production, advanced SEO ranking metrics, and developer AI prompt optimizations under one lightning-fast, ultra-secure engine.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Action CTA buttons
        Button(
            onClick = onLaunchWorkspace,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(54.dp)
                .testTag("get_started_free_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Get Started Free",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "Forward arrow",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Proactively generated hero banner visual aspect
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "NovaPilot Cosmic Hero Visual Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Interactive Showcase Analytics Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "novapilot-dashboard.app",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Metric 1
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = BgDark.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("AI MODELS LIVE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("6 Active", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    // Metric 2
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = BgDark.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("SYSTEM UPTIME", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("99.98%", color = AccentGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Pricing Header
        Text(
            text = "Pricing Engineered for Scale",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            text = "Start instantly. Scale seamlessly. Cancel anytime.",
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stacked pricing cards
        PricingCard(
            title = "Starter Free",
            price = "₹0",
            period = "/ forever",
            desc = "Perfect for beginners and hobbyists.",
            features = listOf("10,000 Credits Monthly", "Access to standard AI tools"),
            missingFeatures = listOf("Advanced analytics console"),
            buttonText = "Activate Free",
            onAction = onLaunchWorkspace,
            highlight = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        PricingCard(
            title = "Professional Pro",
            price = "₹1,599",
            period = "/ month",
            desc = "Optimized for agency workflows and creators.",
            features = listOf("100,000 Credits Monthly", "Complete Access to 18 AI tools", "Dynamic API Engine Panel"),
            missingFeatures = emptyList(),
            buttonText = "Unlock Pro Suite",
            onAction = onLaunchWorkspace,
            highlight = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        PricingCard(
            title = "Enterprise Elite",
            price = "Custom",
            period = "/ quote",
            desc = "Built for high volume scale & custom pipelines.",
            features = listOf("Unlimited credit allocation", "Dedicated secure model pipelines", "SLA support contracts"),
            missingFeatures = emptyList(),
            buttonText = "Contact Enterprise",
            onAction = { onShowToast("Sales team notified! We will contact you soon.", "success") },
            highlight = false
        )

        Spacer(modifier = Modifier.height(40.dp))

        // FAQ Section Header
        Text(
            text = "Frequently Answered Queries",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        FaqItem(
            q = "How do credit limits work?",
            a = "Each tier receives a monthly balance of credits. Generating tools consumes credits relative to processing weight (e.g., YouTube script is 120 credits)."
        )
        Spacer(modifier = Modifier.height(10.dp))
        FaqItem(
            q = "Can I connect my own AI keys directly?",
            a = "Yes! In the developer dashboard settings, you can plug in your Google Gemini key. This redirects calls and preserves platform credits."
        )
        Spacer(modifier = Modifier.height(10.dp))
        FaqItem(
            q = "Is there support for Indian GST invoices?",
            a = "Absolutely. All transactions automatically generate valid GST-compliant invoices immediately downloadable in your billing tab."
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Newsletter Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Join NovaPilot Dispatch",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Get exclusive, high-performance prompting guides straight to your inbox.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    placeholder = { Text("Enter your business email", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            onShowToast("Subscription verified!", "success")
                            emailInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Join Dispatch", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Footer
        Divider(color = Color.White.copy(alpha = 0.05f))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "© 2026 NovaPilot Inc. Powered by advanced neural architecture pipelines globally.",
            color = TextMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

@Composable
fun PricingCard(
    title: String,
    price: String,
    period: String,
    desc: String,
    features: List<String>,
    missingFeatures: List<String>,
    buttonText: String,
    onAction: () -> Unit,
    highlight: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (highlight) 2.dp else 1.dp,
                brush = if (highlight) Brush.verticalGradient(listOf(PrimaryIndigo, AccentPurple)) else SolidColor(Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = if (highlight) BgCard.copy(alpha = 0.8f) else BgCard),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (highlight) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(PrimaryIndigo)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .align(Alignment.End)
                ) {
                    Text("Popular", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(desc, color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(price, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(period, color = TextMuted, fontSize = 12.sp)
            }

            Divider(color = Color.White.copy(alpha = 0.05f))

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                features.forEach { feat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Included", tint = AccentPink, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feat, color = TextLight, fontSize = 13.sp)
                    }
                }
                missingFeatures.forEach { feat ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Not included", tint = TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feat, color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = if (highlight) PrimaryIndigo else BgCardSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun FaqItem(q: String, a: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = q,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(0.9f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand icon",
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = a,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// --- 2. AUTH SCREEN ---

@Composable
fun AuthView(
    authMode: String,
    onSwitchMode: (String) -> Unit,
    onAuthorize: () -> Unit,
    onSocialLogin: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (authMode) {
                        "login" -> "Welcome Back Pilot"
                        "signup" -> "Initiate Account"
                        else -> "Recover Access"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Text(
                    text = "Access NovaPilot Global AI Workspace Engine",
                    color = TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (authMode != "reset") {
                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("EMAIL ADDRESS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("pilot@novapilot.ai", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = BgDark,
                                unfocusedContainerColor = BgDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 14.dp)
                        )
                    }

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PASSWORD", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            if (authMode == "login") {
                                Text(
                                    "Forgot?",
                                    color = PrimaryIndigo,
                                    fontSize = 11.sp,
                                    modifier = Modifier.clickable { onSwitchMode("reset") }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = BgDark,
                                unfocusedContainerColor = BgDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 14.dp)
                        )
                    }
                } else {
                    // Email Field only for reset
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("ENTER REGISTERED EMAIL", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("pilot@novapilot.ai", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = BgDark,
                                unfocusedContainerColor = BgDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 14.dp)
                        )
                    }
                }

                Button(
                    onClick = onAuthorize,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_button")
                ) {
                    Text(
                        text = when (authMode) {
                            "login" -> "Authorize System"
                            "signup" -> "Create Space"
                            else -> "Send Code"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.05f))
                    Text(
                        text = "OR INTEGRATE WITH",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.05f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onSocialLogin("Google") },
                        colors = ButtonDefaults.buttonColors(containerColor = BgDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Google", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onSocialLogin("GitHub") },
                        colors = ButtonDefaults.buttonColors(containerColor = BgDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("GitHub", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (authMode == "login") "Need a professional workspace? Create Account" else "Already registered? Login Here",
                    color = PrimaryIndigo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clickable {
                            if (authMode == "login") onSwitchMode("signup") else onSwitchMode("login")
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

// --- 3. DASHBOARD / WORKSPACE TAB ENGINES ---

@Composable
fun DashboardView(
    activeTab: String,
    selectedTool: AiTool?,
    toolInputs: Map<String, String>,
    generationOutput: String,
    isGenerating: Boolean,
    userProfile: UserProfile,
    userGeminiKey: String,
    showKey: Boolean,
    savedProjects: List<SavedProject>,
    developerKeys: List<DeveloperKey>,
    invoiceLogs: List<InvoiceLog>,
    onTabSelected: (String) -> Unit,
    onToolSelected: (AiTool) -> Unit,
    onBackToTools: () -> Unit,
    onInputChange: (String, String) -> Unit,
    onGenerate: () -> Unit,
    onCopyOutput: (String) -> Unit,
    onGenerateKey: (String) -> Unit,
    onRevokeKey: (String) -> Unit,
    onToggleShowKey: () -> Unit,
    onUpdateGeminiKey: (String) -> Unit,
    onUpdateProfileName: (String) -> Unit,
    onDeleteProject: (SavedProject) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Tab scrolling bar (Compact responsive navigation for Android)
        ScrollableTabRow(
            selectedTabIndex = when (activeTab) {
                "overview" -> 0
                "tools" -> 1
                "api" -> 2
                "billing" -> 3
                else -> 4
            },
            containerColor = BgCard,
            contentColor = PrimaryIndigo,
            edgePadding = 12.dp,
            divider = { Divider(color = Color.White.copy(alpha = 0.05f)) }
        ) {
            Tab(
                selected = activeTab == "overview",
                onClick = { onTabSelected("overview") },
                text = { Text("Overview", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "tools" || selectedTool != null,
                onClick = { onTabSelected("tools") },
                text = { Text("AI Tools", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "api",
                onClick = { onTabSelected("api") },
                text = { Text("API Console", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "billing",
                onClick = { onTabSelected("billing") },
                text = { Text("Billing", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "settings",
                onClick = { onTabSelected("settings") },
                text = { Text("Portal Config", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (selectedTool != null) {
                ToolWorkspaceView(
                    tool = selectedTool,
                    toolInputs = toolInputs,
                    generationOutput = generationOutput,
                    isGenerating = isGenerating,
                    onBack = onBackToTools,
                    onInputChange = onInputChange,
                    onGenerate = onGenerate,
                    onCopyOutput = onCopyOutput
                )
            } else {
                when (activeTab) {
                    "overview" -> DashboardOverviewTab(
                        userProfile = userProfile,
                        savedProjects = savedProjects,
                        onCopyOutput = onCopyOutput,
                        onDeleteProject = onDeleteProject,
                        onNavigateToTools = { onTabSelected("tools") }
                    )
                    "tools" -> DashboardToolsTab(onToolSelected = onToolSelected)
                    "api" -> DashboardApiTab(
                        developerKeys = developerKeys,
                        onGenerateKey = onGenerateKey,
                        onRevokeKey = onRevokeKey
                    )
                    "billing" -> DashboardBillingTab(
                        userProfile = userProfile,
                        invoiceLogs = invoiceLogs
                    )
                    "settings" -> DashboardSettingsTab(
                        userProfile = userProfile,
                        userGeminiKey = userGeminiKey,
                        showKey = showKey,
                        onToggleShowKey = onToggleShowKey,
                        onUpdateGeminiKey = onUpdateGeminiKey,
                        onUpdateProfileName = onUpdateProfileName
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardOverviewTab(
    userProfile: UserProfile,
    savedProjects: List<SavedProject>,
    onCopyOutput: (String) -> Unit,
    onDeleteProject: (SavedProject) -> Unit,
    onNavigateToTools: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Diagnostics",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Real-time usage reports for ${userProfile.name}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = onNavigateToTools,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Sparkles", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Tools", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Metrics Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricWidget(
                    title = "AI ENGINES RESPONSE",
                    value = "99.8% Perfect",
                    subtext = "Live routing load-balanced",
                    color = AccentGreen
                )
                MetricWidget(
                    title = "ACTIVE USER TOKEN",
                    value = userProfile.plan,
                    subtext = "${userProfile.credits.toLocaleString()} / ${userProfile.creditLimit.toLocaleString()} credits left",
                    color = PrimaryIndigo
                )
                MetricWidget(
                    title = "LIVE LATENCY",
                    value = "340 ms",
                    subtext = "Average server-side speed",
                    color = AccentPink
                )
            }
        }

        // Saved outputs history
        item {
            Text(
                text = "Saved Projects & Output History",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }

        if (savedProjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved projects yet. Generate content to populate your workspace.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(savedProjects) { project ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(0.8f)) {
                                Text(project.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(project.tool, color = TextMuted, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(project.date, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
                                IconButton(
                                    onClick = { onCopyOutput(project.content) },
                                    modifier = Modifier.size(28.dp).testTag("copy_output_button")
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteProject(project) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = project.content,
                                color = TextLight,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgDark)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricWidget(title: String, value: String, subtext: String, color: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
                Text(subtext, color = TextMuted, fontSize = 11.sp)
            }
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = "Trending icon",
                tint = color.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun DashboardToolsTab(onToolSelected: (AiTool) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Interactive AI Tools Suite",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Select any tool to activate its live production space",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(aiTools) { tool ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToolSelected(tool) }
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Tool icon", tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(BgCardSecondary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(tool.cat.uppercase(), color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(tool.desc, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolWorkspaceView(
    tool: AiTool,
    toolInputs: Map<String, String>,
    generationOutput: String,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onInputChange: (String, String) -> Unit,
    onGenerate: () -> Unit,
    onCopyOutput: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back navigation header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tools", color = TextMuted, fontSize = 12.sp)
            Icon(imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight, contentDescription = "Arrow right", tint = TextMuted, modifier = Modifier.size(14.dp))
            Text(tool.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        // Dynamic parameter inputs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Input Parameters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tool.desc, color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tool.inputs.forEach { inputName ->
                        Column {
                            Text(inputName.uppercase(), color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = toolInputs[inputName] ?: "",
                                onValueChange = { onInputChange(inputName, it) },
                                placeholder = { Text("Enter ${inputName.lowercase()}...", color = TextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = BgDark,
                                    unfocusedContainerColor = BgDark,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = BorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onGenerate,
                        enabled = !isGenerating && tool.inputs.all { (toolInputs[it] ?: "").isNotBlank() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("run_neural_process_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing Engine...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Neural Process", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Output Display Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LIVE WORKSPACE OUTPUT", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    if (generationOutput.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onCopyOutput(generationOutput) }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Output", color = PrimaryIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp)
                        .background(BgDark)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    if (generationOutput.isBlank()) {
                        Text(
                            text = "[ Fill out inputs and press 'Run Neural Process' to trigger generative output. ]",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        Text(
                            text = generationOutput,
                            color = TextLight,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Consumption: ~120 credits per generation", color = TextMuted, fontSize = 9.sp)
                    Text("Output Type: Production-Ready", color = TextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
fun DashboardApiTab(
    developerKeys: List<DeveloperKey>,
    onGenerateKey: (String) -> Unit,
    onRevokeKey: (String) -> Unit
) {
    var keyName by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Developer API Console",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Build external applications directly backed by NovaPilot pipelines",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Create Custom Client Access Token", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Each key has an isolated rate-limit of 100 queries/min.", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = keyName,
                            onValueChange = { keyName = it },
                            placeholder = { Text("Name of Application (e.g. Chatbot)", color = TextMuted, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = BgDark,
                                unfocusedContainerColor = BgDark,
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = BorderColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f)
                        )

                        Button(
                            onClick = {
                                if (keyName.isNotBlank()) {
                                    onGenerateKey(keyName)
                                    keyName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("Generate Key", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("Active Developers Tokens", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        }

        if (developerKeys.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BgCard)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No active keys configured.", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(developerKeys) { key ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(key.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${key.prefix}••••••••••••••••••••", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Created: ${key.created}", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = "Revoke Token",
                                color = AccentRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { onRevokeKey(key.id) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardBillingTab(
    userProfile: UserProfile,
    invoiceLogs: List<InvoiceLog>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Billing & Invoice Console",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Manage payment mechanisms, view invoice lists, and set up auto-refills.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(PrimaryIndigo.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Plan Active", color = PrimaryIndigo, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(userProfile.plan, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Renews automatically on August 10, 2026 for ₹1,599.00.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = BgCardSecondary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel Cycle", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(AccentGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Primary Card", color = AccentGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("HDFC Bank Debit Card (Visa)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("•••• •••• •••• 4291", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))

                    Text("Update Billing Details", color = PrimaryIndigo, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.clickable {})
                }
            }
        }

        item {
            Text("Paid Invoices & GST Receipts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        }

        items(invoiceLogs) { inv ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(inv.invoiceId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Dated: ${inv.date}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(inv.amount, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(AccentGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(inv.status.uppercase(), color = AccentGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardSettingsTab(
    userProfile: UserProfile,
    userGeminiKey: String,
    showKey: Boolean,
    onToggleShowKey: () -> Unit,
    onUpdateGeminiKey: (String) -> Unit,
    onUpdateProfileName: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(userProfile.name) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Portal Configurations",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
        Text(
            text = "Personalize security frameworks, notification triggers, and live AI API connections.",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Gemini Key section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Direct Gemini Core Integration (Optional)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "By plugging in your personal Google Gemini API key, you redirect prompt processing requests directly to Google servers. This bypasses system credit limits entirely. Your token key stays safely stored local-only inside this session!",
                    color = TextMuted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text("GOOGLE GEMINI API KEY", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = userGeminiKey,
                        onValueChange = onUpdateGeminiKey,
                        placeholder = { Text("AIzaSy...", color = TextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = BgDark,
                            unfocusedContainerColor = BgDark,
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = BorderColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f)
                    )

                    IconButton(
                        onClick = onToggleShowKey,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BgCardSecondary)
                    ) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = Color.White
                        )
                    }
                }

                if (userGeminiKey.trim().isNotEmpty()) {
                    Text(
                        text = "✓ Direct Live Gemini Integration Active",
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Profile details section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Profile Identity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))

                Text("FULL NAME", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp)
                )

                Text("EMAIL ADDRESS", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = userProfile.email,
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextMuted,
                        disabledContainerColor = BgDark,
                        disabledBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp)
                )

                Button(
                    onClick = { onUpdateProfileName(nameInput) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// --- 4. SYSTEM ADMIN SUITEVIEW ---

@Composable
fun AdminView(
    activeTab: String,
    providerConfigs: List<ProviderConfig>,
    userProfile: UserProfile,
    onTabSelected: (String) -> Unit,
    onToggleProvider: (ProviderConfig) -> Unit,
    onAddCredits: () -> Unit,
    onExitAdmin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Tab header row
        TabRow(
            selectedTabIndex = when (activeTab) {
                "analytics" -> 0
                "providers" -> 1
                else -> 2
            },
            containerColor = BgCard,
            contentColor = PrimaryIndigo,
            divider = { Divider(color = Color.White.copy(alpha = 0.05f)) }
        ) {
            Tab(
                selected = activeTab == "analytics",
                onClick = { onTabSelected("analytics") },
                text = { Text("Uptime Reports", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "providers",
                onClick = { onTabSelected("providers") },
                text = { Text("AI Pipelines", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeTab == "users",
                onClick = { onTabSelected("users") },
                text = { Text("User Operations", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp)
        ) {
            when (activeTab) {
                "analytics" -> AdminAnalyticsTab(onExitAdmin = onExitAdmin)
                "providers" -> AdminProvidersTab(
                    providerConfigs = providerConfigs,
                    onToggleProvider = onToggleProvider
                )
                "users" -> AdminUsersTab(
                    userProfile = userProfile,
                    onAddCredits = onAddCredits
                )
            }
        }
    }
}

@Composable
fun AdminAnalyticsTab(onExitAdmin: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Global Infrastructure Analytics",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Review pipeline execution frequency, cost reports, and active models status.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricWidget(
                    title = "TOTAL PIPELINE COST (MTD)",
                    value = "₹12,482.50",
                    subtext = "Calculated across active APIs",
                    color = AccentPink
                )
                MetricWidget(
                    title = "ACTIVE USER COUNT",
                    value = "1,245 Users",
                    subtext = "Growth: +14% weekly trend",
                    color = PrimaryIndigo
                )
                MetricWidget(
                    title = "AVERAGE SYSTEM UPTIME",
                    value = "99.98%",
                    subtext = "Failover automated successfully",
                    color = AccentGreen
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pipeline Metrics Visualizer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(BgDark)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "[ Live Active SVG Visualizer Module Active. No errors found. ]",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onExitAdmin,
                colors = ButtonDefaults.buttonColors(containerColor = BgCardSecondary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Exit Admin Panel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AdminProvidersTab(
    providerConfigs: List<ProviderConfig>,
    onToggleProvider: (ProviderConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "AI Infrastructure Routing",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Toggle provider models active/inactive, configure API settings, and track cost distributions.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(providerConfigs) { provider ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.7f)) {
                        Text(provider.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Usage: ${provider.usageCount.toLocaleString()} queries | Cost: ${provider.cost}",
                            color = TextMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(if (provider.active) AccentGreen.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (provider.active) "Active" else "Disabled",
                                color = if (provider.active) AccentGreen else AccentRed,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Toggle",
                            color = PrimaryIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable { onToggleProvider(provider) }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminUsersTab(
    userProfile: UserProfile,
    onAddCredits: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Global User Operations",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )
            Text(
                text = "Track, block, or allocate extra credits to registered account holders.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Siddharth Raj user row
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(userProfile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(userProfile.email, color = TextMuted, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(userProfile.plan, color = PrimaryIndigo, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CREDITS REMAINING", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${userProfile.credits.toLocaleString()} / ${userProfile.creditLimit.toLocaleString()}",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Button(
                            onClick = onAddCredits,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_credits_button")
                        ) {
                            Text("Add 15k Credits", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Simulated Japanese account Tanaka Keigo
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tanaka Keigo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("tanaka@novapilot.jp", color = TextMuted, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(BgCardSecondary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Starter (Free)", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("CREDITS REMAINING", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "2,100 / 10,000",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Revoke Access",
                                color = AccentRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {}.padding(6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper extension to format integers with commas
fun Int.toLocaleString(): String {
    return String.format("%,d", this)
}
