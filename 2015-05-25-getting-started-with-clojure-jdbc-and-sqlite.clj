(./pull 'org.clojure/java.jdbc "0.3.7")

;; {[org.clojure/clojure "1.4.0"] nil, [org.clojure/java.jdbc "0.3.7"]
;; #{[org.clojure/clojure "1.4.0"]}}

(require '[clojure.java.jdbc :as sql])

(def db {:classname   "org.sqlite.JDBC", :subprotocol "sqlite", :subname
"test.db"})

(sql/query db "select 3*5 as result")

;; SQLException No suitable driver found for jdbc:sqlite:test.db
;; java.sql.DriverManager.getConnection (DriverManager.java:689)

(./pull 'org.xerial/sqlite-jdbc "3.7.2")

(sql/query db "select 3*5 as result")
;; => ({:result 15})

(-> (sql/query db "select 3*5 as result") first :result)
;; => 15

(sql/execute! db ["drop table if exists countries"])
(let [cs (sql/create-table-ddl :countries
                               [:id :integer
                                :primary :key
                                :autoincrement]
                               [:name :text]
                               [:capital :text])]
  (sql/execute! db [cs]))

(sql/execute! db ["insert into countries (name, capital) values (?, ?)"
                  "Denmark"
                  "Copenhagen"])


(sql/insert! db :countries {:name "France",
                            :capital "Paris"})


(def pairs [["USA" "Washington, D.C."]
            ["Argentina" "Buenos Aires"]
            ["Peru" "Lima"]])

(apply sql/insert! db :countries
       (map (partial zipmap [:name :capital]) pairs))

(sql/query db "select id, name, capital from countries")

;; => ({:capital "Copenhagen", :name "Denmark", :id 1}
;;     {:capital "Paris", :name "France", :id 2}
;;     {:capital "Washington, D.C.", :name "USA", :id 3}
;;     {:capital "Buenos Aires", :name "Argentina", :id 4}
;;     {:capital "Lima", :name "Peru", :id 5})

(-> (sql/query db ["select id  from countries where name=?" "Argentina"])
    first
    :id)
;; => 4

(sql/with-db-connection [db-handle db]
  (doall (sql/query db-handle ["select count(*) as count from countries"]))
  ;; do more things with the connection
  )

(let [a 123]
  (println a) ;; here "a" is accessible
  )
;; now "a" is no longer accessible

(with-open [rdr (clojure.java.io/reader "/etc/group")]
  (->> rdr line-seq first))
;; => root:x:0:
