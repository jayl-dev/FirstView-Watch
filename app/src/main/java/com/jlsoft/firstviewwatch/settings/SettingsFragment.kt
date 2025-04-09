package com.jlsoft.firstviewwatch.settings

import android.app.AlertDialog
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jlsoft.firstviewwatch.MyApplication
import com.jlsoft.firstviewwatch.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("logout")?.setOnPreferenceClickListener {
            performLogout()
            true
        }
        findPreference<Preference>("back")?.setOnPreferenceClickListener {
            requireActivity().finish()
            true
        }
        findPreference<Preference>("restart")?.setOnPreferenceClickListener {
            MyApplication.forceRestart(requireActivity())
            true
        }

        // Find the CheckBoxPreference by its key
//        val forceRestartPref: CheckBoxPreference? = findPreference("keep_screen_on")
//        forceRestartPref?.setOnPreferenceChangeListener { preference: Preference, newValue: Any ->
//            showRestartConfirmationDialog()
//            true
//        }


    }
    @Suppress("unused")
    private fun showRestartConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Restart FirstView Watch")
            .setMessage("Changing this setting requires a restart of the app")
            .setPositiveButton("Restart") { _, _ ->
                MyApplication.forceRestart(requireActivity())
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }





    private fun performLogout() {
        MyApplication.myPrefs().edit().apply {
            remove("email")
            remove("login_token")
            remove("auth_token")
            apply()
        }
        requireActivity().finish()

    }

}