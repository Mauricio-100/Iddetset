package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class, Actfile::class, Message::class, Follow::class, ActfileComment::class, Notification::class, SavedAccount::class, ChannelMessage::class], version = 18, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun actfileDao(): ActfileDao
    abstract fun messageDao(): MessageDao
    abstract fun followDao(): FollowDao
    abstract fun commentDao(): CommentDao
    abstract fun notificationDao(): NotificationDao
    abstract fun savedAccountDao(): SavedAccountDao
    abstract fun channelMessageDao(): ChannelMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        var appContext: Context? = null

        fun getDatabase(context: Context): AppDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iddet_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
