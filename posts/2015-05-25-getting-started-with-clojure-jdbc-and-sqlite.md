---
title: "Getting started with JDBC and SQLite in Clojure"
---

Using a relational database is, perhaps, not among the most glamorous things you
can do from a functional programming language. SQL is, however, very useful for
all kinds of real-world tasks. As it turns out, it is also an area where Clojure
can show its strengths. Clojure's emphasis on manipulating simple data
structures (lists, vectors and hash-maps) also means that working with the results of
SQL queries is easy, in many cases obviating the need for a ORM or other helper
library. But as a hosted language, Clojure also benefits from access to Java's
JDBC database drivers, which essentially means that there is hardly any database
out there that you cannot use -- all it takes is an additional line of code.

In this post, I will show how to work with [SQLite](https://www.sqlite.org), an
embedded (in-process) database. I chose SQlite because it is easily available
and does not require any set up. However, as is the case with JDBC, switching to
a database server such as PostgreSQL or MySQL should be as simple as swapping
out the connection string.

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
(./pull 'org.clojure/java.jdbc "0.3.7")

;; {[org.clojure/clojure "1.4.0"] nil, [org.clojure/java.jdbc "0.3.7"]
;; #{[org.clojure/clojure "1.4.0"]}}

(require '[clojure.java.jdbc :as sql])
```

We'll also need a database connection:

``` clojure
(def db {:classname   "org.sqlite.JDBC", :subprotocol "sqlite", :subname
"test.db"})
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
(./pull 'org.xerial/sqlite-jdbc "3.7.2")

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
(sql/execute! db ["drop table if exists countries"])
(let [cs (sql/create-table-ddl :countries
                               [:id :integer
                                :primary :key
                                :autoincrement]
                               [:name :text]
                               [:capital :text])]
  (sql/execute! db [cs]))
```

You can see that we wrap the string containing the SQL statement in a
vector. The reason is that `execute!` supports prepared statements, where the
SQL string includes placeholders such as `?`. [^placeholders] On execution, the
placeholders in the query will be filled on the database side with the values
provided. This is generally more efficient than including values in the SQL
string, as the DBMS does not need to parse the values as strings. It is also
more secure, as it rules out the possibility of SQL injection attacks. For these
reasons, parameterized SQL queries should be used whenever possible.

That's why there's a vector in the example. `execute!` expects, as its second
parameter, a sequence of the form `[sql-string param1 param2 ...]`. So you can
perform an `INSERT` in this way:

```clojure
(sql/execute! db ["insert into countries (name, capital) values (?, ?)"
                  "Denmark"
                  "Copenhagen"])
```

However, there's a simple convenience function that takes care of creating the
SQL statement for you:

``` clojure

(sql/insert! db :countries {:name "France",
                            :capital "Paris"})
```

We can also insert multiple rows in one go by passing multiple hash-maps as
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

;; => ({:capital "Copenhagen", :name "Denmark", :id 1}
;;     {:capital "Paris", :name "France", :id 2}
;;     {:capital "Washington, D.C.", :name "USA", :id 3}
;;     {:capital "Buenos Aires", :name "Argentina", :id 4}
;;     {:capital "Lima", :name "Peru", :id 5})
```

`query` accepts a simple string as its second argument, but we can also supply a
parameterized query:


```clojure
(-> (sql/query db ["select id  from countries where name=?" "Argentina"])
    first
    :id)
;; => 4
```

Back in the command line, we can now verify that *SQLite` has written the data
to disk:

``` bash
$ sqlite3 test.db .dump
PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE countries (id integer primary key autoincrement, name text, capital text);
INSERT INTO "countries" VALUES(1,'Denmark','Copenhagen');
INSERT INTO "countries" VALUES(2,'France','Paris');
INSERT INTO "countries" VALUES(3,'USA','Washington, D.C.');
INSERT INTO "countries" VALUES(4,'Argentina','Buenos Aires');
INSERT INTO "countries" VALUES(5,'Peru','Lima');
DELETE FROM sqlite_sequence;
INSERT INTO "sqlite_sequence" VALUES('countries',5);
COMMIT;
```

## Reusing database connection

All functions in `clojure.java.jdbc` expect a database specification as their
first argument. In the examples we have been passing a map containing connection
information to these functions. This is very convenient for experimenting in the
REPL. However, this also means that each database interaction requires
establishing a new connection or, in the case SQLite, closing and re-opening the
`.db3` file. While this is usually fast enough for interactive use, it is
inefficient when used in a production system.

In other words, `java.jdbc` does not try to be smart by guessing that you're
going to need to perform multiple queries on your database connection. That's a
feature, not a bug, as handling expensive resources such as open database
connections should be left as an explicit job of the programmer, making the
scope in which the resource is available obvious.

In Clojure you can use a convenient macro for this:

```clojure
(sql/with-db-connection [db-handle db]
  (doall (sql/query db-handle ["select count(*) as count from countries"]))
  ;; do more things with the connection
  )
```

The macro `with-db-connections` is modelled on the syntax of `let`. With `let`
you can bind variables to a value within a certain scope:

``` clojure
(let [a 123]
  (println a) ;; here "a" is accessible
  )
;; now "a" is no longer accessible
```

Similarly, the function `with-open` keeps a file open and its handle operational
for any operations, but only within a certain scope:

``` clojure
(with-open [rdr (clojure.java.io/reader "/etc/group")]
  (->> rdr line-seq first))
;; => root:x:0:
```

After execution proceeds beyond the scope of the `with-open` macro, the resource
is automatically freed (the file handle closed).

The macro `with-db-connection` has the same signature. We can see verify this
from the REPL:

```
user=> (doc sql/with-db-connection)
-------------------------
clojure.java.jdbc/with-db-connection
([binding & body])
Macro
  Evaluates body in the context of an active connection to the database.
  (with-db-connection [con-db db-spec]
  ... con-db ...)
```

The macro's first argument is expected to be a "binding", which in Clojure-speak
is a pair, i.e. a *vector* with two elements. The second element is the resource
to be made available to the body macro. The first element is the name of the
binding. If you use the variable "db-handle" within the scope of the macro, it
will re-use the current database connection. Once execution moves beyond its
confines, the connection will be cleaned up automatically. [^doall] Finally,
connection re-use can only work if you supply `db-handle` to query functions
instead of `db`, i.e. the binding generated by the macro rather than the
original connection map.

## Conclusion

We have seen that interacting with a relational database requires very little
bureaucracy in Clojure. Its database functions are polymorphic in that they
accept different types of values as *database specifications*:

- a map describing how to connect to the database (in the case of SQLite
  including the file name, otherwise including a host name, user name and
  password)
- a binding generated by the `with-db-connection` macro

But the same abstraction also allows two other types of handles as arguments:

- a binding generated by
  [the `with-db-transaction` macro](https://clojure.github.io/java.jdbc/#clojure.java.jdbc/with-db-transaction),
  which causes the queries performed using the specification to be executed in
  the context of a database transaction (which can be rolled back or committed
  atomically)
- a handle representing a
  [connection pool](http://clojure-doc.org/articles/ecosystem/java_jdbc/connection_pooling.html),
  which allows a long-running multithreaded program to make use of a certain
  number of persistent database connections (not applicable for SQLite)

Database specifications, then, provide a common interface for different patterns of
interacting with relational databases. While reconnecting to the database may be
sufficient when experimenting form the REPL, a production deployment will
typically require a database pool, which fortunately is not difficult to set up
as well.

For more on the using SQL databases in Clojure, check out the official
[java.jdbc tutorial](http://clojure-doc.org/articles/ecosystem/java_jdbc/home.html)
and the generated [API documentation](http://clojure.github.io/java.jdbc/). The
chapter
[in the Clojure Cookbook](https://github.com/clojure-cookbook/clojure-cookbook/blob/master/06_databases/6-03_manipulating-an-SQL-database.asciidoc)
is relevant as well. Finally, it may also be useful to have a look at Sun's
official
[JDBC documentation](http://docs.oracle.com/javase/tutorial/jdbc/basics/index.html),
much of which applies also to Clojure.

[^placeholders]: Unfortunately you'll need to use positional (indexed)
parameters (multiple occurrences of `?`) as JDBC
[does not support](http://stackoverflow.com/a/2309984/239678) named parameters
(`:name` and `:capital`) out of the box, a feature that lesser languages have
provided for a while.

[^doall]: This is also the reason why the example includes an explicit call to
the function `doall`. When you use `with-db-connection` and its cognates, you
may run into a gotcha related to the fact that `query` returns a *lazy
sequence*. The problem is that when the REPL, in printing, retrieves the values
form (realizes) the lazy-sequence, the macro may have already closed the
database connection. As a consequence you'll see a puzzling error stating that
the connection is already closed. The solution, as in other similar cases, is to
force the realization of the sequence using `doall` while the connection is
still open. Note, however, that `query` usually calls `doall` on its result set
(the behavior is described [here](http://clojure-doc.org/articles/ecosystem/java_jdbc/using_sql.html#processing-a-result-set-lazilyly)).
