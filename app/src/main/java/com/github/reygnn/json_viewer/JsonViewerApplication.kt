package com.github.reygnn.json_viewer

import android.app.Application
import com.github.reygnn.json_viewer.core.dispatchers.CoroutineDispatchers
import com.github.reygnn.json_viewer.core.dispatchers.DispatchersProvider
import com.github.reygnn.json_viewer.data.repository.JsonRepositoryImpl
import com.github.reygnn.json_viewer.domain.repository.JsonRepository
import com.github.reygnn.json_viewer.domain.usecase.LoadJsonUseCase

/**
 * Application-scoped manual DI container. Replaces Hilt: the dependency
 * graph is small enough that wiring it by hand here is simpler than a DI
 * framework. Singletons are created eagerly in [onCreate] and exposed for
 * the [JsonViewModelFactory].
 */
class JsonViewerApplication : Application() {

    lateinit var dispatchers: CoroutineDispatchers
        private set

    lateinit var loadJsonUseCase: LoadJsonUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        dispatchers = DispatchersProvider()
        val repository: JsonRepository = JsonRepositoryImpl(this, dispatchers)
        loadJsonUseCase = LoadJsonUseCase(repository)
    }
}
