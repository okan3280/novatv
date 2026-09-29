package com.hp.novatv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        ProgramEntity::class,
        FavoriteEntity::class,
        WatchlistEntity::class,
        HistoryEntity::class,
        RecordingEntity::class,
        ScheduleEntity::class,
        ParentalLockEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NovaDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao
    abstract fun userDataDao(): UserDataDao

    companion object {
        private const val NAME = "novatv.db"

        @Volatile
        private var instance: NovaDatabase? = null

        fun get(context: Context): NovaDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): NovaDatabase =
            Room.databaseBuilder(context, NovaDatabase::class.java, NAME)
                // CASCADE silme yalnizca FK'ler aktifse calisir.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
