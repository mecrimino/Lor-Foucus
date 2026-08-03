package com.lorfocus.app.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.lorfocus.app.data.AppSettings
import com.lorfocus.app.data.BlockedApp
import com.lorfocus.app.data.DayStat
import com.lorfocus.app.data.FeedMode
import com.lorfocus.app.data.MonitoredFeed
import com.lorfocus.app.detection.DetectionDiagnostics
import com.lorfocus.app.ui.theme.LocalLorColors
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/* ---------------------------------------------------------------- routing */

object Dest {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"; const val HOWWORKS = "howworks"; const val PERMISSIONS = "permissions"
    const val PICKFEEDS = "pickfeeds"; const val APPROACH = "approach"; const val LOGO = "logo"
    const val TODAY = "today"; const val RULES = "rules"; const val RULEDETAIL = "ruledetail"
    const val FOCUS = "focus"; const val ADDCHANNEL = "addchannel"; const val SCHEDULE = "schedule"
    const val INSIGHTS = "insights"; const val SETTINGS = "settings"; const val STRICT = "strict"; const val GOALS = "goals"
    const val DIAG = "diag"; const val APPDETAIL = "appdetail"
}

/** A minimal router — one screen + optional edited-feed id + a back stack. Simpler than
 * NavHost arg-plumbing for a scaffold, and mirrors the working web prototype. */
class Router(start: String) {
    var route by mutableStateOf(start); private set
    var editing by mutableStateOf<String?>(null); private set
    private val stack = ArrayDeque<Pair<String, String?>>()
    fun go(dest: String, id: String? = null) { stack.addLast(route to editing); route = dest; editing = id }
    fun tab(dest: String) { stack.clear(); editing = null; route = dest }
    fun back() { val p = stack.removeLastOrNull(); route = p?.first ?: Dest.TODAY; editing = p?.second }
    fun reset(dest: String) { stack.clear(); editing = null; route = dest }
}

private val MAIN_TABS = setOf(Dest.TODAY, Dest.RULES, Dest.FOCUS, Dest.INSIGHTS)

@Composable
fun AppRoot(vm: AppViewModel, settings: AppSettings) {
    val c = LocalLorColors.current
    val router = remember { Router(Dest.SPLASH) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val real = vm.firstSettings()                    // reliable one-shot datastore read
        router.reset(if (real.onboarded) Dest.TODAY else Dest.WELCOME)
        started = true
    }
    if (!started) { Box(Modifier.fillMaxSize().background(c.paper)); return }

    Column(Modifier.fillMaxSize().background(c.paper)) {
        Box(Modifier.weight(1f)) { CurrentScreen(router, vm, settings) }
        if (router.route in MAIN_TABS) BottomBar(router)
    }
}

@Composable
private fun CurrentScreen(router: Router, vm: AppViewModel, settings: AppSettings) = when (router.route) {
    Dest.WELCOME -> WelcomeScreen(router, settings, vm)
    Dest.HOWWORKS -> HowItWorksScreen(router)
    Dest.PERMISSIONS -> PermissionsScreen(router, vm)
    Dest.PICKFEEDS -> PickFeedsScreen(router, vm)
    Dest.APPROACH -> ApproachScreen(router, vm)
    Dest.LOGO -> LogoPickerScreen(router, settings, vm)
    Dest.TODAY -> TodayScreen(router, vm, settings)
    Dest.RULES -> RulesScreen(router, vm)
    Dest.RULEDETAIL -> RuleDetailScreen(router, vm)
    Dest.FOCUS -> FocusScreen(router, settings, vm)
    Dest.ADDCHANNEL -> AddChannelScreen(router, vm)
    Dest.SCHEDULE -> ScheduleScreen(router)
    Dest.INSIGHTS -> InsightsScreen(vm)
    Dest.SETTINGS -> SettingsScreen(router, settings, vm)
    Dest.STRICT -> StrictScreen(router, settings, vm)
    Dest.GOALS -> GoalsScreen(router, settings, vm)
    Dest.DIAG -> DiagnosticsScreen(router)
    Dest.APPDETAIL -> AppDetailScreen(router, vm)
    else -> Box(Modifier.fillMaxSize())
}

/* ---------------------------------------------------------------- shared UI */

private val LightKnob = Color(0xFFF7F5F0)

@Composable
private fun col() = LocalLorColors.current

@Composable
fun Emblem(id: String, size: Dp) {
    // Always on a light "app-icon" disc so the two-tone mark reads in both themes
    // (the vector fills are hardcoded for the light palette).
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(Color(0xFFFAF9F6)),
        contentAlignment = Alignment.Center,
    ) { Image(painterResource(Badger.from(id).res), null, Modifier.size(size * 0.7f)) }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val c = col()
    Box(
        Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
            .background(if (enabled) c.primary else c.primary.copy(alpha = .16f))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) c.onPrimary else c.ink.copy(alpha = .38f), fontSize = 15.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun Toggle(on: Boolean, onToggle: () -> Unit) {
    val c = col()
    Box(
        Modifier.width(46.dp).height(28.dp).clip(RoundedCornerShape(14.dp))
            .background(if (on) c.focus else c.hair).clickable { onToggle() }.padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(22.dp).clip(CircleShape).background(LightKnob)) }
}

