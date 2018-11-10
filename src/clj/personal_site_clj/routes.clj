(ns personal-site-clj.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
						[ring.util.response :refer [response redirect]]
						[environ.core :refer [env]]
						[personal-site-clj.mail :as mail]))

(defn home-routes [endpoint]
  (routes
		(GET "/" req
			(-> "public/index.html"
					io/resource
					io/input-stream
					response
					(assoc :headers { "Content-Type" "text/html; charset=utf-8" })))
		(POST "/contact-form" req
			(mail/contact-submission (req :params))
			(redirect "/#contact"))
		(resources "/")
		; @TODO: add 404 page.
		(ANY "/*" [path]
			(redirect "/"))))
