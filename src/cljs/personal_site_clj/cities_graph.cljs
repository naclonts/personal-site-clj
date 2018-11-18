(ns personal-site-clj.cities-graph
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [ajax.core :refer [GET]]
            [cljs.core.async
             :as async
             :refer [>! <! go chan close!]]
            ;; Graph data type implementation
            [personal-site-clj.graph :as G]))

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
;; Application logic

(defn connect-cities?
  [city1 city2]
  (< (distance-between-cities city1 city2) 600))

(defn add-city
  "Adds a city to `g` as a vertex, including creating connections
  to neighbors."
  [city {:keys [vertices] :as graph}]
  (println (str "city = " city))
  (let [g (G/add-vertex (:city city) city graph)
        v (G/get-vertex (:city city) g)
        vertex-list (into () (seq vertices))]
    (loop [existing-vertices (rest vertex-list)
           [key u] (peek vertex-list)
           new-graph g]
      (if (nil? u)
        new-graph
        (if (connect-cities? city (:value u))
          (recur
           (rest existing-vertices) (peek existing-vertices)
           (G/add-edge v u new-graph))
          (recur
           (rest existing-vertices) (peek existing-vertices)
           new-graph))))))

(defn build-cities-graph
  "Transforms the vector of cities data into a graph."
  [data]
  (println data)
  (loop [graph (G/->Graph {})
         data (into () data)]
    (let [city (peek data)]
      (if (nil? city)
        graph
        (recur
         (add-city city graph)
         (rest data))))))

(def initial-data (atom {}))
(defn cities-setup []
  (q/background (q/color 10 80 70 0))
  (q/frame-rate 1)
  (let [g (build-cities-graph @initial-data)]
    {:graph g :vertex-explorer (G/bfs (G/get-vertex "Seattle" g) g)}))

(defn translate
  [value start-min start-max end-min end-max]
  (+ (* (/ (- value start-min) (- start-max start-min))
        (- end-max end-min))
     end-min))


(def MIN-LON -125)
(def MAX-LON -68)
(def MIN-LAT 50)
(def MAX-LAT 25)
(defn point-to-coords
  [lat lon]
  [(translate lon MIN-LON MAX-LON 0 (q/width))
   (translate lat MIN-LAT MAX-LAT 0 (q/height))])

(defn draw-city!
  [city]
  (let [[x y] (point-to-coords (:latitude city) (:longitude city))]
    (q/stroke 200)
    (q/fill (q/color 0 0 0 1))
    (q/ellipse x y 20 20)))

(def next-draw-city (atom {}))

(defn cities-update [{:keys [graph vertex-explorer] :as state}]
  (go (let [v (<! vertex-explorer)]
        (swap! next-draw-city (fn [] (get v :value)))))
  state)

(defn cities-draw [state]
  (if (not (nil? @next-draw-city))
    (draw-city! @next-draw-city)))

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

