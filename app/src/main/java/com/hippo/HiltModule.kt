package com.hippo

import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.client.EhClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
object HiltModule {

    @Provides
    fun provideEhClient(): EhClient {
        return EhClient(EhApplication.getInstance())
    }
}