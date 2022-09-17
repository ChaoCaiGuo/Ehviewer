package com.hippo

import android.content.Context
import com.hippo.ehviewer.EhApplication
import com.hippo.ehviewer.EhProxySelector
import com.hippo.ehviewer.client.EhDns
import com.hippo.ehviewer.client.EhSSLSocketFactory
import com.hippo.ehviewer.client.EhX509TrustManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.security.KeyStore
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@InstallIn(SingletonComponent::class)
@Module
object HiltModule {
    @Singleton
    @Provides
    fun provideOkHttpClient(@ApplicationContext context: Context,mEhProxySelector: EhProxySelector): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .cookieJar(EhApplication.getEhCookieStore(context))
            .cache(Cache(File(context.applicationContext.cacheDir, "http_cache"), 50L * 1024L * 1024L))
            .dns(EhDns(context))
            .addNetworkInterceptor(Interceptor { chain: Interceptor.Chain ->
                try {
                    return@Interceptor chain.proceed(chain.request())
                } catch (e: NullPointerException) {
                    throw IOException(e.message)
                }
            })
            .proxySelector(mEhProxySelector)

        try {
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(null as KeyStore?)
            val trustManagers = trustManagerFactory.trustManagers
            check(!(trustManagers.size != 1 || trustManagers[0] !is X509TrustManager)) {
                "Unexpected default trust managers:" + Arrays.toString(
                    trustManagers
                )
            }
            val trustManager = trustManagers[0] as X509TrustManager
            builder.sslSocketFactory(EhSSLSocketFactory(), trustManager)
        } catch (e: Exception) {
            e.printStackTrace()
            builder.sslSocketFactory(EhSSLSocketFactory(), EhX509TrustManager())
        }


        return builder.build()
    }
}