package com.dicoding.todoapp.setting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.work.*
import com.dicoding.todoapp.R
import com.dicoding.todoapp.notification.NotificationWorker
import com.dicoding.todoapp.utils.NOTIFICATION_CHANNEL_ID
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val prefNotification = findPreference<SwitchPreference>(getString(R.string.pref_key_notify))
            prefNotification?.setOnPreferenceChangeListener { _, newValue ->
                val channelName = getString(R.string.notify_channel_name)
                // TODO 13: Schedule and cancel daily reminder using WorkManager with data channelName
                val workManager = WorkManager.getInstance(requireContext())

                if (newValue as Boolean) {
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    val notificationData = Data.Builder()
                        .putString(NOTIFICATION_CHANNEL_ID, channelName)
                        .build()

                    val reminderInterval = 1L
                    val reminderIntervalMillis = TimeUnit.DAYS.toMillis(reminderInterval)

                    val periodicWorkRequest = PeriodicWorkRequest.Builder(
                        NotificationWorker::class.java,
                        reminderIntervalMillis,
                        TimeUnit.MILLISECONDS
                    )
                        .setInputData(notificationData)
                        .setConstraints(constraints)
                        .build()

                    workManager.enqueueUniquePeriodicWork(
                        "NotificationWork",
                        ExistingPeriodicWorkPolicy.REPLACE,
                        periodicWorkRequest
                    )
                } else {
                    workManager.cancelUniqueWork("NotificationWork")
                }

                true
            }
        }
    }
}
