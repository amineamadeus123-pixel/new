package com.amine.coachhabitudes

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : ComponentActivity() {
    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { CoachHabitudesApp(this) }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("coach_habitudes", "Rappels Coach Habitudes", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Notifications qui rappellent les tâches du planning"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

enum class TaskStatus { TODO, DONE, LATER, SKIPPED }

data class HabitTask(
    val id: Int,
    var time: String,
    var title: String,
    var subtitle: String,
    var category: String,
    var icon: String,
    var status: TaskStatus = TaskStatus.TODO,
    var postponedCount: Int = 0
)

object LocalStorage {
    private const val PREF = "coach_habitudes_storage"
    private const val KEY_TASKS = "tasks_json"
    private const val KEY_DATE = "last_date"

    fun load(context: Context): MutableList<HabitTask> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_DATE, null)
        val raw = prefs.getString(KEY_TASKS, null)
        val tasks = if (raw.isNullOrBlank()) defaultTasks().toMutableList() else parseTasks(raw)
        // Chaque nouveau jour, on garde le planning mais on remet les statuts à zéro.
        if (savedDate != today) {
            tasks.forEach { it.status = TaskStatus.TODO; it.postponedCount = 0 }
            save(context, tasks)
        }
        prefs.edit().putString(KEY_DATE, today).apply()
        return tasks
    }

    fun save(context: Context, tasks: List<HabitTask>) {
        val arr = JSONArray()
        tasks.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id); put("time", it.time); put("title", it.title); put("subtitle", it.subtitle)
                put("category", it.category); put("icon", it.icon); put("status", it.status.name); put("postponedCount", it.postponedCount)
            })
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY_TASKS, arr.toString())
            .putString(KEY_DATE, LocalDate.now().toString())
            .apply()
    }

    private fun parseTasks(raw: String): MutableList<HabitTask> {
        val arr = JSONArray(raw)
        val list = mutableListOf<HabitTask>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(HabitTask(
                id = o.optInt("id", i + 1),
                time = o.optString("time", "08:00"),
                title = o.optString("title", "Tâche"),
                subtitle = o.optString("subtitle", ""),
                category = o.optString("category", "Perso"),
                icon = o.optString("icon", "✅"),
                status = runCatching { TaskStatus.valueOf(o.optString("status", "TODO")) }.getOrDefault(TaskStatus.TODO),
                postponedCount = o.optInt("postponedCount", 0)
            ))
        }
        return list.sortedBy { it.time }.toMutableList()
    }
}

fun defaultTasks() = listOf(
    HabitTask(1,"07:00","Réveil","Se lever sans rester au lit","Santé","🌅"),
    HabitTask(2,"07:10","Boire un verre d’eau","Commencer doucement la journée","Santé","💧"),
    HabitTask(3,"07:30","Petit-déjeuner","Prendre un repas simple","Santé","🥣"),
    HabitTask(4,"08:00","Définir 3 objectifs","Choisir 3 actions réalistes pour aujourd’hui","Productivité","🎯"),
    HabitTask(5,"12:30","Déjeuner","Faire une vraie pause","Santé","🍽️"),
    HabitTask(6,"18:30","Bouger / marcher / sport","Même 10 minutes suffisent pour commencer","Santé","🏃"),
    HabitTask(7,"20:00","Projet perso / formation","Avancer 30 minutes, sans chercher parfait","Productivité","📚"),
    HabitTask(8,"21:30","Préparer demain","Noter la première action de demain","Organisation","📝"),
    HabitTask(9,"22:30","Routine sommeil","Réduire les écrans et préparer le repos","Sommeil","🌙")
)

