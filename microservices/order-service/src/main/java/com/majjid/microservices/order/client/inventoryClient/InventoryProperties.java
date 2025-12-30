package com.majjid.microservices.order.client.inventoryClient;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "inventory")
@Getter
@Setter
public class InventoryProperties {

    private String serverHost;
    private int serverPort;
    private String basepath;



    public String buildInventoryServiceUrl() {
        return "http://" + serverHost + ":" + serverPort + basepath;
    }

    public String buildInventoryServiceUrl(int port) {
        return "http://" + serverHost + ":" + port + basepath;
    }
}
