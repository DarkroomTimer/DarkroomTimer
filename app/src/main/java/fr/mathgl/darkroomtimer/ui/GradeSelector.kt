package fr.mathgl.darkroomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.mathgl.darkroomtimer.math.ContrastGrade
import fr.mathgl.darkroomtimer.ui.theme.DarkroomBlack
import fr.mathgl.darkroomtimer.ui.theme.DarkroomRedBright

@Composable
fun GradeSelector(
    selectedGrade: ContrastGrade,
    onGradeSelected: (ContrastGrade) -> Unit,
    modifier: Modifier = Modifier
) {
    val grades = ContrastGrade.entries
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedGrade.index)

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(grades) { grade ->
            GradeItem(
                grade = grade,
                isSelected = grade == selectedGrade,
                onClick = { onGradeSelected(grade) }
            )
        }
    }
}

@Composable
private fun GradeItem(
    grade: ContrastGrade,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) DarkroomRedBright else Color.Transparent
    val textColor = if (isSelected) DarkroomBlack else DarkroomRedBright
    val borderColor = DarkroomRedBright

    Box(
        modifier = Modifier
            .size(48.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = grade.label,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
