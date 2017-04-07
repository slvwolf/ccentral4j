package com.ccentral4j;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mousio.etcd4j.EtcdClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


class CCEtcdClient implements CCClient {

  private final static String CLIENT_VERSION = "java-0.1.0";
  private final static int CHECK_INTERVAL = 40;
  private final static ObjectMapper MAPPER = new ObjectMapper();
  private final static String API_VERSION = "1";
  private static Logger LOG = LoggerFactory.getLogger(CCEtcdClient.class);
  private int startedEpoch;
  private HashMap<String, SchemaItem> schema;
  private HashMap<String, Object> clientData;
  private EtcdAccess client;
  private HashMap<String, Counter> counters;
  private String clientId;
  private long lastCheck;

  public CCEtcdClient(EtcdAccess client) {
    try {
      init();
      this.client = client;
      client.setClientId(this.getClientId());
    } catch (Throwable e) {
      LOG.error("Could not initialise using provided EtcdClient", e);
      throw e;
    }
  }

  public CCEtcdClient(String serviceId, URI[] hosts) {
    // TODO: Instead of throwing exception library should work in a dummy mode instead.
    if (hosts == null || hosts.length == 0) {
      LOG.error("No hosts provided or hosts is null. Can not initialize CCentral.");
      throw new RuntimeException("No hosts provided or hosts is null. Can not initialize CCentral.");
    }
    for (URI host : hosts) {
      LOG.info("Creating ETCD connection: %r", host.toASCIIString());
    }
    try {
      EtcdClient cli = new EtcdClient(hosts);
      init();
      this.client = new EtcdAccess(cli, serviceId, this.getClientId());
    } catch (Throwable e) {
      LOG.error("Could not initialise EtcdClient", e);
      throw e;
    }
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

  private void init() {
    LOG.info("Initializing");
    clientId = UUID.randomUUID().toString();
    this.startedEpoch = (int) (System.currentTimeMillis() / 1000);
    schema = new HashMap<>();
    counters = new HashMap<>();
    clientData = new HashMap<>();
    addIntField("v", "Version", "Schema version for tracking instances", 0);
    lastCheck = 0;
  }

  @Override
  public void addField(String key, String title, String description, String defaultValue) {
    addFieldType(key, title, description, defaultValue, SchemaItem.Type.STRING);
  }

  @Override
  public void addIntField(String key, String title, String description, int defaultValue) {
    addFieldType(key, title, description, Integer.toString(defaultValue), SchemaItem.Type.INTEGER);
  }

  @Override
  public void addFloatField(String key, String title, String description, float defaultValue) {
    addFieldType(key, title, description, Float.toString(defaultValue), SchemaItem.Type.FLOAT);
  }

  @Override
  public void addPasswordField(String key, String title, String description, String defaultValue) {
    addFieldType(key, title, description, defaultValue, SchemaItem.Type.PASSWORD);
  }

  @Override
  public void addListField(String key, String title, String description, List<String> defaultValue) {
    try {
      addFieldType(key, title, description, MAPPER.writeValueAsString(defaultValue), SchemaItem.Type.LIST);
    } catch (JsonProcessingException e) {
      LOG.error("Could not register list type: ", e);
    }
  }

  @Override
  public void addBooleanField(String key, String title, String description, boolean defaultValue) {
    String value;
    if (defaultValue) {
      value = "1";
    } else {
      value = "0";
    }
    addFieldType(key, title, description, value, SchemaItem.Type.BOOLEAN);
  }

  @Override
  public List<String> getConfigList(String key) {
    try {
      String value = getConfigString(key);
      if (value == null) {
        return null;
      }
      return MAPPER.readValue(value, new TypeReference<List<String>>() {});
    } catch (JsonParseException e) {
      LOG.warn("Could not parse configuration value. Value needs to be a valid json list of strings.");
    } catch (IOException e) {
      LOG.warn("Could not parse configuration value.", e);
    }
    return null;
  }

  private void addFieldType(String key, String title, String description, String defaultValue, SchemaItem.Type type) {
    key = filterKey(key);
    schema.put(key, new SchemaItem(key, title, description, defaultValue, type));
    if (lastCheck > 0) {
      LOG.warn("Schema was updated after refresh. This might result in some abnormal behavior on " +
          "administration UI and degrades the performance. Before setting any stats or instance " +
          "variables always make sure all configurations have been already defined. As a remedy " +
          "will now resend the updated schema.");
      sendSchema();
    }
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
      client.sendServiceInfo(key, data);
    } catch (Throwable e) {
      LOG.error("Failed to add service info: " + e.getMessage(), e);
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

  @Override
  public void setInstanceCounter(String key, int amount) {
    refresh();
    key = filterKey(key);
    Counter counter = counters.get(key);
    if (counter == null) {
      counter = new Counter();
      counters.put(key, counter);
    }
    counter.set(amount);
  }

  private void sendSchema() {
    try {
      LOG.info("Sending schema information");
      String schemaJson = MAPPER.writeValueAsString(schema);
      client.sendSchema(schemaJson);
    } catch (Throwable e) {
      LOG.error("Failed to send schema: " + e.getMessage(), e);
    }
  }

  private void pullConfigData() {
    try {
      LOG.info("Pulling configuration");
      String data = client.fetchConfig();
      Map<String, Object> configMap = MAPPER.readValue(data, new TypeReference<Map<String, Object>>() {});
      for (Map.Entry<String, Object> entry : configMap.entrySet()) {
        SchemaItem schemaItem = schema.get(entry.getKey());
        if (schemaItem != null) {
          schemaItem.configValue = ((HashMap<String, Object>) (entry.getValue())).get("value")
              .toString();
        }
      }
      LOG.info("Configuration pulled successfully");
    } catch (Throwable e) {
      LOG.error("Failed to pull configuration data: " + e.getMessage(), e);
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
      client.sendClientInfo(json);
    } catch (Throwable e) {
      LOG.error("Failed to send client data: " + e.getMessage(), e);
    }
  }

}
