package com.example.treasure.user.account;

import com.example.treasure.net.NetClient;
import com.example.treasure.user.UserApi;
import com.example.treasure.user.UserPrefs;
import com.hannesdorfmann.mosby.mvp.MvpNullObjectBasePresenter;

import java.io.File;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 个人信息页面业务处理
 * 主要做了头像更新服务：先做头像上传处理，再做头像更新处理
 * Created by 93432 on 2016/7/15.
 */
public class AccountPresenter extends MvpNullObjectBasePresenter<AccountView> {

    private Call<UploadRestult> uploadCall;//头像上传call
    private Call<UpdateResult> updateCall;//头像上传call

    /**
     * 上传头像
     * @param file
     */
    public void uploadPhoto(File file){
        getView().showProgress();
        UserApi userApi = NetClient.getInstance().getUserApi();
        //构建“部分”
        RequestBody body = RequestBody.create(null, file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("image", "photo.png", body);
        //上传头像（我们接口其实只要一个部分（头像文件））
        if (uploadCall != null) uploadCall.cancel();
        uploadCall = userApi.upload(part);
        uploadCall.enqueue(uploadCallback);
    }

    /**
     * 上传头像的callback
     */
    private Callback<UploadRestult> uploadCallback = new Callback<UploadRestult>() {
        @Override
        public void onResponse(Call<UploadRestult> call, Response<UploadRestult> response) {
            //成功响应
            if (response != null && response.isSuccessful()){
                //取得响应体内数据，结果
                UploadRestult restult = response.body();
                if (restult == null){
                    getView().showMessage("unknown error");
                    return;
                }
                getView().showMessage(restult.getMsg());
                if (restult.getCount() != 1){ //上传不成功(@see 接口文档)
                    return;
                }
                //上传成功，取出结果内的头像地址
                String photoUrl = restult.getUrl();//上传后，头像URL地址
                UserPrefs.getInstance().setPhoto(NetClient.BASE_URL + photoUrl);
                getView().updatePhoto(NetClient.BASE_URL + photoUrl);//视图更新头像
                //用户头像（在更新用户头像时要用到@see接口文档）
                String photoName = photoUrl.substring(photoUrl.lastIndexOf("/") + 1, photoUrl.length());
                //用户token（在更新用户头像时要用到@see接口文档）
                int tokenid = UserPrefs.getInstance().getTokenid();
                //头像更新
                UserApi userApi = NetClient.getInstance().getUserApi();
                if (updateCall != null) updateCall.cancel();
                updateCall = userApi.update(new Update(tokenid, photoName));
                updateCall.enqueue(updateCallback);
            }
        }

        //头像更新callback
        private Callback<UpdateResult> updateCallback = new Callback<UpdateResult>() {
            @Override
            public void onResponse(Call<UpdateResult> call, Response<UpdateResult> response) {
                getView().hideProgress();
                if (response != null && response.isSuccessful()){
                    //取出当前“更新”响应结果
                    UpdateResult result = response.body();
                    if (result == null){
                        getView().showMessage("unknown error");
                        return;
                    }
                    getView().showMessage(result.getMsg());
                    if (result.getCode() != 1){
                        return;
                    }
                }
            }

            @Override
            public void onFailure(Call<UpdateResult> call, Throwable t) {
                getView().hideProgress();
                getView().showMessage(t.getMessage());
            }
        };

        @Override
        public void onFailure(Call<UploadRestult> call, Throwable t) {
            getView().hideProgress();
            getView().showMessage(t.getMessage());
        }
    };

    @Override
    public void detachView(boolean retainInstance) {
        super.detachView(retainInstance);
        if (uploadCall != null) uploadCall.cancel();
        if (updateCall != null) updateCall.cancel();
    }
}












