package util;

import model.OrderItem;
import model.Orders;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure();

            // Register your annotated entity classes here
            configuration.addAnnotatedClass(OrderItem.class);
            configuration.addAnnotatedClass(Orders.class);
            configuration.addAnnotatedClass(model.Products.class);
            configuration.addAnnotatedClass(model.Category.class); 
            configuration.addAnnotatedClass(model.User.class);     

            return configuration.buildSessionFactory();
        } catch (Exception ex) {
            System.err.println("SessionFactory creation failed: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
