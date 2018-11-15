(ns personal-site-clj.cities-graph
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [ajax.core :refer [GET]]))

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
            (->Vertex key value [] :unexplored nil))))

(defn get-vertex [key {:keys [vertices] :as graph}]
  (get vertices key))

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

(defn print-graph [{:keys [vertices] :as g}]
  (println "Graph:")
  (doseq [v (map val vertices)]
    (println (str (:key v) ": " (:connections v)))))


(defn cities-setup []
  (q/background (q/color 0 30 70 100))
  (as-> (->Graph {}) g
    (add-vertex "hi" 42 (add-vertex "bye" 55 g))
    (add-edge (get-vertex "hi" g) (get-vertex "bye" g) g)
    (print-graph g)
    {:graph g}))

(defn cities-draw [state]
  (q/background (q/color 0 30 70 100)))

(def CANVAS-WIDTHS (- (.-innerWidth js/window) 25))

(q/defsketch cities-graph-sketch
  :host "cities-graph-canvas"
  :size [CANVAS-WIDTHS 750]
  :setup cities-setup
  :draw cities-draw
  :middleware [m/fun-mode])

