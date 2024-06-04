package uk.gov.hmcts.reform.em.hrs.ingestor.http;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import uk.gov.hmcts.reform.em.hrs.ingestor.model.Metadata;

public interface HrsHttpClient {
    @GET("/folders/{folder}")
    Call<ResponseBody> getFiles(@Path("folder") String folderName);

    @POST("/segments")
    Call<ResponseBody> postFile(@Body Metadata metadata);
}
