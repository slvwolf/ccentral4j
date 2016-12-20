package com.ccentral4j;

public interface CCClient {
  String getClientId();

  void addField(String key, String title, String description);

  void addField(String key, String title, String description, String defaultValue);

  /**
   * Get configuration.
   *
   * @param key Key for configuration.
   * @return value or null if not found.
   * @throws UnknownConfigException If configuration has not been defined on init.
   */
  String getConfig(String key) throws UnknownConfigException;

  /**
   * Get boolean value from configuration.
   *
   * @param key Key for configuration.
   * @return value or null if not found.
   */
  Boolean getConfigBool(String key);

  /**
   * Get int value from configuration.
   *
   * @param key Key for configuration.
   * @return value or null if not found.
   */
  Integer getConfigInt(String key);

  /**
   * Get float value from configuration
   *
   * @param key Key for configuration
   * @return value or null if not found
   */
  Float getConfigFloat(String key);

  /**
   * Get string value from configuration.
   *
   * @param key Key for configuration.
   * @return Value or null if not found.
   */
  String getConfigString(String key);

  void addInstanceInfo(String key, String data);

  void addServiceInfo(String key, String data);

  void refresh();

  void incrementInstanceCounter(String key, int amount);

  void incrementInstanceCounter(String key);

  String getApiVersion();
}
