package io.github.slvwolf;

import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Simple wrapper for Etcd.
 */
public class EtcdAccess {

  private static final String LOCATION_SERVICE_BASE = "/ccentral/services/%s";
  private static final String LOCATION_SCHEMA = LOCATION_SERVICE_BASE + "/schema";
  private static final String LOCATION_CONFIG = LOCATION_SERVICE_BASE + "/config";
  private static final String LOCATION_CLIENTS = LOCATION_SERVICE_BASE + "/clients/%s";
  private static final String LOCATION_SERVICE_INFO = LOCATION_SERVICE_BASE + "/info/%s";
  private static final int INSTANCE_TTL = 3 * 60;
  private static final int TTL_DAY = 26 * 60 * 60;
  private static final int TIMEOUT_SECONDS = 20;
  private final EtcdClient client;
  private final String serviceId;
  private String clientId;

  public EtcdAccess(EtcdClient client, String serviceId, String clientId) {
    this.client = client;
    this.serviceId = serviceId;
    this.clientId = clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public EtcdClient getClient() {
    return client;
  }

  public void sendClientInfo(String json) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
    client.put(String.format(LOCATION_CLIENTS, serviceId, clientId), json)
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .ttl(INSTANCE_TTL)
            .send()
            .get();
  }

  public String fetchConfig() throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
    EtcdKeysResponse response = client.get(String.format(LOCATION_CONFIG, serviceId))
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .send()
            .get();
    return response.node.value;
  }

  public void sendSchema(String schemaJson) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
    client.put(String.format(LOCATION_SCHEMA, serviceId), schemaJson)
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .send()
            .get();
  }

  public void sendServiceInfo(String key, String data) throws IOException, EtcdAuthenticationException, TimeoutException, EtcdException {
    client.put(String.format(LOCATION_SERVICE_INFO, serviceId, key), data)
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .ttl(TTL_DAY)
            .send()
            .get();
  }
}
