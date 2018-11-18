(ns personal-site-clj.cities-graph
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [ajax.core :refer [GET]]
            [cljs.core.async
             :as async
             :refer [>! <! go chan close!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utility functions
(defn log [x]
  (.log js/console x))

(defn get-cities-data []
  "Returns channel that gives the array of city data map objects."
  (let [out (chan)]
    (let [handler
          (fn [res] (go (log res) (>! out res) (close! out)))]
      (GET "/json/cities_partial.json"
           {:handler handler
            :response-format :json
            :keywords? true}))
    out))

(defn distance-between-points
  "Returns miles between two points on the earth."
  [lat1 lon1 lat2 lon2]
  (let [r 6371e3 ;; earth's radius in meters
        p1 (q/radians lat1)
        p2 (q/radians lat2)
        delta-p (q/radians (- lat2 lat1))
        delta-l (q/radians (- lon2 lon1))]
    (let [a (+
             (q/sq (q/sin (/ delta-p 2)))
             (* (q/cos p1) (q/cos p2)
                (q/sq (q/sin (/ delta-l 2)))))
          c (* 2 (q/atan2 (q/sqrt a) (q/sqrt (- 1 a))))]
      ;; calculate and convert to miles
      (* r c 0.0006214))))

(defn distance-between-cities
  "Returns miles between two city maps."
  [city1 city2]
  (distance-between-points
   (:latitude city1) (:longitude city1)
   (:latitude city2) (:longitude city2)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Graph structure implementation
(defrecord Graph [vertices])
(defrecord Vertex
    ;; Vertex structure:
		;; 	- key : unique string
		;; 	- value : payload
		;; 	- connections : set of keys to connected vertices
		;; 	- state : search state (undiscovered, discovered,
    ;;            or completely-explored)
		;; 	- parent : key of parent vertex
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

(defn connect-cities?
  [city1 city2]
  (< (distance-between-cities city1 city2) 600))

(defn add-city
  "Adds a city to `g` as a vertex, including creating connections
  to neighbors."
  [city {:keys [vertices] :as graph}]
  (println (str "city = " city))
  (let [g (add-vertex (:city city) city graph)
        v (get-vertex (:city city) g)
        vertex-list (into () (seq vertices))]
    (loop [existing-vertices (rest vertex-list)
           [key u] (peek vertex-list)
           new-graph g]
      (if (nil? u)
        new-graph
        (if (connect-cities? city (:value u))
          (recur
           (rest existing-vertices) (peek existing-vertices)
           (add-edge v u new-graph))
          (recur
           (rest existing-vertices) (peek existing-vertices)
           new-graph))))))

(defn build-cities-graph
  "Transforms the vector of cities data into a graph."
  [data]
  (println data)
  (loop [graph (->Graph {})
         data (into () data)]
    (let [city (peek data)]
      (if (nil? city)
        graph
        (recur
         (add-city city graph)
         (rest data))))))

(def initial-data (atom {}))
(defn cities-setup []
  (q/background (q/color 0 30 70 100))
  (q/frame-rate 1)
  (let [g (build-cities-graph @initial-data)]
    (println "graph built:")
    (println g)
    {:graph g :vertex-explorer (traverse g)}))

(defn cities-update [{:keys [graph vertex-explorer] :as state}]
  (go (print-vertex (<! vertex-explorer)))
  state)

(defn cities-draw [state]
  (q/background (q/color 0 30 70 100)))

(def CANVAS-WIDTHS (- (.-innerWidth js/window) 25))

(defn start [data]
  (log "startings...")
  (swap! initial-data (constantly data))
  (q/defsketch cities-graph-sketch
    :host "cities-graph-canvas"
    :size [CANVAS-WIDTHS 750]
    :setup cities-setup
    :update cities-update
    :draw cities-draw
    :middleware [m/fun-mode]))

;; Start the sketch once the JSON is loaded in
(go (start (<! (get-cities-data))))

