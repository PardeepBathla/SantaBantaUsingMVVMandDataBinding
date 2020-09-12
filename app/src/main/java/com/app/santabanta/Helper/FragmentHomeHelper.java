package com.app.santabanta.Helper;

import android.app.Activity;
import android.app.Dialog;
import android.media.Image;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.santabanta.Adapter.HomeCategoriesAdapter;
import com.app.santabanta.Adapter.HomeItemAdapter;
import com.app.santabanta.AppController;
import com.app.santabanta.Fragment.FragmentHome;
import com.app.santabanta.Modals.FeaturedCategory;
import com.app.santabanta.Modals.HomeDetailsModel;
import com.app.santabanta.RestClient.Webservices;
import com.app.santabanta.Utils.Utils;
import com.google.gson.Gson;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentHomeHelper {

    private static final int PAGE_START = 1;
    private Activity mActivity;
    private Webservices mInterface_method = AppController.getRetroInstance().create(Webservices.class);
    private FragmentHome fragmentHome;
    private HomeItemAdapter mAdapter;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int TOTAL_PAGES = 10;
    private int currentPage = PAGE_START;
    private LinearLayoutManager mLinearLayoutManager;

    public FragmentHomeHelper(Activity mActivity,FragmentHome fragmentHome) {
        this.mActivity = mActivity;
        this.fragmentHome = fragmentHome;
        initViews();
    }

    private void initViews() {

        Dialog progressDialog = Utils.getProgressDialog(mActivity);
        progressDialog.show();

        //// TODO: 9/12/20  make language dynamic
        Call<HomeDetailsModel> call  = mInterface_method.getHomeList("English",currentPage);
        call.enqueue(new Callback<HomeDetailsModel>() {
            @Override
            public void onResponse(Call<HomeDetailsModel> call, Response<HomeDetailsModel> response) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                if (response.isSuccessful()){
                    fragmentHome.rvSubCategory.setLayoutManager(new LinearLayoutManager(mActivity,RecyclerView.HORIZONTAL,false));
                    fragmentHome.rvSubCategory.setAdapter(new HomeCategoriesAdapter(response.body().getFeaturedCategories(), mActivity, new HomeCategoriesAdapter.HomeCategoryClickListener() {
                        @Override
                        public void onItemClicked(FeaturedCategory model) {

                        }
                    }));

                    mAdapter = new HomeItemAdapter(mActivity,response.body().getData());
                    mAdapter.setHasStableIds(true);
                    mLinearLayoutManager = new LinearLayoutManager(mActivity, LinearLayoutManager.VERTICAL, false);
                    fragmentHome.recyclerHome.setLayoutManager(mLinearLayoutManager);
                    fragmentHome.recyclerHome.setMediaObjects(response.body().getData());
                    fragmentHome.recyclerHome.setAdapter(mAdapter);
                    fragmentHome.recyclerHome.setNestedScrollingEnabled(false);
                    fragmentHome.recyclerHome.setHasFixedSize(false);

                    //TODO uncomment when pagination values comes in api response
//
//                    if (currentPage < TOTAL_PAGES) {
//                        if (mFeedAdapter != null)
//                            mFeedAdapter.addLoadingFooter();
//                    } else isLastPage = true;
//
//                    mRvFeed.addOnScrollListener(new PaginationScrollListener(mLinearLayoutManager) {
//                        @Override
//                        protected void loadMoreItems() {
//                            isLoading = true;
//                            currentPage += 1;
//                            new Handler().postDelayed(HomeFragmentHelper.this::loadNextPage, 1000);
//                        }
//
//                        @Override
//                        public int getTotalPageCount() {
//                            return TOTAL_PAGES;
//                        }
//
//                        @Override
//                        public boolean isLastPage() {
//                            return isLastPage;
//                        }
//
//                        @Override
//                        public boolean isLoading() {
//                            return isLoading;
//                        }
//
//                        @Override
//                        public void onScrolled() {
//
//                        }
//                    });

                }
            }

            @Override
            public void onFailure(Call<HomeDetailsModel> call, Throwable t) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();
            }
        });
    }
}
