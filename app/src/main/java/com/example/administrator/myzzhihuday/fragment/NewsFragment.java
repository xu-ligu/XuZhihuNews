package com.example.administrator.myzzhihuday.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.administrator.myzzhihuday.R;
import com.example.administrator.myzzhihuday.activity.MainActivity;
import com.example.administrator.myzzhihuday.activity.NewsContentActivity;
import com.example.administrator.myzzhihuday.activity.latestContentActivity;
import com.example.administrator.myzzhihuday.adapter.NewsItemAdapter;
import com.example.administrator.myzzhihuday.model.News;
import com.example.administrator.myzzhihuday.model.StoriesEntity;
import com.example.administrator.myzzhihuday.util.Constant;
import com.example.administrator.myzzhihuday.util.HttpUtils;
import com.example.administrator.myzzhihuday.util.PreUtils;
import com.google.gson.Gson;
import com.loopj.android.http.TextHttpResponseHandler;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.net.HttpCookie;
import java.util.List;

import cz.msebera.android.httpclient.Header;


@SuppressLint("ValidFragment")
public class NewsFragment extends BaseFragment {
    @Override
    public void onResume() {
        ((MainActivity)mActivity).setToolbarTitle(title);
        super.onResume();
    }

    private ImageLoader mImageLoader;
    private ListView lv_news;
    private ImageView iv_title;
    private TextView tv_title;
    private NewsItemAdapter mAdapter;
    private String title;
    private String urlId;
    private News news;


    public NewsFragment(String id,String title){
        urlId=id;
        this.title=title;
    }

    @Override
    protected void initData() {
        super.initData();

        if(HttpUtils.isNetWorkConnected(mActivity)){
            HttpUtils.get(Constant.THEMENEWS + urlId, new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    SQLiteDatabase db=((MainActivity)mActivity).getdbHelper().getWritableDatabase();
                    db.execSQL("replace into Cache(date,json) values("+Constant.BASE_COLUMN+",'"+responseString+"')");
                    db.close();
                    parseJson(responseString);


                }
            });
        }else{
            SQLiteDatabase db=((MainActivity)mActivity).getdbHelper().getReadableDatabase();
            Cursor cursor=db.rawQuery("select * from Cache where date="+Constant.BASE_COLUMN,null);
            if(cursor.moveToFirst()){
                String response=cursor.getString(cursor.getColumnIndex("json"));
                parseJson(response);
            }else {

            }
            cursor.close();
            db.close();
        }
    }

    @Override
    protected View initView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ((MainActivity)mActivity).setToolbarTitle(title);
        View view=inflater.inflate(R.layout.main_news_layout, container, false);
        mImageLoader=ImageLoader.getInstance();
        lv_news=(ListView)view.findViewById(R.id.lv_news);
        View header=LayoutInflater.from(mActivity).inflate(R.layout.news_header,lv_news,false);
        tv_title=(TextView)header.findViewById(R.id.tv_title);
        iv_title=(ImageView)header.findViewById(R.id.iv_title);
        lv_news.addHeaderView(header);
        lv_news.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (lv_news != null && lv_news.getChildCount() > 0) {
                    boolean enable = (firstVisibleItem == 0) && (view.getChildAt(firstVisibleItem).getTop() == 0);
                    ((MainActivity) mActivity).setSwipeRefreshEnable(enable);
                }

            }
        });
        lv_news.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int[] startingLocation=new int[2];
                view.getLocationOnScreen(startingLocation);
                startingLocation[0]+=view.getWidth()/2;
                StoriesEntity entity=(StoriesEntity)parent.getAdapter().getItem(position);
                Intent intent=new Intent(mActivity,NewsContentActivity.class);
                intent.putExtra(Constant.START_LOCATION, startingLocation);
                intent.putExtra("entity",entity);
                intent.putExtra("isLight",((MainActivity)mActivity).isLight());
                String readSequence = PreUtils.getStringFromDefault(mActivity, "read", "");
                String[] splits = readSequence.split(",");
                StringBuffer sb = new StringBuffer();
                if (splits.length >= 200) {
                    for (int i = 100; i < splits.length; i++) {
                        sb.append(splits[i] + ",");
                    }
                    readSequence = sb.toString();
                }

                if (!readSequence.contains(entity.getId() + "")) {
                    readSequence = readSequence + entity.getId() + ",";
                }
                PreUtils.putStringToDefault(mActivity, "read", readSequence);
                TextView tv_title = (TextView) view.findViewById(R.id.tv_title);
                tv_title.setTextColor(getResources().getColor(R.color.clicked_tv_textcolor));

                startActivity(intent);
                mActivity.overridePendingTransition(0,0);
            }
        });

        return view;
    }

    public void parseJson(String responseString){
        Gson gson=new Gson();
        news=gson.fromJson(responseString,News.class);
        DisplayImageOptions options=new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        tv_title.setText(news.getDescription());
        mImageLoader.displayImage(news.getImage(), iv_title, options);
        mAdapter=new NewsItemAdapter(mActivity,news.getStories());
        lv_news.setAdapter(mAdapter);
    }
    public void updateTheme(){
        mAdapter.updateTheme();
    }
}
