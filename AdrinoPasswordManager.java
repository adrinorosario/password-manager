package com.adrino.passmanager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import com.adrino.passmanager.DatabaseHandler.User;
import com.adrino.passmanager.DatabaseHandler.VaultItem;
import com.adrino.passmanager.DatabaseHandler.Role;

import java.util.Optional;

public class AdrinoPasswordManager extends Application {

    private DatabaseHandler.User currentUser = null;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("Adrino Password Manager");

        new Thread(() -> {
            DatabaseHandler.initDB();
            Platform.runLater(this::showLoginScene);
        }).start();

        StackPane root = new StackPane(new Label("Connecting to Secure Database..."));
        root.setStyle("-fx-background-color: #1a1b26; -fx-text-fill: #7aa2f7; -fx-font-size: 18px;");
        Scene scene = new Scene(root, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    private void showLoginScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root");

        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setMaxWidth(350);
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Adrino Secure Vault");
        title.getStyleClass().add("header-label");

        Label subtitle = new Label("Sign in to access your passwords");
        subtitle.setStyle("-fx-text-fill: #565f89; -fx-font-size: 12px;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Master Password");

        Button loginBtn = new Button("Unlock Vault");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink registerLink = new Hyperlink("Create new account");
        registerLink.setStyle("-fx-text-fill: #7aa2f7; -fx-border-color: transparent;");
        registerLink.setOnAction(e -> showRegisterDialog());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f7768e; -fx-font-size: 12px;");

        loginBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText(); 
                                                
            System.out.println("DEBUG: Attempting login for user: '" + u + "'");

            if (u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Please enter credentials.");
                return;
            }

            User user = DatabaseHandler.login(u, p);

            if (user != null) {
                System.out.println("DEBUG: Login successful. Role: " + user.role);
                currentUser = user;
                if (currentUser.role == Role.ADMIN) {
                    System.out.println("DEBUG: Showing Admin Dashboard");
                    showAdminDashboard();
                } else {
                    System.out.println("DEBUG: Showing User Dashboard");
                    try {
                        showUserDashboard();
                        System.out.println("DEBUG: User Dashboard Loaded");
                    } catch (Exception ex) {
                        System.err.println("DEBUG: ERROR Loading User Dashboard!");
                        ex.printStackTrace();
                        errorLabel.setText("System Error: Check logs");
                    }
                }
            } else {
                System.out.println("DEBUG: Login failed. Invalid credentials.");
                errorLabel.setText("Invalid username or password.");
            }
        });

        card.getChildren().addAll(title, subtitle, new Separator(), new Label("Username"), usernameField,
                new Label("Password"), passwordField, errorLabel, loginBtn, registerLink);
        root.getChildren().add(card);

        applyScene(root, 900, 600);
    }

    private void showRegisterDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        dialog.setHeaderText("Register new Adrino Vault");

        ButtonType regBtnType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(regBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #24283b;");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(password, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == regBtnType) {
                if (username.getText().isEmpty() || password.getText().isEmpty())
                    return false;
                return DatabaseHandler.registerUser(username.getText(), password.getText());
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Registration successful! Please login.");
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Registration failed. Username might be taken.");
                alert.show();
            }
        }
    }

    private void showUserDashboard() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        HBox topBar = createTopBar("My Vault");
        root.setTop(topBar);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        Label greeting = new Label("Hello, " + currentUser.username + "!");
        greeting.getStyleClass().add("header-label");

        ObservableList<VaultItem> items = FXCollections
                .observableArrayList(DatabaseHandler.getVaultItems(currentUser.id));
        FilteredList<VaultItem> filteredData = new FilteredList<>(items, p -> true);

        HBox stats = new HBox(15);
        Label countLbl = new Label(String.valueOf(items.size()));
        countLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #7aa2f7;");
        stats.getChildren().add(createStatCard("Total Passwords", countLbl));

        VBox netStatBox = new VBox(5);
        netStatBox.getStyleClass().add("card");
        netStatBox.setMinWidth(250);

        Label localIpLbl = new Label("Local: -");
        localIpLbl.setStyle("-fx-text-fill: #a9b1d6; -fx-font-size: 11px;");
        Label publicIpLbl = new Label("Public: -");
        publicIpLbl.setStyle("-fx-text-fill: #7aa2f7; -fx-font-weight: bold;");

        Button refreshNetBtn = new Button("Refresh Info");
        refreshNetBtn.setStyle(
                "-fx-font-size: 9px; -fx-padding: 2 5; -fx-background-color: #24283b; -fx-text-fill: #7aa2f7;");
        refreshNetBtn.setOnAction(e -> {
            refreshNetBtn.setDisable(true);
            refreshNetBtn.setText("...");
            localIpLbl.setText("Local: Detecting...");
            publicIpLbl.setText("Public: Fetching...");

            NetworkHelper.getLocalInfo(info -> {
                localIpLbl.setText("Local: " + info);
                NetworkHelper.getPublicIP(ip -> {
                    publicIpLbl.setText("Public IP: " + ip);
                    refreshNetBtn.setDisable(false);
                    refreshNetBtn.setText("Refresh Info");
                });
            });
        });

        refreshNetBtn.fire();

        HBox netHeader = new HBox(10, new Label("Network Stats"), refreshNetBtn);
        netHeader.setAlignment(Pos.CENTER_LEFT);
        netHeader.getChildren().get(0).setStyle("-fx-text-fill: #a9b1d6; -fx-font-weight: bold;");

        netStatBox.getChildren().addAll(netHeader, publicIpLbl, localIpLbl);

        stats.getChildren().add(netStatBox);

        VBox healthBox = new VBox(5);
        healthBox.getStyleClass().add("card");
        healthBox.setMinWidth(200);

        long weakCount = items.stream().filter(i -> SecurityUtil.calculateStrength(i.getPassword()) < 3).count();
        Label healthLbl = new Label(weakCount == 0 ? "All Good!" : weakCount + " Weak Passwords");
        healthLbl.setStyle(weakCount == 0 ? "-fx-text-fill: #9ece6a; -fx-font-weight: bold;"
                : "-fx-text-fill: #f7768e; -fx-font-weight: bold;");
        Label healthTitle = new Label("Vault Health");
        healthTitle.setStyle("-fx-text-fill: #a9b1d6;");

        healthBox.getChildren().addAll(healthLbl, healthTitle);
        stats.getChildren().add(healthBox);

        TextField searchField = new TextField();
        searchField.setPromptText("Search accounts...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (item.getSite().toLowerCase().contains(lowerCaseFilter))
                    return true;
                if (item.getUsername().toLowerCase().contains(lowerCaseFilter))
                    return true;
                return false;
            });
        });

        TableView<VaultItem> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<VaultItem, String> siteCol = new TableColumn<>("Website / App");
        siteCol.setCellValueFactory(new PropertyValueFactory<>("site"));

        TableColumn<VaultItem, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<VaultItem, String> passCol = new TableColumn<>("Password");
        passCol.setCellValueFactory(cell -> new SimpleStringProperty("••••••••"));

        TableColumn<VaultItem, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button copyBtn = new Button("Copy");
            private final Button revealBtn = new Button("Show");
            private final Button deleteBtn = new Button("X");
            private final Button checkBtn = new Button("Check");
            private final Button breachBtn = new Button("Pwned?");

            {
                
                breachBtn.getStyleClass().add("button-secondary");
                breachBtn.setStyle(
                        "-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: #f7768e; -fx-text-fill: #1a1b26;");
                breachBtn.setOnAction(e -> {
                    VaultItem entry = getTableView().getItems().get(getIndex());
                    breachBtn.setDisable(true);
                    breachBtn.setText("...");

                    String sha1 = SecurityUtil.sha1(entry.getPassword());
                    String prefix = sha1.substring(0, 5);
                    String suffix = sha1.substring(5);

                    NetworkHelper.checkPwdBreach(prefix, suffixes -> {
                        breachBtn.setDisable(false);
                        breachBtn.setText("Pwned?");

                        boolean found = false;
                        int count = 0;
                        for (String line : suffixes) {
                            String[] parts = line.split(":");
                            if (parts[0].equals(suffix)) {
                                found = true;
                                count = Integer.parseInt(parts[1]);
                                break;
                            }
                        }

                        if (found) {
                            Alert a = new Alert(Alert.AlertType.WARNING);
                            a.setTitle("Breach Alert");
                            a.setHeaderText("Password Compromised!");
                            a.setContentText("This password has been seen " + count
                                    + " times in data breaches.\nCHANGE IT IMMEDIATELY!");
                            a.show();
                        } else {
                            Alert a = new Alert(Alert.AlertType.INFORMATION);
                            a.setTitle("Safe");
                            a.setHeaderText("Clean Record");
                            a.setContentText("This password has not been found in any known public breaches.");
                            a.show();
                        }
                    });
                });
                checkBtn.getStyleClass().add("button-secondary");
                checkBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8; -fx-background-color: #414868;");
                checkBtn.setOnAction(e -> {
                    VaultItem entry = getTableView().getItems().get(getIndex());
                    checkBtn.setText("...");
                    checkBtn.setDisable(true);

                    NetworkHelper.checkSiteStatus(entry.getSite(),
                            status -> {
                                checkBtn.setText(status.length() > 10 ? "Working..." : status);
                                
                            },
                            result -> {
                                checkBtn.setText("Check");
                                checkBtn.setDisable(false);
                                Alert a = new Alert(Alert.AlertType.INFORMATION);
                                a.setTitle("Site Inspector");
                                a.setHeaderText("Connection Status for " + entry.getSite());
                                a.setContentText(result);
                                a.show();
                            },
                            error -> {
                                checkBtn.setText("Check");
                                checkBtn.setDisable(false);
                                Alert a = new Alert(Alert.AlertType.ERROR);
                                a.setTitle("Site Inspector");
                                a.setHeaderText("Connection Failed");
                                a.setContentText(error);
                                a.show();
                            });
                });
                copyBtn.getStyleClass().add("button-secondary");
                copyBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
                copyBtn.setOnAction(e -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(getTableView().getItems().get(getIndex()).getPassword());
                    Clipboard.getSystemClipboard().setContent(content);
                });

                revealBtn.getStyleClass().add("button-secondary");
                revealBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
                revealBtn.setOnAction(e -> {
                    VaultItem entry = getTableView().getItems().get(getIndex());
                    Alert info = new Alert(Alert.AlertType.INFORMATION, "Password: " + entry.getPassword());
                    info.setHeaderText(entry.getSite());
                    info.show();
                });

                deleteBtn.getStyleClass().add("button-danger");
                deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
                deleteBtn.setOnAction(e -> {
                    VaultItem entry = getTableView().getItems().get(getIndex());
                    DatabaseHandler.deleteVaultItem(entry.id);
                    items.remove(entry);
                    countLbl.setText(String.valueOf(items.size()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5, checkBtn, breachBtn, revealBtn, copyBtn, deleteBtn);
                    box.setAlignment(Pos.CENTER);
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(siteCol, userCol, passCol, actionCol);
        table.setItems(filteredData);

        Button addBtn = new Button("+ Add New Password");
        addBtn.setOnAction(e -> showAddPasswordDialog(items, countLbl));

        content.getChildren().addAll(greeting, stats, new Separator(),
                new HBox(10, new Label("Search:"), searchField),
                new HBox(10, new Label("Your Saved Passwords"), addBtn),
                table);
        root.setCenter(content);

        applyScene(root, 1000, 700);
    }

    private void showAdminDashboard() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(createTopBar("Admin Console"));

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        Label title = new Label("System Overview");
        title.getStyleClass().add("header-label");

        ObservableList<User> userList = FXCollections.observableArrayList(DatabaseHandler.getAllUsers());

        HBox stats = new HBox(15);
        Label userCountLbl = new Label(String.valueOf(userList.size()));
        userCountLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #bb9af7;");
        stats.getChildren().add(createStatCard("Total Users", userCountLbl));

        TableView<User> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<User, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<User, Void> actionCol = new TableColumn<>("Actions");
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.getStyleClass().add("button-danger");
                deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.role == Role.ADMIN) {
                        Alert a = new Alert(Alert.AlertType.WARNING, "Cannot delete Admin!");
                        a.show();
                        return;
                    }
                    DatabaseHandler.deleteUser(u.id);
                    userList.remove(u);
                    userCountLbl.setText(String.valueOf(userList.size()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        table.getColumns().addAll(nameCol, roleCol, actionCol);
        table.setItems(userList);

        content.getChildren().addAll(title, stats, new Separator(), new Label("User Management"), table);
        root.setCenter(content);

        applyScene(root, 1000, 700);
    }

    private void applyScene(javafx.scene.Parent root, int w, int h) {
        Scene scene = new Scene(root, w, h);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
    }

    private HBox createTopBar(String titleStr) {
        HBox topBar = new HBox(20);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle(
                "-fx-background-color: #16161e; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.3), 0, 0, 0, 4);");

        Label logo = new Label("Adrino");
        logo.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #7aa2f7;");

        Label title = new Label(titleStr);
        title.setStyle("-fx-text-fill: #565f89; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button-secondary");
        logoutBtn.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });

        topBar.getChildren().addAll(logo, new Separator(javafx.geometry.Orientation.VERTICAL), title, spacer,
                logoutBtn);
        return topBar;
    }

    private VBox createStatCard(String label, Label valueLbl) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setMinWidth(200);
        Label nameLbl = new Label(label);
        nameLbl.setStyle("-fx-text-fill: #a9b1d6;");
        card.getChildren().addAll(valueLbl, nameLbl);
        return card;
    }

    private void showAddPasswordDialog(ObservableList<VaultItem> items, Label countLbl) {
        Dialog<VaultItem> dialog = new Dialog<>();
        dialog.setTitle("Add Password");
        dialog.setHeaderText("Add to Vault");

        ButtonType saveBtnType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #24283b;");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField site = new TextField();
        site.setPromptText("Site");
        TextField username = new TextField();
        username.setPromptText("Username");

        grid.add(new Label("Site/App:"), 0, 0);
        grid.add(site, 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(username, 1, 1);
        TextField password = new TextField();
        password.setPromptText("Password");

        ProgressBar strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        strengthBar.setStyle("-fx-accent: red;");
        Label strengthLbl = new Label("Strength: -");
        strengthLbl.setStyle("-fx-text-fill: #565f89; -fx-font-size: 10px;");

        password.textProperty().addListener((obs, old, newVal) -> {
            int score = SecurityUtil.calculateStrength(newVal);
            strengthBar.setProgress(score / 4.0);
            strengthLbl.setText("Strength: " + SecurityUtil.getStrengthLabel(score));

            if (score < 2)
                strengthBar.setStyle("-fx-accent: #f7768e;"); 
            else if (score < 3)
                strengthBar.setStyle("-fx-accent: #e0af68;"); 
            else
                strengthBar.setStyle("-fx-accent: #9ece6a;"); 
        });

        grid.add(new Label("Password:"), 0, 2);
        grid.add(password, 1, 2);
        grid.add(strengthLbl, 1, 3);
        grid.add(strengthBar, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                DatabaseHandler.addVaultItem(currentUser.id, site.getText(), username.getText(), password.getText());
                
                return new VaultItem(0, currentUser.id, site.getText(), username.getText(), password.getText());
            }
            return null;
        });

        Optional<VaultItem> result = dialog.showAndWait();
        result.ifPresent(item -> {
            
            items.setAll(DatabaseHandler.getVaultItems(currentUser.id));
            countLbl.setText(String.valueOf(items.size()));
        });
    }
}