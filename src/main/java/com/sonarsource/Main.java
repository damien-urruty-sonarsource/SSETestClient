package com.sonarsource;

public class Main {

  private static final String SERVER_API_URL = "http://localhost:8080/eventStream";

  public static void main(String[] args) throws Exception {
    ApacheHttpClient client = new ApacheHttpClient();
    client.getStreamed(SERVER_API_URL, System.out::println);
  }
}
