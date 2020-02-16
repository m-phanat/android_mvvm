package com.panat.mvvm.retrofit.service

import com.panat.mvvm.retrofit.model.GithubEvents
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("events")
    suspend fun getEventsCoroutines(): Response<List<GithubEvents>>
}
