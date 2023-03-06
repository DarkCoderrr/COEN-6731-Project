package com.DistributedSystems.Client;
import com.DistributedSystems.model.Skiers;
import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientLoadTest {
    private static final int NUM_OF_REQUESTS = 10000; // Number of requests to send
    private static final int NUM_OF_THREADS = 32; // Number of concurrent threads to use
    private static final AtomicInteger COUNT_SKIER_ID = new AtomicInteger(1);
    private static final ExecutorService THREAD_POOL = Executors.newFixedThreadPool(NUM_OF_THREADS);
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Semaphore SEMAPHORE = new Semaphore(NUM_OF_THREADS);
    private static final AtomicInteger TOTAL_SUCCESSFUL_REQUESTS = new AtomicInteger(0);
    private static final AtomicInteger TOTAL_FAILED_REQUESTS = new AtomicInteger(0);
    private static final Object MONITOR = new Object();
    private final List<Long> latencyList = Collections.synchronizedList(new ArrayList<>());

    private final Gson gson = new Gson();
    private final String apiUrl;

    public static void main(String[] args) {
        String apiUrl = "http://204.216.111.180:8080/skiers";
//    	 String apiUrl = "http://localhost:8080/skiers";
        ClientLoadTest loadTest = new ClientLoadTest(apiUrl);
        try {
            loadTest.start();
        } catch (InterruptedException e) {
            System.out.println("Load test interrupted: " + e.getMessage());
        }
    }

    public ClientLoadTest(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public void start() throws InterruptedException {
        Instant start = Instant.now();
        FileWriter csvWriter;
        try {
            csvWriter = new FileWriter("load_test_results.csv");
            csvWriter.append("Start Time,Request Type,Latency (ms),Response Code\n");
        } catch (IOException e) {
            System.out.println("Failed to create CSV file: " + e.getMessage());
            return;
        }

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            THREAD_POOL.submit(new ApiClient(apiUrl, csvWriter));
        }
        THREAD_POOL.shutdown();
        THREAD_POOL.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        long elapsedTime = duration.toMillis();
        int sum = 0;
        for (int i = 0; i < latencyList.size(); i++) {
            sum += latencyList.get(i);
        }
        double mean = (double) sum / latencyList.size();
        Collections.sort(latencyList);
        int middle = latencyList.size() / 2;
        double median;
        if (latencyList.size() % 2 == 1) {
            median = latencyList.get(middle);
        } else {
            median = (latencyList.get(middle - 1) + latencyList.get(middle)) / 2.0;
        }
        double index99 = Math.floor(latencyList.size() * 0.99) - 1;
        double percentile99 = latencyList.get((int) index99);

       
        double max = Collections.max(latencyList);
        double min = Collections.min(latencyList);

        

        float throughput = (float) NUM_OF_REQUESTS / elapsedTime * 1000;
        System.out.println("All requests Provided Response successfully in " + elapsedTime + " ms.");
        System.out.println("Total Successful requests: " + TOTAL_SUCCESSFUL_REQUESTS.get());
        System.out.println("Total Failed requests: " + TOTAL_FAILED_REQUESTS.get());
        System.out.println("Throughput: " + throughput + " requests/s.");
        System.out.println("Maximum value: " + max);
        System.out.println("Minimum value: " + min);
        System.out.println("Median: " + median);
        System.out.println("Mean: " + mean);
        System.out.println("99th percentile value: " + percentile99);

        try {
            csvWriter.close();
        } catch (IOException e) {
            System.out.println("Failed to terminate CSV file: " + e.getMessage());
        }
    }

    private class ApiClient implements Runnable {
        private final String apiUrl;
        private final FileWriter csvWriter;
        private final int TOTAL_RETRY_ATTEMPTS = 5;
        

        public ApiClient(String apiUrl, FileWriter csvWriter) {
            this.apiUrl = apiUrl;
            this.csvWriter = csvWriter;
        }

        @Override
        public void run() {
            while (true) {
                int skierID = COUNT_SKIER_ID.getAndIncrement();
                if (skierID > NUM_OF_REQUESTS) {
                    break;
                }

                Skiers skiers = new Skiers(
                        (int) (Math.random() * 10) + 1,
                        "2022",
                        "1",
                        String.valueOf(skierID),
                        (int) (Math.random() * 360) + 1,
                        (int) (Math.random() * 40) + 1
                );

                Instant startTime = Instant.now();

                int retryCount = 0;
                while (retryCount <= TOTAL_RETRY_ATTEMPTS) {
                    try {
                        URI uri = URI.create(apiUrl);
                        String requestBody = gson.toJson(skiers);
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(uri)
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                                .build();
                        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                        System.out.println("Request " + skierID + " status code: " + response.statusCode());
                        if (response.statusCode() == 201) {
                            Instant endTime = Instant.now();
                            long latency = Duration.between(startTime, endTime).toMillis();
                            synchronized (MONITOR) {
                                TOTAL_SUCCESSFUL_REQUESTS.getAndIncrement();
                                csvWriter.append(startTime.toString() + ",POST," + latency + "," + response.statusCode() + "\n");
                                csvWriter.flush();
                                latencyList.add(latency);
                            }
                            SEMAPHORE.release();
                            break; 
                        } else if (response.statusCode() >= 400 && response.statusCode() < 500) {
                         
                            retryCount++;
                            System.out.println("Request " + skierID + " failed with status code " + response.statusCode());
                        } else if (response.statusCode() >= 500 && response.statusCode() < 600) {
                            // Retry the request if it failed with a 5XX status code
                            retryCount++;
                            System.out.println("Request " + skierID + " failed with status code " + response.statusCode());
                        } else {
                            // Terminate the loop if the response status code is not 4XX or 5XX
                            break;
                        }
                    } catch (Exception e) {
                        // Retry the request due ti network error
                        retryCount++;
                        System.out.println("Request " + skierID + " failed: " + e.getMessage());
                    }
                }

                if (retryCount > TOTAL_RETRY_ATTEMPTS) {
                    synchronized (MONITOR) {
                        TOTAL_FAILED_REQUESTS.getAndIncrement();
                        try {
                            csvWriter.append(startTime.toString() + ",POST,-1,-1\n");
                            csvWriter.flush();
                        } catch (IOException e) {
                            System.out.println("Failed to write to CSV file: " + e.getMessage());
                        }
                    }
                    System.out.println("Request #" + skierID + " failed after " + TOTAL_RETRY_ATTEMPTS + " attempts.");
                }
            }
        }
    }
}
