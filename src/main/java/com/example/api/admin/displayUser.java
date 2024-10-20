package com.example.api.admin;

import javax.servlet.annotation.WebServlet;

import com.google.gson.Gson;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.example.Model.User.User;
import com.example.Utils.DBConnection;
@WebServlet("/api/admin/user")
public class displayUser extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Thiết lập kiểu dữ liệu trả về là JSON
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3001");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement("SELECT user_id,name, username, phone FROM users");
                ResultSet rs = stmt.executeQuery()) {

            // Khởi tạo danh sách các đối tượng User
            List<User> userList = new ArrayList<>();

            // Lấy kết quả từ ResultSet và thêm vào danh sách
            while (rs.next()) {
                userList.add(new User(rs.getInt("user_id"),

                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("phone")));
            }

            // Chuyển đổi danh sách user thành JSON
            String userJson = new Gson().toJson(userList);

            // Trả JSON về cho client
            response.getWriter().write(userJson);

        } catch (SQLException e) {
            // In lỗi ra console và trả lỗi HTTP 500 về cho client
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error: " + e.getMessage());
        }
    }
}