@Composable
fun Serif(text: String, size: Int, modifier: Modifier = Modifier, color: Color = col().ink, align: TextAlign? = null) {
    Text(text, modifier = modifier, color = color, fontFamily = FontFamily.Serif, fontSize = size.sp,
        lineHeight = (size * 1.1).sp, textAlign = align)
}

@Composable
fun Eyebrow(text: String) = Text(text, color = col().faint, fontSize = 11.sp, letterSpacing = 0.5.sp)

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    val c = col()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.ChevronLeft, "Back", tint = c.ink,
            modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onBack() })
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = c.ink)
    }
}

@Composable
private fun ColumnScope.HairlineRow(onClick: (() -> Unit)? = null, content: @Composable RowScope.() -> Unit) {
    val c = col()
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, content = content,
    )
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
}

@Composable
fun Mono(text: String) {
    val c = col()
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(c.chip), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = c.muted)
    }
}

@Composable
private fun BottomBar(router: Router) {
    val c = col()
    Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 22.dp, start = 16.dp, end = 16.dp)) {
        data class T(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)
        val tabs = listOf(
            T(Dest.TODAY, Icons.Rounded.Schedule, "Today"),
            T(Dest.RULES, Icons.Rounded.Menu, "Rules"),
            T(Dest.FOCUS, Icons.Rounded.CenterFocusStrong, "Focus"),
            T(Dest.INSIGHTS, Icons.Rounded.BarChart, "Insights"),
        )
        tabs.forEach { t ->
            val active = router.route == t.route
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { router.tab(t.route) }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(t.icon, t.label, tint = if (active) c.ink else c.faint, modifier = Modifier.size(20.dp))
                Text(t.label, fontSize = 10.sp, color = if (active) c.ink else c.faint)
            }
        }
    }
}

/**
 * Screen body: scrollable content over the standard 24dp gutters, with an optional pinned
 * bottom block (the CTA). Weight lives in the outer non-scrolling column, never inside the
 * scroll — putting weight() inside a verticalScroll throws.
 */
@Composable
private fun Body(
    top: Int = 40,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = top.dp, bottom = if (bottom == null) 32.dp else 12.dp),
            content = content,
        )
        if (bottom != null) Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp),
            content = bottom,
        )
    }
}

private fun modeSummary(f: MonitoredFeed): String = when (f.mode) {
    FeedMode.BLOCK -> "Blocked fully"
    FeedMode.PAUSE -> "${f.pauseSeconds} second pause"
    FeedMode.LIMIT -> "${f.budgetMinutes} min a day · ${f.activeFrom}–${f.activeUntil}"
}

/* --- real-stats helpers --- */
private fun statOn(stats: List<DayStat>, date: LocalDate): DayStat? = stats.firstOrNull { it.date == date.toString() }
private fun hm(sec: Int): String { val m = sec / 60; return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m" }
private fun currentStreak(stats: List<DayStat>): Int {
    val active = stats.filter { it.blocks > 0 }.map { it.date }.toHashSet()
    var d = LocalDate.now(); var n = 0
    if (!active.contains(d.toString())) d = d.minusDays(1)   // today may not have fired yet
    while (active.contains(d.toString())) { n++; d = d.minusDays(1) }
    return n
}

private fun pickTime(context: Context, current: String, onSet: (String) -> Unit) {
    val parts = current.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    TimePickerDialog(context, { _, hh, mm -> onSet("%02d:%02d".format(hh, mm)) }, h, m, true).show()
}

/** Live permission status that refreshes whenever the app returns to foreground (F1.3). */
@Composable
private fun rememberLiveStatus(check: () -> Boolean): Boolean {
    val owner = LocalLifecycleOwner.current
    var value by remember { mutableStateOf(check()) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) value = check() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return value
}

/* ---------------------------------------------------------------- onboarding */

@Composable
private fun WelcomeScreen(router: Router, settings: AppSettings, vm: AppViewModel) {
    val c = col()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Emblem(settings.logo, 48.dp)
            Spacer(Modifier.height(18.dp))
            Serif("Lor Focus", 42)
            Spacer(Modifier.height(14.dp))
            Text("Take back your attention.", color = c.muted, fontSize = 16.sp, lineHeight = 26.sp)
            Spacer(Modifier.height(16.dp))
            Text("Choose your emblem →", color = c.focus, fontSize = 14.sp,
                modifier = Modifier.clickable { router.go(Dest.LOGO) })
        }
        PrimaryButton("Get started") { router.go(Dest.HOWWORKS) }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("I've used Lor Focus before", color = c.muted, fontSize = 13.sp,
                modifier = Modifier.clickable { vm.completeOnboarding(); router.reset(Dest.TODAY) })
        }
    }
}

