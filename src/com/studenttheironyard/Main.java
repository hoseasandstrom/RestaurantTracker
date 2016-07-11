package com.studenttheironyard;

import org.eclipse.jetty.http.HttpGenerator;
import org.h2.tools.Server;
import spark.ModelAndView;
import spark.Session;
import spark.Spark;
import spark.template.mustache.MustacheTemplateEngine;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main {

    static void insertRestaurant(Connection conn, String name, String location, int rating, String comment, int userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO restaurants VALUES( NULL, ?, ?, ?, ?, ? )");
        stmt.setString(1, name);
        stmt.setString(2, location);
        stmt.setInt(3, rating);
        stmt.setString(4, comment);
        stmt.setInt(5, userId);
        stmt.execute();

    }
    static void updateRestaurant(Connection conn, int id, String name, String location, int rating, String comment) throws SQLException {
        Restaurant restList = new Restaurant(id, name, location, rating, comment);

        PreparedStatement stmt = conn.prepareStatement("UPDATE restaurants SET name=?, location=?, rating=?, comment=? WHERE id=?");
        stmt.setString(1, name);
        stmt.setString(2, location);
        stmt.setInt(3, rating);
        stmt.setString(4, comment);
        stmt.setString(5, String.valueOf(id));
        stmt.execute();

    }



    static void deleteRestaurant(Connection conn, int id) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("DELETE FROM restaurants WHERE id = ?");
        stmt.setInt(1, id);
        stmt.execute();
    }

    static ArrayList<Restaurant> selectRests(Connection conn, int userId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM restaurants INNER JOIN users ON restaurants.user_id = users.id");
        ArrayList<Restaurant> rests = new ArrayList<>();
        ResultSet results = stmt.executeQuery();
        while (results.next()) {
            int id = results.getInt("id");
            String name = results.getString("name");
            String location = results.getString("location");
            int rating = results.getInt("rating");
            String comment = results.getString("comment");
            Restaurant r = new Restaurant(id, name, location, rating, comment);
            rests.add(r);
        }
        return rests;
    }
    static void insertUser(Connection conn, String name, String password) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO users VALUES(NULL, ?, ?)");
        stmt.setString(1, name);
        stmt.setString(2, password);
        stmt.execute();
    }
    static User selectUser(Connection conn, String name) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
        stmt.setString(1, name);
        ResultSet results = stmt.executeQuery();
        if (results.next()) {
            int id = results.getInt("id");
            String password = results.getString("password");
            return new User(id, name, password);
        }
        return null;
    }

    public static void main(String[] args) throws SQLException {
        Server.createWebServer().start();
        Connection conn = DriverManager.getConnection("jdbc:h2:./main");
        Statement stmt = conn.createStatement();
        stmt.execute("CREATE TABLE IF NOT EXISTS restaurants(id IDENTITY, name VARCHAR, location VARCHAR, rating INT, comment VARCHAR, user_id INT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS users(id IDENTITY, name VARCHAR, password VARCHAR)");

        Spark.init();
        Spark.get(
                "/",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    HashMap m = new HashMap();
                    if (username == null) {
                        return new ModelAndView(m, "login.html");
                    } else {
                        User user = selectUser(conn, username);
                        m.put("restaraunts", selectRests(conn, user.id));
                        return new ModelAndView(m, "home.html");
                    }
                },
                new MustacheTemplateEngine()
        );
        Spark.post(
                "/login",
                (request, response) -> {
                    String name = request.queryParams("username");
                    String password = request.queryParams("password");
                    if (name == null || password == null) {
                        throw new Exception("Name or pass not sent");
                    }

                    User user = selectUser(conn, name);
                    if (user == null) {
                        insertUser(conn, name, password);

                    } else if (!password.equals(user.name)) {
                        throw new Exception("Wrong password");
                    }

                    Session session = request.session();
                    session.attribute("username", name);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "/create-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        throw new Exception("Not logged in");
                    }
                    String name = request.params("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");

                    User user = selectUser(conn, username);
                    if (user == null) {
                        throw new Exception("User does not exist");
                    }
                    insertRestaurant(conn, name, location, rating, comment, user.id);


                    response.redirect("/");
                    return "";

                }
        );
        Spark.post(
                "/logout",
                (request, response) -> {
                    Session session = request.session();
                    session.invalidate();
                    response.redirect("/");
                    return "/";
                }
        );
        Spark.post(
                "/delete-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        throw new Exception("Not logged in");
                    }

                    int id = Integer.valueOf(request.queryParams("id"));

                    deleteRestaurant(conn, id);

                    response.redirect("/");
                    return "";
                }
        );
        Spark.post(
                "edit-restaurant",
                (request, response) -> {
                    Session session = request.session();
                    String username = session.attribute("username");
                    if (username == null) {
                        throw new Exception("Not logged in");
                    }
                    int id = Integer.valueOf(request.queryParams("id"));
                    String name = request.queryParams("name");
                    String location = request.queryParams("location");
                    int rating = Integer.valueOf(request.queryParams("rating"));
                    String comment = request.queryParams("comment");

                    updateRestaurant(conn, id, name, location, rating, comment);
                    response.redirect("/");
                    return "";
                }
        );
    }

}
