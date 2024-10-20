package com.example.api.user;

import com.example.Utils.DBConnection;
import com.example.api.user.UpdateUserServlet.UserResponse;
import com.google.gson.Gson;


import java.io.BufferedReader;
import java.sql.ResultSet; // Add this import
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/getUser/*")
public class UpdateUserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCORSHeaders(response);

        String userIdParam = request.getPathInfo();
        if (userIdParam == null || userIdParam.length() < 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "User ID is required");
            return;
        }

        int userId = Integer.parseInt(userIdParam.substring(1)); // loại bỏ dấu "/"

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT name, address, email, role, username, img FROM users WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Trả về dữ liệu người dùng dưới dạng JSON
                UserResponse userResponse = new UserResponse();
                userResponse.setName(rs.getString("name"));
                userResponse.setAddress(rs.getString("address"));
                userResponse.setEmail(rs.getString("email"));
                userResponse.setRole(rs.getString("role"));
                userResponse.setUsername(rs.getString("username"));
                userResponse.setImg(rs.getString("img"));

                response.setContentType("application/json");
                response.getWriter().write(new Gson().toJson(userResponse));
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    private void setCORSHeaders(HttpServletResponse response) {
         response.setContentType("application/json");
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3001");
        response.setHeader("Access-Control-Allow-Methods", "GET");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setCharacterEncoding("UTF-8");
    }

    class UserResponse {
        private String name;
        private String address;
        private String email;
        private String role;
        private String username;
        private String img;

        // Getters và Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getImg() {
            return img;
        }

        public void setImg(String img) {
            this.img = img;
        }
    }
}