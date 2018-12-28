package com.clevertap.android.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class CTInboxMessageAdapter extends RecyclerView.Adapter {

    private ArrayList<CTInboxMessage> inboxMessages;
    private SimpleExoPlayer player;
    private Context context;
    private int dotsCount;
    private CTInboxMessage inboxMessage;
    private Fragment fragment;
    private PlayerView playerView;
    private static final int SIMPLE = 0;
    private static final int ICON = 1;
    private static final int CAROUSEL = 2;
    private static final int IMAGE_CAROUSEL = 3;

    CTInboxMessageAdapter(ArrayList<CTInboxMessage> inboxMessages, Activity activity, Fragment fragment){
        this.inboxMessages = inboxMessages;
        this.context = activity;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType){
            case SIMPLE :
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_simple_message_layout,viewGroup,false);
                CTSimpleMessageViewHolder ctSimpleMessageViewHolder = new CTSimpleMessageViewHolder(view);
                return ctSimpleMessageViewHolder;
            case ICON:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_icon_message_layout,viewGroup,false);
                CTIconMessageViewHolder ctIconMessageViewHolder = new CTIconMessageViewHolder(view);
                return ctIconMessageViewHolder;
            case CAROUSEL:
            case IMAGE_CAROUSEL:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.inbox_carousel_layout,viewGroup,false);
                CTCarouselMessageViewHolder ctCarouselMessageViewHolder = new CTCarouselMessageViewHolder(view);
                return ctCarouselMessageViewHolder;
        }
        return null;
    }

    @Override
    public void onBindViewHolder(final @NonNull RecyclerView.ViewHolder viewHolder, int i) {
        inboxMessage = this.inboxMessages.get(i);
        if(inboxMessage != null){
            switch (viewHolder.getItemViewType()){
                case SIMPLE:
                    ((CTSimpleMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTSimpleMessageViewHolder)viewHolder).title.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    ((CTSimpleMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    ((CTSimpleMessageViewHolder)viewHolder).message.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getMessageColor()));
                    ((CTSimpleMessageViewHolder)viewHolder).bodyRelativeLayout.setBackgroundColor(Color.parseColor(inboxMessage.getBgColor()));
                    String displayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTSimpleMessageViewHolder)viewHolder).timestamp.setText(displayTimestamp);
                    ((CTSimpleMessageViewHolder)viewHolder).timestamp.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    if(inboxMessage.isRead()){
                        ((CTSimpleMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTSimpleMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    JSONArray linksArray = inboxMessage.getInboxMessageContents().get(0).getLinks();
                    if(linksArray != null){
                        int size = linksArray.length();
                        JSONObject cta1Object,cta2Object,cta3Object;
                        try {
                        switch (size){

                            case 1:
                                cta1Object = linksArray.getJSONObject(0);
                                ((CTSimpleMessageViewHolder) viewHolder).cta1.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder) viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                ((CTSimpleMessageViewHolder) viewHolder).cta1.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta1Object)));
                                hideTwoButtons(((CTSimpleMessageViewHolder) viewHolder).cta1, ((CTSimpleMessageViewHolder) viewHolder).cta2, ((CTSimpleMessageViewHolder) viewHolder).cta3);
                                break;
                            case 2:
                                cta1Object = linksArray.getJSONObject(0);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta1Object)));
                                cta2Object = linksArray.getJSONObject(1);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta2Object)));
                                hideOneButton(((CTSimpleMessageViewHolder)viewHolder).cta1,((CTSimpleMessageViewHolder)viewHolder).cta2,((CTSimpleMessageViewHolder)viewHolder).cta3);;
                                break;
                            case 3:
                                cta1Object = linksArray.getJSONObject(0);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                ((CTSimpleMessageViewHolder)viewHolder).cta1.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta1Object)));
                                cta2Object = linksArray.getJSONObject(1);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                ((CTSimpleMessageViewHolder)viewHolder).cta2.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta2Object)));
                                cta3Object = linksArray.getJSONObject(2);
                                ((CTSimpleMessageViewHolder)viewHolder).cta3.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).cta3.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta3Object));
                                ((CTSimpleMessageViewHolder)viewHolder).cta3.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta3Object)));
                                break;
                            }
                        }catch (JSONException e){
                            Logger.d("Error parsing CTA JSON - "+e.getLocalizedMessage());
                        }
                    }else{
                        ((CTSimpleMessageViewHolder)viewHolder).ctaLinearLayout.setVisibility(View.GONE);
                    }
                    switch (inboxMessage.getOrientation()){
                        case "l" :
                            if(inboxMessage.getInboxMessageContents().get(0).mediaIsImage()) {
                                ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                Glide.with(((CTSimpleMessageViewHolder)viewHolder).mediaImage.getContext())
                                        .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                        .into(((CTSimpleMessageViewHolder)viewHolder).mediaImage);
                            } else if(inboxMessage.getInboxMessageContents().get(0).mediaIsGIF()){
                                ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                                ((CTSimpleMessageViewHolder)viewHolder).mediaImage.setScaleType(ImageView.ScaleType.FIT_XY);
                                Glide.with(((CTSimpleMessageViewHolder)viewHolder).mediaImage.getContext())
                                        .asGif()
                                        .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                        .into(((CTSimpleMessageViewHolder)viewHolder).mediaImage);
                            }else if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()) {
                                addVideoView(inboxMessage.getType(),viewHolder, context,i);
                            }
                            break;
                        case "p" : if(inboxMessage.getInboxMessageContents().get(0).mediaIsImage()) {
                            ((CTSimpleMessageViewHolder)viewHolder).squareImage.setVisibility(View.VISIBLE);
                            ((CTSimpleMessageViewHolder)viewHolder).squareImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Glide.with(((CTSimpleMessageViewHolder)viewHolder).squareImage.getContext())
                                    .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                    .into(((CTSimpleMessageViewHolder)viewHolder).squareImage);
                        } else if(inboxMessage.getInboxMessageContents().get(0).mediaIsGIF()){
                            ((CTSimpleMessageViewHolder)viewHolder).squareImage.setVisibility(View.VISIBLE);
                            ((CTSimpleMessageViewHolder)viewHolder).squareImage.setScaleType(ImageView.ScaleType.FIT_XY);
                            Glide.with(((CTSimpleMessageViewHolder)viewHolder).squareImage.getContext())
                                    .asGif()
                                    .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                    .into(((CTSimpleMessageViewHolder)viewHolder).squareImage);
                        }else if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()) {
                            addVideoView(inboxMessage.getType(),viewHolder, context,i);
                        }
                        break;
                    }
                    final int position = i;
                    Runnable simpleRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if(fragment != null){
                                ((CTInboxTabBaseFragment)fragment).markReadForMessageId(inboxMessage);
                                ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CTInboxTabBaseFragment)fragment).didShow(null,position);
                                        ((CTSimpleMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                                    }
                                });
                            }else if(context != null){
                                ((CTInboxActivity)context).markReadForMessageId(inboxMessage);
                                ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CTInboxActivity)context).didShow(null,inboxMessages.get(position));
                                        ((CTSimpleMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                                    }
                                });
                            }

                        }
                    };
                    Handler simpleHandler = new Handler();
                    simpleHandler.postDelayed(simpleRunnable,2000);
                    if(fragment!=null) {
                        ((CTSimpleMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, null,fragment));
                        ((CTSimpleMessageViewHolder) viewHolder).cta1.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTSimpleMessageViewHolder) viewHolder).cta1, fragment));
                        ((CTSimpleMessageViewHolder) viewHolder).cta2.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTSimpleMessageViewHolder) viewHolder).cta2, fragment));
                        ((CTSimpleMessageViewHolder) viewHolder).cta3.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTSimpleMessageViewHolder) viewHolder).cta3, fragment));
                    }else{
                        ((CTSimpleMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i,inboxMessage,null, (Activity) context));
                        ((CTSimpleMessageViewHolder) viewHolder).cta1.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTSimpleMessageViewHolder) viewHolder).cta1, (Activity) context));
                        ((CTSimpleMessageViewHolder) viewHolder).cta2.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTSimpleMessageViewHolder) viewHolder).cta2, (Activity) context));
                        ((CTSimpleMessageViewHolder) viewHolder).cta3.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTSimpleMessageViewHolder) viewHolder).cta3, (Activity) context));
                    }
                    break;
                case ICON:
                    ((CTIconMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTIconMessageViewHolder)viewHolder).title.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    ((CTIconMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    ((CTIconMessageViewHolder)viewHolder).message.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getMessageColor()));
                    ((CTIconMessageViewHolder)viewHolder).clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.getBgColor()));
                    String iconDisplayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTIconMessageViewHolder)viewHolder).timestamp.setText(iconDisplayTimestamp);
                    ((CTIconMessageViewHolder)viewHolder).timestamp.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    if(inboxMessage.isRead()){
                        ((CTIconMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTIconMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    JSONArray iconlinksArray = inboxMessage.getInboxMessageContents().get(0).getLinks();
                    if(iconlinksArray != null){
                        int size = iconlinksArray.length();
                        JSONObject cta1Object,cta2Object,cta3Object;
                        try {
                            switch (size){

                                case 1:
                                    cta1Object = iconlinksArray.getJSONObject(0);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta1Object)));
                                    hideTwoButtons(((CTIconMessageViewHolder)viewHolder).cta1,((CTIconMessageViewHolder)viewHolder).cta2,((CTIconMessageViewHolder)viewHolder).cta3);
                                    break;
                                case 2:
                                    cta1Object = iconlinksArray.getJSONObject(0);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta1Object)));
                                    cta2Object = iconlinksArray.getJSONObject(1);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta2Object)));
                                    hideOneButton(((CTIconMessageViewHolder)viewHolder).cta1,((CTIconMessageViewHolder)viewHolder).cta2,((CTIconMessageViewHolder)viewHolder).cta3);;
                                    break;
                                case 3:
                                    cta1Object = iconlinksArray.getJSONObject(0);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta1Object));
                                    ((CTIconMessageViewHolder)viewHolder).cta1.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta1Object)));
                                    cta2Object = iconlinksArray.getJSONObject(1);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta2Object));
                                    ((CTIconMessageViewHolder)viewHolder).cta2.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta2Object)));
                                    cta3Object = iconlinksArray.getJSONObject(2);
                                    ((CTIconMessageViewHolder)viewHolder).cta3.setVisibility(View.VISIBLE);
                                    ((CTIconMessageViewHolder)viewHolder).cta3.setText(inboxMessage.getInboxMessageContents().get(0).getLinkText(cta3Object));
                                    ((CTIconMessageViewHolder)viewHolder).cta3.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getLinkColor(cta3Object)));
                                    break;
                            }
                        }catch (JSONException e){
                            Logger.d("Error parsing CTA JSON - "+e.getLocalizedMessage());
                        }
                    }
                    switch (inboxMessage.getOrientation()){
                        case "l" :
                            if(inboxMessage.getInboxMessageContents().get(0).mediaIsImage()) {
                                ((CTIconMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                                ((CTIconMessageViewHolder)viewHolder).mediaImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                Glide.with(((CTIconMessageViewHolder)viewHolder).mediaImage.getContext())
                                        .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                        .into(((CTIconMessageViewHolder)viewHolder).mediaImage);
                            } else if(inboxMessage.getInboxMessageContents().get(0).mediaIsGIF()){
                                ((CTIconMessageViewHolder)viewHolder).mediaImage.setVisibility(View.VISIBLE);
                                ((CTIconMessageViewHolder)viewHolder).mediaImage.setScaleType(ImageView.ScaleType.FIT_XY);
                                Glide.with(((CTIconMessageViewHolder)viewHolder).mediaImage.getContext())
                                        .asGif()
                                        .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                        .into(((CTIconMessageViewHolder)viewHolder).mediaImage);
                            }else if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()) {
                                addVideoView(inboxMessage.getType(),viewHolder, context,i);
                            }
                            break;
                        case "p" : if(inboxMessage.getInboxMessageContents().get(0).mediaIsImage()) {
                            ((CTIconMessageViewHolder)viewHolder).squareImage.setVisibility(View.VISIBLE);
                            ((CTIconMessageViewHolder)viewHolder).squareImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Glide.with(((CTIconMessageViewHolder)viewHolder).squareImage.getContext())
                                    .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                    .into(((CTIconMessageViewHolder)viewHolder).squareImage);
                        } else if(inboxMessage.getInboxMessageContents().get(0).mediaIsGIF()){
                            ((CTIconMessageViewHolder)viewHolder).squareImage.setVisibility(View.VISIBLE);
                            ((CTIconMessageViewHolder)viewHolder).squareImage.setScaleType(ImageView.ScaleType.FIT_XY);
                            Glide.with(((CTIconMessageViewHolder)viewHolder).squareImage.getContext())
                                    .asGif()
                                    .load(inboxMessage.getInboxMessageContents().get(0).getMedia())
                                    .into(((CTIconMessageViewHolder)viewHolder).squareImage);
                        }else if(inboxMessage.getInboxMessageContents().get(0).mediaIsVideo()) {
                            addVideoView(inboxMessage.getType(), viewHolder, context, i);
                        }
                        final int imagePosition = i;
                        Runnable iconRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if(fragment != null){
                                    ((CTInboxTabBaseFragment)fragment).markReadForMessageId(inboxMessage);
                                    ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((CTInboxTabBaseFragment)fragment).didShow(null,imagePosition);
                                            ((CTIconMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                                        }
                                    });
                                }else if(context != null){
                                    ((CTInboxActivity)context).markReadForMessageId(inboxMessage);
                                    ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((CTInboxActivity)context).didShow(null,inboxMessages.get(imagePosition));
                                            ((CTIconMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                                        }
                                    });
                                }
                            }
                        };
                        Handler iconHandler = new Handler();
                        iconHandler.postDelayed(iconRunnable,2000);

                        if(!inboxMessage.getInboxMessageContents().get(0).getIcon().isEmpty()) {
                            ((CTIconMessageViewHolder)viewHolder).iconImage.setVisibility(View.VISIBLE);
                            Glide.with(((CTIconMessageViewHolder) viewHolder).iconImage.getContext())
                                    .load(inboxMessage.getInboxMessageContents().get(0).getIcon())
                                    .into(((CTIconMessageViewHolder) viewHolder).iconImage);
                        }else{
                            ((CTIconMessageViewHolder)viewHolder).iconImage.setVisibility(View.GONE);
                        }
                        break;
                    }
                    if(fragment!=null) {
                        ((CTIconMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, null,fragment));
                        ((CTIconMessageViewHolder) viewHolder).cta1.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTIconMessageViewHolder) viewHolder).cta1, fragment));
                        ((CTIconMessageViewHolder) viewHolder).cta2.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTIconMessageViewHolder) viewHolder).cta2, fragment));
                        ((CTIconMessageViewHolder) viewHolder).cta3.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTIconMessageViewHolder) viewHolder).cta3, fragment));
                    }else{
                        ((CTIconMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i,inboxMessage,null, (Activity) context));
                        ((CTIconMessageViewHolder) viewHolder).cta1.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTIconMessageViewHolder) viewHolder).cta1, (Activity) context));
                        ((CTIconMessageViewHolder) viewHolder).cta2.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTIconMessageViewHolder) viewHolder).cta2, (Activity) context));
                        ((CTIconMessageViewHolder) viewHolder).cta3.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage, ((CTIconMessageViewHolder) viewHolder).cta3, (Activity) context));
                    }
                    break;
                case CAROUSEL:
                    ((CTCarouselMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(0).getTitle());
                    ((CTCarouselMessageViewHolder)viewHolder).title.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    ((CTCarouselMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(0).getMessage());
                    ((CTCarouselMessageViewHolder)viewHolder).message.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getMessageColor()));
                    ((CTCarouselMessageViewHolder)viewHolder).clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.getBgColor()));
                    String carouselDisplayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setText(carouselDisplayTimestamp);
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    if(inboxMessage.isRead()){
                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    }else{
                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.VISIBLE);
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).carouselTimestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                    final int carouselPosition = i;
                    LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.getLayoutParams();
                    CTCarouselViewPagerAdapter carouselViewPagerAdapter = new CTCarouselViewPagerAdapter(context,inboxMessage,layoutParams,carouselPosition);
                    ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.setAdapter(carouselViewPagerAdapter);
                    dotsCount = carouselViewPagerAdapter.getCount();
                    ImageView[] dots = new ImageView[dotsCount];
                    for(int k=0;k<dotsCount;k++){
                        dots[k] = new ImageView(context);
                        dots[k].setVisibility(View.VISIBLE);
                        dots[k].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.unselected_dot));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(8, 0, 8, 0);
                        params.gravity = Gravity.CENTER;
                        ((CTCarouselMessageViewHolder)viewHolder).sliderDots.addView(dots[k],params);
                    }
                    dots[0].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.selected_dot));
                    CarouselPageChangeListener carouselPageChangeListener = new CarouselPageChangeListener((CTCarouselMessageViewHolder)viewHolder,dots,inboxMessage);
                    ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.addOnPageChangeListener(carouselPageChangeListener);

                    if(fragment!=null) {
                        ((CTCarouselMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage,null,fragment,((CTCarouselMessageViewHolder)viewHolder).imageViewPager));
                    }else{
                        ((CTCarouselMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage,null, (Activity) context,((CTCarouselMessageViewHolder)viewHolder).imageViewPager));
                    }

                    Runnable carouselRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if(fragment != null){
                                ((CTInboxTabBaseFragment)fragment).markReadForMessageId(inboxMessage);
                                ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CTInboxTabBaseFragment)fragment).didShow(null,carouselPosition);
                                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                                    }
                                });
                            }else if(context != null){
                                ((CTInboxActivity)context).markReadForMessageId(inboxMessage);
                                ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CTInboxActivity)context).didShow(null,inboxMessages.get(carouselPosition));
                                        ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }
                    };
                    Handler carouselHandler = new Handler();
                    carouselHandler.postDelayed(carouselRunnable,2000);
                    break;
                case IMAGE_CAROUSEL:
                    ((CTCarouselMessageViewHolder)viewHolder).title.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).message.setVisibility(View.GONE);
                    String carouselImageDisplayTimestamp  = calculateDisplayTimestamp(inboxMessage.getDate());
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setText(carouselImageDisplayTimestamp);
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(0).getTitleColor()));
                    ((CTCarouselMessageViewHolder)viewHolder).clickLayout.setBackgroundColor(Color.parseColor(inboxMessage.getBgColor()));
                    if(inboxMessage.isRead()){
                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                    }else{
                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.VISIBLE);
                    }
                    ((CTCarouselMessageViewHolder)viewHolder).timestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).readDot.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).carouselTimestamp.setVisibility(View.GONE);
                    ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                    final int imageCarouselPos = i;
                    LinearLayout.LayoutParams layoutImageParams = (LinearLayout.LayoutParams) ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.getLayoutParams();
                    CTCarouselViewPagerAdapter carouselImageViewPagerAdapter = new CTCarouselViewPagerAdapter(context,inboxMessage,layoutImageParams,imageCarouselPos);
                    ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.setAdapter(carouselImageViewPagerAdapter);
                    dotsCount = carouselImageViewPagerAdapter.getCount();
                    ImageView[] imageDots = new ImageView[dotsCount];
                    for(int k=0;k<dotsCount;k++){
                        imageDots[k] = new ImageView(context);
                        imageDots[k].setVisibility(View.VISIBLE);
                        imageDots[k].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.unselected_dot));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(8, 0, 8, 0);
                        params.gravity = Gravity.CENTER;
                        ((CTCarouselMessageViewHolder)viewHolder).sliderDots.addView(imageDots[k],params);
                    }
                    imageDots[0].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.selected_dot));
                    CarouselPageChangeListener carouselImagePageChangeListener = new CarouselPageChangeListener((CTCarouselMessageViewHolder)viewHolder,imageDots,inboxMessage);
                    ((CTCarouselMessageViewHolder)viewHolder).imageViewPager.addOnPageChangeListener(carouselImagePageChangeListener);
                    if(fragment!=null) {
                        ((CTCarouselMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage,null,fragment,((CTCarouselMessageViewHolder)viewHolder).imageViewPager));
                    }else{
                        ((CTCarouselMessageViewHolder) viewHolder).clickLayout.setOnClickListener(new CTInboxButtonClickListener(i, inboxMessage,null, (Activity) context,((CTCarouselMessageViewHolder)viewHolder).imageViewPager));
                    }

                    Runnable runnableImage = new Runnable() {
                        @Override
                        public void run() {
                            if(fragment != null){
                                ((CTInboxTabBaseFragment)fragment).markReadForMessageId(inboxMessage);
                                ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CTInboxTabBaseFragment)fragment).didShow(null,imageCarouselPos);
                                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                                    }
                                });
                            }else if(context != null){
                                ((CTInboxActivity)context).markReadForMessageId(inboxMessage);
                                ((CTInboxActivity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((CTInboxActivity)context).didShow(null,inboxMessages.get(imageCarouselPos));
                                        ((CTCarouselMessageViewHolder)viewHolder).carouselReadDot.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }
                    };
                    Handler imageHandler = new Handler();
                    imageHandler.postDelayed(runnableImage,2000);
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return inboxMessages.size();
    }

    private void hideTwoButtons(Button mainButton, Button secondaryButton, Button tertiaryButton){
        secondaryButton.setVisibility(View.GONE);
        tertiaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,6);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
        LinearLayout.LayoutParams tertiaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        tertiaryButton.setLayoutParams(tertiaryLayoutParams);
    }

    private void hideOneButton(Button mainButton, Button secondaryButton, Button tertiaryButton){
        tertiaryButton.setVisibility(View.GONE);
        LinearLayout.LayoutParams mainLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,3);
        mainButton.setLayoutParams(mainLayoutParams);
        LinearLayout.LayoutParams secondaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,3);
        secondaryButton.setLayoutParams(secondaryLayoutParams);
        LinearLayout.LayoutParams tertiaryLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT,0);
        tertiaryButton.setLayoutParams(tertiaryLayoutParams);
    }

    private String calculateDisplayTimestamp(int time){
        long now = System.currentTimeMillis();
        long diff = now-time;
        if(diff < 60*1000){
            return "Just Now";
        }else if(diff > 60*1000 && diff < 59*60*1000){
            return (diff/(60*1000)) + "mins ago";
        }else if(diff > 59*60*1000 && diff < 23*59*60*1000 ){
            return diff/(60*60*1000) + "hours ago";
        }else if(diff > 24*60*60*1000 && diff < 48*60*60*1000){
            return "Yesterday";
        }else {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd MMM");
            return sdf.format(new Date(time));
        }
    }

    private void addVideoView(CTInboxMessageType inboxMessageType, RecyclerView.ViewHolder viewHolder, Context context, int pos){
        playerView = new PlayerView(context);
        playerView.setTag(pos);
        playerView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.WRAP_CONTENT));
        playerView.setShowBuffering(true);
        playerView.setUseArtwork(true);
        playerView.setControllerAutoShow(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        // 2. Create the player
        player = ExoPlayerFactory.newSimpleInstance(context, trackSelector);
        // 3. Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context,
                Util.getUserAgent(context, context.getPackageName()), (TransferListener<? super DataSource>) bandwidthMeter);
        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(inboxMessage.getInboxMessageContents().get(0).getMedia()));
        // 4. Prepare the player with the source.
        player.prepare(hlsMediaSource);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.seekTo(1000);
        playerView.requestFocus();
        playerView.setVisibility(View.VISIBLE);
        playerView.setPlayer(player);
        player.setPlayWhenReady(false);

        switch (inboxMessageType){
            case IconMessage:
                CTIconMessageViewHolder iconMessageViewHolder = (CTIconMessageViewHolder) viewHolder;

                iconMessageViewHolder.iconMessageFrameLayout.addView(playerView);
                iconMessageViewHolder.iconMessageFrameLayout.setVisibility(View.VISIBLE);
            case SimpleMessage:
                CTSimpleMessageViewHolder simpleMessageViewHolder = (CTSimpleMessageViewHolder) viewHolder;

                simpleMessageViewHolder.simpleMessageFrameLayout.addView(playerView);
                simpleMessageViewHolder.simpleMessageFrameLayout.setVisibility(View.VISIBLE);
                break;
        }


    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

    }

    @Override
    public int getItemViewType(int position) {
        switch (inboxMessages.get(position).getType()){
            case SimpleMessage: return SIMPLE;
            case IconMessage: return ICON;
            case CarouselMessage: return CAROUSEL;
            case CarouselImageMessage: return IMAGE_CAROUSEL;
            default:return -1;
        }
    }

    void filterMessages(final String tab){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(tab.equalsIgnoreCase("all"))
                    return;
                ArrayList<CTInboxMessage> filteredMessages = new ArrayList<>();
                for(CTInboxMessage inboxMessage : inboxMessages){
                    if(inboxMessage.getTags().contains(tab)){
                        filteredMessages.add(inboxMessage);
                    }
                }
                inboxMessages = filteredMessages;
                ((Activity)context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }



    class CarouselPageChangeListener implements ViewPager.OnPageChangeListener{
        private RecyclerView.ViewHolder viewHolder;
        private ImageView[] dots;
        private CTInboxMessage inboxMessage;
        CarouselPageChangeListener(RecyclerView.ViewHolder viewHolder, ImageView[] dots, CTInboxMessage inboxMessage){
            this.viewHolder = viewHolder;
            this.dots = dots;
            this.inboxMessage = inboxMessage;
        }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int position) {
        for(int i = 0; i< dotsCount; i++){
            dots[i].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.unselected_dot));
        }
        dots[position].setImageDrawable(context.getApplicationContext().getResources().getDrawable(R.drawable.selected_dot));
        ((CTCarouselMessageViewHolder)viewHolder).title.setText(inboxMessage.getInboxMessageContents().get(position).getTitle());
        ((CTCarouselMessageViewHolder)viewHolder).title.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(position).getTitleColor()));
        ((CTCarouselMessageViewHolder)viewHolder).message.setText(inboxMessage.getInboxMessageContents().get(position).getMessage());
        ((CTCarouselMessageViewHolder)viewHolder).message.setTextColor(Color.parseColor(inboxMessage.getInboxMessageContents().get(position).getMessageColor()));
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
}

    private class TapGestureListener extends GestureDetector.SimpleOnGestureListener {

        private ViewPager viewPager;
        private int row;
        TapGestureListener(ViewPager viewPager, int row){
            this.viewPager = viewPager;
            this.row = row;
        }
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            if(fragment != null){
                ((CTInboxTabBaseFragment)fragment).handleViewPagerClick(row,viewPager.getCurrentItem());
            }else if(context != null){
                ((CTInboxActivity)context).handleViewPagerClick(row,viewPager.getCurrentItem());
            }

            return super.onSingleTapConfirmed(e);
        }
    }

}
