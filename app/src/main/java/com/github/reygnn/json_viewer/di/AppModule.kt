package com.github.reygnn.json_viewer.di

import android.content.Context
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.dispatchers.DispatchersProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(provider: DispatchersProvider): CoroutineDispatchers = provider

    @Provides
    @Singleton
    fun provideKotlinxJson(): Json = Json {
        prettyPrint = true      // Wichtig für die Anzeige
        ignoreUnknownKeys = true
        isLenient = true        // Erlaubt leicht fehlerhaftes JSON
    }
}