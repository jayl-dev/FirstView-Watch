import android.util.Log
import androidx.lifecycle.ViewModel
import com.jlsoft.firstviewwatch.api.EtaResponse
import com.jlsoft.firstviewwatch.api.FirstViewClient
import com.jlsoft.firstviewwatch.api.VehicleLocation
import kotlinx.coroutines.flow.*
import kotlin.random.Random

class MapViewModel : ViewModel() {

    companion object {
        private const val TAG = "MapViewModel"
    }

    // Holds the latest API response.
    private val _etaResponse = MutableStateFlow<EtaResponse?>(null)
    val etaResponse: StateFlow<EtaResponse?> = _etaResponse

    // Loading state flag.
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * Refreshes the data from the API
     */
    suspend fun refreshData() {
        try {
            _isLoading.value = true
            val response = FirstViewClient.instance.getEta()
            response.result?.forEach {
//                if(it.vehicle_location == null){
//                    val mock = VehicleLocation(
//                        lat = 40.1252448+ Random.nextFloat()* 0.01f,
//                        lng = -75.0385262+ Random.nextFloat()* 0.01f,
//                        bearing = 129 + Random.nextInt(100)
//                    )
//                    it.vehicle_location = mock
//                }
            }
            _etaResponse.value = response
        } catch (e: Exception) {
            Log.e(TAG, "refreshData - Error fetching API data", e)
        }finally {
            _isLoading.value = false
        }

    }

}