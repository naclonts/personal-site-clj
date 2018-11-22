(ns personal-site-clj.views
  (:require [clojure.string :as str]
            [hiccup.page :as page]
						[ring.util.anti-forgery :as util]
						[markdown.core :refer [md-to-html-string]]
            [clojure.string :refer [capitalize]]))

(defn page-head [title]
	[:head
	 [:title title]
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (page/include-css "/css/normalize.css")
   (page/include-css "/css/skeleton.css")
   (page/include-css "/css/hamburgers.css")
   (page/include-css "/css/style.css")])

(defn menu []
  ;; Menu!
  [:nav {:id "menu" :class "menu hidden"}
   [:a {:href "/" :class "menu-link"} [:span "work"]]
   [:a {:href "/#projects" :class "menu-link"} [:span "projects"]]
   [:a {:href "/#contact" :class "menu-link"} [:span "contact"]]])

(defn menu-button []
  [:button {:class "hamburger hamburger--vortex" :type "button"
            :id "hamburger"}
   [:span {:class "hamburger-box"}
    [:span {:class "hamburger-inner"}]]])

(defn content []
  [:div.section
   [:div.canvas-wrapper
    [:canvas#cities-graph-canvas {:width "100%"}]]
   ;; Intro stuff
   [:div.above-fold
    [:div {:class "eight columns offset-by-two"}
     [:h3 {:id "typing-intro"} "&nbsp;"
      [:noscript "Build Your Bridges"]]
     [:p "I'm a full stack web developer in Fort Collins, Colorado."]]]])

(defn avl-section []
  [:div.section
   [:div.canvas-wrapper
    [:canvas {:width "100%" :id "avl-canvas"}]]])

(defn contact-form []
  (let [gen-field (fn [name id type el-type]
                   [:div
                    [:label {:for name} (capitalize name)]
                    [el-type {:type type
                              :id id
                              :name name
                              :class "twelve columns"
                              :required true}]])]
    [:div#contact.six.columns.offset-by-two
     [:h5 "Let’s Talk"]
     [:form#contact-form {:action "/contact-form" :method "post"}
      (gen-field "name" "name" "text" :input)
      (gen-field "email" "email" "email" :input)
      (gen-field "message" "message" "text" :textarea)
      [:div [:p {:id "contact-response-message"}]]
      [:button {:type "submit"} "Send"]]]))

(defn scripts []
  [])

(defn home-page []
	(page/html5
   [:div#app.body-wrapper {:style "min-height: 2000px;"}
		(page-head "Nathan Clonts")
		(menu)
    (menu-button)
    (content)
    (avl-section)
    (contact-form)]
   [:script {:src "js/compiled/personal_site_clj.js"
             :type "text/javascript"}]
   [:script {:type "text/javascript"}
    "personal_site_clj.system.go();"]))

