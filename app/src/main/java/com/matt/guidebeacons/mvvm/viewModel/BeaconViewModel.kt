package com.matt.guidebeacons.mvvm.viewModel

import android.content.Context
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import androidx.lifecycle.asLiveData
import com.matt.guidebeacons.mvvm.repository.BeaconRepository
import com.matt.guidebeacons.mvvm.models.Beacon

class BeaconViewModel(context: Context) : ViewModel() {

    private val repository = BeaconRepository(context.applicationContext)

    // Expose Room data as LiveData for the UI
    val beacons: LiveData<List<Beacon>> = repository
        .getAllBeaconsStream()
        .asLiveData()

    fun insertBeacon(beacon: Beacon) {
        viewModelScope.launch {
            repository.insertBeacon(beacon)
        }
    }

    // Keep suspend API if you still need it
    suspend fun getAllBeacons(): List<Beacon> = repository.getAllBeacons()

    class BeaconViewModelFactory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BeaconViewModel(appContext) as T
        }
    }
}
