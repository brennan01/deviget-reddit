package com.brennan.deviget.redditposts.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.transition.Explode;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.brennan.deviget.redditposts.R;
import com.brennan.deviget.redditposts.domain.RedditChildrenResponse;
import com.brennan.deviget.redditposts.domain.RedditDataResponse;
import com.brennan.deviget.redditposts.domain.RedditNewsResponse;
import com.brennan.deviget.redditposts.ui.commons.EndlessRecyclerOnScrollListener;

import java.util.ArrayList;
import java.util.List;


public class ListFragment extends Fragment implements GetRedditPostFragment.Listener {

    private static final String TAG = "ListFragment";
    private static final String POSTS_LIST = "posts_list";
    private static final String READ_POSTS_LIST = "read_posts_list";
    private static final String GET_REDDIT_POSTS_FRAGMENT_TAG = "GET_REDDIT_POSTS_FRAGMENT_TAG";

    private Listener mListener;
    private RecyclerView mRecyclerView;
    private EndlessRecyclerOnScrollListener mEndlessRecyclerOnScrollListener;

    private RedditPostAdapter mAdapter;
    private TextView mDismissAll;
    private SwipeRefreshLayout mSwipeRefreshLayout;


    public static ListFragment newInstance() {
        return new ListFragment();
    }

    interface Listener {
        void onItemClick(RedditChildrenResponse item);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof Listener)) {
            throw new RuntimeException("Context must implement mListener");
        }
        mListener = (Listener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new RedditPostAdapter();
        mAdapter.setListener(new RedditPostAdapter.Listener() {
            @Override
            public void onItemClick(int position, RedditChildrenResponse item) {
                if(mListener != null){
                    mListener.onItemClick(item);
                }
            }

            @Override
            public void onDismissItemClick(int position, RedditChildrenResponse item) {
                mAdapter.remove(position);
                updateDismissView();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(mAdapter);
        mDismissAll = view.findViewById(R.id.dismiss_all);

        mEndlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                loadMoreInfo(mAdapter.getAfter());
            }
        };

        mRecyclerView.addOnScrollListener(mEndlessRecyclerOnScrollListener);

        mSwipeRefreshLayout = view.findViewById(R.id.refresh);
        mSwipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.RED, Color.BLUE);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadFreshInfo();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            RedditDataResponse data = (RedditDataResponse) savedInstanceState.getSerializable(POSTS_LIST);
            ArrayList<RedditChildrenResponse> readPosts = (ArrayList<RedditChildrenResponse>) savedInstanceState.getSerializable(READ_POSTS_LIST);
            mAdapter.init(data, readPosts);
            updateDismissView();
        } else {
            loadFreshInfo();
        }

        mDismissAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Rect viewRect = new Rect();

                Transition explode = new Explode();
                explode.setEpicenterCallback(new Transition.EpicenterCallback() {
                            @Override
                            public Rect onGetEpicenter(Transition transition) {
                                return viewRect;
                            }
                        });
                explode.setDuration(1000);
                TransitionManager.beginDelayedTransition(mRecyclerView, explode);

                mEndlessRecyclerOnScrollListener.reset();
                mAdapter.clear();
                updateDismissView();

            }
        });

    }

    private void loadFreshInfo(){
        mEndlessRecyclerOnScrollListener.reset();
        mAdapter.clear();
        loadMoreInfo("");
    }

    private void loadMoreInfo(String after) {

        FragmentManager fragmentManager = getFragmentManager();
        GetRedditPostFragment getRedditPostFragment = (GetRedditPostFragment) getFragmentManager()
                .findFragmentByTag(GET_REDDIT_POSTS_FRAGMENT_TAG);

        if(getRedditPostFragment == null){
            getRedditPostFragment = GetRedditPostFragment.newInstance();
            fragmentManager.beginTransaction()
                    .add(getRedditPostFragment, GET_REDDIT_POSTS_FRAGMENT_TAG)
                    .commit();
        }
        getRedditPostFragment.setListener(this);
        getRedditPostFragment.setContext(getContext());
        getRedditPostFragment.getRedditPosts(after);
    }

    public void addItems(RedditDataResponse data){
        mAdapter.addItems(data);
        updateDismissView();
    }

    private void updateDismissView() {
        List<RedditChildrenResponse> items = mAdapter.getItems();
        if(items == null || items.size() == 0){
            mDismissAll.setText(getString(R.string.dismill_all));
        } else {
            mDismissAll.setText(getString(R.string.dismill_all_count,  items.size()));
        }
    }

    private RedditDataResponse getItems(){
        return mAdapter.getRedditDataResponse();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(POSTS_LIST, getItems());
        outState.putSerializable(READ_POSTS_LIST, mAdapter.getReadPosts());

    }


    @Override
    public void onRedditPostStart() {
        mSwipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void onRedditPostComplete(RedditNewsResponse redditNewsResponse) {
        mSwipeRefreshLayout.setRefreshing(false);
        addItems(redditNewsResponse.getData());
    }

    @Override
    public void onRedditPostFailed(Throwable e) {
        mSwipeRefreshLayout.setRefreshing(false);
        Log.d(TAG, "Error", e);

    }
}
