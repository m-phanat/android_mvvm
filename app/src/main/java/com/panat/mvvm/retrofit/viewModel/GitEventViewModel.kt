package com.panat.mvvm.retrofit.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.panat.mvvm.retrofit.model.GithubEvents
import com.panat.mvvm.retrofit.service.ApiService
import com.panat.mvvm.retrofit.Repository.GitRepository
import kotlinx.coroutines.*


class GitEventViewModel(private val retrofit: ApiService) : ViewModel() {

    private val _events = MutableLiveData<List<GithubEvents>>()
    val events: LiveData<List<GithubEvents>>
        get() = _events

    val gitRepository = GitRepository()

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = async { gitRepository.getEvent() }
                withContext(Dispatchers.Main) {
                    if (response.await().isSuccessful) {
                        _events.postValue(response.await().body())
                    }
                }
                println("GithubEvents $response")
            } catch (e: Exception) {
                println("GithubEvents CoroutineScope Exception ${e.message}")
            }
        }
    }
}