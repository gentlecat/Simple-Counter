package me.tsukanov.counter.view.counters;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import java.util.List;
import me.tsukanov.counter.CounterApplication;
import me.tsukanov.counter.R;
import me.tsukanov.counter.domain.IntegerCounter;
import me.tsukanov.counter.infrastructure.Actions;
import me.tsukanov.counter.infrastructure.BroadcastHelper;
import me.tsukanov.counter.view.dialogs.AddDialog;

public class CountersListFragment extends ListFragment {

  private static final String TAG = CountersListFragment.class.getSimpleName();

  private CountersListAdapter listAdapter;

  @Override
  public View onCreateView(
      @NonNull final LayoutInflater inflater,
      final ViewGroup container,
      final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    @SuppressLint("InflateParams")
    final View view = inflater.inflate(R.layout.menu, null);

    final LinearLayout addButton = view.findViewById(R.id.add_counter);
    addButton.setOnClickListener(v -> showAddDialog());

    return view;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    updateList();

    final IntentFilter counterSetChangeFilter =
        new IntentFilter(Actions.COUNTER_SET_CHANGE.getActionName());
    counterSetChangeFilter.addCategory(Intent.CATEGORY_DEFAULT);
    requireActivity()
        .getApplication()
        .registerReceiver(new UpdateReceiver(), counterSetChangeFilter);
  }

  @Override
  public void onListItemClick(
      @NonNull final ListView lv, @NonNull final View v, final int position, final long id) {
    new BroadcastHelper(this.requireContext())
        .sendSelectCounterBroadcast(listAdapter.getItem(position).getName());
  }

  private void updateList() {
    if (!isFragmentActive()) return;

    List<IntegerCounter> counters = CounterApplication.getComponent().localStorage().readAll(false);

    listAdapter = new CountersListAdapter(getActivity());
    for (final IntegerCounter c : counters) {
      listAdapter.add(c);
    }
    setListAdapter(listAdapter);
  }

  private void showAddDialog() {
    final AddDialog dialog = new AddDialog();
    dialog.show(getParentFragmentManager(), TAG);
  }

  private class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      updateList();
    }
  }

  private boolean isFragmentActive() {
    return isAdded() && !isDetached() && !isRemoving();
  }
}
