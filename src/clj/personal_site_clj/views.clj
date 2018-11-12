(ns personal-site-clj.views
  (:require [clojure.string :as str]
            [hiccup.page :as page]
						[ring.util.anti-forgery :as util]
						[markdown.core :refer [md-to-html-string]]))

(defn page-head [title]
	[:head
		[:title title]
		(page/include-css "/css/normalize.css")
		(page/include-css "/css/skeleton.css")
		(page/include-css "/css/hamburgers.css")
		(page/include-css "/css/style.css")])

(defn home-page []
	(page/html5
		(page-head "Nathan Clonts")
		[:h1 "Hello!"]))