@Composable
fun CoachHabitudesApp(activity: ComponentActivity) {
    var tab by remember { mutableIntStateOf(0) }
    var sleepMode by remember { mutableStateOf(false) }
    val tasks = remember { mutableStateListOf<HabitTask>() }

    LaunchedEffect(Unit) {
        tasks.clear(); tasks.addAll(LocalStorage.load(activity))
        ReminderScheduler.scheduleToday(activity, tasks)
    }

    fun persistAndReschedule() {
        val sorted = tasks.sortedBy { it.time }
        tasks.clear(); tasks.addAll(sorted)
        LocalStorage.save(activity, tasks)
        ReminderScheduler.scheduleToday(activity, tasks)
    }

    LaunchedEffect(sleepMode) {
        if (sleepMode) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    val bg = if (sleepMode) Color(0xFF050505) else Color(0xFFF4F6F8)
    val fg = if (sleepMode) Color(0xFFBDBDBD) else Color(0xFF1F2937)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize().background(bg), color = bg) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Header(sleepMode, onSleepToggle = { sleepMode = !sleepMode }, fg)
                Spacer(Modifier.height(16.dp))
                if (sleepMode) SleepModeScreen(tasks, fg) else {
                    when(tab) {
                        0 -> TodayScreen(tasks, onChange = { persistAndReschedule() })
                        1 -> PlanningEditorScreen(tasks, onChange = { persistAndReschedule() })
                        2 -> StatsScreen(tasks)
                    }
                    Spacer(Modifier.weight(1f))
                    NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("🏠") }, label = { Text("Aujourd’hui") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("✏️") }, label = { Text("Planning") })
                        NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("📊") }, label = { Text("Stats") })
                    }
                }
            }
        }
    }
}

@Composable fun Header(sleepMode: Boolean, onSleepToggle: () -> Unit, fg: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("Coach Habitudes", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = fg)
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)), color = fg.copy(alpha=.75f))
        }
        Button(onClick = onSleepToggle, colors = ButtonDefaults.buttonColors(containerColor = if (sleepMode) Color.DarkGray else Color(0xFF111827))) {
            Text(if (sleepMode) "☀️ Réveiller" else "🌙 Mode veille")
        }
    }
}

fun currentTask(tasks: List<HabitTask>): HabitTask = tasks.firstOrNull { it.status == TaskStatus.TODO || it.status == TaskStatus.LATER } ?: tasks.lastOrNull() ?: HabitTask(0,"--:--","Aucune tâche","Ajoute une tâche dans Planning","Perso","✅")
fun nextTask(tasks: List<HabitTask>, current: HabitTask): HabitTask? = tasks.dropWhile { it.id != current.id }.drop(1).firstOrNull { it.status == TaskStatus.TODO || it.status == TaskStatus.LATER }

@Composable fun TodayScreen(tasks: MutableList<HabitTask>, onChange: () -> Unit) {
    if (tasks.isEmpty()) { Text("Aucune tâche. Va dans Planning pour en ajouter.", fontSize = 24.sp); return }
    val done = tasks.count { it.status == TaskStatus.DONE }
    val current = currentTask(tasks)
    val progress = done.toFloat() / tasks.size
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Bonjour Amine 👋", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(14.dp), color = Color(0xFF16A34A))
        Text("$done tâches faites sur ${tasks.size}", modifier = Modifier.padding(8.dp))
        Spacer(Modifier.height(24.dp))
        TaskCard(current)
        Spacer(Modifier.height(18.dp))
        BigActionButton("✅ FAIT", Color(0xFF16A34A)) { current.status = TaskStatus.DONE; onChange() }
        BigActionButton("⏰ PLUS TARD", Color(0xFF2563EB)) { current.status = TaskStatus.LATER; current.postponedCount++; onChange() }
        BigActionButton("❌ IGNORER", Color(0xFFDC2626)) { current.status = TaskStatus.SKIPPED; onChange() }
        nextTask(tasks, current)?.let { Text("Prochaine tâche : ${it.time} ${it.title}", fontSize = 18.sp, modifier = Modifier.padding(top=16.dp)) }
    }
}

@Composable fun TaskCard(task: HabitTask) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(28.dp), elevation = CardDefaults.cardElevation(6.dp), modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Maintenant", fontSize = 18.sp, color = Color.Gray)
            Text("${task.icon} ${task.title}", fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(task.subtitle, fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top=12.dp))
            Text("${task.time} · ${task.category}", fontSize = 18.sp, color = Color.Gray, modifier = Modifier.padding(top=12.dp))
            if (task.postponedCount >= 2) Text("Tu as déjà repoussé cette tâche. Commence seulement par 5 minutes.", color = Color(0xFFB45309), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top=12.dp))
        }
    }
}

