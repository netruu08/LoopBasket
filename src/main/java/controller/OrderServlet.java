package controller;

import model.OrderItem;
import model.Orders;
import model.User;
import util.HibernateUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.time.ZoneId;

@WebServlet("/orders/*")
public class OrderServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // ✅ Set UTF-8 encoding to fix ₹ symbol issue
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect("/admin/login");
            return;
        }

        out.println("<html><head><meta charset='UTF-8'><title>Loop Basket - Orders</title>");
        out.println("<link rel='stylesheet' href='/static/css/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'><h1>Your Orders</h1>");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:a");
        NumberFormat inrFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN")); // ✅ Proper ₹ formatting

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            List<Orders> orders = dbSession.createQuery(
                "SELECT DISTINCT o FROM Orders o JOIN FETCH o.orderItems WHERE o.user.userId = :userId",
                Orders.class
            ).setParameter("userId", user.getUserId()).list();

            if (orders.isEmpty()) {
                out.println("<p>You have no orders yet. <a href='/products'>Shop now</a>.</p>");
            } else {
                out.println("<div class='order-grid'>");
                for (Orders order : orders) {
                    out.println("<div class='order-card'>");
                    out.println("<h3>Order #" + order.getOrderId() + "</h3>");

                    Date orderDate = Date.from(order.getOrderDate().atZone(ZoneId.systemDefault()).toInstant());
                    out.println("<p>Date: " + sdf.format(orderDate) + "</p>");

                    for (OrderItem item : order.getOrderItems()) {
                        out.println("<p>" + item.getProduct().getName() +
                                " - " + item.getQuantity() + " x " + inrFormat.format(item.getUnitPrice()) + "</p>");
                    }

                    out.println("<p><strong>Total: " + inrFormat.format(order.getTotalAmount()) + "</strong></p>");
                    out.println("<p>Status: " + order.getStatus() + "</p>");

                    if (!"Cancelled".equals(order.getStatus()) && !"Completed".equals(order.getStatus())) {
                        out.println("<form method='post' action='/orders/cancel' onsubmit='return confirm(\"Cancel this order?\");'>");
                        out.println("<input type='hidden' name='orderId' value='" + order.getOrderId() + "'/>");
                        out.println("<button type='submit'>Cancel Order</button>");
                        out.println("</form>");
                    }

                    out.println("</div>");
                }
                out.println("</div>");
            }
        } catch (Exception e) {
            out.println("<h1>Error</h1><p>Failed to load orders: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }

        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect("/admin/login");
            return;
        }

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            dbSession.beginTransaction();

            if ("/cancel".equals(path)) {
                int orderId = Integer.parseInt(req.getParameter("orderId"));
                Orders order = dbSession.get(Orders.class, orderId);
                if (order != null && order.getUser().getUserId().equals(user.getUserId())) {
                    order.setStatus("Cancelled");
                    dbSession.merge(order);
                }
            }

            dbSession.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
        }

        resp.sendRedirect("/orders");
    }

    private String getNavBar(User user) {
        StringBuilder nav = new StringBuilder("<ul class='nav-list'>");
        nav.append("<li><a href='/admin'>Home</a></li>");
        nav.append("<li><a href='/products'>Products</a></li>");
        nav.append("<li><a href='/cart'>Cart</a></li>");
        nav.append("<li><a href='/orders'>Orders</a></li>");
        nav.append("<li><a href='/products/add'>Add Product</a></li>");
        nav.append("<li><a href='/admin/logout'>Logout (" + user.getUsername() + ")</a></li>");
        nav.append("</ul>");
        return nav.toString();
    }
}
