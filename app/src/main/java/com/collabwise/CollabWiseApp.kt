package com.collabwise

import android.app.Application
import com.collabwise.data.seed.FireStoreSeeder
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class CollabWiseApp : Application() {
    /*
    @Inject
    lateinit var seeder: FireStoreSeeder

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            seeder.seedDatabase()
        }
    }
    */
}