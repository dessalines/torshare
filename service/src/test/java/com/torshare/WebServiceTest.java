package com.torshare;


import com.despegar.sparkjava.test.SparkClient;
import com.despegar.sparkjava.test.SparkServer;
import com.torshare.webservice.WebService;
import org.junit.ClassRule;
import org.junit.Test;
import spark.servlet.SparkApplication;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * Created by tyler on 11/30/16.
 */
public class WebServiceTest {

    String ubuntuInfoHash = "0403fb4728bd788fbcb67e87d6feb241ef38c75a";

    public static class TestContollerTestSparkApplication implements SparkApplication {

        @Override
        public void init() {
            try {
                WebService.main(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @ClassRule
    public static SparkServer<TestContollerTestSparkApplication> testServer = new SparkServer<>(WebServiceTest.TestContollerTestSparkApplication.class, 4567);

    @Test
    public void hello() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET", "/hello", null);
        assertEquals(200, response.status);
        assertEquals("hello", response.body);
        assertNotNull(testServer.getApplication());
    }

    @Test
    public void searchResults() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET",
                "/search" +
                        "?limit=4" +
                        "&page=1" +
                        "&orderBy=path-desc" +
                        "&q=ubuntu" +
                        "&orderBy=peers-desc" +
                        "", null);
//        System.out.println(response.body);
        assertEquals(200, response.status);
        assertTrue(response.body.contains(ubuntuInfoHash));
    }

    @Test
    public void searchResults2() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET",
                "/search" +
                        "?limit=4" +
                        "&page=1" +
                        "&q=ubuntu" +
                        "&orderBy=peers-desc" +
                        "", null);
//        System.out.println(response.body);
        assertEquals(200, response.status);
        assertTrue(response.body.contains(ubuntuInfoHash));
    }

    @Test
    public void searchResults3() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET",
                "/search" +
                        "?limit=4" +
                        "&page=1" +
                        "&q=ubuntu" +
                        "", null);
//        System.out.println(response.body);
        assertEquals(200, response.status);
        assertTrue(response.body.contains(ubuntuInfoHash));
    }

    @Test
    public void pgDump() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET",
                "/torshare.pgdump", null);
        assertTrue(response.body.contains("info_hash character varying(40) NOT NULL"));
    }

    @Test
    public void jsonDump() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET",
                "/torshare.json", null);
        assertTrue(response.body.contains(ubuntuInfoHash));
    }

    @Test
    public void csvDump() throws Exception {
        SparkClient.UrlResponse response = testServer.getClient().doMethod("GET",
                "/torshare.csv", null);
        assertTrue(response.body.contains(ubuntuInfoHash));
    }




}
