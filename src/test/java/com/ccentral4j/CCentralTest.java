package com.ccentral4j;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import mousio.etcd4j.responses.EtcdAuthenticationException;
import mousio.etcd4j.responses.EtcdException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CCentralTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private CCClient cCentral;
  @Mock
  private EtcdAccess client;

  @Captor
  private ArgumentCaptor<String> stringCaptor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    cCentral = new CCEtcdClient(client);
  }

  /**
   * Schema is sent on first refresh
   */
  @Test
  public void sendSchema() throws Exception {
    cCentral.refresh();
    verify(client).sendSchema(
        "{\"v\":{\"key\":\"v\",\"title\":\"Version\",\"description\":\"Schema version for tracking instances\",\"type\":\"integer\",\"default\":\"0\"}}");
  }

  /** List types, get defaults */
  @Test
  public void getListDefault() {
    cCentral.addListField("list", "title", "description", Collections.singletonList("default"));

    List<String> values = cCentral.getConfigList("list");

    assertThat("Exactly one item in list", values.size(), is(1));
    assertThat("Item should be 'default'", values.get(0), is("default"));
  }

  /** List types, get value */
  @Test
  public void getListValue() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"list\": {\"value\": \"[\\\"current\\\"]\"}}");
    cCentral.addListField("list", "title", "description", Collections.singletonList("default"));

    List<String> values = cCentral.getConfigList("list");

    assertThat("Exactly one item in list", values.size(), is(1));
    assertThat("Item should be 'current'", values.get(0), is("current"));
  }

  /** Bool types, get defaults */
  @Test
  public void getBoolDefault() {
    cCentral.addBooleanField("bool", "title", "description", false);

    assertThat("Result should be false", cCentral.getConfigBool("bool"), is(false));
  }

  /** Bool types, get value */
  @Test
  public void getBoolValue() throws Exception {
    when(client.fetchConfig()).thenReturn("{\"bool\": {\"value\": \"1\"}}");
    cCentral.addBooleanField("bool", "title", "description", false);

    assertThat("Result should be true", cCentral.getConfigBool("bool"), is(true));
  }

  /** Pull configuration on late field definitions */
  @Test
  public void pullConfigLate() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
    cCentral.addField("key", "title", "desc", "def");
    cCentral.refresh();
    reset(client);

    cCentral.addField("key2", "title", "desc", "def");
    verify(client).fetchConfig();
  }

  /** Increment with groups */
  @Test
  public void incGroups() throws Exception {
    cCentral.incrementInstanceCounter("key", "group1", "group2");
    cCentral.refresh();

    verify(client).sendClientInfo(stringCaptor.capture());
    assertTrue(stringCaptor.getValue().contains("c_key.group1.group2"));
  }

  @Test
  public void histogram() throws EtcdAuthenticationException, TimeoutException, EtcdException, IOException {
    cCentral.addHistogram("latency", 10);
    cCentral.addHistogram("latency", 12);
    cCentral.addHistogram("latency", 7);
    ((CCEtcdClient) cCentral).setClock(Clock.offset(((CCEtcdClient) cCentral).getClock(), Duration.ofMinutes(1)));
    cCentral.refresh();
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(client, times(2)).sendClientInfo(captor.capture());
    String data = captor.getAllValues().get(1);
    Map<String, Object> values = MAPPER.readValue(data, new TypeReference<Map<String, Object>>() {
    });
    @SuppressWarnings("unchecked")
    List<Double> latencies = (List) values.get("h_latency");
    assertThat(latencies, notNullValue());
    assertThat(latencies, hasItems(12.0, 12.0, 12.0, 10.0));
  }
}