package com.majjid.microservices.order.order.stubs;

import com.github.tomakehurst.wiremock.client.WireMock;

import com.majjid.microservices.order.client.inventoryClient.InventoryProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static com.github.tomakehurst.wiremock.client.WireMock.*;



@Component
@Data

public class InventoryClientStub {

    private InventoryProperties inventoryProperties;

    private  int port;

//    @Value("${inventory.basepath}")
    private  String BASE_PATH;

//    @Value("${inventory.serverHost}")
    private  String serverHost  ;


    public String getBaseUrl() {
        return    "http://" + serverHost + ":" + port + BASE_PATH;
    }

    public InventoryClientStub( InventoryProperties inventoryProperties) {

        this.inventoryProperties = inventoryProperties;
      BASE_PATH = inventoryProperties.getBasepath();
        serverHost  = inventoryProperties.getServerHost();

        this.port = 8082; // Default port
        WireMock.configureFor(serverHost, port);

    }
    public void configureInventoryStub(int port) {
        this.port=port;
        WireMock.configureFor(serverHost, port);

    }


    public void stubSellInventory(String skuCode, int quantity) {
        stubFor(post(urlPathEqualTo( BASE_PATH + skuCode + "/sell"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                    "data": {"id": 1, "skuCode": "%s", "quantity": %d, "isInStock": true},
                                    "message": "Inventory reserved successfully",
                                    "status": "%s",
                                    "success": true
                                }
                                """.formatted(skuCode, quantity, HttpStatus.OK))
                ));
    }

    public void stubPurchaseInventory(String skuCode, int quantity) {
        stubFor(post(urlPathEqualTo( BASE_PATH + skuCode + "/purchase"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""
                                {
                                    "data": {"id": 1, "skuCode": "%s", "quantity": %d, "isInStock": true},
                                    "message": "Inventory restored successfully",
                                    "status": "%s",
                                    "success": true
                                }
                                """.formatted(skuCode, quantity, HttpStatus.OK))
                ));
    }

    public void reset() {
        WireMock.reset();
    }
}
