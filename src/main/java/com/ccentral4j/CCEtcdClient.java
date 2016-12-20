package com.ccentral4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;


class CCEtcdClient implements CCClient {

  private final static String CLIENT_VERSION = "java-0.0.2";
  private final static int CHECK_INTERVAL = 40;
  private final static int INSTANCE_TTL = 3 * 60;
  private final static int TTL_DAY = 26 * 60 * 60;
  private final static String LOCATION_SERVICE_BASE = "/ccentral/services/%s";
  private final static String LOCATION_SCHEMA = LOCATION_SERVICE_BASE + "/schema";
  private final static String LOCATION_CONFIG = LOCATION_SERVICE_BASE + "/config";
  private final static String LOCATION_CLIENTS = LOCATION_SERVICE_BASE + "/clients/%s";
  private final static String LOCATION_SERVICE_INFO = LOCATION_SERVICE_BASE + "/info/%s";
  private final static ObjectMapper MAPPER = new ObjectMapper();
  private final static String API_VERSION = "0";
  private static Logger LOG = LoggerFactory.getLogger(CCEtcdClient.class);
  private int startedEpoch;
  private HashMap<String, SchemaItem> schema;
  private HashMap<String, Object> clientData;
  private EtcdClient client;
  private String serviceId;
  private HashMap<String, Counter> counters;
  private String clientId;
  private long lastCheck;

  public CCEtcdClient(String serviceId, EtcdClient client) {
    init(serviceId, client);
  }

  public CCEtcdClient(String serviceId, URI[] hosts) {
    LOG.info("Creating ETCD connection");
    EtcdClient cli = new EtcdClient(hosts);
    init(serviceId, cli);
  }

  @Override
  public String getApiVersion() {
    return API_VERSION;
  }

  @Override
  public String getClientId() {
    return clientId;
  }

  private String filterKey(String key) {
    key = key.replace(" ", "_");
    String validKeyChars = "[^a-zA-Z0-9_-]";
    return key.replaceAll(validKeyChars, "");
  }

  private void init(String serviceId, EtcdClient client) {
    LOG.info("Initializing");
    this.startedEpoch = (int) (System.currentTimeMillis() / 1000);
    this.serviceId = serviceId;
    this.client = client;
    schema = new HashMap<>();
    counters = new HashMap<>();
    clientData = new HashMap<>();
    schema.put("v", new SchemaItem("v", "Version",
        "Schema version for tracking instances", "default"));
    clientId = UUID.randomUUID().toString();
    lastCheck = 0;
  }

  @Override
  public void addField(String key, String title, String description) {
    key = filterKey(key);
    schema.put(key, new SchemaItem(key, title, description, ""));
  }

  @Override
  public void addField(String key, String title, String description, String defaultValue) {
    key = filterKey(key);
    schema.put(key, new SchemaItem(key, title, description, defaultValue));
  }


  @Override
  public String getConfig(String key) throws UnknownConfigException {
    refresh();
    key = filterKey(key);
    SchemaItem item = schema.get(key);
    if (item == null) {
      throw new UnknownConfigException();
    }
    if (item.configValue == null) {
      return item.defaultValue;
    }
    return item.configValue;
  }

  @Override
  public Boolean getConfigBool(String key) {
    Integer value = getConfigInt(key);
    if (value == null) {
      return null;
    }
    return value == 1;
  }

  @Override
  public Integer getConfigInt(String key) {
    try {
      return Integer.valueOf(getConfigString(key));
    } catch (NumberFormatException e) {
      LOG.warn("Could not convert configuration %s value '%s' to int.",
          key, getConfigString(key));
      return null;
    }
  }

  @Override
  public Float getConfigFloat(String key) {
    try {
      return Float.valueOf(getConfigString(key));
    } catch (NumberFormatException e) {
      LOG.warn("Could not convert configuration %s value '%s' to float.",
          key, getConfigString(key));
      return null;
    }
  }

  @Override
  public String getConfigString(String key) {
    try {
      return getConfig(key);
    } catch (UnknownConfigException e) {
      LOG.warn("Configuration %s was requested before initialized. Always introduce all " +
          "configurations with addField method before using them.");
      return null;
    }
  }

  @Override
  public void addInstanceInfo(String key, String data) {
    refresh();
    key = filterKey(key);
    clientData.put("k_" + key, data);
  }

  @Override
  public void addServiceInfo(String key, String data) {
    refresh();
    key = filterKey(key);
    try {
      client.put(String.format(LOCATION_SERVICE_INFO, serviceId, key), data).ttl(TTL_DAY).send();
    } catch (Exception e) {
      LOG.error("Failed to add service info: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  public void refresh() {
    if (lastCheck == 0) {
      LOG.info("First refresh, sending Schema");
      sendSchema();
      LOG.debug("Schema updated");
    }
    if (lastCheck < (System.currentTimeMillis() - CHECK_INTERVAL * 1000)) {
      LOG.info("Check interval triggered");
      lastCheck = System.currentTimeMillis();
      sendClientData();
      pullConfigData();
    }
  }

  @Override
  public void incrementInstanceCounter(String key, int amount) {
    refresh();
    key = filterKey(key);
    Counter counter = counters.get(key);
    if (counter == null) {
      counter = new Counter();
      counters.put(key, counter);
    }
    counter.increment(amount);
  }

  @Override
  public void incrementInstanceCounter(String key) {
    incrementInstanceCounter(key, 1);
  }

  private void sendSchema() {
    try {
      LOG.info("Sending schema information");
      String schemaJson = MAPPER.writeValueAsString(schema);
      client.put(String.format(LOCATION_SCHEMA, serviceId), schemaJson).send();
    } catch (Exception e) {
      LOG.error("Failed to send schema: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void pullConfigData() {
    try {
      LOG.info("Pulling configuration");
      EtcdKeysResponse response = client.get(String.format(LOCATION_CONFIG, serviceId)).send()
          .get();
      String data = response.node.value;
      Map<String, Object> configMap = MAPPER.readValue(data,
          new TypeReference<Map<String, Object>>() {
          });
      for (Map.Entry<String, Object> entry : configMap.entrySet()) {
        SchemaItem schemaItem = schema.get(entry.getKey());
        if (schemaItem != null) {
          schemaItem.defaultValue = ((HashMap<String, Object>) (entry.getValue())).get("value")
              .toString();
        }
      }
      LOG.info("Configuration pulled successfully");
    } catch (Exception e) {
      LOG.error("Failed to pull configuration data: " + e.getMessage());
      e.printStackTrace();
    }

  }

  private void sendClientData() {
    LOG.info("Sending client data");
    clientData.put("ts", Integer.toString((int) (System.currentTimeMillis() / 1000)));
    String configVersion = getConfigString("v");
    clientData.put("v", configVersion == null ? "unknown" : configVersion);
    clientData.put("cv", CLIENT_VERSION);
    clientData.put("av", API_VERSION);
    clientData.put("hostname", System.getenv("HOSTNAME"));
    clientData.put("lv", System.getProperty("java.version"));
    clientData.put("started", startedEpoch);
    clientData.put("uinterval", "60");

    for (Map.Entry<String, Counter> entry : counters.entrySet()) {
      LinkedList<Integer> counts = new LinkedList<>();
      counts.add(entry.getValue().getValue());
      clientData.put("c_" + entry.getKey(), counts);
    }

    try {
      String json = MAPPER.writeValueAsString(clientData);
      client.put(String.format(LOCATION_CLIENTS, serviceId, clientId), json).ttl(INSTANCE_TTL)
          .send();
    } catch (Exception e) {
      LOG.error("Failed to send client data: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
