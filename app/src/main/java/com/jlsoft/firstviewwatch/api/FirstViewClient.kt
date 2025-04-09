package com.jlsoft.firstviewwatch.api
import android.util.Base64
import android.util.Log
import com.jlsoft.firstviewwatch.MyApplication
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface AuthApiService{

    @POST("v1/sign-in")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @POST("v1/get-token")
    suspend fun getToken(@Body tokenRequest: TokenRequest): TokenResponse

}
interface FirstViewApiService {

    @GET("v1/eta")
    suspend fun getEta(): EtaResponse

}
object AuthClient {
    private const val BASE_URL = "https://firstviewbackend.com/api/"

    val instance: AuthApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApiService::class.java)
    }
}


object FirstViewClient {
    private const val BASE_URL = "https://firstviewbackend.com/api/"

    // Helper function to build an OkHttpClient with the AuthInterceptor.
    private fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor()) // Our interceptor that refreshes tokens if needed
            .build()
    }

    val instance: FirstViewApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(provideOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FirstViewApiService::class.java)
    }
}
// Configure a logging interceptor.
private val loggingInterceptor: HttpLoggingInterceptor by lazy {
    HttpLoggingInterceptor { message ->
        Log.d("HTTP_LOG", message)
    }.apply {
        level = when (MyApplication.isRunningOnEmulator()) {
            true -> HttpLoggingInterceptor.Level.BASIC
            false -> HttpLoggingInterceptor.Level.NONE
        }
    }
}


class AuthInterceptor() : Interceptor {

    private fun getToken(): String? {
        return MyApplication.myPrefs().getString("auth_token", null)
    }

    // Parse the JWT to extract the "exp" claim (expiration time in seconds) and convert it to milliseconds.
    private fun getExpireTimeFromJwt(token: String): Long? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            // The payload is the second part and is Base64 URL-encoded.
            val payload = parts[1]
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            val decodedPayload = String(decodedBytes, Charsets.UTF_8)
            val json = JSONObject(decodedPayload)
            val expSeconds = json.getLong("exp")
            expSeconds * 1000L
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isTokenExpired(token: String): Boolean {
        val expireTime = getExpireTimeFromJwt(token)
        return expireTime == null || System.currentTimeMillis() > expireTime
    }

    private suspend fun refreshTokenCoroutine(): String {
        val loginToken = MyApplication.myPrefs().getString("login_token", null)
        val email = MyApplication.myPrefs().getString("email", null)
        if(loginToken != null && email != null){
            return try {
                val requestPayload = TokenRequest(
                    email = email,
                    login_token = loginToken
                )
                val response = AuthClient.instance.getToken(requestPayload)
                MyApplication.myPrefs().edit().apply {
                    putString("auth_token", response.auth_token)
                    apply()
                }
                response.auth_token

            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
        return ""
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        // Retrieve the token from storage.
        var token = getToken()
        // If missing or expired, refresh it using our coroutine call wrapped in runBlocking.
        if (token == null || isTokenExpired(token)) {
            token = runBlocking { refreshTokenCoroutine() }
        }
        val originalRequest = chain.request()
        // Attach the token to the Authorization header if available.
        val requestBuilder = originalRequest.newBuilder()
        token.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}

data class LoginRequest(
    val email_or_phone: String,
    val password: String,
    val remember_me: Boolean,
    val device_name: String,
    val device_uid: String
)

data class LoginResponse(
    val login_token: String?,
    val refresh_token: String,
    val message: String,
    val response: ResponseCode
)

// Request data for /get-token
data class TokenRequest(
    val email: String,
    val login_token: String
)

// Response data from /get-token
data class TokenResponse(
    val auth_token: String,
    val expiry: Int,
    val message: String,
    val response: ResponseCode
)
data class EtaResponse(
    val result: List<Result>?,
    var message: String,
    val response: ResponseCode
)

data class Student(
    val id: Int,
    val student_number: String,
    val first_name: String,
    val last_name: String,
    val linked_activity_student: Boolean,
    val activity_student: Boolean,
    val school: String?, // This field may be null
    @Transient val district: Any?
)

data class Result(
    val period: String,
    val pickup_or_dropoff: String,
    val dispatch_type: String,
    val time_zone: String,
    val scheduled_time: String?,
    val template_scheduled_time: String?,
    val average_time: String?,
    val route_id: String,
    val route: String?,
    val journey_id: String?,
    val stop: Stop?,
    val school: String?,
    val school_closed: Boolean,
    val contractor_id: Int,
    val valid_monday: Boolean,
    val valid_tuesday: Boolean,
    val valid_wednesday: Boolean,
    val valid_thursday: Boolean,
    val valid_friday: Boolean,
    val valid_saturday: Boolean,
    val valid_sunday: Boolean,
    val created_at: String,
    val early_late_minutes: Int,
    val estimated_time: String?,
    val estimated_time_from_now_minutes: String?,
    val status: String,
    val type: String,
    val stop_id: String,
    var vehicle_location: VehicleLocation?,
    val student: Student?,
    val service_start_time: String?,
    val run_complete: String?,
    val run_code: String?,
    val ridership_daily_record: String?
)

data class VehicleLocation(
    val lat: Double,
    val lng: Double,
    val bearing: Int,
    val text: String? = null,
    val timestamp: String? = null
)

data class Stop(
    val name: String,
    val id: String,
    val lat: Double,
    val lng: Double,
    val time_zone: String,
    val created_at: String
)

data class ResponseCode(
    val code: Int
)