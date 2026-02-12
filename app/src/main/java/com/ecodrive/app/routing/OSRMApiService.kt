package com.ecodrive.app.routing

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

interface OSRMApiService {
    
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @retrofit2.http.Path("coordinates", encoded = true) coordinates: String,
        @Query("alternatives") alternatives: String = "true",
        @Query("steps") steps: String = "false",
        @Query("geometries") geometries: String = "polyline",
        @Query("overview") overview: String = "full"
    ): Response<OSRMResponse>
    
    companion object {
        private const val BASE_URL = "https://router.project-osrm.org/"
        
        fun create(): OSRMApiService {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            return retrofit.create(OSRMApiService::class.java)
        }
    }
}
