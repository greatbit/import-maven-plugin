package com.testquack.maven.client;

import com.testquack.client.HttpClientBuilder;

import javax.servlet.http.HttpServletRequest;

public class QuackClietnUtils {

    public static QuackClient getClient(String apiToken, String quackApiEndpoint, long quackApiTimeoutMs) {
        return HttpClientBuilder.builder(quackApiEndpoint, quackApiTimeoutMs, apiToken).build().
                create(QuackClient.class);
    }
}
