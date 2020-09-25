package io.github.slvwolf;

/**
 * Functional interface for listening configuration updates
 */
public interface ConfigUpdate {
  void valueChanged(String configKey);
}
