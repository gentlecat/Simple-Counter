package me.tsukanov.counter.view.settings;

import static me.tsukanov.counter.activities.SettingsActivity.KEY_EXPORT_COUNTERS;
import static me.tsukanov.counter.activities.SettingsActivity.KEY_REMOVE_COUNTERS;
import static me.tsukanov.counter.activities.SettingsActivity.KEY_VERSION;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import me.tsukanov.counter.R;
import me.tsukanov.counter.SharedPrefKeys;

public final class SettingsFragment extends PreferenceFragmentCompat {

  private static final String TAG = SettingsFragment.class.getSimpleName();

  private OnPreferenceClickListener onRemoveCountersClickListener;
  private OnPreferenceClickListener onExportClickListener;
  private String appVersion;
  private String theme;

  public void setOnRemoveCountersClickListener(
      final @NonNull OnPreferenceClickListener onRemoveCountersClickListener) {
    this.onRemoveCountersClickListener = onRemoveCountersClickListener;
  }

  public void setOnExportClickListener(
      final @NonNull OnPreferenceClickListener onExportClickListener) {
    this.onExportClickListener = onExportClickListener;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public void setTheme(String theme) {
    this.theme = theme;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    try {

      // Setting summaries for necessary preferences
      findPreference(SharedPrefKeys.THEME.getName()).setSummary(theme);
      findPreference(KEY_VERSION).setSummary(appVersion);

      findPreference(KEY_REMOVE_COUNTERS)
          .setOnPreferenceClickListener(onRemoveCountersClickListener);
      findPreference(KEY_EXPORT_COUNTERS).setOnPreferenceClickListener(onExportClickListener);

    } catch (NullPointerException e) {
      Log.e(TAG, "Unable to retrieve one of the preferences", e);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    setPreferencesFromResource(R.xml.settings, rootKey);
  }
}
