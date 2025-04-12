package controller;

import model.OrderItem;
import model.Orders;
import model.Products;
import model.User;
import util.HibernateUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/cart/*")
public class CartServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    	// ✅ Set UTF-8 encoding to fix ₹ symbol issue
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        if (user == null) {
            resp.sendRedirect("/admin/login");
            return;
        }

        out.println("<html><head><meta charset='UTF-8'><title>Loop Basket - Cart</title>");
        out.println("<link rel='stylesheet' href='/static/css/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'><h1>Your Cart</h1>");

        List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            out.println("<p>Your cart is empty. <a href='/products'>Add some products</a>.</p>");
        } else {
            out.println("<div class='cart-grid'>");
            BigDecimal cartTotal = BigDecimal.ZERO;

            for (OrderItem item : cart) {
                BigDecimal itemTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                cartTotal = cartTotal.add(itemTotal);
                out.println("<div class='cart-item'>");
                out.println("<h3>" + item.getProduct().getName() + "</h3>");
                out.println("<p>Price: ₹" + item.getUnitPrice() + "</p>");
                out.println("<p>Quantity: " + item.getQuantity() + "</p>");
                out.println("<p>Total: ₹" + itemTotal + "</p>");

                out.println("<form method='post' action='/cart/delete'>");
                out.println("<input type='hidden' name='productId' value='" + item.getProduct().getProductId() + "'>");
                out.println("<button type='submit'>Remove</button>");
                out.println("</form>");

                out.println("</div>");
            }

            out.println("</div>");
            out.println("<p><strong>Cart Total: ₹" + cartTotal + "</strong></p>");

            out.println("<form method='post' action='/cart/checkout'>");
            out.println("<button type='submit'>Checkout</button>");
            out.println("</form>");
        }

        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String pathInfo = req.getPathInfo();

        if (user == null) {
            resp.sendRedirect("/admin/login");
            return;
        }

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo)) {
                int productId = Integer.parseInt(req.getParameter("product_Id"));
                dbSession.beginTransaction();
                Products product = dbSession.get(Products.class, productId);
                dbSession.getTransaction().commit();

                if (product == null || product.getStock() <= 0) {
                    resp.sendRedirect("/products");
                    return;
                }

                List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
                if (cart == null) {
                    cart = new ArrayList<>();
                    session.setAttribute("cart", cart);
                }

                OrderItem item = cart.stream()
                        .filter(ci -> ci.getProduct().getProductId().equals(productId))
                        .findFirst()
                        .orElse(null);

                if (item == null) {
                    OrderItem newItem = new OrderItem();
                    newItem.setProduct(product);
                    newItem.setQuantity(1);
                    newItem.setUnitPrice(product.getPrice());
                    cart.add(newItem);
                } else if (item.getQuantity() < product.getStock()) {
                    item.setQuantity(item.getQuantity() + 1);
                }

                resp.sendRedirect("/cart");

            } else if ("/checkout".equals(pathInfo)) {
                List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");

                if (cart == null || cart.isEmpty()) {
                    resp.sendRedirect("/cart");
                    return;
                }

                dbSession.beginTransaction();

                Orders order = new Orders();
                order.setUser(user);
                order.setStatus("Pending");
                order.setOrderItems(new ArrayList<>());

                BigDecimal totalAmount = BigDecimal.ZERO;

                for (OrderItem item : cart) {
                    Products product = dbSession.get(Products.class, item.getProduct().getProductId());

                    if (product == null || product.getStock() < item.getQuantity()) {
                        dbSession.getTransaction().rollback();
                        out.println("<h1>Error</h1><p>Product " + item.getProduct().getName() + " is out of stock.</p>");
                        return;
                    }

                    totalAmount = totalAmount.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                }

                order.setTotalAmount(totalAmount);
                dbSession.persist(order);

                for (OrderItem item : cart) {
                    Products product = dbSession.get(Products.class, item.getProduct().getProductId());

                    OrderItem newItem = new OrderItem();
                    newItem.setProduct(product);
                    newItem.setQuantity(item.getQuantity());
                    newItem.setUnitPrice(item.getUnitPrice());
                    newItem.setOrder(order);

                    dbSession.persist(newItem);
                    order.getOrderItems().add(newItem);

                    product.setStock(product.getStock() - item.getQuantity());
                    dbSession.merge(product);
                }

                dbSession.getTransaction().commit();

                session.removeAttribute("cart");
                resp.sendRedirect("/orders");

            } else if ("/delete".equals(pathInfo)) {
                int productId = Integer.parseInt(req.getParameter("productId"));
                List<OrderItem> cart = (List<OrderItem>) session.getAttribute("cart");
                if (cart != null) {
                    cart.removeIf(item -> item.getProduct().getProductId() == productId);
                }
                resp.sendRedirect("/cart");

            } else {
                out.println("<h1>Error</h1><p>Invalid action.</p>");
            }
        } catch (Exception e) {
            out.println("<h1>Error</h1><p>Cart operation failed: " + e.getMessage() + "</p>");
        }
    }

    private String getNavBar(User user) {
        StringBuilder nav = new StringBuilder("<ul class='nav-list'>");
        nav.append("<li><a href='/admin'>Home</a></li>");
        nav.append("<li><a href='/products'>Products</a></li>");
        nav.append("<li><a href='/cart'>Cart</a></li>");
        nav.append("<li><a href='/orders'>Orders</a></li>");
        nav.append("<li><a href='/products/add'>Add Products</a></li>");
        nav.append("<li><a href='/admin/logout'>Logout (" + user.getUsername() + ")</a></li>");
        nav.append("</ul>");
        return nav.toString();
    }
}
