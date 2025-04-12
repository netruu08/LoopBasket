package controller;

import model.Category;
import model.Products;
import model.User;
import util.HibernateUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.hibernate.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@WebServlet("/products/*")
public class ProductsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();

        out.println("<html><head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Loop Basket - Products</title>");
        out.println("<link rel='stylesheet' href='/static/css/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo) && user != null) {
                out.println("<h1>Add New Product</h1>");
                out.println("<form method='post' action='/products/add'>");
                out.println("<input type='text' name='title' placeholder='Product Name' required>");
                out.println("<input type='number' name='price' placeholder='Price' step='0.01' required>");
                out.println("<input type='number' name='stock' placeholder='Stock' required>");
                out.println("<input type='text' name='categoryName' placeholder='Category Name' required>");
                out.println("<button type='submit'>Add Product</button>");
                out.println("</form>");
            } else {
                out.println("<h1>Our Products</h1>");
                List<Products> products = dbSession.createQuery(
                    "FROM Products p JOIN FETCH p.category", Products.class
                ).list();

                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
                out.println("<div class='product-grid'>");

                for (Products product : products) {
                    out.println("<div class='product-card'>");
                    out.println("<h3>" + product.getName() + "</h3>");
                    out.println("<p>Category: " + product.getCategory().getName() + "</p>");
                    out.println("<p class='price'>" + formatter.format(product.getPrice()) + "</p>");
                    if (product.getStock() == 0) {
                        out.println("<p class='stock-empty'>Stock is empty</p>");
                    } else {
                        out.println("<p>Stock: " + product.getStock() + "</p>");
                        if (user != null) {
                            out.println("<form method='post' action='/cart/add'>");
                            out.println("<input type='hidden' name='product_Id' value='" + product.getProductId() + "'>");
                            out.println("<button type='submit'>Add to Cart</button></form>");
                        }
                    }
                    out.println("</div>");
                }
                out.println("</div>");
            }
        }

        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        String pathInfo = req.getPathInfo();

        if (user == null) {
            resp.sendRedirect("/admin/login");
            return;
        }

        out.println("<html><head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<title>Loop Basket - Products</title>");
        out.println("<link rel='stylesheet' href='/static/css/style.css'></head><body>");
        out.println("<header><nav>" + getNavBar(user) + "</nav></header>");
        out.println("<div class='container'>");

        try (Session dbSession = HibernateUtil.getSessionFactory().openSession()) {
            if ("/add".equals(pathInfo)) {
                String productName = req.getParameter("title");
                BigDecimal price = new BigDecimal(req.getParameter("price"));
                int stock = Integer.parseInt(req.getParameter("stock"));
                String categoryName = req.getParameter("categoryName");

                dbSession.beginTransaction();

                Category category = dbSession.createQuery(
                    "FROM Category WHERE name = :name", Category.class
                ).setParameter("name", categoryName).uniqueResult();

                if (category == null) {
                    category = new Category();
                    category.setName(categoryName);
                    dbSession.persist(category);
                }

                Products product = new Products();
                product.setName(productName);
                product.setPrice(price);
                product.setStock(stock);
                product.setCategory(category);

                dbSession.persist(product);
                dbSession.getTransaction().commit();

                out.println("<h1>Success</h1><p>Product added! <a href='/products'>Back to Products</a>.</p>");
            }
        } catch (Exception e) {
            out.println("<h1>Error</h1><p>Failed to add product: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }

        out.println("</div></body></html>");
    }

    private String getNavBar(User user) {
        StringBuilder nav = new StringBuilder("<ul class='nav-list'>");
        nav.append("<li><a href='/admin'>Home</a></li>");
        nav.append("<li><a href='/products'>Products</a></li>");
        nav.append("<li><a href='/cart'>Cart</a></li>");
        if (user != null) {
            nav.append("<li><a href='/orders'>Orders</a></li>");
            nav.append("<li><a href='/products/add'>Add Product</a></li>");
            nav.append("<li><a href='/admin/logout'>Logout (" + user.getUsername() + ")</a></li>");
        } else {
            nav.append("<li><a href='/admin/login'>Login</a></li>");
            nav.append("<li><a href='/admin/register'>Register</a></li>");
        }
        nav.append("</ul>");
        return nav.toString();
    }
}
