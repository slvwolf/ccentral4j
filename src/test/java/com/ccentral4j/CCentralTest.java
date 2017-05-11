package com.ccentral4j;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CCentralTest {

  private CCClient cCentral;
  @Mock
  private EtcdAccess client;

  @Before
  public void setUp() throws Exception {
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
  public void getListDefault() throws Exception {
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
  public void getBoolDefault() throws Exception {
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
}