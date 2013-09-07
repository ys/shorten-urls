(ns web.core
  (:gen-class)
  (:require [taoensso.carmine :as car :refer (wcar)]))

(import java.security.SecureRandom)
(use '[compojure.route :only [files not-found]]
     '[compojure.handler :only [site]] ; form, query params decode; cookie; session, etc
     '[compojure.core :only [defroutes GET POST DELETE ANY context]]
     'clostache.parser
     'org.httpkit.server)

(def server1-conn {:pool {} :spec {}})
(defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(defn get-url [key] (wcar* (car/hget "short.urls" key)))

(defn add-url [url]
  (let [key (subs (.toString (BigInteger. 130 (SecureRandom.)) 32) 0 7)]
    (wcar* (car/hset "short.urls" key url))
    key
  )
)

(defn delete-url [key] (wcar* (car/hdel "short.urls" key)))

(defn home [req] (render-resource "templates/home.mustache" {}))

(defn redirect-to-full-url [req]
  (let [key (-> req :params :key)]
    {:status 301
     :headers {"Location" (get-url key)}}
  )
)

(defn remove-key [req]
  (let [key (-> req :params :key)]
    {:status 200
     :headers { "Content-Type" "application/json" }
     :body (delete-url key)}
  )
)

(defn create-new-link [req]
  (let [url (-> req :params :url) key (-> req :params :key)]
     (render-resource "templates/link.mustache" {:key (add-url url) :url url })
  )
)

(defroutes all-routes
  (GET "/" [] home)
  (GET "/:key" [] redirect-to-full-url)
  (DELETE "/:key" [] remove-key)
  (POST "/" [] create-new-link))

(run-server (site #'all-routes) {:port 8089})
