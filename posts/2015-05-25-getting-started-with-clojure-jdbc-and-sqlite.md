---
layout: post
title: "Getting started with JDBC and SQLite in Clojure"
description: ""
category: clojure
tags: []
---

Using a relational database is, perhaps, not among the most glamorous things you
can do from a functional programming language. Working with SQL is, however,
very useful for real-life tasks. As it turns out, it is also an area where
Clojure can show some of its strengths. Clojure's emphasis on manipulating
simple data structures (vectors and hash-maps) also means that working with the
results of SQL queries is easy, in many cases obviating the need for a ORM or
other helper library. But as a hosted language, Clojure also benefits from
access to Java's JDBC database drivers, which essentially means that there is
hardly any database out there that you cannot use with great performance.

In this post, I will show how to work with SQLite, an embedded (in-process)
database, for the obvious reason that it does not require a lot of set
up. However, as in Java, switching to a database server such as PostgreSQL or
MySQL should be as simple as swapping out the JDBC connection string.

Clojure's main interface to SQL databases the `java.jdbc` package. To get going,
we're going to need a database connection:

``` clojure
(>pull 'org.clojure/java.jdbc "0.3.7")

;; {[org.clojure/clojure "1.4.0"] nil, [org.clojure/java.jdbc "0.3.7"]
;; #{[org.clojure/clojure "1.4.0"]}}

(require '[clojure.java.jdbc :as sql])
```

Now let's try a simple query:

``` clojure
(sql/query db "select 3*5")

;; SQLException No suitable driver found for jdbc:sqlite:test.db
;; java.sql.DriverManager.getConnection (DriverManager.java:689)
```
