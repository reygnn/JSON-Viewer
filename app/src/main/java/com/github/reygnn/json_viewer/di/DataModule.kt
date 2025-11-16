package com.github.reygnn.json_viewer.di

package com.github.reygnn.json_viewer.di

import com.github.reygnn.json_viewer.data.repository.JsonRepositoryImpl
import com.github.reygnn.json_viewer.domain.repository.JsonRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindJsonRepository(
        jsonRepositoryImpl: JsonRepositoryImpl
    ): JsonRepository
}