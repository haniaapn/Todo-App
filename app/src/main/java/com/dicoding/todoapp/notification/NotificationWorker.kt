package com.dicoding.todoapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.dicoding.todoapp.R
import com.dicoding.todoapp.data.Task
import com.dicoding.todoapp.data.TaskRepository
import com.dicoding.todoapp.ui.detail.DetailTaskActivity
import com.dicoding.todoapp.utils.NOTIFICATION_CHANNEL_ID
import com.dicoding.todoapp.utils.TASK_ID
import java.text.SimpleDateFormat
import java.util.*

class NotificationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val channelName = inputData.getString(NOTIFICATION_CHANNEL_ID)

    private fun getPendingIntent(task: Task): PendingIntent? {
        val intent = Intent(applicationContext, DetailTaskActivity::class.java).apply {
            putExtra(TASK_ID, task.id)
        }
        return TaskStackBuilder.create(applicationContext).run {
            addNextIntentWithParentStack(intent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            } else {
                getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }
    }

    override fun doWork(): Result {
        // TODO 14: If notification preference is on, get the nearest active task from the repository and show the notification with pending intent
        val repository = TaskRepository.getInstance(applicationContext)
        val nearestActiveTask = repository.getNearestActiveTask()

        showNotification(nearestActiveTask)

        return Result.success()
    }

    private fun showNotification(task: Task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && channelName != null) {
            val notificationChannel = NotificationChannel(
                channelName,
                applicationContext.getString(R.string.notify_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val pendingIntent = getPendingIntent(task)

        val formattedDueDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.dueDateMillis)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelName ?: NOTIFICATION_CHANNEL_ID)
            .setContentTitle(task.title)
            .setContentText("Due in $formattedDueDate")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(1, notificationBuilder.build())
    }
}
