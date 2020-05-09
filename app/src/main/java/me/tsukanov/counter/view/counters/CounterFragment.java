package me.tsukanov.counter.view.counters;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import java.util.Objects;
import me.tsukanov.counter.CounterApplication;
import me.tsukanov.counter.R;
import me.tsukanov.counter.SharedPrefKeys;
import me.tsukanov.counter.activities.CountersActivity;
import me.tsukanov.counter.domain.IntegerCounter;
import me.tsukanov.counter.repository.CounterStorage;
import me.tsukanov.counter.repository.exceptions.MissingCounterException;
import me.tsukanov.counter.view.dialogs.DeleteDialog;
import me.tsukanov.counter.view.dialogs.EditDialog;

public class CounterFragment extends Fragment {

  private static final String TAG = CounterFragment.class.getSimpleName();

  public static final String COUNTER_NAME_ATTRIBUTE = "COUNTER_NAME";

  public static final int DEFAULT_VALUE = 0;
  private static final long DEFAULT_VIBRATION_DURATION = 30; // Milliseconds

  private static final String STATE_SELECTED_COUNTER_NAME = "SELECTED_COUNTER_NAME";

  /** Name is used as a key to look up and modify counter value in storage. */
  private String name;

  private SharedPreferences sharedPrefs;
  private Vibrator vibrator;
  private TextView counterLabel;
  private Button incrementButton;
  private Button decrementButton;

  private IntegerCounter counter;

  private MediaPlayer incrementSoundPlayer;
  private MediaPlayer decrementSoundPlayer;

  private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
    if (savedInstanceState == null) return;

    this.name = savedInstanceState.getString(STATE_SELECTED_COUNTER_NAME);
  }

  @Override
  public void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    restoreSavedState(savedInstanceState);

    vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());

    initCounter();

    setHasOptionsMenu(true);
  }

  private void initCounter() {
    final CounterStorage<IntegerCounter> storage = CounterApplication.getComponent().localStorage();
    try {
      final String requestedCounter = requireArguments().getString(COUNTER_NAME_ATTRIBUTE);
      if (requestedCounter == null) {
        this.counter = storage.readAll(true).get(0);
        return;
      }
      this.counter = storage.read(requestedCounter);
    } catch (MissingCounterException e) {
      Log.w(TAG, "Unable to find provided counter. Retrieving a different one.", e);
      this.counter = storage.readAll(true).get(0);
    }
  }

  @Override
  public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
    savedInstanceState.putString(STATE_SELECTED_COUNTER_NAME, this.name);
    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    /* Setting up sounds */
    incrementSoundPlayer = MediaPlayer.create(getContext(), R.raw.increment_sound);
    decrementSoundPlayer = MediaPlayer.create(getContext(), R.raw.decrement_sound);
  }

  @Override
  public void onPause() {
    super.onPause();

    incrementSoundPlayer.reset();
    incrementSoundPlayer.release();
    decrementSoundPlayer.reset();
    decrementSoundPlayer.release();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    final View view = inflater.inflate(R.layout.counter, container, false);

    incrementButton = view.findViewById(R.id.incrementButton);
    incrementButton.setOnClickListener(v -> increment());

    decrementButton = view.findViewById(R.id.decrementButton);
    decrementButton.setOnClickListener(v -> decrement());

    counterLabel = view.findViewById(R.id.counterLabel);
    counterLabel.setOnClickListener(
        v -> {
          if (sharedPrefs.getBoolean(SharedPrefKeys.LABEL_CONTROL_ON.getName(), true)) {
            increment();
          }
        });

    invalidateUI();

    return view;
  }

  @Override
  public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
    inflater.inflate(R.menu.counter_menu, menu);
  }

  @Override
  public void onPrepareOptionsMenu(@NonNull final Menu menu) {
    boolean isDrawerOpen = ((CountersActivity) requireActivity()).isNavigationOpen();

    MenuItem editItem = menu.findItem(R.id.menu_edit);
    editItem.setVisible(!isDrawerOpen);

    MenuItem deleteItem = menu.findItem(R.id.menu_delete);
    deleteItem.setVisible(!isDrawerOpen);

    MenuItem resetItem = menu.findItem(R.id.menu_reset);
    resetItem.setVisible(!isDrawerOpen);
  }

  public boolean onKeyDown(int keyCode) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_VOLUME_UP:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          increment();
          return true;
        }
        return false;

      case KeyEvent.KEYCODE_VOLUME_DOWN:
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
          decrement();
          return true;
        }
        return false;

      default:
        return false;
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_reset:
        showResetConfirmationDialog();
        return true;
      case R.id.menu_edit:
        showEditDialog();
        return true;
      case R.id.menu_delete:
        showDeleteDialog();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void showResetConfirmationDialog() {
    final Dialog dialog =
        new AlertDialog.Builder(getActivity())
            .setMessage(getResources().getText(R.string.dialog_reset_title))
            .setCancelable(false)
            .setPositiveButton(
                getResources().getText(R.string.dialog_button_reset), (d, id) -> reset())
            .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
            .create();
    Objects.requireNonNull(dialog.getWindow())
        .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    dialog.show();
  }

  private void showEditDialog() {
    final EditDialog dialog = EditDialog.newInstance(counter.getName(), counter.getValue());
    dialog.show(getParentFragmentManager(), EditDialog.TAG);
  }

  private void showDeleteDialog() {
    final DeleteDialog dialog = DeleteDialog.newInstance(counter.getName());
    dialog.show(getParentFragmentManager(), DeleteDialog.TAG);
  }

  private void increment() {
    counter.increment();
    vibrate(DEFAULT_VIBRATION_DURATION);
    playSound(incrementSoundPlayer);

    invalidateUI();
    saveValue();
  }

  private void decrement() {
    counter.decrement();
    vibrate(DEFAULT_VIBRATION_DURATION + 20);
    playSound(decrementSoundPlayer);

    invalidateUI();
    saveValue();
  }

  private void reset() {
    try {
      counter.reset();
    } catch (Exception e) {
      Log.getStackTraceString(e);
      throw new RuntimeException(e);
    }

    invalidateUI();
    saveValue();
  }

  /** Updates UI elements of the fragment based on current value of the counter. */
  @SuppressLint("SetTextI18n")
  private void invalidateUI() {
    counterLabel.setText(Integer.toString(counter.getValue()));

    if (counter.getValue() >= IntegerCounter.MAX_VALUE) incrementButton.setEnabled(false);
    else incrementButton.setEnabled(true);

    if (counter.getValue() <= IntegerCounter.MIN_VALUE) decrementButton.setEnabled(false);
    else decrementButton.setEnabled(true);
  }

  private void saveValue() {
    final CounterStorage<IntegerCounter> storage = CounterApplication.getComponent().localStorage();
    storage.write(counter);
  }

  /** Triggers vibration for a specified duration, if vibration is turned on. */
  private void vibrate(long duration) {
    if (sharedPrefs.getBoolean(SharedPrefKeys.VIBRATION_ON.getName(), true)) {
      vibrator.vibrate(duration);
    }
  }

  /** Plays sound if sounds are turned on. */
  private void playSound(@NonNull final MediaPlayer soundPlayer) {
    if (sharedPrefs.getBoolean(SharedPrefKeys.SOUNDS_ON.getName(), false)) {
      if (soundPlayer.isPlaying()) {
        soundPlayer.seekTo(0);
      }
      soundPlayer.start();
    }
  }
}
