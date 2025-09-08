module com.basis.javafx {
    // 所需的 JavaFX 模块
    requires javafx.controls;
    requires javafx.fxml;
    requires java.management;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;
    requires java.logging;
    requires org.slf4j;
    requires java.prefs;

    // 导出包给 FXML 加载器
    exports com.basis.fingerbrowser to javafx.graphics;
    exports com.basis.fingerbrowser.controller to javafx.fxml;

    // 打开包允许反射访问
    opens com.basis.fingerbrowser.controller to javafx.fxml;
    opens com.basis.fingerbrowser to javafx.fxml, javafx.graphics;
}
