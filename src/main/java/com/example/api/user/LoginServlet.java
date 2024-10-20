package com.example.api.user;

import com.example.Utils.DBConnection;
import com.example.api.user.LoginServlet.CartItem;
import com.example.api.user.LoginServlet.OrderItem;
import com.example.api.user.LoginServlet.UserWithOrders;
import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCORSHeaders(response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        setCORSHeaders(response);

        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String jsonInput = sb.toString();

        Gson gson = new Gson();
        LoginRequest loginRequest = gson.fromJson(jsonInput, LoginRequest.class);
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT user_id, name, email, img, role, username,address,phone FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String email = rs.getString("email");
                String nameUser = rs.getString("name");
                String img = rs.getString("img");
                String role = rs.getString("role");
                String usernameFromDb = rs.getString("username");
                String address = rs.getString("address");
                String phone = rs.getString("phone");

                List<Order> orderHistory = getOrderHistory(conn, userId);
                List<CartItem> cartItems = getCartItems(conn, userId);

                response.setStatus(HttpServletResponse.SC_OK);
                UserWithOrders user = new UserWithOrders(userId, nameUser, email, role, img, usernameFromDb, address,
                        orderHistory, cartItems, phone);
                String userJson = gson.toJson(user);
                response.setContentType("application/json");
                response.getWriter().write(userJson);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid credentials\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    private List<CartItem> getCartItems(Connection conn, int userId) throws SQLException {
        String query = "SELECT " +
                "ci.product_id, " +
                "p.name AS product_name, " +
                "ci.quantity, " +
                "p.price, " +
                "cat.category_name, " +
                "p.image " +
                "FROM carts c " +
                "LEFT JOIN cart_items ci ON c.cart_id = ci.cart_id " +
                "LEFT JOIN products p ON ci.product_id = p.product_id " +
                "LEFT JOIN categories cat ON p.category_id = cat.category_id " +
                "WHERE c.user_id = ? " +
                "AND ci.product_id IS NOT NULL";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        List<CartItem> cartItemList = new ArrayList<>();
        while (rs.next()) {
            cartItemList.add(new CartItem(
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),
                    rs.getString("category_name"),
                    rs.getString("image"))); // Make sure this matches your table field exactly
        }
        return cartItemList;
    }

    private List<Order> getOrderHistory(Connection conn, int userId) throws SQLException {
        String query = "SELECT o.order_id, o.date_order, o.total_price, o.status, o.shipping_address, "
                + "oi.quantity, oi.price, p.product_id, p.image, p.name AS product_name, categories.category_name "
                + "FROM orders o "
                + "JOIN order_items oi ON o.order_id = oi.order_id "
                + "JOIN products p ON oi.product_id = p.product_id "
                + "JOIN categories ON p.category_id = categories.category_id "
                + "WHERE o.user_id = ? AND o.status = 'pending'";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();

        List<Order> orderList = new ArrayList<>();
        Order currentOrder = null;
        List<OrderItem> orderItems = new ArrayList<>();

        while (rs.next()) {
            int orderId = rs.getInt("order_id");

            if (currentOrder == null || currentOrder.orderId != orderId) {
                if (currentOrder != null) {
                    currentOrder.items = new ArrayList<>(orderItems);
                    orderList.add(currentOrder);
                }
                currentOrder = new Order(
                        orderId,
                        rs.getString("date_order"),
                        rs.getDouble("total_price"),
                        rs.getString("status"),
                        rs.getString("shipping_address"),
                        new ArrayList<OrderItem>());
                orderItems.clear();
            }

            orderItems.add(new OrderItem(
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price"),

                    rs.getString("category_name"),
                    rs.getString("image")));
        }

        if (currentOrder != null) {
            currentOrder.items = new ArrayList<>(orderItems);
            orderList.add(currentOrder);
        }

        return orderList;
    }

    private void setCORSHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:3001");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setCharacterEncoding("UTF-8");
    }

    class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    class UserWithOrders {
        private int userId;
        private String nameUser;
        private String email;
        private String role;
        private String img;
        private String username;
        private List<Order> orders;
        private List<CartItem> cartItems;
        private String address;
        private String phone;

        public UserWithOrders(int userId, String nameUser, String email, String role, String img, String username,
                String address,
                List<Order> orders, List<CartItem> cartItems, String phone) {
            this.userId = userId;
            this.nameUser = nameUser;
            this.email = email;
            this.role = role;
            this.img = img;
            this.username = username;
            this.address = address;
            this.orders = orders;
            this.cartItems = cartItems;
            this.phone = phone;
        }
    }

    class Order {

        private int orderId;
        private String dateOrder;
        private double totalPrice;
        private String status;
        private String shippingAddress;
        private List<OrderItem> items;

        public Order(int orderId, String dateOrder, double totalPrice, String status, String shippingAddress,
                List<OrderItem> items) {
            this.orderId = orderId;
            this.dateOrder = dateOrder;
            this.totalPrice = totalPrice;
            this.status = status;
            this.shippingAddress = shippingAddress;

            this.items = items;
        }
    }

    class OrderItem {
        private int productId;
        private String productName;
        private int quantity;
        private double price;
        private String categoryName;
        private String image;

        public OrderItem(int productId, String productName, int quantity, double price, String categoryName,
                String image) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.categoryName = categoryName;
            this.image = image;
        }
    }

    class CartItem {
        private int productId;
        private String productName;
        private int quantity;
        private double price;
        private String categoryName;
        private String image;

        public CartItem(int productId, String productName, int quantity, double price, String categoryName,
                String image) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.categoryName = categoryName;
            this.image = image;
        }
    }
}