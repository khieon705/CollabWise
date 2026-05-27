package com.collabwise.ui.project

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collabwise.data.model.Skill
import com.collabwise.data.model.SkillCategory
import com.collabwise.data.model.Task
import com.collabwise.data.model.TaskStatus
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalLocale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ── CreateTaskSheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskSheet(
    skillsGrouped: Map<SkillCategory, List<Skill>>,
    eligibleDependencies: List<Task>,
    isLoading: Boolean,
    createTaskError: String?,
    cycleError: String?,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        description: String,
        requiredSkillIds: List<String>,
        dueDate: String?,
        dependsOn: List<String>
    ) -> Unit
) {
    // ── Form state ────────────────────────────────────────────────────────────
    var title            by remember { mutableStateOf("") }
    var description      by remember { mutableStateOf("") }
    var selectedSkillIds by remember { mutableStateOf(setOf<String>()) }
    var selectedDepIds   by remember { mutableStateOf(setOf<String>()) }
    var dueDate          by remember { mutableStateOf<String?>(null) }

    // Section expand toggles
    var skillsExpanded by remember { mutableStateOf(false) }
    var depsExpanded   by remember { mutableStateOf(false) }

    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle       = {
            Surface(
                color  = Color(0xFFDADCE0),
                shape  = RoundedCornerShape(2.dp),
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Sheet header
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text       = "Add task",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(onClick = { if (!isLoading) onDismiss() }) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }

            // ── Errors ────────────────────────────────────────────────────────
            val errorToShow = cycleError ?: createTaskError
            if (errorToShow != null) {
                Card(
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFFCE8E8)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Warning,
                            contentDescription = null,
                            tint               = Color(0xFFC5221F),
                            modifier           = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text     = errorToShow,
                            color    = Color(0xFFC5221F),
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Title field ───────────────────────────────────────────────────
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it },
                label         = { Text("Task title *") },
                placeholder   = { Text("e.g. Design event poster") },
                singleLine    = true,
                isError       = createTaskError != null && title.isBlank(),
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // ── Description field ─────────────────────────────────────────────
            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text("Description (optional)") },
                placeholder   = { Text("Brief details about this task") },
                minLines      = 2,
                maxLines      = 3,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // ── Due date picker ───────────────────────────────────────────────
            SectionLabel("Due date")

            if (dueDate != null) {
                val formatter = DateTimeFormatter.ofPattern(
                    "EEE, MMM d yyyy",
                    LocalLocale.current.platformLocale
                )

                val formatted = LocalDate.parse(dueDate).format(formatter)

                FilterChip(
                    selected = true,
                    onClick = { showDatePicker = true },
                    label = { Text(formatted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { dueDate = null }
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

            } else {
                FilterChip(
                    selected = false,
                    onClick = { showDatePicker = true },
                    label = { Text("Pick due date") },
                    leadingIcon = {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // ── Required skills (collapsible) ─────────────────────────────────
            CollapsibleSection(
                title        = "Required skills",
                badge        = if (selectedSkillIds.isNotEmpty()) "${selectedSkillIds.size} selected" else null,
                expanded     = skillsExpanded,
                onToggle     = { skillsExpanded = !skillsExpanded },
                modifier     = Modifier.padding(bottom = 12.dp)
            ) {
                if (skillsGrouped.isEmpty()) {
                    Text(
                        "No skills available.",
                        fontSize = 13.sp,
                        color    = Color(0xFF5F6368),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    skillsGrouped.forEach { (category, skills) ->
                        Text(
                            text     = category.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color    = Color(0xFF5F6368),
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                        )
                        SkillChipRow(
                            skills           = skills,
                            selectedSkillIds = selectedSkillIds,
                            onToggle         = { skillId ->
                                selectedSkillIds = if (skillId in selectedSkillIds)
                                    selectedSkillIds - skillId
                                else
                                    selectedSkillIds + skillId
                            }
                        )
                    }
                }
            }

            // ── Dependencies (collapsible) ────────────────────────────────────
            if (eligibleDependencies.isNotEmpty()) {
                CollapsibleSection(
                    title    = "Depends on",
                    badge    = if (selectedDepIds.isNotEmpty()) "${selectedDepIds.size} selected" else null,
                    expanded = depsExpanded,
                    onToggle = { depsExpanded = !depsExpanded },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text     = "This task will be BLOCKED until all selected tasks are DONE.",
                        fontSize = 12.sp,
                        color    = Color(0xFF5F6368),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    eligibleDependencies.forEach { dep ->
                        DependencyCheckRow(
                            task      = dep,
                            checked   = dep.id in selectedDepIds,
                            onChecked = { checked ->
                                selectedDepIds = if (checked)
                                    selectedDepIds + dep.id
                                else
                                    selectedDepIds - dep.id
                            }
                        )
                    }
                }
            }

            // ── Auto-assignment notice ────────────────────────────────────────
            Card(
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.AutoAwesome, null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text     = "This task will be automatically assigned to the best-" +
                                "skill-matched member using the Skill-Based Greedy " +
                                "Task Scheduling Algorithm.",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Submit button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    if (title.isNotBlank() && dueDate != null) {
                        onCreate(
                            title,
                            description,
                            selectedSkillIds.toList(),
                            dueDate,
                            selectedDepIds.toList()
                        )
                    }
                },
                enabled  = title.isNotBlank() && !isLoading,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Add task", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color    = Color(0xFF5F6368),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun CollapsibleSection(
    title: String,
    badge: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors   = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceContainer),
        shape    = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                if (badge != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            badge, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = Color(0xFF5F6368)
                )
            }
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        content = content
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillChipRow(
    skills: List<Skill>,
    selectedSkillIds: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.fillMaxWidth()
    ) {
        skills.forEach { skill ->
            val selected = skill.id in selectedSkillIds
            FilterChip(
                selected = selected,
                onClick  = { onToggle(skill.id) },
                label    = { Text(skill.name, fontSize = 12.sp) },
                leadingIcon = if (selected) {
                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(13.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,

                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface,

                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun DependencyCheckRow(
    task: Task,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onChecked(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked         = checked,
            onCheckedChange = onChecked,
            colors          = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(top = 2.dp)
            ) {
                Surface(
                    color = taskStatusBg(task.status) as Color,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text     = task.status,
                        fontSize = 10.sp,
                        color    = taskStatusColor(task.status) as Color,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                task.assignedMemberName.takeIf { it.isNotBlank() }?.let { name ->
                    Text(
                        " · $name",
                        fontSize = 11.sp,
                        color    = Color(0xFF5F6368)
                    )
                }
            }
        }
    }
}

private fun taskStatusBg(status: String) = when (status) {
    TaskStatus.AVAILABLE.name   -> Color(0xFFE8F0FE)
    TaskStatus.IN_PROGRESS.name -> Color(0xFFE6F4EA)
    TaskStatus.LOCKED.name     -> Color(0xFFFEF3DC)
    TaskStatus.DONE.name        -> Color(0xFFF1F3F4)
    else -> {}
}

private fun taskStatusColor(status: String) = when (status) {
    TaskStatus.AVAILABLE.name   -> Color(0xFF1967D2)
    TaskStatus.IN_PROGRESS.name -> Color(0xFF137333)
    TaskStatus.LOCKED.name     -> Color(0xFFB06000)
    TaskStatus.DONE.name        -> Color(0xFF5F6368)
    else -> {}
}