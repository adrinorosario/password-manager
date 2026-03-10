package com.adrino.mlstudio.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

abstract class Product {
    protected int id;
    protected String name;
    protected double price;

    public Product(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public abstract String getCategory();

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return id + " | " + name + " | ₹" + price + " | " + getCategory();
    }
}

class PhysicalProduct extends Product {
    private int stock;

    public PhysicalProduct(int id, String name, double price, int stock) {
        super(id, name, price);
        this.stock = stock;
    }

    public int getStock() {
        return stock;
    }

    @Override
    public String getCategory() {
        return "Physical";
    }
}

class DigitalProduct extends Product {
    private double sizeMB;

    public DigitalProduct(int id, String name, double price, double sizeMB) {
        super(id, name, price);
        this.sizeMB = sizeMB;
    }

    @Override
    public String getCategory() {
        return "Digital";
    }
}

class GenericStore<T> {
    private List<T> items = new ArrayList<>();

    public void add(T item) {
        items.add(item);
    }

    public void remove(T item) {
        items.remove(item);
    }

    public void display() {
        items.forEach(System.out::println);
    }

    public List<T> getItems() {
        return items;
    }
}

@FunctionalInterface
interface Criteria<T> {
    boolean test(T t);
}

class ProductProcessor<T extends Product> {
    public void validate(T product) {
        System.out.println(product.getPrice() > 0 ? "Valid Product" : "Invalid Product");
    }

    public void compare(T p1, T p2) {
        System.out.println(p1.getPrice() > p2.getPrice() ? p1.name + " is costlier" : p2.name + " is costlier");
    }
}

class GenericUtil {
    public static <T> void displayCollection(Collection<T> items) {
        items.forEach(System.out::println);
    }
}

@FunctionalInterface
interface Arithmetic {
    double operate(double a, double b);
}

@FunctionalInterface
interface StringOp {
    String apply(String s);
}

@FunctionalInterface
interface Check {
    boolean test(int n);
}

public class MLStudioGenericsDemo extends Application {

    private final List<Product> productList = new ArrayList<>();
    private int currentIndex = 0;

    TextField idField = new TextField();
    TextField nameField = new TextField();
    TextField priceField = new TextField();
    ImageView imageView = new ImageView();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        
        initializeData();

        Button first = new Button("First");
        Button next = new Button("Next");
        Button prev = new Button("Previous");
        Button last = new Button("Last");
        Button update = new Button("Update Price");

        first.setOnAction(e -> moveFirst());
        next.setOnAction(e -> moveNext());
        prev.setOnAction(e -> movePrev());
        last.setOnAction(e -> moveLast());
        update.setOnAction(e -> updatePrice());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 20px; -fx-alignment: center;");

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Price:"), 0, 2);
        grid.add(priceField, 1, 2);

        VBox imageBox = new VBox(imageView);
        imageBox.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");
        imageBox.setMinSize(150, 150);
        imageView.setFitWidth(150);
        imageView.setPreserveRatio(true);
        grid.add(new Label("Image:"), 0, 3);
        grid.add(imageBox, 1, 3);

        HBox controls = new HBox(10, first, prev, next, last);
        controls.setStyle("-fx-alignment: center;");

        VBox root = new VBox(20, grid, controls, update);
        root.setStyle("-fx-padding: 20px; -fx-alignment: center; -fx-font-family: 'Arial';");
        update.setMaxWidth(Double.MAX_VALUE);

        Scene scene = new Scene(root, 500, 600);
        stage.setScene(scene);
        stage.setTitle("Adrino Product Manager");
        stage.show();

        showRecord();
    }

    private void initializeData() {
        productList.add(new PhysicalProduct(1, "Gaming Laptop", 120000, 10));
        productList.add(new DigitalProduct(2, "Java Mastery E-Book", 499, 5.5));
        productList.add(new PhysicalProduct(3, "Wireless Mouse", 1500, 50));
        productList.add(new DigitalProduct(4, "Adrino Premium Subscription", 999, 0));
    }

    void showRecord() {
        if (productList.isEmpty())
            return;
        Product p = productList.get(currentIndex);
        idField.setText(String.valueOf(p.id));
        nameField.setText(p.name);
        priceField.setText(String.valueOf(p.getPrice()));

        imageView.setImage(null);
    }

    void moveFirst() {
        currentIndex = 0;
        showRecord();
    }

    void moveNext() {
        if (currentIndex < productList.size() - 1) {
            currentIndex++;
            showRecord();
        }
    }

    void movePrev() {
        if (currentIndex > 0) {
            currentIndex--;
            showRecord();
        }
    }

    void moveLast() {
        currentIndex = productList.size() - 1;
        showRecord();
    }

    void updatePrice() {
        try {
            double newPrice = Double.parseDouble(priceField.getText());
            Product p = productList.get(currentIndex);
            p.price = newPrice; 

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Price updated!");
            alert.show();
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid price format.");
            alert.show();
        }
    }
}