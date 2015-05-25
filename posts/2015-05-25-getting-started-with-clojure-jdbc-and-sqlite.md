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

To approximate a typical Clojure workflow as closely as possible, this
walkthrough is presented as the transcript of a REPL session. To follow along,
open a REPL using `lein repl` and type each line as presented here. I'd
encourage you to actually type out each line to get a feel for how the library
works.

Note that to follow along you don't need a `leiningen` project; a session opened
from a directory without a `project.clj` will be sufficient. However, I am using
the useful `vinyasa.pull/pull` function which allows you to pull in `jar` files
with external dependencies into a running REPL.

Clojure's main interface to SQL databases the `java.jdbc` package. To get started,
we're going to need a database connection:

``` clojure
(>pull 'org.clojure/java.jdbc "0.3.7")

;; {[org.clojure/clojure "1.4.0"] nil, [org.clojure/java.jdbc "0.3.7"]
;; #{[org.clojure/clojure "1.4.0"]}}

(require '[clojure.java.jdbc :as sql])
```

Let's start by executing a simple query:

``` clojure
(sql/query db "select 3*5 as result")

;; SQLException No suitable driver found for jdbc:sqlite:test.db
;; java.sql.DriverManager.getConnection (DriverManager.java:689)
```

Apparently installing the `clojure.java/jdbc` jar does not automatically pull in
the `sqlite` JDBC driver. We need to add this dependency explicitly:

``` clojure
(>pull 'org.xerial/sqlite-jdbc "3.7.2")

(sql/query db "select 3*5 as result")
;; => ({:result 15})
```

Ok, now it works. The call to `query` returns a single row with a single column,
which we can extract easily:

``` clojure
(-> (sql/query db "select 3*5 as result") first :result)
;; => 15
```

Let's create a table:

``` clojure
(sql/db-do-commands db
                    "drop table if exists countries")
(let [cs (sql/create-table-ddl :countries
                               [:id :integer
                                :primary :key
                                :autoincrement]
                               [:name :text]
                               [:capital :text])]
  (sql/db-do-commands db ))
```

and add some data:

``` clojure

(sql/insert! db :countries {:name "France",
                            :capital "Paris"})
```

We can also insert multiple rows in one go by passing many hash-maps as
arguments to the `insert!` function:

``` clojure

(def pairs [["USA" "Washington, D.C."]
            ["Argentina" "Buenos Aires"]
            ["Peru" "Lima"]])

(apply sql/insert! db :countries
       (map (partial zipmap [:name :capital]) pairs))
```

Now we can retrieve the rows we just inserted:

``` clojure
(sql/query db "select id, name, capital from countries")

;; => ({:capital "Paris", :name "France", :id 1}
;;     {:capital "Washington, D.C.", :name "USA", :id 2}
;;     {:capital "Buenos Aires", :name "Argentina", :id 3}
;;     {:capital "Lima", :name "Peru", :id 4})
```
