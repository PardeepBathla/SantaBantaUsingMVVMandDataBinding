package com.app.santabanta.Helper;

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.os.Bundle;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.santabanta.Adapter.SmsCategoriesAdapter;
import com.app.santabanta.Adapter.SmsHomeAdapter;
import com.app.santabanta.AppController;
import com.app.santabanta.Fragment.FragmentSms;
import com.app.santabanta.Modals.AddFavouriteRequest;
import com.app.santabanta.Modals.SmsDetailModel;
import com.app.santabanta.Modals.SmsFavouriteModel;
import com.app.santabanta.Modals.SmsFeaturedCategory;
import com.app.santabanta.Modals.SmsResponseModel;
import com.app.santabanta.R;
import com.app.santabanta.RestClient.Webservices;
import com.app.santabanta.Utils.ShareableIntents;
import com.app.santabanta.Utils.GlobalConstants;
import com.app.santabanta.Utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentSmsHelper {

    private static final int PAGE_START = 1;
    SmsHomeAdapter smsHomeAdapter;
    private Activity mActivity;
    private Webservices mInterface_method = AppController.getRetroInstance().create(Webservices.class);
    private FragmentSms fragmentSms;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int TOTAL_PAGES = 10;
    private int currentPage = PAGE_START;
    private LinearLayoutManager mLinearLayoutManager;


    public FragmentSmsHelper(Activity mActivity, FragmentSms fragmentSms) {
        this.mActivity = mActivity;
        this.fragmentSms = fragmentSms;

        initViews();
    }

    private void initViews() {

        fragmentSms.swipeRefreshSms.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getSms("English", "", "");
            }
        });

        getSms("English", "", "");
    }

    private void enterSubCategorySms(SmsFeaturedCategory model) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(GlobalConstants.INTENT_PARAMS.IS_SUB_CATEGORY_SMS, true);
        bundle.putString(GlobalConstants.INTENT_PARAMS.SMS_SLUG, model.getSlug());
        FragmentSms fragmentSms1 = FragmentSms.newInstance(bundle);
        Utils.switchFragment(fragmentSms.getChildFragmentManager().beginTransaction(), fragmentSms1, R.id.frameSms);
    }

    private void getSms(String language, String slug, String selectedCategory) {
        Call<SmsResponseModel> call;

        call = mInterface_method.getSmsList("English", fragmentSms.subCatSlug, 1, fragmentSms.selectedCategory);

        Dialog progressDialog = Utils.getProgressDialog(mActivity);
        progressDialog.show();
        call.enqueue(new Callback<SmsResponseModel>() {
            @Override
            public void onResponse(Call<SmsResponseModel> call, Response<SmsResponseModel> response) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                if (fragmentSms.swipeRefreshSms != null && fragmentSms.swipeRefreshSms.isRefreshing())
                    fragmentSms.swipeRefreshSms.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {

                    fragmentSms.rvSubCategorySms.setLayoutManager(new LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false));
                    fragmentSms.rvSubCategorySms.setAdapter(new SmsCategoriesAdapter(response.body().getFeaturedCategories(), mActivity, new SmsCategoriesAdapter.HomeCategoryClickListener() {
                        @Override
                        public void onItemClicked(SmsFeaturedCategory model) {
                            enterSubCategorySms(model);
                        }
                    }));


                    fragmentSms.recyclerSms.setLayoutManager(new LinearLayoutManager(mActivity));

                    smsHomeAdapter = new SmsHomeAdapter(FragmentSmsHelper.this, response.body().getData(), mActivity);
                    fragmentSms.recyclerSms.setAdapter(smsHomeAdapter);
                }
            }

            @Override
            public void onFailure(Call<SmsResponseModel> call, Throwable t) {
                if (progressDialog.isShowing())
                    progressDialog.dismiss();

                if (fragmentSms.swipeRefreshSms != null && fragmentSms.swipeRefreshSms.isRefreshing())
                    fragmentSms.swipeRefreshSms.setRefreshing(false);
            }
        });
    }


    public void addSmsToFav(SmsDetailModel obj, int position, Dialog progressBar, CheckBox cbLike) {

        AddFavouriteRequest addFavouriteRequest = new AddFavouriteRequest();
        addFavouriteRequest.setDeviceId(Utils.getMyDeviceId(mActivity));
        addFavouriteRequest.setType("sms");
        addFavouriteRequest.setItemId(obj.getId().intValue());



        Call<ResponseBody> call = null;

        call = mInterface_method.saveFavouriteJoke(addFavouriteRequest);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if (jsonObject.has("status")) {
                            String status = jsonObject.getString("status");

                            if (status.equals("success")) {
                                progressBar.dismiss();
                                cbLike.setClickable(true);
                                obj.setFav_count(obj.getFav_count() + 1);
                                setFavItemToModel(position, addFavouriteRequest, obj, jsonObject.getInt("fav_id"));
                                Toast.makeText(mActivity, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("onFailure", "onFailure");
            }
        });




    }


    private void setFavItemToModel(int position, AddFavouriteRequest addFavouriteRequest, SmsDetailModel obj, int fav_id) {
        SmsFavouriteModel smsFavouriteModel = new SmsFavouriteModel();
        smsFavouriteModel.setDeviceId(addFavouriteRequest.getDeviceId());
        smsFavouriteModel.setItemId(String.valueOf(addFavouriteRequest.getItemId()));
        smsFavouriteModel.setId(fav_id);

        ArrayList<SmsDetailModel> pagedLists = null;
        pagedLists = smsHomeAdapter.getCurrentList();
        if (obj.getmFavourite() != null && obj.getmFavourite().size() != 0) {
            obj.getmFavourite().add(0, smsFavouriteModel);
        } else {
            List<SmsFavouriteModel> favouriteModelList = new ArrayList<>();
            favouriteModelList.add(0, smsFavouriteModel);
            obj.setmFavourite(favouriteModelList);
        }
        pagedLists.get(position).setmFavourite(obj.getmFavourite());
        smsHomeAdapter.updateList(pagedLists); //paging method

    }

    public void removeFromFav(SmsDetailModel obj, int position, int id, Dialog progressBar, CheckBox cbLike) {

        Call<ResponseBody> call = null;

        call = mInterface_method.removeJokeFromFav(id);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.body().string());
                        if (jsonObject.has("status")) {
                            String status = jsonObject.getString("status");

                            if (status.equals("success")) {
                                progressBar.dismiss();
                                cbLike.setClickable(true);

                                removeFavItemFromModel(position, obj);
                                Toast.makeText(mActivity, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
                            }
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });


    }

    private void removeFavItemFromModel(int position, SmsDetailModel obj) {
        ArrayList<SmsDetailModel> pagedLists = null;
        pagedLists = smsHomeAdapter.getCurrentList();

        pagedLists.get(position).setmFavourite(null);
        obj.setFav_count(obj.getFav_count() - 1);
        smsHomeAdapter.updateList(pagedLists);

    }

}
