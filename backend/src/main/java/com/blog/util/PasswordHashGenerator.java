package com.blog.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt hashed admin keys for production deployment.
 * 
 * Usage:
 *   java -cp ".:target/classes" com.blog.util.PasswordHashGenerator "your-secret-key"
 * 
 * Output:
 *   $2a$10$... (hashed key)
 * 
 * Then set ADMIN_KEY environment variable to the hashed value.
 */
public class PasswordHashGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: PasswordHashGenerator <plain-text-key>");
            System.out.println("");
            System.out.println("Example: PasswordHashGenerator 'my-super-secret-admin-key'");
            System.out.println("");
            System.out.println("Output will be a BCrypt hash. Set this as ADMIN_KEY environment variable.");
            System.exit(1);
        }

        String plainKey = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedKey = encoder.encode(plainKey);

        System.out.println("Plain Key:  " + plainKey);
        System.out.println("Hashed Key: " + hashedKey);
        System.out.println("");
        System.out.println("For production, set environment variable:");
        System.out.println("  ADMIN_KEY=" + hashedKey);
    }
}
