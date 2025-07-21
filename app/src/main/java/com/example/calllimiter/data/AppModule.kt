package com.example.calllimiter.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Provides instances that live as long as the application
object AppModule {

    @Provides
    @Singleton // Ensures only one instance of AppDatabase is created
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext, // Use applicationContext to avoid leaks
            AppDatabase::class.java,
            "call_limiter_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton // Ensures only one instance of AppDao is created (scoped to AppDatabase)
    fun provideAppDao(database: AppDatabase): AppDao {
        return database.appDao()
    }
}