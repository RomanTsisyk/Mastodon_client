package io.github.romantsisyk.mastodon.data.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.romantsisyk.mastodon.data.database.TimelineDatabase
import io.github.romantsisyk.mastodon.data.repository.TimelineRepositoryImpl
import io.github.romantsisyk.mastodon.domain.repository.TimelineRepository
import io.github.romantsisyk.mastodon.utils.AppConstants.BASE_URL
import io.github.romantsisyk.mastodon.utils.AppConstants.CONNECT_TIMEOUT_SECONDS
import io.github.romantsisyk.mastodon.utils.AppConstants.DATABASE_NAME
import io.github.romantsisyk.mastodon.utils.AppConstants.READ_TIMEOUT_SECONDS
import io.github.romantsisyk.mastodon.utils.AppConstants.WRITE_TIMEOUT_SECONDS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTimelineRepository(
        timelineRepositoryImpl: TimelineRepositoryImpl
    ): TimelineRepository

    companion object {

        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): TimelineDatabase {
            return Room.databaseBuilder(
                context,
                TimelineDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        @Provides
        @Singleton
        fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }

        @Provides
        @Singleton
        fun provideCoroutineScope(): CoroutineScope {
            return CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

        @Provides
        @Singleton
        fun provideCoroutineDispatcher(): CoroutineDispatcher {
            return Dispatchers.IO
        }
    }
}