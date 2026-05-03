package com.carcomplianceapp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carcomplianceapp.domain.model.*
import com.carcomplianceapp.ui.theme.AppColors

// ── Urgency badge ─────────────────────────────────────────────────────────────

@Composable
fun UrgencyBadge(urgency: UrgencyLevel) {
    val (bg, fg, text) = when (urgency) {
        UrgencyLevel.CRITICAL -> Triple(AppColors.Red50, AppColors.Red800, "Overdue")
        UrgencyLevel.HIGH     -> Triple(AppColors.Amber50, AppColors.Amber800, "This week")
        UrgencyLevel.MEDIUM   -> Triple(AppColors.Amber50, AppColors.Amber600, "Soon")
        UrgencyLevel.LOW      -> Triple(AppColors.Teal50, AppColors.Teal800, "Planned")
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

// ── Category badge ────────────────────────────────────────────────────────────

@Composable
fun CategoryBadge(category: TaskCategory) {
    val (bg, fg) = when (category) {
        TaskCategory.LEGAL         -> Pair(AppColors.Blue50, AppColors.Blue800)
        TaskCategory.INSURANCE     -> Pair(AppColors.Teal50, AppColors.Teal800)
        TaskCategory.MAINTENANCE   -> Pair(AppColors.Gray50, AppColors.Gray800)
        TaskCategory.DOCUMENTATION -> Pair(AppColors.Blue50, AppColors.Blue600)
    }
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Text(
            text = category.displayName,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ── Compliance task card ──────────────────────────────────────────────────────

@Composable
fun TaskCard(
    task: ComplianceTask,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onEdit: () -> Unit,
    showActions: Boolean = false,
    modifier: Modifier = Modifier
) {
    val urgencyColor = when (task.urgency) {
        UrgencyLevel.CRITICAL -> AppColors.Red400
        UrgencyLevel.HIGH     -> AppColors.Amber400
        UrgencyLevel.MEDIUM   -> AppColors.Amber400
        UrgencyLevel.LOW      -> AppColors.Teal400
    }
    var expanded by remember { mutableStateOf(showActions) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        // Left accent line
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(urgencyColor)
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    UrgencyBadge(task.urgency)
                }

                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(task.category)
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = task.dueDateWindow,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // "Why this is shown" always visible
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Why",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = task.why,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }

                // Actions (expandable)
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onMarkDone,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Done", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onSnooze,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Snooze, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Snooze 7d", fontSize = 12.sp)
                            }
                            if (task.isUserEditable) {
                                OutlinedButton(
                                    onClick = onEdit,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, null, Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── API error banner ──────────────────────────────────────────────────────────

@Composable
fun ApiErrorBanner(
    errorMessage: String,
    onGoToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AppColors.Red50,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, AppColors.Red400.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = AppColors.Red600,
                modifier = Modifier.size(18.dp).padding(top = 1.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI key issue",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppColors.Red800,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Red600,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your saved tasks are still available below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.Red600,
                    fontStyle = FontStyle.Italic
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onGoToSettings,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = BorderStroke(0.5.dp, AppColors.Red600)
                ) {
                    Text("Update key in Settings", fontSize = 12.sp, color = AppColors.Red600)
                }
            }
        }
    }
}

// ── Car selector pill ─────────────────────────────────────────────────────────

@Composable
fun CarPill(
    car: com.carcomplianceapp.domain.model.Car,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isActive) AppColors.Teal50 else MaterialTheme.colorScheme.surface
    val borderColor = if (isActive) AppColors.Teal400 else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isActive) 1.5.dp else 0.5.dp

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Car icon circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = AppColors.Teal600, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(car.nickname, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${car.year} · ${car.fuelType.displayName} · ${car.countryCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isActive) {
                Icon(Icons.Default.CheckCircle, null, tint = AppColors.Teal400, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.outlineVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Notification toggle row ───────────────────────────────────────────────────

@Composable
fun NotifToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Teal400))
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ── Loading shimmer placeholder ───────────────────────────────────────────────

@Composable
fun LoadingTaskPlaceholder() {
    repeat(3) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}
        Spacer(Modifier.height(10.dp))
    }
}
