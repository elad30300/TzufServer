package com.example.tzufserver.di.module

import com.example.tzufserver.provider.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EntitiesBleGeoProvider

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EntitiesTempProvider

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RasterTilesBleGeoProvider

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RasterTilesGeopackageProvider

@Module
@InstallIn(ApplicationComponent::class)
abstract class ProviderModule {

    @EntitiesBleGeoProvider
    @Binds
    abstract fun bindBleGeoEntityProvider(provider: BleGeoProvider): EntitiesProvider

    @EntitiesTempProvider
    @Binds
    abstract fun bindTempEntityProvider(provider: TempEntitiesProvider): EntitiesProvider

    @RasterTilesBleGeoProvider
    @Binds
    abstract fun bindBleGeoRasterTilesProvider(provider: BleGeoProvider): RasterTilesProvider

    @RasterTilesGeopackageProvider
    @Binds
    abstract fun bindGeopackageRasterTilesProvider(provider: GeopackageMapProvider): RasterTilesProvider

}