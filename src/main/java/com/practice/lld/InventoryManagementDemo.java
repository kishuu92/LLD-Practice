package com.practice.lld;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class InventoryManagementDemo {

    public static void main(String[] args) {
        System.out.println("To do: implementation");
    }
}


class InventoryManager {

    Map<String, Warehouse> warehouses;

    boolean addStock(String warehouseId, String productId, int quantity) {return false;}
    boolean removeStock(String warehouseId, String productId, int quantity) {return false;}
    boolean transfer(String fromWarehouseId, String toWarehouseId, String productId, int quantity) {return false;}
    List<String> getWarehousesWithAvailability(String productId, int quantity) {return null;}
    void setLowStockAlert(String warehouseId, String productId, int threshold, AlertListener listener) {}
}


class Warehouse {

    String id;
    Map<String, Integer> inventory;
    Map<String, Set<AlertConfig>> alertConfigs;

    boolean addStock(String productId, int quantity) {return false;}
    boolean removeStock(String productId, int quantity) {return false;}
    int getStock(String productId) {return 0;}
    boolean checkAvailability(String productId, int quantity) {return false;}
    void setLowStockAlert(String productId, int threshold, AlertListener listener) {}
    List<Alert> getAlertsToFire(String productId, int previousQty, int newQty) {return null;}
}

class Alert {
    AlertListener listener;
    String productId;
    int quantity;
}

class AlertConfig {
    int threshold;
    AlertListener listener;
}

interface AlertListener {
    void onLowStock(String warehouseId, String productId, int currentQuantity);
}
