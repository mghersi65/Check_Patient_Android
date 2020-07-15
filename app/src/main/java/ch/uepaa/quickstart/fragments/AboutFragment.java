package ch.uepaa.quickstart.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import ch.uepaa.p2pkit.P2PKit;
import ch.uepaa.p2pkit.discovery.DiscoveryListener;
import ch.uepaa.quickstart.R;
import ch.uepaa.quickstart.utils.Logger;

/**
 * Console fragment.
 * Created by uepaa on 09/02/16.
 */
public class AboutFragment extends DialogFragment implements Logger.LogHandler {

    public interface ConsoleListener {
        void startDiscovery();
        void stopDiscovery();
    }

    public static final String FRAGMENT_TAG = "about_fragment";

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    private TextView mLogView;
    private View mainView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {

        mainView = view;
        getDialog().setTitle(R.string.console);


        mLogView = view.findViewById(R.id.logTextView);
        TextView clearLogs = view.findViewById(R.id.clearTextView);
        clearLogs.setOnClickListener(v -> clearLogs());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        String logs = Logger.getLogs();
        mLogView.setText(logs);
        Logger.addObserver(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        Logger.removeObserver(this);
        mLogView.setText("");
    }

    public void handleLogMessage(String message) {
        String updated = message + "\n" + mLogView.getText();
        mLogView.setText(updated);
    }

    private void clearLogs() {
        Logger.clearLogs();
        mLogView.setText("");
    }

    private boolean getKitEnabled() {
        return P2PKit.isEnabled();
    }
    private boolean getDiscoveryEnabled() {

        if (getKitEnabled()) {
            return !(P2PKit.getDiscoveryState() == DiscoveryListener.STATE_OFF);
        }else{
            return false;
        }
    }

}
