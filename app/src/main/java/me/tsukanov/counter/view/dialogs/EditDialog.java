package me.tsukanov.counter.view.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import java.util.Objects;
import me.tsukanov.counter.CounterApplication;
import me.tsukanov.counter.R;
import me.tsukanov.counter.activities.CountersActivity;
import me.tsukanov.counter.domain.IntegerCounter;
import me.tsukanov.counter.domain.exception.CounterException;
import me.tsukanov.counter.infrastructure.BroadcastHelper;
import me.tsukanov.counter.repository.CounterStorage;
import me.tsukanov.counter.view.counters.CounterFragment;

public class EditDialog extends DialogFragment {

  public static final String TAG = EditDialog.class.getSimpleName();
  private static final String BUNDLE_ARGUMENT_NAME = "name";
  private static final String BUNDLE_ARGUMENT_VALUE = "value";

  public static EditDialog newInstance(final @NonNull String counterName, int counterValue) {
    final EditDialog dialog = new EditDialog();

    final Bundle arguments = new Bundle();
    arguments.putString(BUNDLE_ARGUMENT_NAME, counterName);
    arguments.putInt(BUNDLE_ARGUMENT_VALUE, counterValue);
    dialog.setArguments(arguments);

    return dialog;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(final Bundle savedInstanceState) {

    final String oldName = requireArguments().getString(BUNDLE_ARGUMENT_NAME);
    final int oldValue = requireArguments().getInt(BUNDLE_ARGUMENT_VALUE);

    final CountersActivity activity = (CountersActivity) getActivity();

    final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_edit, null);

    final EditText nameInput = dialogView.findViewById(R.id.edit_name);
    nameInput.setText(oldName);

    final EditText valueInput = dialogView.findViewById(R.id.edit_value);
    valueInput.setText(String.valueOf(oldValue));
    valueInput.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
    final InputFilter[] valueFilter = new InputFilter[1];
    valueFilter[0] = new InputFilter.LengthFilter(IntegerCounter.getValueCharLimit());
    valueInput.setFilters(valueFilter);

    final Dialog dialog =
        new AlertDialog.Builder(getActivity())
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_edit_title))
            .setPositiveButton(
                getResources().getText(R.string.dialog_button_apply),
                (d, which) -> {
                  final String newName = nameInput.getText().toString();
                  if (newName.equals("")) {
                    Toast.makeText(
                            activity,
                            getResources().getText(R.string.toast_no_name_message),
                            Toast.LENGTH_SHORT)
                        .show();
                  } else {

                    int newValue;
                    String valueInputContents = valueInput.getText().toString();
                    if (!valueInputContents.equals("")) {
                      try {
                        newValue = Integer.parseInt(valueInputContents);
                      } catch (NumberFormatException e) {
                        Log.w(TAG, "Unable to parse new value", e);
                        Toast.makeText(
                                activity,
                                getResources().getText(R.string.toast_unable_to_modify),
                                Toast.LENGTH_SHORT)
                            .show();
                        newValue = CounterFragment.DEFAULT_VALUE;
                      }
                    } else {
                      newValue = CounterFragment.DEFAULT_VALUE;
                    }

                    final CounterStorage<IntegerCounter> storage =
                        CounterApplication.getComponent().localStorage();

                    storage.delete(oldName);
                    try {
                      storage.write(new IntegerCounter(newName, newValue));
                    } catch (CounterException e) {
                      Log.getStackTraceString(e);
                      Toast.makeText(
                              getContext(), R.string.toast_unable_to_modify, Toast.LENGTH_SHORT)
                          .show();
                    }

                    new BroadcastHelper(requireContext()).sendSelectCounterBroadcast(newName);
                  }
                })
            .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
            .create();

    dialog.setCanceledOnTouchOutside(true);
    Objects.requireNonNull(dialog.getWindow())
        .setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

    return dialog;
  }
}