@Composable fun BigActionButton(label: String, color: Color, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = color), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().height(76.dp).padding(vertical=6.dp)) {
        Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable fun PlanningEditorScreen(tasks: MutableList<HabitTask>, onChange: () -> Unit) {
    var editing by remember { mutableStateOf<HabitTask?>(null) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Planning", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { editing = HabitTask((tasks.maxOfOrNull { it.id } ?: 0) + 1, "09:00", "Nouvelle tâche", "Décris l’action à faire", "Perso", "✅") }) { Text("+ Ajouter") }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn { items(tasks, key={it.id}) { task ->
            val icon = when(task.status) { TaskStatus.DONE -> "✅"; TaskStatus.LATER -> "⏰"; TaskStatus.SKIPPED -> "❌"; TaskStatus.TODO -> "⬜" }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical=6.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("$icon ${task.time}", fontSize = 21.sp, modifier = Modifier.width(115.dp))
                    Column(Modifier.weight(1f)) { Text("${task.icon} ${task.title}", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(task.subtitle, color = Color.Gray) }
                    TextButton(onClick = { editing = task.copy() }) { Text("Modifier") }
                    TextButton(onClick = { tasks.removeIf { it.id == task.id }; onChange() }) { Text("Supprimer", color = Color(0xFFDC2626)) }
                }
            }
        } }
    }
    editing?.let { draft -> EditTaskDialog(draft, onDismiss={ editing=null }, onSave={ saved ->
        val idx = tasks.indexOfFirst { it.id == saved.id }
        if (idx >= 0) tasks[idx] = saved else tasks.add(saved)
        editing = null
        onChange()
    }) }
}

@Composable fun EditTaskDialog(task: HabitTask, onDismiss: () -> Unit, onSave: (HabitTask) -> Unit) {
    var time by remember { mutableStateOf(task.time) }
    var title by remember { mutableStateOf(task.title) }
    var subtitle by remember { mutableStateOf(task.subtitle) }
    var category by remember { mutableStateOf(task.category) }
    var icon by remember { mutableStateOf(task.icon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la tâche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(time, { time = it }, label = { Text("Heure ex: 20:00") }, singleLine = true)
                OutlinedTextField(title, { title = it }, label = { Text("Titre") }, singleLine = true)
                OutlinedTextField(subtitle, { subtitle = it }, label = { Text("Description") })
                OutlinedTextField(category, { category = it }, label = { Text("Catégorie") }, singleLine = true)
                OutlinedTextField(icon, { icon = it }, label = { Text("Icône") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(task.copy(time=time, title=title, subtitle=subtitle, category=category, icon=icon)) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable fun StatsScreen(tasks: List<HabitTask>) {
    val done = tasks.count { it.status == TaskStatus.DONE }
    val skipped = tasks.count { it.status == TaskStatus.SKIPPED }
    val later = tasks.count { it.status == TaskStatus.LATER }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Statistiques du jour", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("✅ Réalisées : $done / ${tasks.size}", fontSize = 26.sp)
        Text("⏰ Reportées : $later", fontSize = 26.sp)
        Text("❌ Ignorées : $skipped", fontSize = 26.sp)
    }
}

@Composable fun SleepModeScreen(tasks: List<HabitTask>, fg: Color) {
    val current = currentTask(tasks)
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")), color = fg, fontSize = 64.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(36.dp))
        Text("Prochaine action", color = fg.copy(alpha=.7f), fontSize = 22.sp)
        Text("${current.icon} ${current.title}", color = fg, fontSize = 34.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(current.subtitle, color = fg.copy(alpha=.8f), fontSize = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(20.dp))
        Text("Toucher 🌙 Mode veille pour revenir", color = fg.copy(alpha=.5f), fontSize = 16.sp)
    }
}
