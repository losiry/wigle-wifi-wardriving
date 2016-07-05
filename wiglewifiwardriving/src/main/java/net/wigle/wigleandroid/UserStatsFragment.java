package net.wigle.wigleandroid;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import net.wigle.wigleandroid.background.ApiDownloader;
import net.wigle.wigleandroid.background.ApiListener;
import net.wigle.wigleandroid.background.DownloadHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserStatsFragment extends Fragment {
    private static final int MSG_USER_DONE = 101;
    private static final int MENU_SITE_STATS = 201;
    private static final int MENU_RANK_STATS = 202;

    // {"success":true,"statistics":{"visible":"Y","gendisc":"6439","total":"897432","discovered":"498732",
    // "prevmonthcount":"1814","lasttransid":"20151114-00277","monthcount":"34","totallocs":"8615324","gentotal":"9421",
    // "firsttransid":"20010907-01998"},"imageBadgeUrl":"\/bi\/asdf.png","user":"bobzilla","rank":43}

    private static final String KEY_RANK = "rank";
    private static final String KEY_DISCOVERED = "discovered";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_TOTAL_LOCS = "totallocs";
    private static final String KEY_GEN_DISC = "gendisc";
    private static final String KEY_GEN_TOTAL = "gentotal";
    private static final String KEY_MONTH_COUNT = "monthcount";
    private static final String KEY_PREV_MONTH = "prevmonthcount";
    private static final String KEY_FIRST_TRANS = "firsttransid";
    private static final String KEY_LAST_TRANS = "lasttransid";

    private static final String[] ALL_USER_KEYS = new String[] {
            KEY_RANK, KEY_DISCOVERED, KEY_TOTAL, KEY_TOTAL_LOCS, KEY_GEN_DISC,
            KEY_GEN_TOTAL, KEY_MONTH_COUNT, KEY_PREV_MONTH, KEY_FIRST_TRANS, KEY_LAST_TRANS,
        };

    private AtomicBoolean finishing;
    private NumberFormat numberFormat;

    /** Called when the activity is first created. */
    @Override
    public void onCreate( final Bundle savedInstanceState ) {
        MainActivity.info("USERSTATS: onCreate");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // set language
        MainActivity.setLocale(getActivity());

        // media volume
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        finishing = new AtomicBoolean(false);
        numberFormat = NumberFormat.getNumberInstance(Locale.US);
        if (numberFormat instanceof DecimalFormat) {
            numberFormat.setMinimumFractionDigits(0);
            numberFormat.setMaximumFractionDigits(2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final int orientation = getResources().getConfiguration().orientation;
        MainActivity.info("USERSTATS: onCreateView. orientation: " + orientation);
        final ScrollView scrollView = (ScrollView) inflater.inflate(R.layout.userstats, container, false);

        final Handler handler = new UserDownloadHandler(scrollView, numberFormat, getActivity().getPackageName(),
                getResources());
        final ApiDownloader task = new ApiDownloader(getActivity(), ListFragment.lameStatic.dbHelper,
                "user-stats-cache.json", MainActivity.USER_STATS_URL, false, true, true,
                new ApiListener() {
                    @Override
                    public void requestComplete(final JSONObject json) {
                        handleUserStats(json, handler);
                    }
                });
        task.startDownload(this);

        return scrollView;
    }

    private final static class UserDownloadHandler extends DownloadHandler {
        private UserDownloadHandler(final View view, final NumberFormat numberFormat, final String packageName,
                                final Resources resources) {
            super(view, numberFormat, packageName, resources);
        }

        @Override
        public void handleMessage(final Message msg) {
            final Bundle bundle = msg.getData();

            if (msg.what == MSG_USER_DONE) {
                TextView tv;

                for (final String key : ALL_USER_KEYS) {
                    int id = resources.getIdentifier(key, "id", packageName);
                    tv = (TextView) view.findViewById(id);
                    switch (key) {
                        case KEY_FIRST_TRANS:
                        case KEY_LAST_TRANS:
                            tv.setText(bundle.getString(key));
                            break;
                        default:
                            tv.setText(numberFormat.format(bundle.getLong(key)));
                    }
                }
            }
        }
    }

    private void handleUserStats(final JSONObject json, final Handler handler) {
        MainActivity.info("handleUserStats");
        if (json == null) {
            MainActivity.info("handleUserStats null json, returning");
            return;
        }
        MainActivity.info("user stats: " + json);

        final Bundle bundle = new Bundle();
        try {
            final JSONObject stats = json.getJSONObject("statistics");
            for (final String key : ALL_USER_KEYS) {
                final JSONObject lookupJson = (KEY_RANK.equals(key)) ? json : stats;
                switch (key) {
                    case KEY_FIRST_TRANS:
                    case KEY_LAST_TRANS:
                        bundle.putString(key, lookupJson.getString(key));
                        break;
                    default:
                        bundle.putLong(key, lookupJson.getLong(key));
                }
            }
        }
        catch (final JSONException ex) {
            MainActivity.error("json error: " + ex, ex);
        }

        final Message message = new Message();
        message.setData(bundle);
        message.what = MSG_USER_DONE;
        handler.sendMessage(message);
    }

    @Override
    public void onDestroy() {
        MainActivity.info( "STATS: onDestroy" );
        finishing.set( true );

        super.onDestroy();
    }

    @Override
    public void onResume() {
        MainActivity.info("STATS: onResume");
        super.onResume();
        getActivity().setTitle(R.string.user_stats_app_name);
    }

    @Override
    public void onStart() {
        MainActivity.info( "STATS: onStart" );
        super.onStart();
    }

    @Override
    public void onPause() {
        MainActivity.info( "STATS: onPause" );
        super.onPause();
    }

    @Override
    public void onStop() {
        MainActivity.info( "STATS: onStop" );
        super.onStop();
    }

    @Override
    public void onConfigurationChanged( final Configuration newConfig ) {
        MainActivity.info("STATS: config changed");
        super.onConfigurationChanged( newConfig );
    }

    /* Creates the menu items */
    @Override
    public void onCreateOptionsMenu (final Menu menu, final MenuInflater inflater) {
        MenuItem item = menu.add(0, MENU_SITE_STATS, 0, getString(R.string.site_stats_app_name));
        item.setIcon( R.drawable.wiglewifi_small_black_white );
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, MENU_SITE_STATS, 0, getString(R.string.site_stats_app_name));
        item.setIcon(R.drawable.wiglewifi_small_black_white);

        item = menu.add(0, MENU_RANK_STATS, 0, getString(R.string.rank_stats_app_name));
        item.setIcon(android.R.drawable.ic_menu_sort_by_size);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(0, MENU_RANK_STATS, 0, getString(R.string.rank_stats_app_name));
        item.setIcon(android.R.drawable.ic_menu_sort_by_size);

        super.onCreateOptionsMenu(menu, inflater);
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected( final MenuItem item ) {
        final MainActivity main = MainActivity.getMainActivity();
        switch ( item.getItemId() ) {
            case MENU_SITE_STATS:
                main.selectFragment(MainActivity.SITE_STATS_TAB_POS);
                return true;
            case MENU_RANK_STATS:
                main.selectFragment(MainActivity.RANK_STATS_TAB_POS);
                return true;
        }
        return false;
    }

}
