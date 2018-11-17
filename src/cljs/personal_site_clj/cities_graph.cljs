(ns personal-site-clj.cities-graph
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [ajax.core :refer [GET]]
            [cljs.core.async
             :as async
             :refer [>! <! go chan close!]]))

(defn log [x]
  (.log js/console x))

(defn get-cities-data []
  "Returns array of city data map objects."
  (let [out (chan)]
    (let [handler
          (fn [res] (go (log res) (>! out res) (close! out)))]
      (GET "/json/cities_partial.json"
           {:handler handler
            :response-format :json
            :keywords? true}))
    out))

; Graph structure implementation
(defrecord Graph [vertices])
(defrecord Vertex
    ;; "Vertex structure:
		;; 	- key : unique string
		;; 	- value : payload
		;; 	- connections : vector of keys to connected vertices
		;; 	- state : search state keyword
		;; 	- parent : key of parent vertex"
    [key value connections state parent])

(defn add-vertex
  "Add a new vertex with the given key/value to a graph."
  [key value {:keys [vertices] :as graph}]
  (->Graph (assoc
            vertices
            key
            (->Vertex key value #{} :undiscovered nil))))

(defn get-vertex [key {:keys [vertices] :as graph}]
  (get vertices key))

(defn connected? [f t]
  (contains? (:connections f) (:key t)))

(defn add-connection
  "Returns vertex f, now connected to vertex t."
  [f t]
  (assoc f :connections (conj (:connections f) (:key t))))

(defn add-edge
  "Create an undirected edge between vertices f and t."
  [f t {:keys [vertices] :as graph}]
  (->Graph
   (-> vertices
       (assoc (:key t) (add-connection t f))
       (assoc (:key f) (add-connection f t)))))

(defn print-vertex [v]
  (println (str
              (:key v)
              "\t\t- conn: " (:connections v)
              "\t\t- state: " (:state v)
              "\t\t- parent: " (:parent v))))

(defn print-graph [{:keys [vertices] :as g}]
  (println "Graph:")
  (doseq [v (map val vertices)]
    (print-vertex v)))

(defn traverse [{:keys [vertices] :as g}]
  (let [out (chan)]
    (doseq [v (map val vertices)]
      (go (>! out v)))
    out))


(defn cities-setup []
  (q/background (q/color 0 30 70 100))
  (q/frame-rate 1)
  (go
    (log "about to fetch cities...")
    (println (<! (get-cities-data))))
  (as-> (->Graph {}) g
    (add-vertex "hi" 42 (add-vertex "bye" 55 g))
    (add-vertex "sup" 11 g)
    (add-edge (get-vertex "hi" g) (get-vertex "bye" g) g)
    (add-edge (get-vertex "sup" g) (get-vertex "hi" g) g)
    {:graph g :vertex-explorer (traverse g)}))

(defn cities-update [{:keys [graph vertex-explorer] :as state}]
  (go (print-vertex (<! vertex-explorer)))
  state)

(defn cities-draw [state]
  (q/background (q/color 0 30 70 100)))

(def CANVAS-WIDTHS (- (.-innerWidth js/window) 25))

(q/defsketch cities-graph-sketch
  :host "cities-graph-canvas"
  :size [CANVAS-WIDTHS 750]
  :setup cities-setup
  :update cities-update
  :draw cities-draw
  :middleware [m/fun-mode])

