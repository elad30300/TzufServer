package com.example.tzufserver.di.module

import com.example.tzufserver.provider.dispatchers.DefaultDispatcherProvider
import com.example.tzufserver.provider.dispatchers.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BleExecutor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EntitiesExecutor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RasterTilesExecutor

@Module
@InstallIn(ApplicationComponent::class)
class ConcurrentModule {

    @BleExecutor
    @Provides
    fun provideBluetoothModuleExecutor(): Executor = Executors.newSingleThreadExecutor()

    @EntitiesExecutor
    @Provides
    fun provideEntitiesExecutor(): Executor = Executors.newSingleThreadExecutor()

    @RasterTilesExecutor
    @Provides
    fun provideRasterTilesExecutor(): Executor = Executors.newSingleThreadExecutor()

    @Provides
    fun provideDefaultDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

}