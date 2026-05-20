package io.github.manhvu1212.tallyo.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.manhvu1212.tallyo.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun rememberDateFormatter(): (Long) -> String {
    val context = LocalContext.current
    return remember(context) { dateFormatter(context) }
}

private fun dateFormatter(context: Context): (Long) -> String = { ts ->
    val now = Calendar.getInstance()
    val d = Calendar.getInstance().apply { timeInMillis = ts }
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
    when {
        sameDay(d, now) -> {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
            context.getString(R.string.date_today, time)
        }
        sameDay(d, yesterday) -> context.getString(R.string.date_yesterday)
        else -> SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(ts))
    }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
        a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
