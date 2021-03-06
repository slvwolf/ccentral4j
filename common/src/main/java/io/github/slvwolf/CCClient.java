package io.github.slvwolf;

import java.util.List;

public interface CCClient {
  /**
   * Get unique clientId.
   * @return Unique Id.
   */
  String getClientId();

  /**
   * Add a string configuration field.
   *
   * @param key Unique ket for configuration.
   * @param title (UI) Human readable title.
   * @param description (UI) Documentation about the configuration.
   * @param defaultValue Default value.
   */
  void addField(String key, String title, String description, String defaultValue);

  /**
   * Add a integer configuration field.
   *
   * @param key Unique ket for configuration.
   * @param title (UI) Human readable title.
   * @param description (UI) Documentation about the configuration.
   * @param defaultValue Default value.
   */
  void addIntField(String key, String title, String description, int defaultValue);

  /**
   * Add a float configuration field.
   *
   * @param key Unique ket for configuration.
   * @param title (UI) Human readable title.
   * @param description (UI) Documentation about the configuration.
   * @param defaultValue Default value.
   */

  void addFloatField(String key, String title, String description, float defaultValue);

  /**
   * Add a password configuration field. This field will have its value hidden in the UI. This does
   * not protect the password in any other way.
   *
   * @param key Unique ket for configuration.
   * @param title (UI) Human readable title.
   * @param description (UI) Documentation about the configuration.
   * @param defaultValue Default value.
   */
  void addPasswordField(String key, String title, String description, String defaultValue);

  /**
   * Add a list configuration field.
   *
   * @param key Unique ket for configuration.
   * @param title (UI) Human readable title.
   * @param description (UI) Documentation about the configuration.
   * @param defaultValue Default value.
   */
  void addListField(String key, String title, String description, List<String> defaultValue);

  /**
   * Add a boolean configuration field.
   *
   * @param key Unique ket for configuration.
   * @param title (UI) Human readable title.
   * @param description (UI) Documentation about the configuration.
   * @param defaultValue Default value.
   */
  void addBooleanField(String key, String title, String description, boolean defaultValue);

  /**
   * Get list value from configuration.
   *
   * @param key Key for configuration.
   * @return value or null if not found.
   */
  List<String> getConfigList(String key);

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

  /**
   * Increment instance counter
   * @param key Counter key
   * @param amount Amount to increment
   * @param groups Additional groups for this key
   */
  void incrementInstanceCounter(String key, int amount, String ...groups);

  /**
   * Increment instance counter. Groups are optional and will create additional dimensions for that metric.
   * @param key Counter key
   * @param groups Additional groups for this key
   */
  void incrementInstanceCounter(String key, String ...groups);

  /**
   * Reset instance counter to predefined value
   *
   * @param key    Counter key
   * @param amount Value to set
   * @param groups Additional groups for this key
   */
  void setInstanceCounter(String key, int amount, String... groups);

  void addHistogram(String key, long timeInMilliseconds);

  String getApiVersion();

  /**
   * Use callback function when given configuration option changes. This can be handy option in cases where
   * configuration is usually set only once in init and needs new initialization when updated.
   *
   * @param configuration Key for configuration, has to be defined before called
   * @param func          Called function
   * @throws UnknownConfigException Configuration item missing
   */
  void addCallback(String configuration, ConfigUpdate func) throws UnknownConfigException;
}
