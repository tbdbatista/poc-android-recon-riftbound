package com.riftbound.recon.di

import android.content.Context
import com.riftbound.recon.data.local.AppDatabase
import com.riftbound.recon.data.local.CardDao
import com.riftbound.recon.data.repository.CardRepositoryImpl
import com.riftbound.recon.domain.repository.CardRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.riftbound.recon.data.auth.FirebaseAuthRepositoryImpl
import com.riftbound.recon.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(
        impl: CardRepositoryImpl
    ): CardRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(
        impl: com.riftbound.recon.data.sync.FirestoreSyncRepositoryImpl
    ): com.riftbound.recon.domain.repository.SyncRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: com.riftbound.recon.data.backup.FirestoreBackupRepositoryImpl
    ): com.riftbound.recon.domain.repository.BackupRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return AppDatabase.build(context)
        }

        @Provides
        fun provideCardDao(db: AppDatabase): CardDao {
            return db.cardDao()
        }

        @Provides
        @Singleton
        fun provideFirebaseAuth(): FirebaseAuth {
            return FirebaseAuth.getInstance()
        }

        @Provides
        @Singleton
        fun provideFirebaseFirestore(): FirebaseFirestore {
            return FirebaseFirestore.getInstance()
        }
    }
}
