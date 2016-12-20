package com.ccentral4j;

import mousio.etcd4j.EtcdClient;

import java.net.URI;

public class CCentral {

  public static CCClient initWithEtcdClient(String serviceId, EtcdClient client) {
    return new CCEtcdClient(serviceId, client);
  }

  public static CCClient initWithEtcdHost(String serviceId, URI[] hosts) {
    return new CCEtcdClient(serviceId, hosts);
  }
}
