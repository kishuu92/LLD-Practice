package com.practice.lld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FoodOrderingSystem {

    public static void main(String[] args) {

        RestaurantService restaurantService = new RestaurantService();
        OrderService orderService = new OrderService(restaurantService);
        SelectionStrategy lowestPriceStrategy = new LowestPriceStrategy();

        restaurantService.addRestaurant("A2B", Arrays.asList(
                new MenuItem("Idly", 40),
                new MenuItem("Vada", 30),
                new MenuItem("Paper Plain Dosa", 50)
        ), 4);

        restaurantService.addRestaurant("Rasaganga", Arrays.asList(
                new MenuItem("Idly", 45),
                new MenuItem("Set Dosa", 60),
                new MenuItem("Poori", 25)
        ), 6);

        restaurantService.addRestaurant("Eat Fit", Arrays.asList(
                new MenuItem("Idly", 30),
                new MenuItem("Vada", 40)
        ), 2);

        System.out.println();
        orderService.placeOrder(Arrays.asList("Idly", "Poori"), lowestPriceStrategy);
        orderService.placeOrder(Arrays.asList("Idly", "Vada"), lowestPriceStrategy);

        System.out.println("\nStats:");
        orderService.printStats();

        orderService.placeOrder(Arrays.asList("Idly"), lowestPriceStrategy);

        System.out.println();
        orderService.fulfillItems(1, "Eat Fit");
        orderService.fulfillItems(1, "Rasaganga");

        System.out.println();
        orderService.fulfillItems(2, "Eat Fit");
        orderService.fulfillItems(2, "A2B");

        System.out.println();
        orderService.placeOrder(Arrays.asList("Idly"), lowestPriceStrategy);

        System.out.println("\nStats:");
        orderService.printStats();
    }
}


enum OrderItemStatus {
    IN_PROGRESS,
    COMPLETED
}


class MenuItem {

    String name;
    int price;

    MenuItem(String name, int price) {
        this.name = name;
        this.price = price;
    }
}


class OrderItem {
    String itemName;
    Restaurant restaurant;
    OrderItemStatus status;

    OrderItem(String itemName, Restaurant restaurant) {
        this.itemName = itemName;
        this.restaurant = restaurant;
        this.status = OrderItemStatus.IN_PROGRESS;
    }
}


class Order {

    int id;
    java.util.List<OrderItem> items;

    Order(int id, java.util.List<OrderItem> items) {
        this.id = id;
        this.items = items;
    }

    boolean isCompleted() {
        for (OrderItem item : items) {
            if (item.status != OrderItemStatus.COMPLETED) return false;
        }
        return true;
    }
}

class Restaurant {

    String name;
    Map<String, MenuItem> menu;
    int capacity;
    int processingCount;

    Restaurant(String name, java.util.List<MenuItem> items, int capacity) {
        this.name = name;
        this.capacity = capacity;
        this.processingCount = 0;
        this.menu = new HashMap<>();
        for (MenuItem item : items) {
            menu.put(item.name, item);
        }
    }

    boolean hasItem(String item) {
        return menu.containsKey(item);
    }

    int getPrice(String item) {
        return menu.get(item).price;
    }

    synchronized boolean increaseProcessCount() {
        if (processingCount < capacity) {
            processingCount++;
            return true;
        }
        return false;
    }

    synchronized void decreaseProcessCount() {
        if (processingCount > 0) {
            processingCount--;
        }
    }

    synchronized int remainingCapacity() {
        return capacity - processingCount;
    }

    void updateMenu(java.util.List<MenuItem> items) {
        menu.clear();
        for (MenuItem item : items) {
            menu.put(item.name, item);
        }
    }
}


class RestaurantService {

    java.util.List<Restaurant> restaurants = new ArrayList<>();

    void addRestaurant(String name, java.util.List<MenuItem> menu, int capacity) {
        restaurants.add(new Restaurant(name, menu, capacity));
    }

    java.util.List<Restaurant> getAllRestaurants() {
        return restaurants;
    }
}


class OrderService {

    AtomicInteger orderId = new AtomicInteger(1);
    Map<Integer, Order> orders = new ConcurrentHashMap<>();
    RestaurantService restaurantService;

    OrderService(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    Order placeOrder(java.util.List<String> items, SelectionStrategy strategy) {

        List<Restaurant> restaurants = restaurantService.getAllRestaurants();
        List<OrderItem> orderItems = new ArrayList<>();

        for (String item : items) {

            List<Restaurant> candidates = strategy.getRestaurants(item, restaurants);
            boolean allocated = false;

            for (Restaurant r : candidates) {
                if (r.increaseProcessCount()) {
                    orderItems.add(new OrderItem(item, r));
                    allocated = true;
                    break;
                }
            }

            if (!allocated) {
                for (OrderItem oi : orderItems) {
                    oi.restaurant.decreaseProcessCount();
                }
                System.out.println("Order failed for item: " + item);
                return null;
            }
        }

        int id = orderId.getAndIncrement();
        Order order = new Order(id, orderItems);
        orders.put(id, order);

        Set<String> fulfillingRestaurants = new HashSet<>();
        for (OrderItem oi : orderItems) fulfillingRestaurants.add(oi.restaurant.name);

        System.out.println("Order #" + id + " -> " + fulfillingRestaurants);
        return order;
    }

    void fulfillItems(int orderId, String restaurantName) {

        Order order = orders.get(orderId);
        if (order == null) return;

        boolean found = false;

        for (OrderItem item : order.items) {
            if (item.restaurant.name.equals(restaurantName)
                    && item.status == OrderItemStatus.IN_PROGRESS) {

                item.status = OrderItemStatus.COMPLETED;
                item.restaurant.decreaseProcessCount();

                System.out.println("Fulfilled: Order#" + orderId +
                        " Item: " + item.itemName +
                        " by " + restaurantName);

                found = true;
            }
        }

        if (!found) {
            System.out.println("No pending items for restaurant " + restaurantName);
            return;
        }

        if (order.isCompleted()) {
            System.out.println("Order #" + orderId + " COMPLETED");
        }
    }

    void printStats() {
        for (Restaurant r : restaurantService.getAllRestaurants()) {
            System.out.println(r.name + ": " + r.remainingCapacity());
        }
    }
}


interface SelectionStrategy {
    List<Restaurant> getRestaurants(String item, List<Restaurant> restaurants);
}


class LowestPriceStrategy implements SelectionStrategy {

    public List<Restaurant> getRestaurants(String item, List<Restaurant> restaurants) {
        List<Restaurant> res = new ArrayList<>();
        for (Restaurant r : restaurants) {
            if (r.hasItem(item)) res.add(r);
        }
        res.sort(Comparator.comparingInt(r -> r.getPrice(item)));
        return res;
    }
}