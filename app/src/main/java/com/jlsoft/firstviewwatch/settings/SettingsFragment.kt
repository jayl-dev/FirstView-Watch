package com.jlsoft.firstviewwatch.settings

import android.content.Context
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jlsoft.firstviewwatch.MyApplication
import com.jlsoft.firstviewwatch.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load preferences from the XML resource.
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("logout")?.setOnPreferenceClickListener {
            performLogout()
            true
        }

    }

    private fun performLogout() {
        MyApplication.myPrefs().edit().apply {
            remove("email")
            remove("login_token")
            remove("auth_token")
            // Optionally, store expiration info if needed.
            apply()
        }
        requireActivity().finish()

    }

}