package com.testquack.maven.client;

import com.testquack.beans.Launch;
import com.testquack.beans.TestCase;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

public interface QuackClient {

    @POST("{projectId}/testcase/import")
    Call<Void> importTestCases(@Path("projectId") String projectId, @Body List<TestCase> testcases);

    @DELETE("{projectId}/testcase")
    Call<Void> deleteTestcasesByImportResource(@Path("projectId") String projectId, @Query("importResource") String importResource);

    @POST("{projectId}/launch")
    Call<Launch> createLaunch(@Path("projectId") String projectId, @Body Launch launch);
}
