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
package net.atoom.android.youtube;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import net.atoom.android.util.LogBridge;
import net.atoom.android.wkn.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedAdaptor extends BaseAdapter {

	public final static DateFormat DATE_FORMAT_IN = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	public final static DateFormat DATE_FORMAT_OUT = new SimpleDateFormat("MMM dd, yyyy");

	private final Context m_context;
	private final int m_background;
	private final List<FeedEntry> m_entries = new LinkedList<FeedEntry>();

	public FeedAdaptor(Context context) {
		m_context = context;
		TypedArray attr = m_context.obtainStyledAttributes(R.styleable.HelloGallery);
		m_background = attr.getResourceId(R.styleable.HelloGallery_android_galleryItemBackground, 0);
		attr.recycle();
	}

	public void addEntry(FeedEntry entry) {
		m_entries.add(entry);
	}

	@Override
	public int getCount() {
		return m_entries.size();
	}

	@Override
	public Object getItem(int i) {
		return null;
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	private int m_bitMapScale = -1;

	@Override
	public View getView(int i, View convertView, ViewGroup parent) {

		FeedEntry entry = m_entries.get(i);

		LinearLayout hlv;
		LinearLayout vlv;
		ImageView iv;
		TextView titleView;
		TextView dateView;

		if (convertView != null && convertView instanceof LinearLayout) {

			hlv = (LinearLayout) convertView;
			iv = (ImageView) hlv.getChildAt(0);
			vlv = (LinearLayout) hlv.getChildAt(1);
			titleView = (TextView) vlv.getChildAt(0);
			dateView = (TextView) vlv.getChildAt(1);

		} else {

			hlv = new LinearLayout(m_context);
			hlv.setBackgroundResource(m_background);
			hlv.setOrientation(LinearLayout.HORIZONTAL);
			hlv.setBackgroundColor(Color.WHITE);
			hlv.setPadding(5, 5, 5, 5);

			vlv = new LinearLayout(m_context);
			vlv.setBackgroundResource(m_background);
			vlv.setOrientation(LinearLayout.VERTICAL);
			vlv.setBackgroundColor(Color.WHITE);
			vlv.setPadding(5, 5, 5, 5);

			iv = new ImageView(m_context);
			iv.setLayoutParams(new ViewGroup.LayoutParams(150, 100));
			iv.setScaleType(ImageView.ScaleType.FIT_XY);

			titleView = new TextView(m_context);
			titleView.setTextColor(Color.parseColor("#505050"));
			dateView = new TextView(m_context);
			dateView.setTextColor(Color.parseColor("#505050"));
			dateView.setGravity(Gravity.RIGHT);

			hlv.addView(iv);
			hlv.addView(vlv);
			vlv.addView(titleView);
			vlv.addView(dateView);
		}

		if (entry.getThumbnailResourceEntity() == null) {
			iv.setImageResource(R.drawable.wknkopano4);
		} else {
			InputStream is;
			try {
				if (m_bitMapScale == -1) {
					BitmapFactory.Options o = new BitmapFactory.Options();
					o.inJustDecodeBounds = true;

					is = entry.getThumbnailResourceEntity().getInputStream();
					BitmapFactory.decodeStream(is, null, o);
					is.close();

					if (o.outHeight > 150 || o.outWidth > 150) {
						m_bitMapScale = (int) Math.pow(2, (int) Math.round(Math.log(150 / (double) Math.max(
								o.outHeight, o.outWidth))
								/ Math.log(0.5)));
					}

					LogBridge.w("Computed scale to " + m_bitMapScale);
				}

				BitmapFactory.Options o2 = new BitmapFactory.Options();
				o2.inSampleSize = m_bitMapScale;
				is = entry.getThumbnailResourceEntity().getInputStream();
				iv.setImageBitmap(BitmapFactory.decodeStream(is, null, o2));
				is.close();
			} catch (IOException e) {
			}
		}

		titleView.setText(entry.getTitle());

		try {
			Date date = DATE_FORMAT_IN.parse(entry.getPublished());
			dateView.setText(DATE_FORMAT_OUT.format(date));
		} catch (ParseException e) {
		}

		return hlv;
	}
}
