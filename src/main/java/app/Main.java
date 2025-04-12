package app;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;
import controller.*;
import util.HibernateUtil;

import org.apache.catalina.servlets.DefaultServlet;

public class Main {
    public static void main(String[] args) throws Exception {
        // Test Hibernate initialization
        System.out.println("Initializing Hibernate...");
        HibernateUtil.getSessionFactory(); 
        System.out.println("Hibernate initialized");

        // Set Tomcat port to 8088
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8088); // <-- changed from 8087 to 8088

        // Set webapp directory as docBase
        String projectRoot = new File(".").getCanonicalPath();
        String docBase = new File(projectRoot, "src/main/webapp").getAbsolutePath();
        File docBaseFile = new  File(docBase);
        System.out.println("Project Root: " + projectRoot);
        System.out.println("DocBase: " + docBase);
        System.out.println("DocBase exists: " + docBaseFile.exists());
        System.out.println("DocBase is directory: " + docBaseFile.isDirectory());

        Context context = tomcat.addContext("", docBase);
        context.addWelcomeFile("/admin");

        // Add DefaultServlet for serving static files
        Tomcat.addServlet(context, "default", new DefaultServlet());
        context.addServletMappingDecoded("/*", "default");

        // Set resources root for static content
        context.setResources(new org.apache.catalina.webresources.StandardRoot(context));

        // Add your custom servlets
        tomcat.addServlet("", "AdminServlet", "controller.AdminServlet");
        context.addServletMappingDecoded("/admin/*", "AdminServlet");

        tomcat.addServlet("", "ProductsServlet", "controller.ProductsServlet");
        tomcat.addServlet("", "CartServlet", "controller.CartServlet");
        tomcat.addServlet("", "OrderServlet", "controller.OrderServlet");
        context.addServletMappingDecoded("/products/*", "ProductsServlet");
        context.addServletMappingDecoded("/cart/*", "CartServlet");
        context.addServletMappingDecoded("/orders/*", "OrderServlet");

        // Force connector initialization
        tomcat.getConnector();

        System.out.println("Starting Tomcat...");
        tomcat.start();
        System.out.println("Tomcat started on http://localhost:8088");

        // Keep the server running
        tomcat.getServer().await();
    }
}
