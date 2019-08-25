package com.testquack.maven.client;

import com.testquack.beans.TestCase;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

import java.util.List;

public interface QuackClient {

    @POST("{projectId}/testcase/import")
    Call<Void> importTestCases(@Path("projectId") String projectId, @Body List<TestCase> testcases);
}
