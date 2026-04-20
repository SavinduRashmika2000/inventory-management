import java.sql.*;
import java.util.Properties;
import java.io.FileInputStream;

public class CheckBackendAudit {
    public static void main(String[] args) {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("src/main/resources/application.properties")) {
            props.load(fis);
            String url = props.getProperty("spring.datasource.url");
            url = url.replace("${DB_URL:", "").split("}")[0];
            String user = props.getProperty("spring.datasource.username").replace("${DB_USERNAME:", "").split("}")[0];
            String pass = ""; // Assuming empty for root
            
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                System.out.println("Checking BACKEND Audit Log...");
                ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM sync_audit_log ORDER BY id DESC LIMIT 5");
                while (rs.next()) {
                    System.out.printf("Table: %s, Records: %d, Status: %s, TS: %s\n", 
                        rs.getString("table_name"), rs.getInt("record_count"), rs.getString("status"), rs.getString("timestamp"));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