@Composable
private fun HowItWorksScreen(router: Router) {
    val c = col()
    Body(top = 44, bottom = { PrimaryButton("Continue") { router.go(Dest.PERMISSIONS) } }) {
        Serif("How it works", 32)
        Spacer(Modifier.height(40.dp))
        val steps = listOf(
            Icons.Rounded.Block to ("Block the feeds" to "Shorts, Reels and Spotlight simply don't open."),
            Icons.Rounded.Schedule to ("Add gentle friction" to "A short pause, so the choice is yours again."),
            Icons.Rounded.BarChart to ("Reclaim your time" to "See the hours come back, quietly."),
        )
        steps.forEach { (icon, txt) ->
            Row(Modifier.padding(bottom = 34.dp)) {
                Icon(icon, null, tint = c.focus, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(18.dp))
                Column {
                    Text(txt.first, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(txt.second, fontSize = 14.sp, color = c.muted, lineHeight = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun PermissionsScreen(router: Router, vm: AppViewModel) {
    val c = col()
    val ctx = LocalContext.current
    val access = rememberLiveStatus { Permissions.accessibilityEnabled(ctx) }
    val overlay = rememberLiveStatus { Permissions.overlayGranted(ctx) }
    val usage = rememberLiveStatus { Permissions.usageGranted(ctx) }

    @Composable
    fun permCard(title: String, desc: String, on: Boolean, open: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
                Spacer(Modifier.height(5.dp))
                Text(desc, fontSize = 13.sp, color = c.muted, lineHeight = 20.sp)
            }
            if (on) Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = c.focus, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp)); Text("On", color = c.focus, fontSize = 13.sp)
            } else Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).border(1.dp, c.hair, RoundedCornerShape(17.dp))
                    .clickable { open() }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Enable", color = c.ink, fontSize = 13.sp) }
        }
    }

    Body(top = 44, bottom = {
        PrimaryButton("Continue", enabled = access) { vm.completeOnboarding(); router.reset(Dest.TODAY) }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("You can turn any of these off later.", color = c.faint, fontSize = 12.sp)
        }
    }) {
        Serif("Three permissions", 32)
        Spacer(Modifier.height(10.dp))
        Text("Android asks for these so Lor Focus can notice a feed opening. Nothing leaves your phone.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(32.dp))
        permCard("Accessibility", "Lets us see which screen you've opened.", access) { Permissions.openAccessibility(ctx) }
        Spacer(Modifier.height(12.dp))
        permCard("Display over other apps", "Shows the calm screen in place of the feed.", overlay) { Permissions.openOverlay(ctx) }
        Spacer(Modifier.height(12.dp))
        permCard("Usage access", "Counts the minutes you get back.", usage) { Permissions.openUsage(ctx) }
    }
}

@Composable
private fun PickFeedsScreen(router: Router, vm: AppViewModel) {
    val c = col()
    val feeds by vm.feeds.collectAsState()
    Body(top = 44, bottom = { PrimaryButton("Continue") { router.go(Dest.APPROACH) } }) {
        Serif("Pick what to quiet", 32)
        Spacer(Modifier.height(10.dp))
        Text("Only the endless parts. The rest of each app stays exactly as it is.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(26.dp))
        feeds.forEach { f ->
            HairlineRow(onClick = { vm.setEnabled(f.id, !f.enabled) }) {
                Mono(f.mono); Spacer(Modifier.width(16.dp))
                Text(f.name, Modifier.weight(1f), fontSize = 15.sp, color = c.ink)
                Toggle(f.enabled) { vm.setEnabled(f.id, !f.enabled) }
            }
        }
    }
}

@Composable
private fun ApproachScreen(router: Router, vm: AppViewModel) {
    val c = col()
    val feeds by vm.feeds.collectAsState()
    var mode by remember { mutableStateOf(FeedMode.PAUSE) }
    val defs = listOf(
        Triple(FeedMode.BLOCK, "Block fully", "The feed simply does not open."),
        Triple(FeedMode.PAUSE, "Add a pause", "A few slow seconds first, then your choice."),
        Triple(FeedMode.LIMIT, "Daily time limit", "A small budget each day, then it rests."),
    )
    Body(top = 44, bottom = {
        PrimaryButton("Done") {
            feeds.forEach { vm.setMode(it.id, mode) }
            vm.completeOnboarding()
            router.reset(Dest.TODAY)
        }
    }) {
        Serif("Choose your approach", 32)
        Spacer(Modifier.height(10.dp))
        Text("Applies to all your feeds now. You can change any of them later.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(28.dp))
        defs.forEach { (m, name, desc) ->
            val sel = mode == m
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp))
                    .background(if (sel) c.focus.copy(alpha = .09f) else c.card)
                    .border(if (sel) 1.2.dp else 1.dp, if (sel) c.focus.copy(alpha = .4f) else c.line, RoundedCornerShape(16.dp))
                    .clickable { mode = m }.padding(22.dp),
            ) {
                Radio(sel); Spacer(Modifier.width(16.dp))
                Column {
                    Text(name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(desc, fontSize = 13.sp, color = c.muted, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
private fun Radio(selected: Boolean) {
    val c = col()
    Box(
        Modifier.size(20.dp).clip(CircleShape).border(1.4.dp, if (selected) c.focus else c.hair, CircleShape),
        contentAlignment = Alignment.Center,
    ) { if (selected) Box(Modifier.size(9.dp).clip(CircleShape).background(c.focus)) }
}

/* ---------------------------------------------------------------- logo picker */

@Composable
private fun LogoPickerScreen(router: Router, settings: AppSettings, vm: AppViewModel) {
    val c = col()
    val fromOnboard = !settings.onboarded
    val done = { if (fromOnboard) router.reset(Dest.WELCOME) else router.back() }
    Body(top = 26) {
        TopBar("Your emblem") { done() }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Emblem(settings.logo, 64.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Serif("The honey badger", 18)
                Text("Small, stubborn, unbothered. Pick the one you like.", color = c.muted, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Badger.values().toList().chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { b ->
                    val sel = settings.logo == b.id
                    Column(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                            .background(if (sel) c.focus.copy(alpha = .09f) else c.card)
                            .border(if (sel) 1.4.dp else 1.dp, if (sel) c.focus else c.line, RoundedCornerShape(20.dp))
                            .clickable { vm.setLogo(b.id) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Emblem(b.id, 52.dp)
                        Spacer(Modifier.height(12.dp))
                        Text(b.label, fontSize = 12.sp, color = if (sel) c.focus else c.muted)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton("Use this one") { done() }
    }
}

/* ---------------------------------------------------------------- main app */

@Composable
private fun TodayScreen(router: Router, vm: AppViewModel, settings: AppSettings) {
    val c = col()
    val today by vm.todayStat.collectAsState()
    val recent by vm.recentStats.collectAsState()
    val blockedApps by vm.blockedApps.collectAsState()
    val reclaimedMin = (today?.reclaimedSec ?: 0) / 60
    val blocks = today?.blocks ?: 0
    val streak = currentStreak(recent)
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"))
    Body(top = 32) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Emblem(settings.logo, 34.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Lor Focus", fontSize = 14.sp, color = c.ink)
                Text(dateText, fontSize = 12.sp, color = c.faint)
            }
            Icon(Icons.Rounded.Settings, "Settings", tint = c.muted,
                modifier = Modifier.size(22.dp).clip(CircleShape).clickable { router.go(Dest.SETTINGS) })
        }
        Spacer(Modifier.height(44.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Serif("$reclaimedMin", 92); Spacer(Modifier.width(8.dp))
            Text("min", color = c.muted, fontSize = 22.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("reclaimed today", color = c.muted, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (blocks > 0) "That's time you didn't lose to Shorts." else "All quiet so far — open YouTube Shorts to see it work.",
            color = c.focus, fontSize = 15.sp, lineHeight = 24.sp,
        )

        Spacer(Modifier.height(44.dp))
        Text(
            if (streak > 0) "$streak quiet ${if (streak == 1) "day" else "days"} in a row" else "No streak yet",
            color = c.faint, fontSize = 12.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (6 downTo 0).forEach { off ->
                val d = LocalDate.now().minusDays(off.toLong())
                val active = (statOn(recent, d)?.blocks ?: 0) > 0
                val isToday = off == 0
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dot = Modifier.size(26.dp).clip(CircleShape)
                    when {
                        active -> Box(dot.background(c.focus))
                        isToday -> Box(dot.border(1.4.dp, c.focus, CircleShape))
                        else -> Box(dot.background(c.ink.copy(alpha = .07f)))
                    }
                    Text(d.dayOfWeek.name.first().toString(), fontSize = 11.sp, color = if (isToday) c.ink else c.faint)
                }
            }
        }

        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Scrolls stopped", "$blocks", Modifier.weight(1f))
            StatCard("Apps blocked", "${blockedApps.size}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Card(onClick = { router.tab(Dest.FOCUS) }) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CenterFocusStrong, null, tint = c.focus, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text("Start a Focus session", Modifier.weight(1f), fontSize = 14.sp, color = c.ink)
                Icon(Icons.Rounded.ChevronRight, null, tint = c.faint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Card(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val c = col()
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) { content() }
}

@Composable
private fun RulesScreen(router: Router, vm: AppViewModel) {
    val c = col()
    val feeds by vm.feeds.collectAsState()
    val blocked by vm.blockedApps.collectAsState()
    val blockedMap = remember(blocked) { blocked.associateBy { it.pkg } }
    val shortsOn = feeds.firstOrNull { it.id == "ys" }?.enabled ?: true
    val apps by vm.installedApps.collectAsState()
    LaunchedEffect(Unit) { vm.ensureAppsLoaded() }
    Body(top = 40) {
        Serif("Rules", 32)
        Spacer(Modifier.height(8.dp))
        Text("Block YouTube Shorts anytime, and tap any app to choose how it's blocked.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Block YouTube Shorts", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
                Spacer(Modifier.height(5.dp))
                Text("A short pause, then back to full videos.", fontSize = 13.sp, color = c.muted, lineHeight = 20.sp)
            }
            Spacer(Modifier.width(16.dp))
            Toggle(shortsOn) { vm.setEnabled("ys", !shortsOn) }
        }
        Spacer(Modifier.height(30.dp))
        Eyebrow("Your apps"); Spacer(Modifier.height(4.dp))
        Text("${blockedMap.size} blocked · tap an app to set Off, Always, During Focus, or a daily limit.",
            color = c.faint, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        if (apps.isEmpty()) {
            Text("Loading your apps…", color = c.faint, fontSize = 14.sp)
        } else apps.forEach { app ->
            val b = blockedMap[app.pkg]
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .clickable { router.go(Dest.APPDETAIL, app.pkg) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (app.icon != null) {
                    Image(app.icon.asImageBitmap(), app.label, Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)))
                } else {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(c.chip), contentAlignment = Alignment.Center) {
                        Text(app.label.take(1).uppercase(), color = c.muted, fontSize = 15.sp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(app.label, fontSize = 15.sp, color = if (b != null) c.ink else c.muted, maxLines = 1)
                    Spacer(Modifier.height(3.dp))
                    Text(appModeSummary(b), fontSize = 12.sp, color = if (b != null) c.focus else c.faint)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = c.faint, modifier = Modifier.size(18.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        }
    }
}

private fun appModeSummary(b: BlockedApp?): String = when (b?.mode) {
    "always" -> "Blocked always"
    "focus" -> "During Focus"
    "limit" -> "${b.limitMinutes} min a day"
    else -> "Off"
}

@Composable
private fun AppDetailScreen(router: Router, vm: AppViewModel) {
    val c = col()
    val pkg = router.editing ?: return
    val blocked by vm.blockedApps.collectAsState()
    val current = blocked.firstOrNull { it.pkg == pkg }
    val app = vm.appFor(pkg)
    val label = app?.label ?: current?.label ?: pkg
    var mode by remember(current?.mode) { mutableStateOf(current?.mode ?: "off") }
    var limit by remember(current?.limitMinutes) { mutableStateOf(current?.limitMinutes ?: 30) }
    val options = listOf(
        Triple("off", "Off", "Runs normally."),
        Triple("focus", "During Focus", "Blocked only while a Focus session runs."),
        Triple("always", "Always", "Blocked all the time — app and its website."),
        Triple("limit", "Daily limit", "Use it for a few minutes a day, then it's blocked."),
    )
    Body(top = 26, bottom = { PrimaryButton("Save") { vm.setAppMode(pkg, label, mode, limit); router.back() } }) {
        TopBar("Block mode") { router.back() }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (app?.icon != null) {
                Image(app.icon.asImageBitmap(), label, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
            } else {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(c.chip), contentAlignment = Alignment.Center) {
                    Text(label.take(1).uppercase(), color = c.muted, fontSize = 18.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(label, fontSize = 18.sp, color = c.ink)
        }
        Spacer(Modifier.height(28.dp))
        options.forEach { (id, title, desc) ->
            val sel = mode == id
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (sel) c.focus.copy(alpha = .09f) else c.card)
                    .border(if (sel) 1.2.dp else 1.dp, if (sel) c.focus.copy(alpha = .4f) else c.line, RoundedCornerShape(14.dp))
                    .clickable { mode = id }.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Radio(sel); Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
                    Spacer(Modifier.height(4.dp))
                    Text(desc, fontSize = 13.sp, color = c.muted, lineHeight = 19.sp)
                }
            }
        }
        if (mode == "limit") {
            Spacer(Modifier.height(14.dp))
            Eyebrow("Minutes a day"); Spacer(Modifier.height(12.dp))
            Stepper(value = "$limit", unit = "min",
                onMinus = { limit = (limit - 5).coerceAtLeast(5) }, onPlus = { limit = (limit + 5).coerceAtMost(240) })
            Spacer(Modifier.height(12.dp))
            Text("Uses Usage access (granted in setup) to count time.", color = c.faint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun RuleDetailScreen(router: Router, vm: AppViewModel) {
    val c = col()
    val ctx = LocalContext.current
    val feeds by vm.feeds.collectAsState()
    val f = feeds.firstOrNull { it.id == router.editing } ?: feeds.firstOrNull() ?: return
    Body(top = 26, bottom = { PrimaryButton("Save") { router.back() } }) {
        TopBar(f.name) { router.back() }
        Spacer(Modifier.height(34.dp))
        Eyebrow("Mode"); Spacer(Modifier.height(14.dp))
        listOf(FeedMode.BLOCK to "Block fully", FeedMode.PAUSE to "Add a pause", FeedMode.LIMIT to "Daily time limit").forEach { (m, label) ->
            val sel = f.mode == m
            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) c.focus.copy(alpha = .09f) else c.card)
                    .border(if (sel) 1.2.dp else 1.dp, if (sel) c.focus.copy(alpha = .4f) else c.line, RoundedCornerShape(12.dp))
                    .clickable { vm.setMode(f.id, m) }.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { Radio(sel); Spacer(Modifier.width(14.dp)); Text(label, fontSize = 15.sp, color = c.ink) }
        }
        Spacer(Modifier.height(28.dp))
        when (f.mode) {
            FeedMode.LIMIT -> {
                Eyebrow("Daily budget"); Spacer(Modifier.height(14.dp))
                Stepper(value = "${f.budgetMinutes}", unit = "min",
                    onMinus = { vm.stepBudget(f.id, -5) }, onPlus = { vm.stepBudget(f.id, 5) })
                Spacer(Modifier.height(38.dp))
                Eyebrow("Active hours"); Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeField("From", f.activeFrom, Modifier.weight(1f)) { pickTime(ctx, f.activeFrom) { vm.setHours(f.id, it, f.activeUntil) } }
                    TimeField("Until", f.activeUntil, Modifier.weight(1f)) { pickTime(ctx, f.activeUntil) { vm.setHours(f.id, f.activeFrom, it) } }
                }
            }
            FeedMode.PAUSE -> {
                Eyebrow("Pause length"); Spacer(Modifier.height(14.dp))
                Stepper(value = "${f.pauseSeconds}", unit = "sec",
                    onMinus = { vm.stepPause(f.id, -1) }, onPlus = { vm.stepPause(f.id, 1) })
            }
            FeedMode.BLOCK -> Card {
                Row(Modifier.padding(20.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = c.focus, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(14.dp))
                    Text("${f.name} won't open at all. A calm screen shows instead.",
                        fontSize = 13.sp, color = c.muted, lineHeight = 21.sp)
                }
            }
        }
    }
}

@Composable
private fun Stepper(value: String, unit: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val c = col()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StepBtn(Icons.Rounded.Remove, onMinus)
        Row(verticalAlignment = Alignment.Bottom) {
            Serif(value, 38); Spacer(Modifier.width(6.dp))
            Text(unit, color = c.muted, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        StepBtn(Icons.Rounded.Add, onPlus)
    }
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val c = col()
    Box(
        Modifier.size(38.dp).clip(CircleShape).border(1.2.dp, c.hair, CircleShape).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = c.ink, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val c = col()
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.card).border(1.dp, c.line, RoundedCornerShape(12.dp))
            .clickable { onClick() }.padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { Eyebrow(label); Text(value, fontSize = 17.sp, color = c.ink) }
}

@Composable
private fun FocusScreen(router: Router, settings: AppSettings, vm: AppViewModel) {
    val c = col()
    val blocked by vm.blockedApps.collectAsState()
    var tick by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val now = remember(tick) { System.currentTimeMillis() }
    val active = settings.focusEndsAt > now
    val remainingSec = (settings.focusEndsAt - now).coerceAtLeast(0L) / 1000
    Body(top = 40, bottom = {
        if (active) PrimaryButton("End focus") { vm.endFocus() }
        else PrimaryButton("Start focus") { vm.startFocus(settings.focusDurationMin) }
    }) {
        Serif("Focus", 32)
        Spacer(Modifier.height(10.dp))
        Text("A timer that blocks the apps you picked in Rules — and their sites in a browser — until it ends. YouTube Shorts stay blocked too. Pick apps in Rules.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(34.dp))
        if (active) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Serif(String.format("%d:%02d", remainingSec / 60, remainingSec % 60), 76)
                    Spacer(Modifier.height(8.dp))
                    Text("left in this session", color = c.muted, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(34.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                    .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Blocked right now", color = c.faint, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Text("YouTube Shorts · ${blocked.size} chosen app${if (blocked.size == 1) "" else "s"} · short-form in browsers",
                    color = c.ink, fontSize = 14.sp, lineHeight = 22.sp)
            }
        } else {
            Eyebrow("Session length"); Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 30, 45, 60, 90).forEach { m ->
                    val sel = settings.focusDurationMin == m
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.focus.copy(alpha = .12f) else c.card)
                            .border(if (sel) 1.2.dp else 1.dp, if (sel) c.focus else c.line, RoundedCornerShape(12.dp))
                            .clickable { vm.setFocusDuration(m) },
                        contentAlignment = Alignment.Center,
                    ) { Text("${m}m", color = if (sel) c.focus else c.ink, fontSize = 14.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Once started, the timer runs to the end — you can stop it here anytime. Pick a length you'll keep.",
                color = c.faint, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun DashedAction(text: String, onClick: () -> Unit) {
    val c = col()
    Box(
        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp))
            .border(1.2.dp, c.focus.copy(alpha = .45f), RoundedCornerShape(25.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = c.focus, fontSize = 14.sp) }
}

@Composable
private fun AddChannelScreen(router: Router, vm: AppViewModel) {
    val c = col()
    var name by remember { mutableStateOf("") }
    Body(top = 26, bottom = {
        PrimaryButton("Add to focus list", enabled = name.isNotBlank()) { vm.addChannel(name.trim()); router.back() }
    }) {
        TopBar("Add channel") { router.back() }
        Spacer(Modifier.height(28.dp))
        Text("Type the channel's name exactly as it shows under its videos on YouTube. In Learning mode, only channels on your list will play — everything else is blocked.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = c.faint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) Text("Channel name", color = c.faint, fontSize = 15.sp)
                BasicTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = c.ink, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(c.focus),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("e.g. \"Veritasium\" or \"3Blue1Brown\".", color = c.faint, fontSize = 12.sp)
    }
}

@Composable
private fun ScheduleScreen(router: Router) {
    val c = col()
    @Composable
    fun window(title: String, time: String, days: String, on: Boolean) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
                Spacer(Modifier.height(8.dp)); Serif(time, 27)
                Spacer(Modifier.height(4.dp)); Text(days, fontSize = 12.sp, color = c.faint)
            }
            Toggle(on) {}
        }
    }
    Body(top = 26, bottom = {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Outside these hours, YouTube behaves normally.", color = c.faint, fontSize = 12.sp)
        }
    }) {
        TopBar("Focus windows") { router.back() }
        Spacer(Modifier.height(26.dp))
        Text("Learning mode turns itself on during these hours.", color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(26.dp))
        window("Study hours", "9:00 – 18:00", "Mon – Fri", true)
        window("Evening reading", "20:00 – 21:30", "Every day", false)
        Spacer(Modifier.height(10.dp))
        DashedAction("+ Add a window") {}
    }
}

@Composable
private fun InsightsScreen(vm: AppViewModel) {
    val c = col()
    val recent by vm.recentStats.collectAsState()
    val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
    val vals = days.map { statOn(recent, it)?.reclaimedSec ?: 0 }
    val totalSec = vals.sum()
    val totalBlocks = days.sumOf { statOn(recent, it)?.blocks ?: 0 }
    val streak = currentStreak(recent)
    val maxV = vals.maxOrNull() ?: 0
    Body(top = 40) {
        Serif("Insights", 32)
        Spacer(Modifier.height(8.dp))
        Text("This week", color = c.muted, fontSize = 14.sp)
        Spacer(Modifier.height(40.dp))
        Serif(hm(totalSec), 58)
        Spacer(Modifier.height(14.dp))
        Text("reclaimed this week", color = c.muted, fontSize = 13.sp)
        Spacer(Modifier.height(34.dp))
        Row(Modifier.fillMaxWidth().height(170.dp), verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            vals.forEachIndexed { i, v ->
                val frac = if (maxV > 0) v.toFloat() / maxV else 0f
                if (frac <= 0f) Box(Modifier.weight(1f)) else Box(
                    Modifier.weight(1f).fillMaxHeight(frac).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (i == vals.lastIndex) c.focus else c.focus.copy(alpha = .28f)),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            days.forEachIndexed { i, d ->
                Text(d.dayOfWeek.name.first().toString(), Modifier.weight(1f),
                    color = if (i == days.lastIndex) c.ink else c.faint, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(36.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Scrolls stopped", "$totalBlocks", Modifier.weight(1f))
            StatCard("Current streak", if (streak == 1) "1 day" else "$streak days", Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (totalSec > 0) "${hm(totalSec)} back with you this week." else "Your numbers fill in as Lor Focus does its thing.",
            color = c.focus, fontSize = 14.sp, lineHeight = 22.sp,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier) {
    val c = col()
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(c.card).border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { Text(label, color = c.muted, fontSize = 12.sp); Serif(value, 33) }
}

@Composable
private fun SettingsScreen(router: Router, settings: AppSettings, vm: AppViewModel) {
    val c = col()
    Body(top = 26) {
        TopBar("Settings") { router.tab(Dest.TODAY) }
        Spacer(Modifier.height(34.dp))
        Eyebrow("Appearance"); Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(4.dp),
        ) {
            listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (id, label) ->
                val sel = settings.theme == id
                Box(
                    Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(9.dp))
                        .background(if (sel) c.paper else Color.Transparent).clickable { vm.setTheme(id) },
                    contentAlignment = Alignment.Center,
                ) { Text(label, fontSize = 14.sp, color = if (sel) c.ink else c.muted) }
            }
        }
        Spacer(Modifier.height(34.dp))
        Eyebrow("Your intention"); Spacer(Modifier.height(6.dp))
        HairlineRow(onClick = { router.go(Dest.LOGO) }) {
            Text("Emblem", Modifier.weight(1f), fontSize = 15.sp, color = c.ink)
            Emblem(settings.logo, 28.dp); Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.ChevronRight, null, tint = c.faint, modifier = Modifier.size(18.dp))
        }
        settingRow("Your goals", "1 written", c.faint) { router.go(Dest.GOALS) }
        settingRow("Strict mode", if (settings.strict) "On" else "Off", if (settings.strict) c.focus else c.faint) { router.go(Dest.STRICT) }
        settingRow("Notifications", "Daily, quiet", c.faint) {}
        Spacer(Modifier.height(34.dp))
        Eyebrow("About"); Spacer(Modifier.height(6.dp))
        settingRow("Privacy", "On-device", c.faint) {}
        settingRow("Diagnostics", "Detection", c.faint) { router.go(Dest.DIAG) }
        settingRow("About Lor Focus", "1.4.0", c.faint) {}
        HairlineRow(onClick = { vm.resetAll(); router.reset(Dest.WELCOME) }) {
            Text("Reset all data", Modifier.weight(1f), fontSize = 15.sp, color = c.quiet)
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Everything stays on this phone.", color = c.faint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ColumnScope.settingRow(label: String, right: String, rightColor: Color, onClick: () -> Unit) {
    val c = col()
    HairlineRow(onClick = onClick) {
        Text(label, Modifier.weight(1f), fontSize = 15.sp, color = c.ink)
        Text(right, color = rightColor, fontSize = 13.sp); Spacer(Modifier.width(14.dp))
        Icon(Icons.Rounded.ChevronRight, null, tint = c.faint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun StrictScreen(router: Router, settings: AppSettings, vm: AppViewModel) {
    val c = col()
    val ctx = LocalContext.current
    Body(top = 26, bottom = {
        PrimaryButton(if (settings.strict) "Keep strict mode on" else "Save") { router.back() }
    }) {
        TopBar("Strict mode") { router.back() }
        Spacer(Modifier.height(34.dp))
        Serif("Protecting the choice you already made.", 27)
        Spacer(Modifier.height(14.dp))
        Text("Turning Lor Focus off will wait ${settings.cooldownMinutes} minutes before it takes effect. " +
            "Long enough for the urge to pass; short enough that you're never stuck.",
            color = c.muted, fontSize = 14.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(34.dp))
        toggleCard("Strict mode", "Changes to rules take ${settings.cooldownMinutes} minutes.", settings.strict) { vm.setStrict(!settings.strict) }
        Spacer(Modifier.height(12.dp))
        toggleCard("Uninstall protection", "Optional. Same ${settings.cooldownMinutes}-minute wait.", settings.uninstall) {
            val turningOn = !settings.uninstall
            vm.setUninstall(turningOn)
            if (turningOn) DeviceAdmin.requestEnable(ctx) else DeviceAdmin.disable(ctx)
        }
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(20.dp),
        ) {
            Icon(Icons.Rounded.Info, null, tint = c.focus, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(14.dp))
            Text("Cooldown is ${settings.cooldownMinutes} minutes. You can change it any time — the change itself waits ${settings.cooldownMinutes} minutes too.",
                fontSize = 13.sp, color = c.muted, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun toggleCard(title: String, desc: String, on: Boolean, onToggle: () -> Unit) {
    val c = col()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
            .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = c.ink)
            Spacer(Modifier.height(5.dp)); Text(desc, fontSize = 13.sp, color = c.muted, lineHeight = 20.sp)
        }
        Spacer(Modifier.width(16.dp)); Toggle(on, onToggle)
    }
}

@Composable
private fun DiagnosticsScreen(router: Router) {
    val c = col()
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val pkg = remember(tick) { DetectionDiagnostics.lastPackage }
    val shorts = remember(tick) { DetectionDiagnostics.lastShorts }
    val reelCount = remember(tick) { DetectionDiagnostics.lastReelCount }
    val channel = remember(tick) { DetectionDiagnostics.lastChannel }
    val ids = remember(tick) { DetectionDiagnostics.interestingIds }
    Body(top = 26) {
        TopBar("Diagnostics") { router.back() }
        Spacer(Modifier.height(20.dp))
        Text("Open YouTube — a Short, then a normal video — come back here and screenshot this. It shows what the detector sees, so the signatures can be tuned to your device.",
            color = c.muted, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(Modifier.height(24.dp))
        diagRow("Foreground app", pkg)
        diagRow("Shorts detected", if (shorts) "yes" else "no")
        diagRow("Reel elements", "$reelCount  (blocks at 7+)")
        diagRow("Channel read", channel ?: "—")
        Spacer(Modifier.height(20.dp))
        Eyebrow("View-ids seen (live)"); Spacer(Modifier.height(10.dp))
        if (ids.isEmpty()) {
            Text("none yet — open YouTube, then return here", color = c.faint, fontSize = 13.sp)
        } else Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { ids.forEach { Text(it, color = c.ink, fontSize = 13.sp) } }
    }
}

@Composable
private fun ColumnScope.diagRow(label: String, value: String) {
    val c = col()
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, Modifier.weight(1f), color = c.muted, fontSize = 14.sp)
        Text(value, color = c.ink, fontSize = 14.sp)
    }
}

@Composable
private fun GoalsScreen(router: Router, settings: AppSettings, vm: AppViewModel) {
    val c = col()
    var text by remember { mutableStateOf(settings.goal) }
    Body(top = 26, bottom = { PrimaryButton("Save") { vm.saveGoal(text); router.back() } }) {
        TopBar("Your goals") { router.back() }
        Spacer(Modifier.height(40.dp))
        Serif("Why are you doing this?", 32)
        Spacer(Modifier.height(12.dp))
        Text("Write it in your own words. You'll see it on the quiet screens, when it helps most.",
            color = c.muted, fontSize = 14.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(30.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.card)
                .border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(22.dp),
        ) {
            BasicTextField(
                value = text, onValueChange = { if (it.length <= 140) text = it },
                textStyle = androidx.compose.ui.text.TextStyle(color = c.ink, fontSize = 18.sp, lineHeight = 29.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(c.focus),
                modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text("${text.length} / 140", Modifier.fillMaxWidth(), color = c.faint, fontSize = 11.sp, textAlign = TextAlign.End)
        }
    }
}
