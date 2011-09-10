/**
 *    Copyright 2010 Bram de Kruijff <bdekruijff [at] gmail [dot] com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package net.atoom.android.wkn;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.atoom.android.wkn.R;
import net.atoom.android.res.ResourceLoader;
import net.atoom.android.youtube.Feed;
import net.atoom.android.youtube.FeedAdaptor;
import net.atoom.android.youtube.FeedEntry;
import net.atoom.android.youtube.FeedLoadListener;
import net.atoom.android.youtube.FeedLoader;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class WKNActivity extends Activity {

	public final static String LOGGING_TAG = "KNN";
	public final static String YOUTUBE_FEED = "http://gdata.youtube.com/feeds/api/users/worldkidsnews/uploads";

	private final static long MIN_SPLASHTIME = 2000;
	private final static long FEED_STALETIME = 60000 * 60;
	private final static DateFormat DATE_FORMAT_IN = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	private final static DateFormat DATE_FORMAT_OUT = new SimpleDateFormat("MMM dd, yyyy '-' HH:mm");

	private Handler m_handler;
	private ExecutorService m_executorService;
	private FeedLoader m_feedLoader;
	private ResourceLoader m_resourceLoader;
	private FeedAdaptor m_feedAdaptor;

	private File m_cacheDir;
	private Feed m_feed;

	private long m_startTime = System.currentTimeMillis();
	private int m_spinnerState = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
		}

		createCacheDir();
		// for (File file : m_cacheDir.listFiles()) {
		// file.delete();
		// }

		m_startTime = System.currentTimeMillis();
		m_handler = new Handler();
		m_executorService = Executors.newFixedThreadPool(4);
		m_resourceLoader = new ResourceLoader(m_executorService, m_cacheDir);
		m_feedLoader = new FeedLoader(m_resourceLoader, m_executorService);
		m_feedAdaptor = new FeedAdaptor(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().setTitle(getResources().getText(R.string.hello));
		getWindow().setSoftInputMode(1);

		Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
			setContentView(R.layout.wkn_portrait);
		} else {
			setContentView(R.layout.wkn_landscape);
		}

		initializeListView();
		loadFeed(YOUTUBE_FEED);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("STATE", "blah blah blah");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && m_spinnerState == 2) {
			m_spinnerState = 1;
			VideoView videoView = (VideoView) findViewById(R.id.entryview_video);
			videoView.stopPlayback();
			ViewFlipper vf = (ViewFlipper) findViewById(R.id.mainflipper);
			vf.setOutAnimation(vf.getContext(), R.anim.push_right_out);
			vf.setInAnimation(vf.getContext(), R.anim.push_left_in);
			vf.showPrevious();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void initializeListView() {

		final ListView listView = (ListView) findViewById(R.id.listview);
		listView.setAdapter(m_feedAdaptor);

		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int index, long time) {

				FeedEntry entry = m_feed.getFeedEntries().get(index);

				TextView titleView = (TextView) findViewById(R.id.entryview_title);
				titleView.setText(entry.getTitle());

				TextView dateView = (TextView) findViewById(R.id.entryview_date);
				try {
					Date date = DATE_FORMAT_IN.parse(entry.getPublished());
					dateView.setText(DATE_FORMAT_OUT.format(date));
				} catch (ParseException e) {
				}

				TextView tv2 = (TextView) findViewById(R.id.entryview_description);
				tv2.setText(entry.getContent());

				VideoView videoView = (VideoView) findViewById(R.id.entryview_video);
				MediaController mc = new MediaController(WKNActivity.this);
				mc.setAnchorView(videoView);
				Uri video = Uri.parse(entry.getUrl());
				videoView.setMediaController(mc);
				videoView.setVideoURI(video);
				videoView.requestFocus();
				videoView.start();

				ViewFlipper vf = (ViewFlipper) findViewById(R.id.mainflipper);
				vf.setOutAnimation(vf.getContext(), R.anim.push_left_out);
				vf.setInAnimation(vf.getContext(), R.anim.push_right_in);
				vf.showNext();

				m_spinnerState = 2;
			}
		});
	}

	private void loadFeed(final String feedUrl) {
		m_feedLoader.loadFeed(feedUrl, new FeedLoadListener() {

			@Override
			public void feedLoaded(Feed feed) {
				m_feed = feed;
				for (FeedEntry feedEntry : m_feed.getFeedEntries()) {
					m_feedAdaptor.addEntry(feedEntry);
				}
				m_handler.post(new Runnable() {
					public void run() {
						m_feedAdaptor.notifyDataSetChanged();
						hideSplashScreen();
					}
				});
			}

			@Override
			public void entryLoaded(final FeedEntry feedEntry) {
			}

			@Override
			public void entryThumbnailLoaded(FeedEntry feedEntry) {
				m_handler.post(new Runnable() {
					public void run() {
						m_feedAdaptor.notifyDataSetChanged();
					}
				});
			}
		}, FEED_STALETIME);
	}

	private void hideSplashScreen() {
		long activeTime = System.currentTimeMillis() - m_startTime;
		long delayTime = 0l;
		if (activeTime < MIN_SPLASHTIME) {
			delayTime = MIN_SPLASHTIME - activeTime;
		}
		m_handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				ImageView splashImage = (ImageView) findViewById(R.id.splashscreen);
				splashImage.setVisibility(View.GONE);
			}
		}, delayTime);
	}

	private void createCacheDir() {
		File root = Environment.getExternalStorageDirectory();
		m_cacheDir = new File(root, LOGGING_TAG);
		m_cacheDir.mkdir();
	}
}