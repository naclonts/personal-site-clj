(ns personal-site-clj.config
  (:require [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [hiccup.middleware :refer [wrap-base-url]]
            [environ.core :refer [env]]))

(def base-url (env :base-url "/home-page"))

(defn config []
	{:http-port  (Integer. (or (env :port) 10555))
	 :auto-reload true
   :middleware [[wrap-defaults api-defaults]
                wrap-with-logger
                wrap-gzip
                [wrap-base-url base-url]]})

