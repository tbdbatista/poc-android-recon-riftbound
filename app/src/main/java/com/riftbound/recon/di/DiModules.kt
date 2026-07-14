package com.riftbound.recon.di

import android.content.Context
import com.riftbound.recon.data.local.AppDatabase
import com.riftbound.recon.data.local.CardDao
import com.riftbound.recon.data.repository.CardRepositoryImpl
import com.riftbound.recon.domain.repository.CardRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiModule {

    @Binds
    @Singleton
    abstract fun bindCardRepository(
        impl: CardRepositoryImpl
    ): CardRepository

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
    }
}
