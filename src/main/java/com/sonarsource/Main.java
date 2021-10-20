package com.sonarsource;

public class Main {

  private static final String SERVER_API_URL = "http://localhost:8080/eventStream";

  public static void main(String[] args) throws Exception {
    ApacheHttpClient client = new ApacheHttpClient();

    do {
      try {
        client.getStreamed(SERVER_API_URL, System.out::println);
      } catch(Exception e) {
        // ignore and reconnect
      }
      System.out.println("reconnecting");
    } while (true);
  }
}
