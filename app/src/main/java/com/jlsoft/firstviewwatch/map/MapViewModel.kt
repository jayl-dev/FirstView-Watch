import android.util.Log
import androidx.lifecycle.ViewModel
import com.jlsoft.firstviewwatch.api.EtaResponse
import com.jlsoft.firstviewwatch.api.FirstViewClient
import com.jlsoft.firstviewwatch.api.VehicleLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

class MapViewModel : ViewModel() {

    companion object {
        private const val TAG = "MapViewModel"
        private const val REFRESH_INTERVAL_MS = 5000L // 5 seconds

    }

    // Holds the latest API response.
    private val _etaResponse = MutableStateFlow<EtaResponse?>(null)
    val etaResponse: StateFlow<EtaResponse?> = _etaResponse

    // Loading state flag.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var lastRefreshTime: Long = 0

    /**
     * Refreshes the data from the API
     */
    suspend fun refreshData() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime < REFRESH_INTERVAL_MS) {
            Log.d(TAG, "Skipping refreshData call; called less than 5 seconds ago.")
            return
        }
        lastRefreshTime = currentTime

        try {
            _isLoading.value = true
            val response = FirstViewClient.instance.getEta()
//            mock(response)
            _etaResponse.value = response
        } catch (e: Exception) {
            Log.e(TAG, "refreshData - Error fetching API data", e)
        }finally {
            _isLoading.value = false
        }

    }

    private fun mock(response: EtaResponse) {
        response.result?.forEach {
            if (it.vehicle_location == null) {
                val mock = VehicleLocation(
                    lat = 40.1252448 + Random.nextFloat() * 0.01f,
                    lng = -75.0385262 + Random.nextFloat() * 0.01f,
                    bearing = 129 + Random.nextInt(100)
                )
                it.vehicle_location = mock
            }
        }
    }

}