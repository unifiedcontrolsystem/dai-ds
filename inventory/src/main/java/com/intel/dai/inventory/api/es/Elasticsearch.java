package com.intel.dai.inventory.api.es;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MainResponse;

import java.io.IOException;

public class Elasticsearch {
    void getRestHighLevelClient(String hostName, int port, String userName, String password) {
        final CredentialsProvider credentialsProvider =
                new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(userName, password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostName, port))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setDefaultCredentialsProvider(credentialsProvider));

        client = new RestHighLevelClient(builder);
    }

    String getElasticsearchServerVersion() throws IOException {
        MainResponse response = client.info(RequestOptions.DEFAULT);
        return response.getVersion().getNumber();
    }

    void close() throws IOException {
        client.close();
    }

    RestHighLevelClient client;
}
