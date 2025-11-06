package com.chatting.di

import android.app.Application
import android.content.Context
import com.chatting.domain.AccountManager
import com.chatting.domain.MediaManager
import com.chatting.domain.MessageManager
import com.chatting.websock.OkHttpWebSocketClient
import com.chatting.websock.WebSocketActionHandler
import com.chatting.websock.WebSocketClient
import com.chatting.websock.WebSocketMessageHandler
import com.data.parser.MessageParser
import com.data.receiver.DataReceiver
import com.data.receiver.WebSocketDataReceiver
import com.data.repository.AuthRepository
import com.data.repository.ChatRepository
import com.data.repository.DefaultChatRepository
import com.data.repository.UserDataStore
import com.data.source.local.LocalDataSource
import com.data.source.local.db.AppDatabase
import com.data.source.local.db.dao.BotDao
import com.data.source.local.db.dao.GroupDao
import com.data.source.local.db.dao.MessageDao
import com.data.source.local.db.dao.UserDao
import com.data.source.remote.RemoteDataSource
import com.service.api.ApiService
import com.service.api.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    /*@Provides
    @Singleton
    fun provideApplication(application: Application): Application {
        return application
    }*/

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(application: Application): AppDatabase {
        return AppDatabase.getDatabase(application)
    }

    @Provides
    @Singleton
    fun provideMessageDao(appDatabase: AppDatabase): MessageDao {
        return appDatabase.messageDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideGroupDao(appDatabase: AppDatabase): GroupDao {
        return appDatabase.groupDao()
    }

    @Provides
    @Singleton
    fun provideBotDao(appDatabase: AppDatabase): BotDao {
        return appDatabase.botDao()
    }

    @Provides
    @Singleton
    fun provideLocalDataSource(messageDao: MessageDao, userDao: UserDao, groupDao: GroupDao, botDao: BotDao): LocalDataSource {
        return LocalDataSource(messageDao, userDao, groupDao, botDao)
    }

    @Provides
    @Singleton
    fun provideRemoteDataSource(webSocketClient: WebSocketClient): RemoteDataSource {
        return RemoteDataSource(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(application: Application, coroutineScope: CoroutineScope, okHttpClient: OkHttpClient): WebSocketClient {
        return OkHttpWebSocketClient(application, coroutineScope, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(application: Application, webSocketClient: WebSocketClient, apiService: ApiService): AuthRepository {
        return AuthRepository(application, webSocketClient, apiService)
    }

    @Provides
    @Singleton
    fun provideUserDataStore(application: Application): UserDataStore {
        return UserDataStore.getInstance(application)
    }

    @Provides
    @Singleton
    fun provideMessageParser(): MessageParser {
        return MessageParser()
    }

    @Provides
    @Singleton
    fun provideMediaManager(application: Application, messageDao: MessageDao, okHttpClient: OkHttpClient): MediaManager {
        return MediaManager(application, messageDao, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideMessageManager(application: Application, localDataSource: LocalDataSource, remoteDataSource: RemoteDataSource, mediaManager: MediaManager, okHttpClient: OkHttpClient): MessageManager {
        return MessageManager(application, localDataSource, remoteDataSource, mediaManager, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideAccountManager(application: Application, apiService: ApiService, authRepository: AuthRepository, userDataStore: UserDataStore): AccountManager {
        return AccountManager(application, apiService, authRepository, userDataStore)
    }

    @Provides
    @Singleton
    fun provideWebSocketActionHandler(application: Application, remoteDataSource: RemoteDataSource, webSocketClient: WebSocketClient, authRepository: AuthRepository): WebSocketActionHandler {
        return WebSocketActionHandler(application, remoteDataSource, webSocketClient, authRepository)
    }

    @Provides
    @Singleton
    fun provideWebSocketDataReceiver(application: Application, localDataSource: LocalDataSource, remoteDataSource: RemoteDataSource, messageParser: MessageParser, mediaManager: MediaManager): DataReceiver {
        return WebSocketDataReceiver(application, localDataSource, remoteDataSource, messageParser, mediaManager)
    }

    @Provides
    @Singleton
    fun provideChatRepository(localDataSource: LocalDataSource, remoteDataSource: RemoteDataSource, mediaManager: MediaManager, application: Application, webSocketActionHandler: WebSocketActionHandler, apiService: ApiService): ChatRepository {
        return DefaultChatRepository(localDataSource, remoteDataSource, mediaManager, application, webSocketActionHandler, apiService)
    }

    @Provides
    @Singleton
    fun provideWebSocketMessageHandler(application: Application, webSocketClient: WebSocketClient, dataReceiver: DataReceiver, webSocketActionHandler: WebSocketActionHandler): WebSocketMessageHandler {
        return WebSocketMessageHandler(application, webSocketClient, dataReceiver, webSocketActionHandler)
    }
}
