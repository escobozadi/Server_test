module com.example.server_test {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.server_test to javafx.fxml;
    exports com.example.server_test;
}