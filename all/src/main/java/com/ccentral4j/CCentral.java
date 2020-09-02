package com.ccentral4j;

import mousio.etcd4j.EtcdClient;

import java.net.URI;

public class CCentral {

  public static CCClient initWithEtcdClient(String serviceId, EtcdClient client) {
    // Client ID will be injected by the CCEtcdClient
    return new CCEtcdClient(new EtcdAccess(client, serviceId, ""));
  }

  public static CCClient initWithEtcdHost(String serviceId, URI[] hosts) {
    return new CCEtcdClient(serviceId, hosts);
  }

}
